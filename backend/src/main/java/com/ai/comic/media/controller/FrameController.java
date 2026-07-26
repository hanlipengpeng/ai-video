package com.ai.comic.media.controller;

import com.ai.comic.common.CurrentUser;
import com.ai.comic.common.Result;
import com.ai.comic.media.entity.FrameImage;
import com.ai.comic.media.service.FrameService;
import com.ai.comic.task.entity.TaskLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 分镜画面控制器（文档 7.2.4）。
 * <p>
 * - POST /api/v1/projects/{id}/frames/generate 整批生成画面（返回 taskId）<br>
 * - POST /api/v1/storyboard/{id}/frame/regenerate 单张重新生成<br>
 * - GET  /api/v1/projects/{id}/frames 获取全部画面
 */
@Tag(name = "分镜画面", description = "图像生成")
@RestController
public class FrameController {

    @Autowired
    private FrameService frameService;

    @Operation(summary = "整批生成分镜画面（异步）")
    @PostMapping("/api/v1/projects/{projectId}/frames/generate")
    public Result<Map<String, Object>> generate(@PathVariable Long projectId) {
        Long userId = CurrentUser.requireId();
        TaskLog task = frameService.submitBatchFramesTask(userId, projectId);
        return Result.ok(Map.of("taskId", task.getId(), "status", task.getStatus()));
    }

    @Operation(summary = "单张画面重新生成")
    @PostMapping("/api/v1/storyboard/{id}/frame/regenerate")
    public Result<Map<String, Object>> regenerate(@PathVariable Long id) {
        Long userId = CurrentUser.requireId();
        TaskLog task = frameService.submitRegenerateFrameTask(userId, id);
        return Result.ok(Map.of("taskId", task.getId(), "status", task.getStatus()));
    }

    @Operation(summary = "获取全部分镜画面")
    @GetMapping("/api/v1/projects/{projectId}/frames")
    public Result<List<FrameImage>> list(@PathVariable Long projectId) {
        CurrentUser.requireId();
        return Result.ok(frameService.listByProject(projectId));
    }
}
