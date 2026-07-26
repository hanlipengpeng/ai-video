package com.ai.comic.aigc;

/**
 * Agnes AI 模型类型（按模型分别计数 RPM，文档 15.4）。
 */
public enum ModelType {

    /** 文本模型 agnes-2.0-flash */
    TEXT,
    /** 图像模型 agnes-image-2.1-flash */
    IMAGE,
    /** 视频模型 agnes-video-v2.0 */
    VIDEO;

    /**
     * 根据 plan 配置的 RPM 字段名获取对应的限制值。
     */
    public int rpmOf(com.ai.comic.aigc.config.AgnesProperties.PlanRpm planRpm) {
        return switch (this) {
            case TEXT -> planRpm.getText();
            case IMAGE -> planRpm.getImage();
            case VIDEO -> planRpm.getVideo();
        };
    }
}
