package com.ai.comic.media.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 视频片段实体（文档 6.2.6）。
 */
@Data
@TableName("video_clip")
public class VideoClip {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long storyboardId;

    /** 输入图（frame_image.id） */
    private Long frameImageId;

    /** AI 生成的片段 URL */
    private String videoUrl;

    /** 片段时长 */
    private BigDecimal durationSec;

    /** 状态：GENERATING/SUCCESS/FAILED */
    private String status;

    private LocalDateTime createdAt;
}
