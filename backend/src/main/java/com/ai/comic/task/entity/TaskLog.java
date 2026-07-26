package com.ai.comic.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务日志实体（文档 6.2.7）。
 * <p>
 * 所有 AI 调用与异步任务都记录到此表。
 */
@Data
@TableName("task_log")
public class TaskLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 项目 ID */
    private Long projectId;

    /** 业务类型：SCRIPT/STORYBOARD/CHARACTER_IMG/FRAME/VIDEO/COMPOSE */
    private String bizType;

    /** 关联业务 ID */
    private Long bizId;

    /** 调用的 Agnes 模型 */
    private String model;

    /** 本次调用使用的 Key ID */
    private Long apiKeyId;

    /** Agnes 返回的任务 ID（视频异步任务） */
    private String providerTaskId;

    /** 状态：PENDING/RUNNING/SUCCESS/FAILED */
    private String status;

    /** 进度 0-100 */
    private Integer progress;

    /** 输入参数 JSON */
    private String input;

    /** 输出结果 JSON */
    private String output;

    /** 错误信息 */
    private String errorMsg;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
}
