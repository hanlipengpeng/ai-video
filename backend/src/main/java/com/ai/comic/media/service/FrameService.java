package com.ai.comic.media.service;

import com.ai.comic.aigc.client.AgnesImageClient;
import com.ai.comic.aigc.config.AgnesProperties;
import com.ai.comic.common.BusinessException;
import com.ai.comic.common.ProjectStatus;
import com.ai.comic.common.StorageService;
import com.ai.comic.common.TaskConstants;
import com.ai.comic.creation.PromptBuilder;
import com.ai.comic.creation.entity.Character;
import com.ai.comic.creation.entity.Storyboard;
import com.ai.comic.creation.service.CharacterService;
import com.ai.comic.creation.service.StoryboardService;
import com.ai.comic.media.entity.FrameImage;
import com.ai.comic.media.mapper.FrameImageMapper;
import com.ai.comic.project.entity.Project;
import com.ai.comic.project.service.ProjectService;
import com.ai.comic.task.entity.TaskLog;
import com.ai.comic.task.service.TaskService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 分镜画面服务（文档 3.2.6、14.4）。
 * <p>
 * - 批量生成分镜画面（图生图，引用角色参考图）<br>
 * - 单张重新生成<br>
 * - 查询项目下全部画面
 * <p>
 * 注意：异步方法通过 {@code self} 引用调用，避免 Spring AOP 自调用导致 @Async 失效。
 */
@Slf4j
@Service
public class FrameService extends ServiceImpl<FrameImageMapper, FrameImage> {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private StoryboardService storyboardService;

    @Autowired
    private CharacterService characterService;

    @Autowired
    private AgnesImageClient agnesImageClient;

    @Autowired
    private AgnesProperties agnesProperties;

    @Autowired
    private PromptBuilder promptBuilder;

    @Autowired
    private TaskService taskService;

    @Autowired
    private StorageService storageService;

    /** 自引用，确保 @Async 方法经由 Spring 代理调用 */
    @Autowired
    @Lazy
    private FrameService self;

    /**
     * 批量生成分镜画面（每分镜一个异步任务）。
     *
     * @return 总任务 ID（聚合进度需前端按分镜数计算）
     */
    public TaskLog submitBatchFramesTask(Long userId, Long projectId) {
        Project p = projectService.getOwned(projectId, userId);
        List<Storyboard> storyboards = storyboardService.listByProject(projectId);
        if (storyboards.isEmpty()) {
            throw new BusinessException(400, "请先生成分镜");
        }
        // 创建一个聚合任务
        TaskLog task = taskService.createTask(userId, projectId, TaskConstants.BIZ_FRAME,
                projectId, agnesProperties.getModels().getImage(),
                "{\"count\":" + storyboards.size() + "}");
        projectService.transitStatus(projectId, ProjectStatus.IMAGE_GENERATING);

        // 为每个分镜异步生成画面
        List<Character> allCharacters = characterService.listByProject(projectId);
        for (Storyboard sb : storyboards) {
            self.doGenerateFrameAsync(task.getId(), projectId, sb, p.getStylePreset(), allCharacters, userId);
        }
        return task;
    }

    @Async("aiTaskExecutor")
    public void doGenerateFrameAsync(Long parentTaskId, Long projectId, Storyboard sb,
                                     String stylePreset, List<Character> allCharacters, Long userId) {
        // 为每个分镜创建独立的子任务（便于单独重试）
        TaskLog subTask = taskService.createTask(userId, projectId, TaskConstants.BIZ_FRAME,
                sb.getId(), agnesProperties.getModels().getImage(),
                "{\"storyboardId\":" + sb.getId() + ",\"parentTaskId\":" + parentTaskId + "}");
        try {
            taskService.markRunning(subTask.getId(), null, null);

            // 构造 prompt
            List<Character> relatedCharacters = resolveRelatedCharacters(sb, allCharacters);
            String prompt = promptBuilder.buildFramePrompt(sb.getPrompt(), stylePreset, relatedCharacters);

            // 收集参考图 URL（图生图，文档 14.4）
            List<String> refUrls = relatedCharacters.stream()
                    .map(Character::getReferenceImageUrl)
                    .filter(u -> u != null && !u.isBlank())
                    .limit(3)
                    .collect(Collectors.toList());

            // 创建 frame_image 记录
            FrameImage frame = new FrameImage();
            frame.setStoryboardId(sb.getId());
            frame.setPrompt(prompt);
            frame.setReferenceImageUrls(String.join(",", refUrls));
            frame.setStatus("GENERATING");
            baseMapper.insert(frame);

            // 调用图像模型（图生图，size=2K，ratio=9:16）
            AgnesImageClient.ImageResult result;
            if (refUrls.isEmpty()) {
                result = agnesImageClient.textToImage(prompt, "2K", "9:16");
            } else {
                result = agnesImageClient.imageToImage(prompt, "2K", "9:16", refUrls);
            }
            if (result.getUrl() == null) {
                throw new BusinessException(500, "图像模型未返回 URL");
            }
            // 下载到本地存储
            String localUrl = storageService.downloadToLocalStorage(result.getUrl(), "frame", "png");
            frame.setImageUrl(localUrl);
            frame.setSeed(result.getSeed());
            frame.setStatus("SUCCESS");
            baseMapper.updateById(frame);
            taskService.markSuccess(subTask.getId(), "{\"frameId\":" + frame.getId() + "}");
            log.info("分镜画面生成成功: storyboardId={}, frameId={}", sb.getId(), frame.getId());
        } catch (Exception e) {
            log.error("分镜画面生成失败: storyboardId={}", sb.getId(), e);
            taskService.markFailed(subTask.getId(), e.getMessage());
        }
    }

    /**
     * 单张画面重新生成。
     */
    public TaskLog submitRegenerateFrameTask(Long userId, Long storyboardId) {
        Storyboard sb = storyboardService.getByIdOwned(storyboardId);
        Project p = projectService.getOwned(sb.getProjectId(), userId);
        List<Character> allCharacters = characterService.listByProject(sb.getProjectId());
        TaskLog task = taskService.createTask(userId, sb.getProjectId(), TaskConstants.BIZ_FRAME,
                storyboardId, agnesProperties.getModels().getImage(),
                "{\"storyboardId\":" + storyboardId + ",\"regenerate\":true}");
        self.doGenerateFrameAsync(task.getId(), sb.getProjectId(), sb, p.getStylePreset(), allCharacters, userId);
        return task;
    }

    /**
     * 查询项目下全部分镜画面。
     */
    public List<FrameImage> listByProject(Long projectId) {
        List<Storyboard> storyboards = storyboardService.listByProject(projectId);
        if (storyboards.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> sbIds = storyboards.stream().map(Storyboard::getId).collect(Collectors.toList());
        return baseMapper.selectList(new LambdaQueryWrapper<FrameImage>()
                .in(FrameImage::getStoryboardId, sbIds)
                .orderByAsc(FrameImage::getStoryboardId));
    }

    /**
     * 查询指定分镜的最新画面。
     */
    public FrameImage getLatestByStoryboard(Long storyboardId) {
        return baseMapper.selectOne(new LambdaQueryWrapper<FrameImage>()
                .eq(FrameImage::getStoryboardId, storyboardId)
                .orderByDesc(FrameImage::getId)
                .last("LIMIT 1"));
    }

    /**
     * 解析分镜关联的角色（根据 characterIds 字段）。
     */
    private List<Character> resolveRelatedCharacters(Storyboard sb, List<Character> allCharacters) {
        List<Long> ids = promptBuilder.parseCharacterIds(sb.getCharacterIds());
        if (ids.isEmpty()) {
            return List.of();
        }
        return allCharacters.stream()
                .filter(c -> ids.contains(c.getId()))
                .collect(Collectors.toList());
    }
}
