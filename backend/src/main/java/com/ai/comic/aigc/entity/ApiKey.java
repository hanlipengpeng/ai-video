package com.ai.comic.aigc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Agnes AI Key 池实体（文档 6.2.8）。
 * <p>
 * M0 简化：api_key 字段存明文。
 */
@Data
@TableName("api_key")
public class ApiKey {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** Key 标识（如 agnes-key-01） */
    private String name;

    /** 明文 Key（M0 简化），不序列化到前端 */
    @JsonIgnore
    private String apiKey;

    /** 订阅计划：free / token-plan / enterprise */
    private String plan;

    /** 状态：ACTIVE / COOLED / DISABLED */
    private String status;

    /** 冷却截止时间 */
    private LocalDateTime cooledUntil;

    /** 累计调用次数 */
    private Long totalCalls;

    /** 最后使用时间 */
    private LocalDateTime lastUsedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
