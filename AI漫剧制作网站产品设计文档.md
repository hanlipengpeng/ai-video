# AI 漫剧制作网站产品设计文档

> 版本：v1.0
> 日期：2026-07-26
> 文档类型：产品设计文档（PRD + 系统设计）
> 状态：初稿

---

## 目录

1. [产品概述](#1-产品概述)
2. [产品定位与目标用户](#2-产品定位与目标用户)
3. [核心功能模块](#3-核心功能模块)
4. [业务流程设计](#4-业务流程设计)
5. [系统架构设计](#5-系统架构设计)
6. [数据库设计](#6-数据库设计)
7. [API 接口设计](#7-api-接口设计)
8. [前端页面设计](#8-前端页面设计)
9. [技术选型](#9-技术选型)
10. [非功能性需求](#10-非功能性需求)
11. [项目规划与里程碑](#11-项目规划与里程碑)
12. [风险与对策](#12-风险与对策)

---

## 1. 产品概述

### 1.1 产品背景

随着 AIGC（AI 生成内容）技术发展，将小说文本快速转化为动漫视频内容的需求日益旺盛。传统动漫制作流程长、成本高、门槛高，而 AI 模型（文本生成、图像生成、视频生成）的能力已足以支撑"小说→剧本→分镜→画面→视频"的自动化生产链路。

本项目旨在打造一个面向 C 端创作者的 **AI 漫剧（动漫短剧）制作平台**，用户输入小说文本，平台通过 Agnes AI 的多模态模型自动完成剧本拆解、角色设定、分镜生成、画面生成、视频合成，最终输出可播放的漫剧视频。

### 1.2 产品愿景

让每一个有故事的人，都能轻松把文字变成动漫。

### 1.3 产品目标

| 阶段 | 目标 |
| --- | --- |
| 短期（MVP） | 打通"小说→剧本→分镜→图片→视频"完整链路，支持单集短剧生成 |
| 中期 | 支持多集连续剧、角色一致性、风格库、社区分享 |
| 长期 | 形成创作者生态，支持付费分发、版权交易、IP 孵化 |

### 1.4 名词解释

| 术语 | 说明 |
| --- | --- |
| 漫剧 | 以 AI 生成的动漫画面为基础，配合字幕、配音、转场形成的短视频剧集 |
| 分镜 | 将剧本拆分为一个个镜头单元，每个镜头对应一段画面描述 |
| Agnes AI | 本项目使用的 AI 模型服务方，提供文本、图像、视频生成能力 |
| 角色一致性 | 同一角色在多个分镜中保持外貌、服饰、特征一致 |
| 任务编排 | 多个 AI 生成任务按依赖关系串联/并联执行的调度机制 |

---

## 2. 产品定位与目标用户

### 2.1 产品定位

- **一句话定位**：基于 Agnes AI 的小说转动漫一站式创作平台。
- **核心价值**：低门槛、低成本、高质量地将小说转化为可播放的漫剧。
- **差异化**：全链路 AI 自动化 + 创作者可干预编辑，兼顾效率与可控性。

### 2.2 目标用户

| 用户画像 | 特征 | 核心诉求 |
| --- | --- | --- |
| 网文作者 | 有小说内容，缺乏视觉化能力 | 把作品可视化，扩大传播 |
| 短视频创作者 | 有流量运营能力，缺内容生产力 | 快速产出动漫短视频素材 |
| 动漫爱好者 | 有创意无制作能力 | 把脑洞/同人变成视频 |
| MCN / 工作室 | 批量化生产需求 | 降本增效，批量生产 |

### 2.3 典型使用场景

1. 网文作者上传小说章节 → 自动生成 1~3 分钟漫剧短片 → 发布到短视频平台。
2. 创作者输入一段故事梗概 → AI 扩写剧本 → 生成分镜 → 生成视频。
3. 工作室批量导入小说 → 后台任务编排 → 批量产出系列漫剧。

---

## 3. 核心功能模块

### 3.1 功能架构总览

```
AI 漫剧制作平台
├── 账户模块
│   ├── 注册 / 登录（手机号、邮箱、第三方）
│   ├── 个人中心
│   └── 会员 / 积分 / 配额
├── 项目管理模块
│   ├── 项目（剧集）创建 / 列表 / 删除
│   └── 多集管理
├── 内容创作模块（核心）
│   ├── 小说导入（粘贴 / 上传 txt / 在线编写）
│   ├── 剧本生成（文本 AI）
│   ├── 分镜拆解（文本 AI）
│   ├── 角色设定管理（文本 AI + 图像 AI）
│   ├── 画面生成（图像 AI）
│   └── 视频合成（视频 AI + 转场 + 字幕）
├── 编辑器模块
│   ├── 分镜编辑器（剧本 / 画面描述 / 字幕）
│   ├── 画面编辑器（重绘 / 局部修改 / 角色替换）
│   └── 时间轴编辑器（镜头顺序 / 时长 / 转场）
├── 资源库模块
│   ├── 风格库（画风预设）
│   ├── 角色库（角色一致性参考图）
│   ├── 音乐 / 音效库
│   └── 素材收藏
├── 渲染导出模块
│   ├── 视频渲染任务
│   ├── 多分辨率导出（720p / 1080p / 竖屏 / 横屏）
│   └── 一键发布
├── 社区 / 分享模块（中期）
│   ├── 作品广场
│   ├── 点赞 / 评论 / 收藏
│   └── 模板分享
└── 后台管理模块
    ├── 用户管理
    ├── 内容审核
    ├── 模型配置（Agnes AI 密钥 / 模型版本）
    ├── 任务监控
    └── 数据统计
```

### 3.2 核心功能详述

#### 3.2.1 小说导入

- 支持粘贴文本、上传 txt/md 文件、在线富文本编辑。
- 单次输入上限：MVP 阶段 5000 字（约 1 集短视频容量）。
- 自动识别章节标题，支持按章节拆分多集。

#### 3.2.2 剧本生成（文本 AI）

- 调用 Agnes AI 文本生成模型，将小说原文改写为剧本结构。
- 输出字段：
  - 场景标题（Scene）
  - 场景描述（环境、时间、氛围）
  - 角色对白（角色名 + 台词）
  - 旁白
  - 镜头建议（近景/远景/特写）
- 用户可对生成结果进行编辑、重新生成单条。

#### 3.2.3 分镜拆解（文本 AI）

- 将剧本按镜头单元拆分为分镜列表。
- 每个分镜包含：
  - 分镜编号
  - 镜头类型（特写/近景/中景/远景）
  - 画面描述（Prompt，用于图像生成）
  - 角色列表（引用角色库）
  - 台词 / 旁白（用于字幕与配音）
  - 建议时长（秒）
- 支持用户手动增删、调整顺序、修改 Prompt。

#### 3.2.4 角色设定管理（文本 AI + 图像 AI）

- 从剧本中自动抽取角色清单。
- 每个角色维护：
  - 角色名 / 别名
  - 外貌描述（性别、年龄、发型、服饰、特征）
  - 性格关键词
  - 角色参考图（图像 AI 生成，用于一致性保持）
- 角色参考图作为后续画面生成的 reference image 输入，保证多分镜角色一致。

#### 3.2.5 画面生成（图像 AI）

- 基于分镜 Prompt + 角色参考图 + 风格预设，调用 Agnes AI 图像生成模型。
- 每个分镜默认生成 1 张主画面，可生成多张供选择。
- 支持参数：画风、宽高比（横屏 16:9 / 竖屏 9:16）、seed。
- 支持重绘、局部修改（inpaint）、放大（upscale）。

#### 3.2.6 视频合成（视频 AI + 后处理）

两种模式：
- **AI 视频生成模式**：调用 Agnes AI 视频生成模型，基于静态画面 + 文本描述生成动态视频片段。
- **合成模式**：静态画面 + Ken Burns 运镜 + 转场 + 字幕 + 配音，本地合成。

视频片段产出后：
- 拼接为完整剧集视频。
- 叠加字幕（来自分镜台词）。
- 叠加背景音乐 / 音效（来自资源库）。
- 输出多种分辨率与画幅。

### 3.3 辅助功能

#### 3.3.1 任务中心

- 所有 AI 生成任务（文本/图像/视频）异步执行，统一在任务中心查看进度。
- 支持任务失败重试、取消、查看日志。

#### 3.3.2 配额与计费

- 每类生成任务消耗对应积分（文本<图像<视频）。
- 免费用户每日配额，会员用户按套餐发放配额。
- 配额扣减在任务发起时预扣，失败返还。

#### 3.3.3 内容审核

- 文本输入与生成内容进行敏感词检测。
- 图像 / 视频生成结果接入图像审核。
- 违规内容拦截并提示用户。

---

## 4. 业务流程设计

### 4.1 主创作流程（Happy Path）

```
用户登录
  └─> 创建项目（剧集）
        └─> 导入小说文本
              └─> [文本AI] 生成剧本
                    └─> 用户编辑剧本
                          └─> [文本AI] 拆解分镜 + 抽取角色
                                └─> [图像AI] 生成角色参考图
                                      └─> [图像AI] 逐分镜生成画面
                                            └─> 用户编辑/重绘画面
                                                  └─> [视频AI] 生成动态片段
                                                        └─> 拼接 + 字幕 + 音乐
                                                              └─> 渲染导出
                                                                    └─> 发布/下载
```

### 4.2 任务编排流程

由于一个项目涉及大量 AI 调用，且存在依赖关系，采用任务编排器：

```
Project Job（项目级任务）
  ├─> ScriptJob（剧本生成） ── 串行 ──> StoryboardJob（分镜拆解）
  │                                       └─> CharacterExtractJob（角色抽取）
  │                                              └─> CharacterImageJob[]（角色图，并行）
  ├─> FrameImageJob[]（分镜画面，依赖角色图，并行）
  ├─> VideoClipJob[]（视频片段，依赖画面，并行）
  └─> ComposeJob（合成，依赖所有 VideoClip）
```

任务状态机：
```
PENDING -> RUNNING -> SUCCESS
         -> FAILED -> RETRYING -> SUCCESS/FAILED
         -> CANCELED
```

### 4.3 状态流转

项目状态：`DRAFT` → `SCRIPTING` → `STORYBOARDING` → `IMAGE_GENERATING` → `VIDEO_GENERATING` → `COMPOSING` → `READY` → `PUBLISHED`

用户可在任意阶段回退编辑。

---

## 5. 系统架构设计

### 5.1 总体架构

```
┌──────────────────────────────────────────────────────────┐
│                      前端（Vue 3）                        │
│   项目管理 / 编辑器 / 任务中心 / 资源库 / 个人中心        │
└────────────────────────┬─────────────────────────────────┘
                         │ HTTPS / WebSocket
┌────────────────────────▼─────────────────────────────────┐
│                  API 网关 / Nginx                         │
└────────────────────────┬─────────────────────────────────┘
                         │
┌────────────────────────▼─────────────────────────────────┐
│              后端服务（Spring Boot）                      │
│  ┌──────────┬──────────┬──────────┬──────────┐           │
│  │ 账户服务 │ 项目服务 │ 创作服务 │ 渲染服务 │           │
│  ├──────────┼──────────┼──────────┼──────────┤           │
│  │ 资源服务 │ 任务服务 │ 审核服务 │ 后台管理 │           │
│  └──────────┴──────────┴──────────┴──────────┘           │
└───┬──────────────────┬──────────────────┬────────────────┘
    │                  │                  │
    ▼                  ▼                  ▼
┌────────┐      ┌────────────┐      ┌──────────────┐
│ MySQL  │      │   Redis    │      │ 对象存储 OSS │
│ 业务数据│      │ 缓存/队列  │      │ 图片/视频/文件│
└────────┘      └────────────┘      └──────────────┘
                         │
                         ▼
                ┌─────────────────┐
                │  Agnes AI 网关  │
                │  (文本/图像/视频)│
                └─────────────────┘
```

### 5.2 模块划分

| 模块 | 职责 |
| --- | --- |
| `account` | 注册登录、用户信息、权限、会员 |
| `project` | 项目（剧集）、集管理 |
| `creation` | 剧本、分镜、角色设定的生成与编辑 |
| `media` | 图片、视频生成任务管理 |
| `render` | 视频拼接、字幕、配乐、导出 |
| `resource` | 风格库、角色库、音乐库 |
| `task` | 任务编排、状态机、调度 |
| `review` | 内容审核 |
| `admin` | 后台管理 |
| `aigc` | Agnes AI 统一接入层（封装鉴权、重试、限流） |

### 5.3 关键设计

#### 5.3.1 Agnes AI 接入层

统一封装对 Agnes AI 三类模型的调用：

```
aigc-gateway
├── TextClient      // 文本生成（剧本、分镜、角色描述）
├── ImageClient     // 图像生成（角色图、分镜画面）
├── VideoClient     // 视频生成（动态片段）
└── common          // 鉴权、限流、重试、日志、计费埋点
```

- 所有调用走异步任务（提交任务 → 轮询/回调获取结果）。
- 限流：按用户、按模型维度限流，防止配额击穿。
- 重试：网络错误自动重试，业务错误（如内容违规）不重试。
- 超时与降级：图像/视频生成耗时长，需合理超时，失败可降级为静态合成。

#### 5.3.2 任务调度

- 任务表 + 状态机 + 调度线程池（或集成 XXL-JOB / Spring Scheduled）。
- 长耗时任务（视频生成）采用"提交即返回 taskId，前端轮询或 WebSocket 推送进度"。
- 任务依赖通过 DAG 描述，父任务完成后触发子任务。

#### 5.3.3 文件存储

- 图片、视频、上传的小说文件统一存对象存储（OSS / MinIO）。
- 数据库只存 URL 与元信息。
- 临时文件设过期策略，节省存储成本。

#### 5.3.4 实时通信

- 任务进度推送采用 WebSocket（Spring Boot + STOMP）。
- 前端订阅 `/topic/task/{taskId}` 接收进度。

---

## 6. 数据库设计

### 6.1 ER 概览

```
user 1───* project 1───* episode 1───* storyboard
                                      └─* frame_image
                                      └─* video_clip
                project 1───* character 1───* character_image
                user 1───* quota_record
                user 1───* membership
                task_log（独立表）
                resource_*（风格、音乐等字典表）
```

### 6.2 核心表结构

#### 6.2.1 `user` 用户表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint PK | 主键 |
| username | varchar(64) | 用户名 |
| phone | varchar(20) | 手机号 |
| email | varchar(128) | 邮箱 |
| password_hash | varchar(128) | 密码哈希 |
| avatar_url | varchar(256) | 头像 |
| status | tinyint | 状态 0禁用 1正常 |
| created_at / updated_at | datetime | 时间 |

#### 6.2.2 `membership` 会员表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint PK | |
| user_id | bigint FK | |
| plan | varchar(32) | FREE/PRO/STUDIO |
| started_at | datetime | |
| expired_at | datetime | |
| quota_text | int | 文本配额 |
| quota_image | int | 图像配额 |
| quota_video | int | 视频配额 |

#### 6.2.3 `project` 项目（剧集）表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint PK | |
| user_id | bigint FK | |
| title | varchar(128) | 剧集标题 |
| cover_url | varchar(256) | 封面 |
| description | text | 简介 |
| style_preset | varchar(32) | 画风预设 |
| aspect_ratio | varchar(8) | 16:9 / 9:16 |
| status | varchar(16) | DRAFT/SCRIPTING/.../PUBLISHED |
| source_text | longtext | 原始小说文本 |
| created_at / updated_at | datetime | |

#### 6.2.4 `episode` 集表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint PK | |
| project_id | bigint FK | |
| episode_no | int | 集号 |
| title | varchar(128) | |
| script_content | longtext | 剧本 JSON |
| final_video_url | varchar(256) | 最终视频 |
| duration_sec | int | 时长 |
| status | varchar(16) | |
| created_at / updated_at | datetime | |

#### 6.2.5 `storyboard` 分镜表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint PK | |
| episode_id | bigint FK | |
| seq | int | 分镜序号 |
| shot_type | varchar(16) | 特写/近景/中景/远景 |
| prompt | text | 画面描述 Prompt |
| dialogue | text | 台词（字幕） |
| narration | text | 旁白 |
| duration_sec | decimal(5,1) | 建议时长 |
| character_ids | varchar(256) | 关联角色 ID，逗号分隔 |

#### 6.2.6 `character` 角色表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint PK | |
| project_id | bigint FK | |
| name | varchar(64) | |
| aliases | varchar(256) | 别名 |
| appearance | text | 外貌描述 |
| personality | text | 性格 |
| reference_image_url | varchar(256) | 参考图 URL |

#### 6.2.7 `frame_image` 分镜画面表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint PK | |
| storyboard_id | bigint FK | |
| image_url | varchar(256) | |
| prompt | text | 实际使用的 Prompt |
| seed | bigint | |
| status | varchar(16) | GENERATING/SUCCESS/FAILED |
| selected | tinyint | 是否选中用于视频 |

#### 6.2.8 `video_clip` 视频片段表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint PK | |
| storyboard_id | bigint FK | |
| frame_image_id | bigint FK | |
| video_url | varchar(256) | |
| duration_sec | decimal(5,1) | |
| mode | varchar(16) | AI_VIDEO / KEN_BURNS |
| status | varchar(16) | |

#### 6.2.9 `task_log` 任务日志表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint PK | |
| user_id | bigint FK | |
| biz_type | varchar(32) | SCRIPT/STORYBOARD/CHARACTER_IMG/FRAME/VIDEO/COMPOSE |
| biz_id | bigint | 关联业务 ID |
| model | varchar(64) | 调用的 Agnes 模型 |
| provider_task_id | varchar(128) | Agnes 返回的任务 ID |
| status | varchar(16) | PENDING/RUNNING/SUCCESS/FAILED/CANCELED |
| progress | int | 0-100 |
| input | longtext | 输入参数 JSON |
| output | longtext | 输出结果 JSON |
| error_msg | text | |
| cost_quota | int | 消耗积分 |
| started_at / finished_at | datetime | |
| created_at | datetime | |

#### 6.2.10 `quota_record` 配额流水表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint PK | |
| user_id | bigint FK | |
| type | varchar(16) | TEXT/IMAGE/VIDEO |
| delta | int | 正数充值，负数消耗 |
| reason | varchar(64) | 任务 ID / 套餐发放 |
| balance_after | int | |
| created_at | datetime | |

#### 6.2.11 资源库表

- `style_preset` 风格预设（name、prompt_fragment、cover）
- `music_library` 音乐库（name、url、duration、category、license）
- `sound_effect` 音效库

### 6.3 索引设计要点

- `task_log`：`(user_id, status, created_at)`、`(biz_type, biz_id)`
- `storyboard`：`(episode_id, seq)`
- `frame_image`：`(storyboard_id, selected)`
- `project`：`(user_id, status)`

---

## 7. API 接口设计

### 7.1 通用约定

- RESTful 风格，统一前缀 `/api/v1`。
- 鉴权：Bearer Token（JWT）。
- 统一响应结构：

```json
{
  "code": 0,
  "message": "ok",
  "data": { ... }
}
```

- 分页统一参数：`page`、`size`，响应含 `total`、`list`。
- 长任务接口统一返回 `taskId`，通过 `/tasks/{taskId}` 查询进度。

### 7.2 接口清单（核心）

#### 7.2.1 账户

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/auth/register` | 注册 |
| POST | `/auth/login` | 登录 |
| POST | `/auth/logout` | 登出 |
| GET | `/users/me` | 当前用户信息 |
| PUT | `/users/me` | 更新个人信息 |
| GET | `/users/me/quota` | 查询配额 |

#### 7.2.2 项目

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/projects` | 创建项目 |
| GET | `/projects` | 项目列表 |
| GET | `/projects/{id}` | 项目详情 |
| PUT | `/projects/{id}` | 更新项目 |
| DELETE | `/projects/{id}` | 删除项目 |
| POST | `/projects/{id}/episodes` | 新增集 |
| GET | `/projects/{id}/episodes` | 集列表 |

#### 7.2.3 创作

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/episodes/{id}/source-text` | 保存小说原文 |
| POST | `/episodes/{id}/script/generate` | 生成剧本（返回 taskId） |
| GET | `/episodes/{id}/script` | 获取剧本 |
| PUT | `/episodes/{id}/script` | 更新剧本 |
| POST | `/episodes/{id}/storyboard/generate` | 生成分镜（返回 taskId） |
| GET | `/episodes/{id}/storyboard` | 获取分镜列表 |
| PUT | `/storyboard/{id}` | 更新单个分镜 |
| POST | `/projects/{id}/characters/extract` | 抽取角色（返回 taskId） |
| GET | `/projects/{id}/characters` | 角色列表 |
| PUT | `/characters/{id}` | 更新角色 |
| POST | `/characters/{id}/image/generate` | 生成角色参考图（taskId） |

#### 7.2.4 媒体生成

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/storyboard/{id}/frame/generate` | 生成分镜画面（taskId） |
| GET | `/storyboard/{id}/frames` | 分镜画面列表 |
| POST | `/frames/{id}/regenerate` | 重绘 |
| POST | `/frames/{id}/select` | 选用 |
| POST | `/storyboard/{id}/video/generate` | 生成视频片段（taskId） |

#### 7.2.5 渲染导出

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/episodes/{id}/compose` | 合成整集视频（taskId） |
| GET | `/episodes/{id}/export` | 导出下载链接 |
| POST | `/episodes/{id}/publish` | 发布到广场（可选） |

#### 7.2.6 任务

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/tasks/{taskId}` | 任务状态与进度 |
| GET | `/tasks` | 任务列表（分页） |
| POST | `/tasks/{taskId}/cancel` | 取消任务 |
| POST | `/tasks/{taskId}/retry` | 重试任务 |
| WS | `/ws/task/{taskId}` | 任务进度 WebSocket 推送 |

#### 7.2.7 资源库

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/resources/styles` | 风格预设列表 |
| GET | `/resources/music` | 音乐列表 |
| GET | `/resources/sfx` | 音效列表 |

#### 7.2.8 后台管理

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/admin/users` | 用户管理 |
| GET | `/admin/tasks` | 任务监控 |
| PUT | `/admin/config/aigc` | Agnes AI 配置 |
| GET | `/admin/stats` | 数据统计 |

### 7.3 接口示例：生成剧本

**请求**
```
POST /api/v1/episodes/12345/script/generate
Authorization: Bearer <token>
Content-Type: application/json

{
  "model": "agnes-text-pro",
  "style": "热血少年",
  "extraInstruction": "保留原著对白，强化动作描写"
}
```

**响应**
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "taskId": "tsk_20260726_0001",
    "status": "PENDING"
  }
}
```

**轮询任务**
```
GET /api/v1/tasks/tsk_20260726_0001
```
```json
{
  "code": 0,
  "data": {
    "taskId": "tsk_20260726_0001",
    "status": "SUCCESS",
    "progress": 100,
    "result": { "scriptId": 8899 }
  }
}
```

---

## 8. 前端页面设计

### 8.1 页面清单

| 页面 | 路由 | 说明 |
| --- | --- | --- |
| 首页 / 落地页 | `/` | 产品介绍、CTA |
| 登录 / 注册 | `/login`, `/register` | |
| 工作台 | `/dashboard` | 项目列表、配额、快捷入口 |
| 项目详情 | `/project/:id` | 集列表、项目设置 |
| 创作工作台 | `/studio/:episodeId` | **核心页面**：剧本/分镜/画面/视频编辑 |
| 角色管理 | `/project/:id/characters` | 角色与参考图 |
| 任务中心 | `/tasks` | 任务进度与历史 |
| 资源库 | `/resources` | 风格、音乐、音效 |
| 预览 / 导出 | `/studio/:episodeId/preview` | 视频预览与导出 |
| 个人中心 | `/me` | 资料、会员、配额 |
| 社区广场 | `/explore` | 作品广场（中期） |
| 后台管理 | `/admin/*` | 管理后台 |

### 8.2 核心页面：创作工作台布局

```
┌────────────────────────────────────────────────────────────┐
│  顶部栏：项目名 | 集号 | 保存 | 渲染导出 | 配额            │
├──────┬──────────────────────────────┬──────────────────────┤
│      │                              │                      │
│ 左侧 │       中间编辑区              │     右侧面板         │
│ 步骤 │  （根据步骤切换）             │   （属性/参数）       │
│ 导航 │                              │                      │
│      │  ① 剧本编辑器                │  - 模型选择          │
│ 1剧本│  ② 分镜列表（卡片）          │  - 风格预设          │
│ 2分镜│  ③ 画面预览（网格）          │  - 角色引用          │
│ 3画面│  ④ 时间轴（视频片段）        │  - 画面参数          │
│ 4视频│                              │  - 重绘/局部修改     │
│ 5导出│                              │                      │
│      │                              │                      │
├──────┴──────────────────────────────┴──────────────────────┤
│  底部：任务进度条 / WebSocket 实时状态                       │
└────────────────────────────────────────────────────────────┘
```

### 8.3 关键交互

- **剧本→分镜**：点击"生成分镜"，左侧步骤推进，中间切换为分镜卡片视图，每张卡可编辑 Prompt、台词、时长。
- **分镜→画面**：分镜卡上方点"生成画面"，进入画面网格视图，每分镜显示多张候选图，点击选用。
- **画面重绘**：图片上悬浮操作按钮（重绘 / 局部修改 / 放大 / 替换角色）。
- **视频时间轴**：底部时间轴展示各分镜视频片段顺序与时长，可拖拽排序、调整时长、添加转场。
- **实时进度**：所有生成任务在底部状态栏与任务中心实时更新（WebSocket）。

### 8.4 前端技术要点

- Vue 3 + Composition API + TypeScript。
- 路由：Vue Router 4。
- 状态：Pinia。
- UI：Element Plus / Naive UI（二选一）。
- 富文本：Toast UI Editor 或自研轻量编辑器（剧本结构化编辑）。
- 图像编辑：Fabric.js（局部修改、蒙版）。
- 时间轴：自研组件或基于 wavesurfer / xgplayer 扩展。
- 视频播放：西瓜播放器（xgplayer）。
- 实时通信：原生 WebSocket 或 socket.io-client。
- 构建：Vite。

---

## 9. 技术选型

### 9.1 后端

| 类别 | 选型 | 说明 |
| --- | --- | --- |
| 语言/框架 | Java 17 + Spring Boot 3 | |
| ORM | MyBatis-Plus | 开发效率高 |
| 数据库 | MySQL 8 | |
| 缓存 | Redis 7 | 配额、任务队列、限流 |
| 对象存储 | MinIO / 阿里云 OSS | 图片视频存储 |
| 任务调度 | XXL-JOB 或 Spring Schedule | 定时与异步任务 |
| 消息队列（可选） | RabbitMQ / Redis Stream | 解耦 AI 任务 |
| 实时通信 | Spring WebSocket + STOMP | |
| 鉴权 | Spring Security + JWT | |
| 接口文档 | SpringDoc OpenAPI 3 | |
| 视频处理 | FFmpeg | 拼接、字幕、转场 |
| 日志 | Logback + ELK | |

### 9.2 AI 服务

| 能力 | 来源 |
| --- | --- |
| 文本生成（剧本/分镜/角色） | Agnes AI 文本模型 |
| 图像生成（角色图/分镜画面） | Agnes AI 图像模型 |
| 视频生成（动态片段） | Agnes AI 视频模型 |

> Agnes AI 接入层需支持模型版本切换、密钥管理、调用计量。

### 9.3 前端

见 8.4。

### 9.4 部署

| 类别 | 选型 |
| --- | --- |
| 容器 | Docker |
| 编排 | Docker Compose（MVP）/ K8s（规模化） |
| 反向代理 | Nginx |
| CI/CD | GitHub Actions / GitLab CI |
| 监控 | Prometheus + Grafana |

---

## 10. 非功能性需求

| 维度 | 要求 |
| --- | --- |
| 性能 | 普通接口 P95 < 300ms；AI 任务接口提交 < 1s |
| 可用性 | 核心服务 99.9%；AI 任务失败可重试，不阻塞主流程 |
| 并发 | MVP 支撑 500 在线用户、50 并发 AI 任务 |
| 安全 | HTTPS、JWT、敏感词过滤、密钥加密存储、SQL 注入防护 |
| 合规 | UGC 内容审核、用户协议、隐私政策、未成年人保护 |
| 成本 | AI 调用成本可控，配额机制防止滥用 |
| 扩展 | 模块化设计，AI 模型可替换（除 Agnes 外可扩展其他供应商） |

---

## 11. 项目规划与里程碑

| 阶段 | 范围 | 里程碑 |
| --- | --- | --- |
| M1：MVP | 账户、项目、剧本/分镜/角色、画面生成、视频合成、导出 | 单集漫剧全链路打通 |
| M2：编辑增强 | 画面重绘、局部修改、时间轴编辑、多分辨率导出 | 创作者可控性提升 |
| M3：多集与角色一致性 | 多集管理、角色库、风格库、音乐库 | 支持连续剧生产 |
| M4：社区与商业化 | 作品广场、分享、会员套餐、付费导出 | 商业闭环 |
| M5：开放与生态 | 模板市场、API 开放、多模型供应商 | 平台化 |

> 本文档当前对应 M1 阶段产品设计。

---

## 12. 风险与对策

| 风险 | 影响 | 对策 |
| --- | --- | --- |
| Agnes AI 调用成本高 | 毛利压力 | 配额机制 + 缓存复用 + 降级策略（静态合成代替视频生成） |
| 视频生成耗时长 | 用户体验差 | 异步任务 + 进度推送 + 任务中心 |
| 角色一致性差 | 产出质量低 | 角色参考图 + reference image + 风格锁定 |
| 内容合规风险 | 法律风险 | 文本/图像/视频三级审核 + 用户协议 |
| AI 结果不可控 | 用户流失 | 全流程可编辑、可重绘、可重新生成 |
| 模型供应商锁定 | 切换成本高 | AIGC 接入层抽象，模型可插拔 |
| 高并发任务积压 | 系统不稳 | 限流 + 队列 + 优先级调度 |

---

## 附录 A：剧本 JSON 结构示例

```json
{
  "title": "第一章 觉醒",
  "scenes": [
    {
      "sceneNo": 1,
      "heading": "外景 山顶 黄昏",
      "description": "夕阳染红云海，少年独立山巅，风卷衣角。",
      "dialogues": [
        { "character": "林风", "line": "终于到了。" }
      ],
      "narration": "十年苦修，只为今日。",
      "shotSuggestion": "远景转特写"
    }
  ]
}
```

## 附录 B：分镜 JSON 结构示例

```json
{
  "storyboards": [
    {
      "seq": 1,
      "shotType": "WIDE",
      "prompt": "epic landscape, mountain peak at sunset, golden clouds, a young swordsman standing on the cliff, wind blowing his robe, cinematic lighting, anime style",
      "dialogue": "终于到了。",
      "narration": "十年苦修，只为今日。",
      "durationSec": 4.0,
      "characterIds": [101]
    }
  ]
}
```

---

> 本文档为产品设计阶段产出，后续将随开发迭代持续更新。
