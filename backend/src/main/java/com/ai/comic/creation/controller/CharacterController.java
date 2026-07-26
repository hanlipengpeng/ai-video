package com.ai.comic.creation.controller;

import com.ai.comic.common.CurrentUser;
import com.ai.comic.common.Result;
import com.ai.comic.creation.entity.Character;
import com.ai.comic.creation.service.CreationService;
import com.ai.comic.creation.service.CharacterService;
import com.ai.comic.task.entity.TaskLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 角色控制器（文档 7.2.3）。
 * <p>
 * - POST /api/v1/projects/{id}/characters/extract 抽取角色（返回 taskId）<br>
 * - GET  /api/v1/projects/{id}/characters 角色列表<br>
 * - PUT  /api/v1/characters/{id} 更新角色<br>
 * - POST /api/v1/characters/{id}/image/generate 生成角色参考图（返回 taskId）
 */
@Tag(name = "角色", description = "角色抽取与参考图生成")
@RestController
public class CharacterController {

    @Autowired
    private CreationService creationService;

    @Autowired
    private CharacterService characterService;

    @Operation(summary = "从剧本抽取角色（异步，返回 taskId）")
    @PostMapping("/api/v1/projects/{projectId}/characters/extract")
    public Result<Map<String, Object>> extract(@PathVariable Long projectId) {
        Long userId = CurrentUser.requireId();
        TaskLog task = creationService.submitExtractCharactersTask(userId, projectId);
        return Result.ok(Map.of("taskId", task.getId(), "status", task.getStatus()));
    }

    @Operation(summary = "角色列表")
    @GetMapping("/api/v1/projects/{projectId}/characters")
    public Result<List<Character>> list(@PathVariable Long projectId) {
        CurrentUser.requireId();
        return Result.ok(characterService.listByProject(projectId));
    }

    @Operation(summary = "更新角色")
    @PutMapping("/api/v1/characters/{id}")
    public Result<Character> update(@PathVariable Long id, @RequestBody UpdateCharacterRequest req) {
        CurrentUser.requireId();
        Character c = characterService.update(id, req.getAppearance(), req.getPersonality(),
                req.getAliases(), req.getName());
        return Result.ok(c);
    }

    @Operation(summary = "生成角色参考图（异步，返回 taskId）")
    @PostMapping("/api/v1/characters/{id}/image/generate")
    public Result<Map<String, Object>> generateImage(@PathVariable Long id) {
        Long userId = CurrentUser.requireId();
        TaskLog task = creationService.submitCharacterImageTask(userId, id);
        return Result.ok(Map.of("taskId", task.getId(), "status", task.getStatus()));
    }

    @Data
    public static class UpdateCharacterRequest {
        private String name;
        private String aliases;
        private String appearance;
        private String personality;
    }
}
