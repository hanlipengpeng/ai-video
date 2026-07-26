package com.ai.comic.common;

/**
 * 任务相关常量（biz_type / status）。
 * <p>
 * 见文档 6.2.7、第 4.2 章任务编排。
 */
public final class TaskConstants {

    private TaskConstants() {}

    /** 任务业务类型：剧本 */
    public static final String BIZ_SCRIPT = "SCRIPT";
    /** 任务业务类型：分镜 */
    public static final String BIZ_STORYBOARD = "STORYBOARD";
    /** 任务业务类型：角色图 */
    public static final String BIZ_CHARACTER_IMG = "CHARACTER_IMG";
    /** 任务业务类型：分镜画面 */
    public static final String BIZ_FRAME = "FRAME";
    /** 任务业务类型：视频片段 */
    public static final String BIZ_VIDEO = "VIDEO";
    /** 任务业务类型：合成 */
    public static final String BIZ_COMPOSE = "COMPOSE";

    /** 任务状态：待执行 */
    public static final String STATUS_PENDING = "PENDING";
    /** 任务状态：执行中 */
    public static final String STATUS_RUNNING = "RUNNING";
    /** 任务状态：成功 */
    public static final String STATUS_SUCCESS = "SUCCESS";
    /** 任务状态：失败 */
    public static final String STATUS_FAILED = "FAILED";
}
