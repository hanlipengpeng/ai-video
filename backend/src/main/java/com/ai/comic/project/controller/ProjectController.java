package com.ai.comic.project.controller;

import com.ai.comic.common.CurrentUser;
import com.ai.comic.common.PageRequest;
import com.ai.comic.common.PageResult;
import com.ai.comic.common.Result;
import com.ai.comic.project.entity.Project;
import com.ai.comic.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 项目控制器（文档 7.2.2）。
 * <p>
 * - POST   /api/v1/projects 创建<br>
 * - GET    /api/v1/projects 列表（分页）<br>
 * - GET    /api/v1/projects/{id} 详情<br>
 * - PUT    /api/v1/projects/{id} 更新<br>
 * - DELETE /api/v1/projects/{id} 删除
 */
@Tag(name = "项目管理", description = "项目 CRUD 与状态流转")
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    /** 创建请求体 */
    @Data
    public static class CreateRequest {
        @NotBlank(message = "标题不能为空")
        private String title;
        private String stylePreset;
        private String aspectRatio;
        private String sourceText;
    }

    /** 更新请求体 */
    @Data
    public static class UpdateRequest {
        private String title;
        private String stylePreset;
        private String aspectRatio;
        private String sourceText;
    }

    @Operation(summary = "创建项目")
    @PostMapping
    public Result<Project> create(@Valid @RequestBody CreateRequest req) {
        Long userId = CurrentUser.requireId();
        Project p = projectService.create(userId, req.getTitle(), req.getStylePreset(),
                req.getAspectRatio(), req.getSourceText());
        return Result.ok(p);
    }

    @Operation(summary = "项目列表（分页）")
    @GetMapping
    public Result<PageResult<Project>> list(@Valid PageRequest pageRequest) {
        Long userId = CurrentUser.requireId();
        PageResult<Project> result = projectService.pageByUser(userId,
                pageRequest.getPage(), pageRequest.getSize());
        return Result.ok(result);
    }

    @Operation(summary = "项目详情")
    @GetMapping("/{id}")
    public Result<Project> detail(@PathVariable Long id) {
        Long userId = CurrentUser.requireId();
        return Result.ok(projectService.getOwned(id, userId));
    }

    @Operation(summary = "更新项目")
    @PutMapping("/{id}")
    public Result<Project> update(@PathVariable Long id, @RequestBody UpdateRequest req) {
        Long userId = CurrentUser.requireId();
        Project p = projectService.update(id, userId, req.getTitle(), req.getStylePreset(),
                req.getAspectRatio(), req.getSourceText());
        return Result.ok(p);
    }

    @Operation(summary = "删除项目")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = CurrentUser.requireId();
        projectService.delete(id, userId);
        return Result.ok();
    }
}
