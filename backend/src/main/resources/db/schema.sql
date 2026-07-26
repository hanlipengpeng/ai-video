-- ============================================================
-- AI 漫剧制作网站 数据库 Schema（共 7 张表，参考文档第 6 章）
-- 数据库：comic_db
-- 字符集：utf8mb4
-- MySQL 8+
-- ============================================================

CREATE DATABASE IF NOT EXISTS `comic_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `comic_db`;

-- ------------------------------------------------------------
-- 1. user 用户表（文档 6.2.1）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`      VARCHAR(64)  NOT NULL COMMENT '用户名（唯一）',
    `password_hash` VARCHAR(128) NOT NULL COMMENT 'BCrypt 哈希',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ------------------------------------------------------------
-- 2. project 项目表（文档 6.2.2）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `project`;
CREATE TABLE `project` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`         BIGINT       NOT NULL COMMENT '用户 ID',
    `title`           VARCHAR(128) NOT NULL COMMENT '标题',
    `style_preset`    VARCHAR(32)  NOT NULL DEFAULT 'anime_jp' COMMENT '画风预设 key',
    `aspect_ratio`    VARCHAR(8)   NOT NULL DEFAULT '9:16' COMMENT '宽高比，固定 9:16',
    `status`          VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/SCRIPTING/STORYBOARDING/IMAGE_GENERATING/VIDEO_GENERATING/COMPOSING/READY',
    `source_text`     LONGTEXT     NULL COMMENT '原始小说文本',
    `script_content`  LONGTEXT     NULL COMMENT '剧本 JSON',
    `final_video_url` VARCHAR(256) NULL COMMENT '最终视频 URL',
    `duration_sec`    INT          NULL COMMENT '视频时长（秒）',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_project_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

-- ------------------------------------------------------------
-- 3. storyboard 分镜表（文档 6.2.3）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `storyboard`;
CREATE TABLE `storyboard` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `project_id`     BIGINT       NOT NULL COMMENT '项目 ID',
    `seq`            INT          NOT NULL COMMENT '分镜序号',
    `prompt`         TEXT         NULL COMMENT '画面描述 Prompt',
    `dialogue`       TEXT         NULL COMMENT '台词（字幕）',
    `narration`      TEXT         NULL COMMENT '旁白',
    `duration_sec`   DECIMAL(5,1) NULL COMMENT '建议时长',
    `character_ids`  VARCHAR(256) NULL COMMENT '关联角色 ID，逗号分隔',
    `scene_ref_id`   BIGINT       NULL COMMENT '关联场景参考图 ID（可空）',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_storyboard_project_seq` (`project_id`, `seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分镜表';

-- ------------------------------------------------------------
-- 4. character 角色表（文档 6.2.4）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `character`;
CREATE TABLE `character` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `project_id`          BIGINT       NOT NULL COMMENT '项目 ID',
    `name`                VARCHAR(64)  NOT NULL COMMENT '角色名',
    `aliases`             VARCHAR(256) NULL COMMENT '别名',
    `appearance`          TEXT         NULL COMMENT '外貌描述（性别、年龄、发型、服饰、特征）',
    `personality`         TEXT         NULL COMMENT '性格关键词',
    `reference_image_url` VARCHAR(256) NULL COMMENT '角色标准参考图 URL',
    `status`              VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/READY',
    `created_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_character_project_status` (`project_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ------------------------------------------------------------
-- 5. frame_image 分镜画面表（文档 6.2.5）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `frame_image`;
CREATE TABLE `frame_image` (
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `storyboard_id`        BIGINT       NOT NULL COMMENT '分镜 ID',
    `image_url`            VARCHAR(256) NULL COMMENT '图片 URL',
    `prompt`               TEXT         NULL COMMENT '实际使用的 Prompt',
    `seed`                 BIGINT       NULL COMMENT '随机种子',
    `reference_image_urls` VARCHAR(512) NULL COMMENT '引用的角色/场景参考图 URL，逗号分隔',
    `status`               VARCHAR(16)  NOT NULL DEFAULT 'GENERATING' COMMENT 'GENERATING/SUCCESS/FAILED',
    `created_at`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_frame_storyboard_status` (`storyboard_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分镜画面表';

-- ------------------------------------------------------------
-- 6. video_clip 视频片段表（文档 6.2.6）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `video_clip`;
CREATE TABLE `video_clip` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `storyboard_id`  BIGINT       NOT NULL COMMENT '分镜 ID',
    `frame_image_id` BIGINT       NULL COMMENT '输入图（frame_image.id）',
    `video_url`      VARCHAR(256) NULL COMMENT 'AI 生成的片段 URL',
    `duration_sec`   DECIMAL(5,1) NULL COMMENT '片段时长',
    `status`         VARCHAR(16)  NOT NULL DEFAULT 'GENERATING' COMMENT 'GENERATING/SUCCESS/FAILED',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_video_storyboard_status` (`storyboard_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频片段表';

-- ------------------------------------------------------------
-- 7. task_log 任务日志表（文档 6.2.7）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `task_log`;
CREATE TABLE `task_log` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`          BIGINT       NOT NULL COMMENT '用户 ID',
    `project_id`       BIGINT       NULL COMMENT '项目 ID',
    `biz_type`         VARCHAR(32)  NOT NULL COMMENT 'SCRIPT/STORYBOARD/CHARACTER_IMG/FRAME/VIDEO/COMPOSE',
    `biz_id`           BIGINT       NULL COMMENT '关联业务 ID',
    `model`            VARCHAR(64)  NULL COMMENT '调用的 Agnes 模型',
    `api_key_id`       BIGINT       NULL COMMENT '本次调用使用的 Key',
    `provider_task_id` VARCHAR(128) NULL COMMENT 'Agnes 返回的任务 ID（视频异步任务）',
    `status`           VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCESS/FAILED',
    `progress`         INT          NOT NULL DEFAULT 0 COMMENT '进度 0-100',
    `input`            LONGTEXT     NULL COMMENT '输入参数 JSON',
    `output`           LONGTEXT     NULL COMMENT '输出结果 JSON',
    `error_msg`        TEXT         NULL COMMENT '错误信息',
    `started_at`       DATETIME     NULL COMMENT '开始时间',
    `finished_at`      DATETIME     NULL COMMENT '完成时间',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_task_user_status_created` (`user_id`, `status`, `created_at`),
    KEY `idx_task_biz` (`biz_type`, `biz_id`),
    KEY `idx_task_apikey_created` (`api_key_id`, `created_at`),
    KEY `idx_task_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务日志表';

-- ------------------------------------------------------------
-- 8. api_key Agnes AI Key 池表（文档 6.2.8）
-- 注意：M0 简化，api_key 字段存明文即可
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `api_key`;
CREATE TABLE `api_key` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`         VARCHAR(64)  NOT NULL COMMENT 'Key 标识（如 agnes-key-01）',
    `api_key`      VARCHAR(256) NOT NULL COMMENT '明文 Key（M0 简化）',
    `plan`         VARCHAR(32)  NOT NULL DEFAULT 'free' COMMENT 'free/token-plan/enterprise',
    `status`       VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/COOLED/DISABLED',
    `cooled_until` DATETIME     NULL COMMENT '冷却截止时间（限流时设置）',
    `total_calls`  BIGINT       NOT NULL DEFAULT 0 COMMENT '累计调用次数',
    `last_used_at` DATETIME     NULL COMMENT '最后使用时间',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_apikey_status_cooled` (`status`, `cooled_until`),
    UNIQUE KEY `uk_apikey_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agnes AI Key 池表';
