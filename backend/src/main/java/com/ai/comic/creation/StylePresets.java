package com.ai.comic.creation;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 画风预设（硬编码 4 个，文档 14.5、6.1 说明）。
 * <p>
 * 不建表，直接在代码中维护。
 */
public final class StylePresets {

    private StylePresets() {}

    /** 日漫 */
    public static final Style ANIME_JP = new Style(
            "anime_jp", "日漫",
            "anime style, cel shading, vibrant color, detailed eyes, studio ghibli inspired",
            "3d, realistic, photo, blurry");

    /** 国漫 */
    public static final Style ANIME_CN = new Style(
            "anime_cn", "国漫",
            "chinese anime style, ink wash painting influence, dynamic composition",
            "3d, western cartoon, photo");

    /** 美漫 */
    public static final Style COMIC_US = new Style(
            "comic_us", "美漫",
            "american comic style, bold line art, high contrast, marvel inspired",
            "anime, watercolor, photo");

    /** 写实 */
    public static final Style REALISTIC = new Style(
            "realistic", "写实",
            "semi-realistic illustration, cinematic lighting, detailed texture",
            "chibi, low detail, sketch");

    /** 全部预设 */
    public static final List<Style> ALL = List.of(ANIME_JP, ANIME_CN, COMIC_US, REALISTIC);

    /**
     * 按 key 查找画风预设，找不到默认返回日漫。
     */
    public static Style of(String key) {
        if (key == null) {
            return ANIME_JP;
        }
        for (Style s : ALL) {
            if (s.key.equals(key)) {
                return s;
            }
        }
        return ANIME_JP;
    }

    /**
     * 画风预设。
     */
    @Data
    @AllArgsConstructor
    public static class Style {
        /** 预设 key */
        private String key;
        /** 中文名 */
        private String name;
        /** 正向片段 */
        private String positive;
        /** 负向片段 */
        private String negative;
    }
}
