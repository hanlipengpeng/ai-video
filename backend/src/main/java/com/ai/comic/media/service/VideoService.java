package com.ai.comic.media.service;

import com.ai.comic.aigc.client.AgnesVideoClient;
import com.ai.comic.aigc.config.AgnesProperties;
import com.ai.comic.common.BusinessException;
import com.ai.comic.common.ProjectStatus;
import com.ai.comic.common.TaskConstants;
import com.ai.comic.creation.entity.Storyboard;
import com.ai.comic.creation.service.StoryboardService;
import com.ai.comic.media.entity.FrameImage;
import com.ai.comic.media.entity.VideoClip;
import com.ai.comic.media.mapper.VideoClipMapper;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 视频片段服务（文档 3.2.7、16.3）。
 * <p>
 * - 批量生成视频片段（每分镜调用 agnes-video-v2.0 图生视频，异步）<br>
 * - 单个重新生成<br>
 * - 创建任务后由 VideoPoller 轮询结果
 * <p>
 * 注意：异步方法通过 {@code self} 引用调用，避免 Spring AOP 自调用导致 @Async 失效。
 */
@Slf4j
@Service
public class VideoService extends ServiceImpl<VideoClipMapper, VideoClip> {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private StoryboardService storyboardService;

    @Autowired
    private FrameService frameService;

    @Autowired
    private AgnesVideoClient agnesVideoClient;

    @Autowired
    private AgnesProperties agnesProperties;

    @Autowired
    private TaskService taskService;

    /** 自引用，确保 @Async 方法经由 Spring 代理调用 */
    @Autowired
    @Lazy
    private VideoService self;

    /**
     * 批量生成视频片段。
     */
    public TaskLog submitBatchVideosTask(Long userId, Long projectId) {
        Project p = projectService.getOwned(projectId, userId);
        List<Storyboard> storyboards = storyboardService.listByProject(projectId);
        if (storyboards.isEmpty()) {
            throw new BusinessException(400, "请先生成分镜");
        }
        // 校验是否所有分镜都有画面
        List<Storyboard> ready = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (Storyboard sb : storyboards) {
            FrameImage frame = frameService.getLatestByStoryboard(sb.getId());
            if (frame == null || !"SUCCESS".equals(frame.getStatus()) || frame.getImageUrl() == null) {
                missing.add("第" + sb.getSeq() + "镜");
            } else {
                ready.add(sb);
            }
        }
        if (ready.isEmpty()) {
            throw new BusinessException(400, "没有可用的分镜画面，请先生成画面");
        }
        if (!missing.isEmpty()) {
            log.warn("部分分镜缺少画面，将跳过: {}", missing);
        }

        TaskLog task = taskService.createTask(userId, projectId, TaskConstants.BIZ_VIDEO,
                projectId, agnesProperties.getModels().getVideo(),
                "{\"count\":" + ready.size() + ",\"skipped\":" + missing.size() + "}");
        projectService.transitStatus(projectId, ProjectStatus.VIDEO_GENERATING);

        for (Storyboard sb : ready) {
            self.doGenerateVideoAsync(task.getId(), projectId, sb, userId);
        }
        return task;
    }

    @Async("aiTaskExecutor")
    public void doGenerateVideoAsync(Long parentTaskId, Long projectId, Storyboard sb, Long userId) {
        TaskLog subTask = taskService.createTask(userId, projectId, TaskConstants.BIZ_VIDEO,
                sb.getId(), agnesProperties.getModels().getVideo(),
                "{\"storyboardId\":" + sb.getId() + ",\"parentTaskId\":" + parentTaskId + "}");
        try {
            // 获取分镜画面
            FrameImage frame = frameService.getLatestByStoryboard(sb.getId());
            if (frame == null || frame.getImageUrl() == null) {
                taskService.markFailed(subTask.getId(), "缺少分镜画面");
                return;
            }

            // 创建 video_clip 记录
            VideoClip clip = new VideoClip();
            clip.setStoryboardId(sb.getId());
            clip.setFrameImageId(frame.getId());
            clip.setStatus("GENERATING");
            if (sb.getDurationSec() != null) {
                clip.setDurationSec(sb.getDurationSec());
            } else {
                clip.setDurationSec(new BigDecimal("5.0"));
            }
            baseMapper.insert(clip);

            // 构造动作描述 prompt（镜头运动）
            String actionPrompt = sb.getPrompt() == null ? "subtle motion, cinematic" : sb.getPrompt();
            // 调用 Agnes 视频模型创建任务（图生视频）
            AgnesVideoClient.CreateResult result = agnesVideoClient.createImageToVideo(actionPrompt, frame.getImageUrl());

            // 记录 provider task id，由 VideoPoller 轮询
            taskService.markRunning(subTask.getId(), null, result.getVideoId());
            // 关联 video_clip 与 provider task id（暂存到 task_log，VideoPoller 用 biz_id 找 clip）
            log.info("视频任务已创建: storyboardId={}, clipId={}, videoId={}",
                    sb.getId(), clip.getId(), result.getVideoId());
        } catch (com.ai.comic.aigc.NoAvailableKeyException e) {
            // M0 简化：无可用 Key 直接标记失败，用户可点击重试（文档 15.6 排队机制留待 M2）
            taskService.markFailed(subTask.getId(), "无可用 Agnes Key，请稍后重试");
            log.warn("视频任务无可用 Key: storyboardId={}", sb.getId());
        } catch (Exception e) {
            log.error("视频任务创建失败: storyboardId={}", sb.getId(), e);
            taskService.markFailed(subTask.getId(), e.getMessage());
        }
    }

    /**
     * 单个视频片段重新生成。
     */
    public TaskLog submitRegenerateVideoTask(Long userId, Long storyboardId) {
        Storyboard sb = storyboardService.getByIdOwned(storyboardId);
        projectService.getOwned(sb.getProjectId(), userId);
        TaskLog task = taskService.createTask(userId, sb.getProjectId(), TaskConstants.BIZ_VIDEO,
                storyboardId, agnesProperties.getModels().getVideo(),
                "{\"storyboardId\":" + storyboardId + ",\"regenerate\":true}");
        self.doGenerateVideoAsync(task.getId(), sb.getProjectId(), sb, userId);
        return task;
    }

    /**
     * 查询项目下全部视频片段。
     */
    public List<VideoClip> listByProject(Long projectId) {
        List<Storyboard> storyboards = storyboardService.listByProject(projectId);
        if (storyboards.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> sbIds = storyboards.stream().map(Storyboard::getId).collect(Collectors.toList());
        return baseMapper.selectList(new LambdaQueryWrapper<VideoClip>()
                .in(VideoClip::getStoryboardId, sbIds)
                .orderByAsc(VideoClip::getStoryboardId));
    }

    /**
     * 查询指定分镜的最新视频片段。
     */
    public VideoClip getLatestByStoryboard(Long storyboardId) {
        return baseMapper.selectOne(new LambdaQueryWrapper<VideoClip>()
                .eq(VideoClip::getStoryboardId, storyboardId)
                .orderByDesc(VideoClip::getId)
                .last("LIMIT 1"));
    }
}
