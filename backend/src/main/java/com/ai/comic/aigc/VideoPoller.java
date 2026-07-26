package com.ai.comic.aigc;

import com.ai.comic.aigc.client.AgnesVideoClient;
import com.ai.comic.common.StorageService;
import com.ai.comic.common.TaskConstants;
import com.ai.comic.config.StorageProperties;
import com.ai.comic.media.entity.VideoClip;
import com.ai.comic.media.mapper.VideoClipMapper;
import com.ai.comic.task.entity.TaskLog;
import com.ai.comic.task.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 视频任务轮询器（文档 16.5）。
 * <p>
 * 每 10s 扫描 task_log 中 biz_type=VIDEO 且 status=RUNNING 的任务，
 * 调用 AgnesVideoClient 查询结果，更新状态。
 * <ul>
 *   <li>成功：下载视频到本地存储，更新 video_clip + task_log；</li>
 *   <li>失败：标记 FAILED；</li>
 *   <li>超时 5min（文档 16.7）：标记 FAILED；</li>
 *   <li>服务重启时自动恢复轮询（文档 16.8）。</li>
 * </ul>
 */
@Slf4j
@Component
public class VideoPoller {

    @Autowired
    private TaskService taskService;

    @Autowired
    private AgnesVideoClient agnesVideoClient;

    @Autowired
    private VideoClipMapper videoClipMapper;

    @Autowired
    private StorageService storageService;

    @Autowired
    private com.ai.comic.aigc.config.AgnesProperties agnesProperties;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 每 10s 扫描一次 RUNNING 状态的视频任务。
     * <p>
     * fixedDelay=10000 表示上次执行结束后等 10s 再执行（避免并发与堆积）。
     */
    @Scheduled(fixedDelay = 10000)
    public void pollRunningVideoTasks() {
        List<TaskLog> running;
        try {
            running = taskService.listByBizTypeAndStatus(
                    TaskConstants.BIZ_VIDEO, TaskConstants.STATUS_RUNNING);
        } catch (Exception e) {
            log.error("查询 RUNNING 视频任务失败", e);
            return;
        }
        if (running == null || running.isEmpty()) {
            return;
        }
        log.debug("轮询 {} 个 RUNNING 视频任务", running.size());
        long maxDurationMs = agnesProperties.getVideo().getMaxPollDurationMs();
        for (TaskLog task : running) {
            try {
                handleOne(task, maxDurationMs);
            } catch (Exception e) {
                log.warn("轮询视频任务失败: taskId={}, err={}", task.getId(), e.getMessage());
            }
        }
    }

    private void handleOne(TaskLog task, long maxDurationMs) {
        // 超时检查（文档 16.7 / 16.8）：以 started_at 为准
        LocalDateTime startedAt = task.getStartedAt() != null ? task.getStartedAt() : task.getCreatedAt();
        if (startedAt != null && Duration.between(startedAt, LocalDateTime.now()).toMillis() > maxDurationMs) {
            String msg = "视频生成超时（> " + (maxDurationMs / 1000) + "s）";
            log.warn("视频任务超时: taskId={}, startedAt={}", task.getId(), startedAt);
            handleFailure(task, msg);
            return;
        }

        String videoId = task.getProviderTaskId();
        if (videoId == null || videoId.isBlank()) {
            // 无 provider_task_id：可能是异步创建尚未完成（race condition），跳过本次轮询
            log.debug("视频任务暂无 video_id，跳过: taskId={}", task.getId());
            return;
        }

        AgnesVideoClient.QueryResult result;
        try {
            result = agnesVideoClient.queryVideo(videoId);
        } catch (AgnesHttpException e) {
            if (e.isUnauthorized()) {
                // Key 鉴权失败，由 KeyPool 已处理 DISABLED；此处不标记任务失败，下次重试
                log.warn("查询视频任务时 Key 鉴权失败: taskId={}, err={}", task.getId(), e.getMessage());
                return;
            }
            if (e.isRateLimited()) {
                // 限流，下次轮询再查
                log.warn("查询视频任务时限流: taskId={}", task.getId());
                return;
            }
            // 其他错误（含 5xx）暂时不标记失败，等待下次轮询
            log.warn("查询视频任务异常: taskId={}, err={}", task.getId(), e.getMessage());
            return;
        } catch (Exception e) {
            log.warn("查询视频任务网络异常: taskId={}, err={}", task.getId(), e.getMessage());
            return;
        }

        String status = result.getStatus();
        if ("succeeded".equals(status) || "success".equals(status) || "completed".equals(status)) {
            handleSuccess(task, result);
        } else if ("failed".equals(status) || "error".equals(status)) {
            handleFailure(task, result.getError() != null ? result.getError() : "Agnes 返回失败");
        } else {
            // processing，更新进度
            if (result.getProgress() != null) {
                taskService.updateProgress(task.getId(), result.getProgress());
            }
        }
    }

    /**
     * 处理成功：下载视频到本地存储，更新 video_clip + task_log。
     */
    private void handleSuccess(TaskLog task, AgnesVideoClient.QueryResult result) {
        String remoteUrl = result.getVideoUrl();
        if (remoteUrl == null || remoteUrl.isBlank()) {
            handleFailure(task, "Agnes 成功但未返回视频 URL");
            return;
        }
        // 下载到本地
        String localUrl = storageService.downloadToLocalStorage(remoteUrl, "video", "mp4");

        // 更新 video_clip
        if (task.getBizId() != null) {
            VideoClip clip = videoClipMapper.selectById(task.getBizId());
            if (clip != null) {
                clip.setVideoUrl(localUrl);
                clip.setStatus("SUCCESS");
                if (clip.getDurationSec() == null) {
                    clip.setDurationSec(new BigDecimal("5.0"));
                }
                videoClipMapper.updateById(clip);
            }
        }
        // 更新 task_log
        try {
            String output = objectMapper.writeValueAsString(
                    java.util.Map.of("videoUrl", localUrl, "remoteUrl", remoteUrl));
            taskService.markSuccess(task.getId(), output);
        } catch (Exception e) {
            taskService.markSuccess(task.getId(), "{\"videoUrl\":\"" + localUrl + "\"}");
        }
        log.info("视频任务成功: taskId={}, videoClipId={}, url={}", task.getId(), task.getBizId(), localUrl);
    }

    /**
     * 处理失败：标记 video_clip + task_log 为 FAILED。
     */
    private void handleFailure(TaskLog task, String errorMsg) {
        if (task.getBizId() != null) {
            VideoClip clip = videoClipMapper.selectById(task.getBizId());
            if (clip != null && "GENERATING".equals(clip.getStatus())) {
                clip.setStatus("FAILED");
                videoClipMapper.updateById(clip);
            }
        }
        taskService.markFailed(task.getId(), errorMsg);
        log.warn("视频任务失败: taskId={}, err={}", task.getId(), errorMsg);
    }
}
