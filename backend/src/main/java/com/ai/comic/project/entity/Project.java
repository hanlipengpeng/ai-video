package com.ai.comic.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目实体（文档 6.2.2）。
 * <p>
 * 单集，剧本直接挂在 project 上。
 */
@Data
@TableName("project")
public class Project {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    /** 画风预设 key（anime_jp / anime_cn / comic_us / realistic） */
    private String stylePreset;

    /** 宽高比，固定 9:16 */
    private String aspectRatio;

    /** 状态：DRAFT/SCRIPTING/STORYBOARDING/IMAGE_GENERATING/VIDEO_GENERATING/COMPOSING/READY */
    private String status;

    /** 原始小说文本 */
    private String sourceText;

    /** 剧本 JSON */
    private String scriptContent;

    /** 最终视频 URL */
    private String finalVideoUrl;

    /** 视频时长（秒） */
    private Integer durationSec;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
