package com.ai.comic.media.controller;

import com.ai.comic.common.CurrentUser;
import com.ai.comic.common.Result;
import com.ai.comic.media.entity.VideoClip;
import com.ai.comic.media.service.VideoService;
import com.ai.comic.task.entity.TaskLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 视频片段控制器（文档 7.2.4）。
 * <p>
 * - POST /api/v1/projects/{id}/videos/generate 整批生成视频片段（返回 taskId）<br>
 * - POST /api/v1/storyboard/{id}/video/regenerate 单个重新生成<br>
 * - GET  /api/v1/projects/{id}/videos 获取全部视频片段
 */
@Tag(name = "视频片段", description = "AI 视频生成")
@RestController
public class VideoController {

    @Autowired
    private VideoService videoService;

    @Operation(summary = "整批生成视频片段（异步）")
    @PostMapping("/api/v1/projects/{projectId}/videos/generate")
    public Result<Map<String, Object>> generate(@PathVariable Long projectId) {
        Long userId = CurrentUser.requireId();
        TaskLog task = videoService.submitBatchVideosTask(userId, projectId);
        return Result.ok(Map.of("taskId", task.getId(), "status", task.getStatus()));
    }

    @Operation(summary = "单个视频片段重新生成")
    @PostMapping("/api/v1/storyboard/{id}/video/regenerate")
    public Result<Map<String, Object>> regenerate(@PathVariable Long id) {
        Long userId = CurrentUser.requireId();
        TaskLog task = videoService.submitRegenerateVideoTask(userId, id);
        return Result.ok(Map.of("taskId", task.getId(), "status", task.getStatus()));
    }

    @Operation(summary = "获取全部视频片段")
    @GetMapping("/api/v1/projects/{projectId}/videos")
    public Result<List<VideoClip>> list(@PathVariable Long projectId) {
        CurrentUser.requireId();
        return Result.ok(videoService.listByProject(projectId));
    }
}
