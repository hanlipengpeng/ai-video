package com.ai.comic.creation.service;

import com.ai.comic.aigc.client.AgnesImageClient;
import com.ai.comic.aigc.client.AgnesTextClient;
import com.ai.comic.aigc.config.AgnesProperties;
import com.ai.comic.common.BusinessException;
import com.ai.comic.common.ProjectStatus;
import com.ai.comic.common.StorageService;
import com.ai.comic.common.TaskConstants;
import com.ai.comic.creation.PromptBuilder;
import com.ai.comic.creation.entity.Character;
import com.ai.comic.creation.entity.Storyboard;
import com.ai.comic.project.entity.Project;
import com.ai.comic.project.service.ProjectService;
import com.ai.comic.task.entity.TaskLog;
import com.ai.comic.task.service.TaskService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 创作编排服务。
 * <p>
 * 编排剧本生成、分镜生成、角色抽取、角色参考图生成，调用 AgnesTextClient / AgnesImageClient。
 * 所有方法异步执行，进度通过 task_log 暴露。
 * <p>
 * 注意：异步方法通过 {@code self} 引用调用，避免 Spring AOP 自调用导致 @Async 失效。
 */
@Slf4j
@Service
public class CreationService {

    @Autowired
    private AgnesTextClient agnesTextClient;

    @Autowired
    private AgnesImageClient agnesImageClient;

    @Autowired
    private AgnesProperties agnesProperties;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private StoryboardService storyboardService;

    @Autowired
    private CharacterService characterService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private PromptBuilder promptBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StorageService storageService;

    /** 自引用，确保 @Async 方法经由 Spring 代理调用 */
    @Autowired
    @Lazy
    private CreationService self;

    // ============================================================
    // 剧本生成（文档 3.2.3）
    // ============================================================

    /**
     * 创建剧本生成任务（同步返回 taskId，异步执行）。
     */
    public TaskLog submitScriptTask(Long userId, Long projectId) {
        Project p = projectService.getOwned(projectId, userId);
        if (p.getSourceText() == null || p.getSourceText().isBlank()) {
            throw new BusinessException(400, "请先填写小说原文");
        }
        TaskLog task = taskService.createTask(userId, projectId, TaskConstants.BIZ_SCRIPT,
                projectId, agnesProperties.getModels().getText(),
                "{\"sourceTextLength\":" + p.getSourceText().length() + "}");
        // 切换项目状态
        projectService.transitStatus(projectId, ProjectStatus.SCRIPTING);
        // 异步执行（通过 self 引用确保 @Async 生效）
        self.doGenerateScriptAsync(task.getId(), projectId, p.getSourceText());
        return task;
    }

    @Async("aiTaskExecutor")
    public void doGenerateScriptAsync(Long taskId, Long projectId, String sourceText) {
        try {
            taskService.markRunning(taskId, null, null);
            String systemPrompt = "你是一位专业的动漫剧本编剧。请将用户提供的小说原文改写为动漫短剧剧本，"
                    + "输出严格的 JSON 格式，结构为：{\"title\":\"章节标题\",\"scenes\":[{\"heading\":\"场景标题\","
                    + "\"description\":\"场景描述\",\"dialogues\":[{\"character\":\"角色名\",\"line\":\"台词\"}],"
                    + "\"narration\":\"旁白\"}]}。只输出 JSON，不要任何解释。";
            String result = agnesTextClient.chatForJson(systemPrompt, sourceText);
            // 校验 JSON 合法性
            objectMapper.readTree(result);
            // 保存剧本
            projectService.saveScript(projectId, result);
            taskService.markSuccess(taskId, "{\"length\":" + result.length() + "}");
            log.info("剧本生成成功: projectId={}", projectId);
        } catch (Exception e) {
            log.error("剧本生成失败: projectId={}", projectId, e);
            taskService.markFailed(taskId, e.getMessage());
        }
    }

    /**
     * 保存剧本 JSON（用户编辑后）。
     */
    public void saveScript(Long userId, Long projectId, String scriptJson) {
        Project p = projectService.getOwned(projectId, userId);
        try {
            objectMapper.readTree(scriptJson);
        } catch (Exception e) {
            throw new BusinessException(400, "剧本不是合法 JSON");
        }
        projectService.saveScript(projectId, scriptJson);
    }

    // ============================================================
    // 分镜生成（文档 3.2.4）
    // ============================================================

    public TaskLog submitStoryboardTask(Long userId, Long projectId) {
        Project p = projectService.getOwned(projectId, userId);
        if (p.getScriptContent() == null || p.getScriptContent().isBlank()) {
            throw new BusinessException(400, "请先生成剧本");
        }
        TaskLog task = taskService.createTask(userId, projectId, TaskConstants.BIZ_STORYBOARD,
                projectId, agnesProperties.getModels().getText(),
                "{}");
        projectService.transitStatus(projectId, ProjectStatus.STORYBOARDING);
        self.doGenerateStoryboardAsync(task.getId(), projectId, p.getScriptContent(), p.getStylePreset());
        return task;
    }

    @Async("aiTaskExecutor")
    public void doGenerateStoryboardAsync(Long taskId, Long projectId, String scriptJson, String stylePreset) {
        try {
            taskService.markRunning(taskId, null, null);
            String systemPrompt = "你是一位专业的动漫分镜师。根据用户提供的剧本 JSON，拆解为分镜列表。"
                    + "每个分镜包含：seq（序号，从1开始）、prompt（英文画面描述，用于图像生成，需详细描述场景、人物、光影、构图），"
                    + "dialogue（台词，可空）、narration（旁白，可空）、durationSec（建议时长，3-6秒）、characterIds（关联角色ID数组，可空）。"
                    + "输出严格 JSON：{\"storyboards\":[...]}。只输出 JSON，不要解释。";
            String result = agnesTextClient.chatForJson(systemPrompt, scriptJson);
            JsonNode root = objectMapper.readTree(result);
            JsonNode arr = root.path("storyboards");
            if (!arr.isArray() || arr.isEmpty()) {
                throw new BusinessException(500, "分镜解析失败：缺少 storyboards 数组");
            }
            List<Storyboard> list = new ArrayList<>();
            for (JsonNode node : arr) {
                Storyboard sb = new Storyboard();
                sb.setSeq(node.path("seq").asInt(list.size() + 1));
                sb.setPrompt(node.path("prompt").asText(""));
                sb.setDialogue(node.path("dialogue").asText(null));
                sb.setNarration(node.path("narration").asText(null));
                if (node.has("durationSec")) {
                    sb.setDurationSec(BigDecimal.valueOf(node.path("durationSec").asDouble(4.0)));
                } else {
                    sb.setDurationSec(BigDecimal.valueOf(4.0));
                }
                // characterIds 转为逗号分隔字符串
                JsonNode charIds = node.path("characterIds");
                if (charIds.isArray() && !charIds.isEmpty()) {
                    StringBuilder sbIds = new StringBuilder();
                    for (JsonNode cid : charIds) {
                        if (sbIds.length() > 0) sbIds.append(",");
                        sbIds.append(cid.asText());
                    }
                    sb.setCharacterIds(sbIds.toString());
                }
                list.add(sb);
            }
            storyboardService.replaceProjectStoryboards(projectId, list);
            taskService.markSuccess(taskId, "{\"count\":" + list.size() + "}");
            log.info("分镜生成成功: projectId={}, count={}", projectId, list.size());
        } catch (Exception e) {
            log.error("分镜生成失败: projectId={}", projectId, e);
            taskService.markFailed(taskId, e.getMessage());
        }
    }

    // ============================================================
    // 角色抽取（文档 3.2.5）
    // ============================================================

    public TaskLog submitExtractCharactersTask(Long userId, Long projectId) {
        Project p = projectService.getOwned(projectId, userId);
        if (p.getScriptContent() == null || p.getScriptContent().isBlank()) {
            throw new BusinessException(400, "请先生成剧本");
        }
        TaskLog task = taskService.createTask(userId, projectId, TaskConstants.BIZ_CHARACTER_IMG,
                projectId, agnesProperties.getModels().getText(), "{}");
        self.doExtractCharactersAsync(task.getId(), projectId, p.getScriptContent());
        return task;
    }

    @Async("aiTaskExecutor")
    public void doExtractCharactersAsync(Long taskId, Long projectId, String scriptJson) {
        try {
            taskService.markRunning(taskId, null, null);
            String systemPrompt = "你是一位动漫角色设计师。从用户提供的剧本中抽取所有出现的角色，"
                    + "为每个角色生成详细的外貌描述（用英文，便于图像生成），包括：性别、年龄、发型、瞳色、服饰、特征。"
                    + "输出严格 JSON：{\"characters\":[{\"name\":\"角色名\",\"aliases\":\"别名\",\"appearance\":\"英文外貌描述\","
                    + "\"personality\":\"性格关键词\"}]}。只输出 JSON，不要解释。";
            String result = agnesTextClient.chatForJson(systemPrompt, scriptJson);
            JsonNode root = objectMapper.readTree(result);
            JsonNode arr = root.path("characters");
            if (!arr.isArray()) {
                throw new BusinessException(500, "角色抽取失败：缺少 characters 数组");
            }
            // 删除旧角色后插入新角色
            List<Character> old = characterService.listByProject(projectId);
            for (Character c : old) {
                characterService.removeById(c.getId());
            }
            for (JsonNode node : arr) {
                Character c = new Character();
                c.setProjectId(projectId);
                c.setName(node.path("name").asText("未命名"));
                c.setAliases(node.path("aliases").asText(null));
                c.setAppearance(node.path("appearance").asText(""));
                c.setPersonality(node.path("personality").asText(null));
                c.setStatus("PENDING");
                characterService.save(c);
            }
            taskService.markSuccess(taskId, "{\"count\":" + arr.size() + "}");
            log.info("角色抽取成功: projectId={}, count={}", projectId, arr.size());
        } catch (Exception e) {
            log.error("角色抽取失败: projectId={}", projectId, e);
            taskService.markFailed(taskId, e.getMessage());
        }
    }

    // ============================================================
    // 角色参考图生成（文档 14.3）
    // ============================================================

    public TaskLog submitCharacterImageTask(Long userId, Long characterId) {
        Character c = characterService.getByIdOwned(characterId);
        Project p = projectService.getOwned(c.getProjectId(), userId);
        TaskLog task = taskService.createTask(userId, p.getId(), TaskConstants.BIZ_CHARACTER_IMG,
                characterId, agnesProperties.getModels().getImage(),
                "{\"characterId\":" + characterId + "}");
        self.doGenerateCharacterImageAsync(task.getId(), characterId, p.getStylePreset());
        return task;
    }

    @Async("aiTaskExecutor")
    public void doGenerateCharacterImageAsync(Long taskId, Long characterId, String stylePreset) {
        try {
            taskService.markRunning(taskId, null, null);
            Character c = characterService.getByIdOwned(characterId);
            String prompt = promptBuilder.buildCharacterReferencePrompt(c, stylePreset);
            // 角色参考图：size=1K，ratio=1:1（文档 14.3）
            AgnesImageClient.ImageResult result = agnesImageClient.textToImage(prompt, "1K", "1:1");
            if (result.getUrl() == null) {
                throw new BusinessException(500, "图像模型未返回 URL");
            }
            // 下载到本地存储
            String localUrl = storageService.downloadToLocalStorage(result.getUrl(), "character", "png");
            characterService.saveReferenceImage(characterId, localUrl);
            taskService.markSuccess(taskId, "{\"imageUrl\":\"" + localUrl + "\"}");
            log.info("角色参考图生成成功: characterId={}", characterId);
        } catch (Exception e) {
            log.error("角色参考图生成失败: characterId={}", characterId, e);
            taskService.markFailed(taskId, e.getMessage());
        }
    }
}
