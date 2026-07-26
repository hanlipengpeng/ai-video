package com.ai.comic.creation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 分镜实体（文档 6.2.3）。
 */
@Data
@TableName("storyboard")
public class Storyboard {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    /** 分镜序号 */
    private Integer seq;

    /** 画面描述 Prompt */
    private String prompt;

    /** 台词（字幕） */
    private String dialogue;

    /** 旁白 */
    private String narration;

    /** 建议时长（秒） */
    private BigDecimal durationSec;

    /** 关联角色 ID，逗号分隔 */
    private String characterIds;

    /** 关联场景参考图 ID（可空） */
    private Long sceneRefId;

    private LocalDateTime createdAt;
}
