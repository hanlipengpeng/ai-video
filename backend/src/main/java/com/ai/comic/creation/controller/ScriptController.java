package com.ai.comic.creation.controller;

import com.ai.comic.common.CurrentUser;
import com.ai.comic.common.Result;
import com.ai.comic.creation.service.CreationService;
import com.ai.comic.task.entity.TaskLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 剧本控制器（文档 7.2.3）。
 * <p>
 * - POST /api/v1/projects/{id}/script/generate 生成剧本（返回 taskId）<br>
 * - PUT  /api/v1/projects/{id}/script 更新剧本 JSON
 */
@Tag(name = "剧本", description = "剧本生成与编辑")
@RestController
@RequestMapping("/api/v1/projects/{projectId}")
public class ScriptController {

    @Autowired
    private CreationService creationService;

    @Operation(summary = "生成剧本（异步，返回 taskId）")
    @PostMapping("/script/generate")
    public Result<Map<String, Object>> generate(@PathVariable Long projectId) {
        Long userId = CurrentUser.requireId();
        TaskLog task = creationService.submitScriptTask(userId, projectId);
        return Result.ok(Map.of("taskId", task.getId(), "status", task.getStatus()));
    }

    @Operation(summary = "更新剧本 JSON")
    @PutMapping("/script")
    public Result<Void> update(@PathVariable Long projectId, @RequestBody UpdateScriptRequest req) {
        Long userId = CurrentUser.requireId();
        creationService.saveScript(userId, projectId, req.getScript());
        return Result.ok();
    }

    @Data
    public static class UpdateScriptRequest {
        private String script;
    }
}
