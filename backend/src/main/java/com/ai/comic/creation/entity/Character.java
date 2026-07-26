package com.ai.comic.creation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色实体（文档 6.2.4）。
 * <p>
 * 用于角色一致性方案（文档第 14 章）。
 */
@Data
@TableName("character")
public class Character {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private String name;

    /** 别名 */
    private String aliases;

    /** 外貌描述（性别、年龄、发型、服饰、特征） */
    private String appearance;

    /** 性格关键词 */
    private String personality;

    /** 角色标准参考图 URL */
    private String referenceImageUrl;

    /** 状态：PENDING/READY */
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
