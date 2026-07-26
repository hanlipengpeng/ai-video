package com.ai.comic.task.controller;

import com.ai.comic.common.BusinessException;
import com.ai.comic.common.CurrentUser;
import com.ai.comic.common.PageRequest;
import com.ai.comic.common.PageResult;
import com.ai.comic.common.Result;
import com.ai.comic.task.entity.TaskLog;
import com.ai.comic.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 任务控制器（文档 7.2.6）。
 * <p>
 * - GET  /api/v1/tasks/{taskId} 任务状态与进度<br>
 * - GET  /api/v1/tasks 任务列表（分页，按项目过滤）<br>
 * - POST /api/v1/tasks/{taskId}/retry 重试任务
 */
@Tag(name = "任务中心", description = "异步任务进度查询与重试")
@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Operation(summary = "任务详情")
    @GetMapping("/{taskId}")
    public Result<TaskLog> detail(@PathVariable Long taskId) {
        Long userId = CurrentUser.requireId();
        TaskLog task = taskService.getById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
        if (!userId.equals(task.getUserId())) {
            throw new BusinessException(403, "无权访问该任务");
        }
        return Result.ok(task);
    }

    @Operation(summary = "任务列表（分页，按项目过滤）")
    @GetMapping
    public Result<PageResult<TaskLog>> list(@Valid PageRequest pageRequest,
                                            @RequestParam(required = false) Long projectId) {
        Long userId = CurrentUser.requireId();
        PageResult<TaskLog> result = taskService.page(pageRequest.getPage(),
                pageRequest.getSize(), userId, projectId);
        return Result.ok(result);
    }

    @Operation(summary = "重试任务")
    @PostMapping("/{taskId}/retry")
    public Result<TaskLog> retry(@PathVariable Long taskId) {
        Long userId = CurrentUser.requireId();
        TaskLog task = taskService.getById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
        if (!userId.equals(task.getUserId())) {
            throw new BusinessException(403, "无权操作该任务");
        }
        return Result.ok(taskService.retry(taskId));
    }
}
