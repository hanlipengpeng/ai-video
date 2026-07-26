package com.ai.comic.render.controller;

import com.ai.comic.common.BusinessException;
import com.ai.comic.common.CurrentUser;
import com.ai.comic.common.Result;
import com.ai.comic.project.entity.Project;
import com.ai.comic.project.service.ProjectService;
import com.ai.comic.render.service.RenderService;
import com.ai.comic.task.entity.TaskLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 渲染导出控制器（文档 7.2.5）。
 * <p>
 * - POST /api/v1/projects/{id}/compose 合成视频（返回 taskId）<br>
 * - GET  /api/v1/projects/{id}/export 获取下载链接
 */
@Tag(name = "渲染导出", description = "FFmpeg 合成与下载")
@RestController
@RequestMapping("/api/v1/projects/{projectId}")
public class RenderController {

    @Autowired
    private RenderService renderService;

    @Autowired
    private ProjectService projectService;

    @Operation(summary = "合成最终视频（异步）")
    @PostMapping("/compose")
    public Result<Map<String, Object>> compose(@PathVariable Long projectId) {
        Long userId = CurrentUser.requireId();
        TaskLog task = renderService.submitComposeTask(userId, projectId);
        return Result.ok(Map.of("taskId", task.getId(), "status", task.getStatus()));
    }

    @Operation(summary = "获取最终视频下载链接")
    @GetMapping("/export")
    public Result<Map<String, Object>> export(@PathVariable Long projectId) {
        Long userId = CurrentUser.requireId();
        Project p = projectService.getOwned(projectId, userId);
        if (p.getFinalVideoUrl() == null) {
            throw new BusinessException(400, "尚未合成视频");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("videoUrl", p.getFinalVideoUrl());
        data.put("durationSec", p.getDurationSec());
        data.put("status", p.getStatus());
        return Result.ok(data);
    }
}
