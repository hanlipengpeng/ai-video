package com.ai.comic.creation.controller;

import com.ai.comic.common.CurrentUser;
import com.ai.comic.common.Result;
import com.ai.comic.creation.entity.Storyboard;
import com.ai.comic.creation.service.CreationService;
import com.ai.comic.creation.service.StoryboardService;
import com.ai.comic.task.entity.TaskLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 分镜控制器（文档 7.2.3）。
 * <p>
 * - POST /api/v1/projects/{id}/storyboard/generate 生成分镜（返回 taskId）<br>
 * - GET  /api/v1/projects/{id}/storyboard 获取分镜列表<br>
 * - PUT  /api/v1/storyboard/{id} 更新单个分镜
 */
@Tag(name = "分镜", description = "分镜生成与编辑")
@RestController
public class StoryboardController {

    @Autowired
    private CreationService creationService;

    @Autowired
    private StoryboardService storyboardService;

    @Operation(summary = "生成分镜（异步，返回 taskId）")
    @PostMapping("/api/v1/projects/{projectId}/storyboard/generate")
    public Result<Map<String, Object>> generate(@PathVariable Long projectId) {
        Long userId = CurrentUser.requireId();
        TaskLog task = creationService.submitStoryboardTask(userId, projectId);
        return Result.ok(Map.of("taskId", task.getId(), "status", task.getStatus()));
    }

    @Operation(summary = "获取分镜列表")
    @GetMapping("/api/v1/projects/{projectId}/storyboard")
    public Result<List<Storyboard>> list(@PathVariable Long projectId) {
        CurrentUser.requireId();
        return Result.ok(storyboardService.listByProject(projectId));
    }

    @Operation(summary = "更新单个分镜")
    @PutMapping("/api/v1/storyboard/{id}")
    public Result<Storyboard> update(@PathVariable Long id, @RequestBody UpdateStoryboardRequest req) {
        CurrentUser.requireId();
        Storyboard sb = storyboardService.update(id, req.getPrompt(), req.getDialogue(),
                req.getNarration(), req.getDurationSec(), req.getCharacterIds());
        return Result.ok(sb);
    }

    @Data
    public static class UpdateStoryboardRequest {
        private String prompt;
        private String dialogue;
        private String narration;
        private BigDecimal durationSec;
        private String characterIds;
    }
}
