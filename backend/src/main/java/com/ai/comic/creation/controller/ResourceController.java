package com.ai.comic.creation.controller;

import com.ai.comic.common.Result;
import com.ai.comic.creation.StylePresets;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 资源库控制器（文档 7.2.7）。
 * <p>
 * 风格预设硬编码 4 个，不建表。
 */
@Tag(name = "资源库", description = "画风预设等")
@RestController
@RequestMapping("/api/v1/resources")
public class ResourceController {

    @Operation(summary = "画风预设列表")
    @GetMapping("/styles")
    public Result<List<StylePresets.Style>> styles() {
        return Result.ok(StylePresets.ALL);
    }
}
