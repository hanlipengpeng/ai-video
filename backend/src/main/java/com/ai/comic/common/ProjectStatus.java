package com.ai.comic.common;

/**
 * 项目状态机（文档 4.3）。
 * <p>
 * DRAFT → SCRIPTING → STORYBOARDING → IMAGE_GENERATING → VIDEO_GENERATING → COMPOSING → READY
 */
public final class ProjectStatus {

    private ProjectStatus() {}

    public static final String DRAFT = "DRAFT";
    public static final String SCRIPTING = "SCRIPTING";
    public static final String STORYBOARDING = "STORYBOARDING";
    public static final String IMAGE_GENERATING = "IMAGE_GENERATING";
    public static final String VIDEO_GENERATING = "VIDEO_GENERATING";
    public static final String COMPOSING = "COMPOSING";
    public static final String READY = "READY";
}
