package com.ai.comic.creation;

import com.ai.comic.creation.entity.Character;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Prompt 拼接逻辑（文档 14.4、14.5、14.6）。
 * <p>
 * 分镜画面 prompt = 画风片段 + 场景片段 + 角色描述 + 镜头描述。
 * <p>
 * 角色参考图 prompt = "character sheet, single character, half-body portrait,
 * front view, neutral expression, plain background, {角色描述}, {画风片段}"
 */
@Component
public class PromptBuilder {

    /**
     * 构造角色参考图 prompt（文档 14.3）。
     *
     * @param character 角色
     * @param styleKey  画风预设 key
     * @return 完整 prompt
     */
    public String buildCharacterReferencePrompt(Character character, String styleKey) {
        StylePresets.Style style = StylePresets.of(styleKey);
        StringBuilder sb = new StringBuilder();
        sb.append("character sheet, single character, half-body portrait, ")
                .append("front view, neutral expression, plain background, ");
        // 角色描述（文档 14.6 格式）
        sb.append(buildCharacterDescription(character)).append(", ");
        sb.append(style.getPositive());
        return sb.toString();
    }

    /**
     * 构造分镜画面 prompt（文档 14.4）。
     *
     * @param storyboardPrompt 分镜的画面描述（镜头描述）
     * @param styleKey         画风预设 key
     * @param characters       该分镜关联的角色列表
     * @return 完整 prompt
     */
    public String buildFramePrompt(String storyboardPrompt, String styleKey, List<Character> characters) {
        StylePresets.Style style = StylePresets.of(styleKey);
        StringBuilder sb = new StringBuilder();
        // 画风片段
        sb.append(style.getPositive()).append(", ");
        // 角色描述（双保险，文档 14.4 注意事项）
        if (characters != null && !characters.isEmpty()) {
            // 单张分镜引用不超过 3 个角色（文档 14.4）
            int limit = Math.min(characters.size(), 3);
            for (int i = 0; i < limit; i++) {
                sb.append(buildCharacterDescription(characters.get(i))).append(", ");
            }
        }
        // 场景/镜头描述（来自分镜 prompt）
        if (storyboardPrompt != null && !storyboardPrompt.isBlank()) {
            sb.append(storyboardPrompt);
        }
        // 负向片段（用 negative prompt 形式追加，部分模型支持 negative_prompt 字段）
        return sb.toString();
    }

    /**
     * 获取画风的负向片段。
     */
    public String buildNegativePrompt(String styleKey) {
        return StylePresets.of(styleKey).getNegative();
    }

    /**
     * 构造角色描述片段（文档 14.6 格式）。
     * <p>
     * {角色名}: {appearance}
     */
    public String buildCharacterDescription(Character character) {
        if (character == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (character.getName() != null) {
            sb.append(character.getName()).append(": ");
        }
        if (character.getAppearance() != null && !character.getAppearance().isBlank()) {
            sb.append(character.getAppearance());
        } else {
            sb.append("unspecified appearance");
        }
        return sb.toString();
    }

    /**
     * 解析 characterIds 字段（逗号分隔）为 ID 列表。
     */
    public List<Long> parseCharacterIds(String characterIds) {
        if (characterIds == null || characterIds.isBlank()) {
            return new ArrayList<>();
        }
        List<Long> ids = new ArrayList<>();
        for (String s : characterIds.split(",")) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                try {
                    ids.add(Long.valueOf(trimmed));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return ids;
    }
}
