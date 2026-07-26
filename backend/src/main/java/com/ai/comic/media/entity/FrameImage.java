package com.ai.comic.media.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分镜画面实体（文档 6.2.5）。
 */
@Data
@TableName("frame_image")
public class FrameImage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long storyboardId;

    /** 图片 URL */
    private String imageUrl;

    /** 实际使用的 Prompt */
    private String prompt;

    /** 随机种子 */
    private Long seed;

    /** 引用的角色/场景参考图 URL，逗号分隔 */
    private String referenceImageUrls;

    /** 状态：GENERATING/SUCCESS/FAILED */
    private String status;

    private LocalDateTime createdAt;
}
