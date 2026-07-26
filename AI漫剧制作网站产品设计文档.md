# AI 漫剧制作网站产品设计文档

> 版本：v1.1（快速验证版 / MVP-Lite）
> 日期：2026-07-26
> 文档类型：产品设计文档（PRD + 系统设计）
> 状态：初稿
>
> **本版定位**：快速验证"小说→动漫"核心链路可行性，**砍掉登录复杂度与付费/会员/配额体系**，仅保留最小可用闭环。会员、计费、社区等放到验证通过后再做。
>
> **v1.2 更新**：补充角色/场景一致性方案、Agnes AI 多 Key 轮询限流方案。

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
13. [Agnes AI 接入调研清单](#13-agnes-ai-接入调研清单开发前必须完成)
14. [角色与场景一致性方案](#14-角色与场景一致性方案)
15. [多 Key 轮询与限流方案](#15-多-key-轮询与限流方案)

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
| **当前阶段（快速验证）** | 以最小代价打通"小说→剧本→分镜→图片→视频"链路，验证 Agnes AI 各模型效果与端到端体验 |
| 后续 MVP | 多集、角色一致性、风格库、内容审核完善 |
| 中期 | 社区分享、多模型供应商 |
| 长期 | 付费分发、版权交易、IP 孵化 |

> 本文档聚焦"快速验证"阶段，登录、付费、会员、配额、社区等均不在本期范围。

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

### 3.1 功能架构总览（快速验证版）

```
AI 漫剧制作平台（MVP-Lite）
├── 账户模块（极简）
│   ├── 注册 / 登录（用户名 + 密码，单方式即可）
│   └── 退出登录
│   ✗ 不做：手机号/邮箱验证、第三方登录、找回密码、会员、配额
├── 项目管理模块
│   ├── 项目创建 / 列表 / 删除
│   └── 单集即可（不做多集）
├── 内容创作模块（核心，必须打通）
│   ├── 小说导入（粘贴文本为主，txt 上传可选）
│   ├── 剧本生成（文本 AI）
│   ├── 分镜拆解（文本 AI）
│   ├── 角色设定（文本 AI，参考图可选）
│   ├── 画面生成（图像 AI）
│   └── 视频合成（视频 AI 或静态合成二选一）
├── 编辑器模块（轻量）
│   ├── 分镜列表编辑（Prompt / 台词 / 时长）
│   └── 画面重新生成（重试即可，不做局部修改）
│   ✗ 不做：时间轴拖拽、局部重绘、角色替换
├── 资源库模块（极简）
│   └── 风格预设（硬编码 4 个：日漫 / 国漫 / 美漫 / 写实，创建项目时选）
│   ✗ 不做：音乐库、音效库、角色库
├── 渲染导出模块
│   ├── 视频合成（固定 1080p 竖屏或横屏其一）
│   └── 下载 mp4
│   ✗ 不做：多分辨率、一键发布到第三方平台
├── 任务中心（必要）
│   └── 任务进度查看（异步 AI 任务）
│   ✗ 不做：取消、重试策略复杂化
└── 后台管理（极简或暂不做）
    └── Agnes AI 密钥配置（写在配置文件即可）
    ✗ 不做：用户管理、内容审核后台、数据统计
```

> 标记 ✗ 的为本期明确不做，待核心链路验证通过后再补。

### 3.2 核心功能详述

#### 3.2.1 账户（极简）

- 注册：用户名 + 密码（明文入库？否，BCrypt 哈希）。
- 登录：用户名 + 密码 → 返回 JWT。
- 退出：前端丢弃 token 即可。
- **不做**：手机号/邮箱、验证码、第三方登录、找回密码、个人资料编辑。

#### 3.2.2 小说导入

- **以粘贴文本为主**，提供 txt 文件上传（可选）。
- 单次输入上限：5000 字（约 1 集短视频容量）。
- 不做富文本编辑、章节识别、多集拆分。

#### 3.2.3 剧本生成（文本 AI）

- 调用 Agnes AI 文本生成模型，将小说原文改写为剧本结构。
- 输出字段（精简）：
  - 场景标题
  - 场景描述
  - 角色对白（角色名 + 台词）
  - 旁白
- 用户可编辑生成结果，或整体重新生成。不做单条重新生成。

#### 3.2.4 分镜拆解（文本 AI）

- 将剧本拆为分镜列表。
- 每个分镜：
  - 分镜编号
  - 画面描述 Prompt（用于图像生成）
  - 台词 / 旁白（用于字幕）
  - 建议时长（秒）
- 镜头类型可省略或由 AI 直接给到 Prompt 中。
- 支持手动修改 Prompt、调整顺序。不做增删分镜（先简化）。

#### 3.2.5 角色设定（文本 AI，参考图可选）

- 从剧本中抽取角色名 + 一句话外貌描述。
- **角色参考图本期可选**：
  - 若 Agnes AI 图像模型支持 reference image，则生成参考图用于一致性。
  - 若链路过长，先跳过参考图，仅靠 Prompt 描述，验证主流程。
- 不做角色库管理页面，角色信息随项目存储即可。

#### 3.2.6 画面生成（图像 AI）

- 基于分镜 Prompt + 风格预设，调用 Agnes AI 图像生成模型。
- 每个分镜生成 1 张画面（不支持多张候选，简化）。
- 固定宽高比（建议竖屏 9:16，适配短视频平台）。
- 支持整批生成 + 单张"重新生成"。不做局部修改、放大。

#### 3.2.7 视频合成（直接走 AI 视频）

> 本期确定直接调用 Agnes AI 视频模型生成动态片段，不做静态合成兜底。

**流程**：
1. 每个分镜的画面图（frame_image）+ 分镜 Prompt → 调用 Agnes AI 视频模型 → 生成 3~5 秒动态片段。
2. 所有分镜片段生成完成后，后端用 FFmpeg 拼接为完整 mp4。
3. 拼接时叠加字幕（来自分镜台词）、简单转场（淡入淡出）。
4. 输出固定 1080p 竖屏 mp4，提供下载链接。

**任务拆分**：
- `VideoClipJob[]`：每个分镜一个视频生成任务，并行执行（受全局并发上限约束）。
- `ComposeJob`：所有片段就绪后触发，FFmpeg 拼接 + 字幕。

**注意事项**：
- 视频生成耗时长（单片段可能 30s~2min），前端必须有明确进度反馈。
- 失败重试：单片段失败可单独重试，不必整集重来。
- 全局并发上限建议 ≤ 3，避免 Agnes AI 限流或账单失控。
- 若 Agnes AI 视频模型对"图生视频"不支持，退化为"文生视频"（仅用 Prompt，不用图），需在调研阶段确认（见第 13 章）。

### 3.3 辅助功能

#### 3.3.1 任务中心（必要）

- 所有 AI 生成任务（文本/图像/视频）异步执行。
- 提供任务列表 + 进度查看。
- 失败可点击"重试"，不做取消、不做日志详情。

#### 3.3.2 内容审核（本期可降级）

- 文本输入做最简敏感词过滤（本地词库即可）。
- 图像/视频审核暂不接入，依赖 Agnes AI 自身过滤。
- 上线前再补正式审核流程。

#### 3.3.3 配额与计费

- **本期不做。** 不做积分、不做会员、不做配额限制。
- 仅在后端做最简单的全局并发限流（防止误操作打爆 Agnes AI 账单）。

---

## 4. 业务流程设计

### 4.1 主创作流程（Happy Path）

```
登录（用户名+密码）
  └─> 创建项目（选画风）
        └─> 粘贴小说文本
              └─> [文本AI] 生成剧本
                    └─> 用户编辑剧本
                          └─> [文本AI] 拆解分镜 + 抽取角色清单
                                └─> [图像AI] 生成角色参考图（并行）
                                      └─> [图像AI] 逐分镜生成画面（并行，引用角色参考图）
                                            └─> 用户可单张重新生成
                                                  └─> [视频AI] 逐分镜生成动态片段（并行）
                                                        └─> FFmpeg 拼接 + 字幕
                                                              └─> 下载 mp4
```

> 角色参考图步骤在 v1.2 加回，用于一致性保证（见第 14 章）。若 Agnes AI 图像模型不支持 reference image，可降级跳过此步。

### 4.2 任务编排流程

```
Project Job
  ├─> ScriptJob（剧本生成） ── 串行 ──> StoryboardJob（分镜拆解 + 角色抽取）
  ├─> CharacterImageJob[]（角色参考图，并行）
  ├─> FrameImageJob[]（分镜画面，依赖角色图，并行，受全局并发上限约束）
  ├─> VideoClipJob[]（视频片段，依赖对应 FrameImage，并行）
  └─> ComposeJob（合成，依赖所有 VideoClip）
```

任务状态机（简化）：
```
PENDING -> RUNNING -> SUCCESS
                 -> FAILED（前端可点"重试"重新提交）
```
不做 CANCELED、不做 RETRYING 中间态。

### 4.3 状态流转

项目状态（简化）：`DRAFT` → `SCRIPTING` → `STORYBOARDING` → `IMAGE_GENERATING` → `VIDEO_GENERATING` → `COMPOSING` → `READY`

用户可在任意阶段回退编辑（修改后重新触发对应步骤）。

---

## 5. 系统架构设计

### 5.1 总体架构（简化）

```
┌──────────────────────────────────────────────────────────┐
│                      前端（Vue 3）                        │
│        登录 / 项目列表 / 创作工作台 / 任务中心            │
└────────────────────────┬─────────────────────────────────┘
                         │ HTTPS / WebSocket
┌────────────────────────▼─────────────────────────────────┐
│                     Nginx                                │
└────────────────────────┬─────────────────────────────────┘
                         │
┌────────────────────────▼─────────────────────────────────┐
│              后端服务（Spring Boot 单体）                 │
│  ┌──────────┬──────────┬──────────┬──────────┐           │
│  │ account  │ project  │ creation │  render  │           │
│  ├──────────┼──────────┼──────────┼──────────┤           │
│  │  media   │   task   │  aigc    │ (其余暂略)│          │
│  └──────────┴──────────┴──────────┴──────────┘           │
└───┬──────────────────┬──────────────────┬────────────────┘
    │                  │                  │
    ▼                  ▼                  ▼
┌────────┐      ┌────────────┐      ┌──────────────┐
│ MySQL  │      │   Redis    │      │ 对象存储 OSS │
│ 业务数据│      │ 任务队列   │      │ 图片/视频    │
└────────┘      └────────────┘      └──────────────┘
                         │
                         ▼
                ┌─────────────────┐
                │  Agnes AI 网关  │
                │  (文本/图像/视频)│
                └─────────────────┘
```

> 本期采用 **Spring Boot 单体应用**，不做微服务拆分。Redis 可选（无 Redis 时用本地内存队列 + 数据库轮询）。

### 5.2 模块划分（本期保留）

| 模块 | 职责 | 本期范围 |
| --- | --- | --- |
| `account` | 注册登录、JWT | ✅ 极简 |
| `project` | 项目 CRUD | ✅ 单集 |
| `creation` | 剧本、分镜生成与编辑 | ✅ 核心 |
| `media` | 图片生成任务 | ✅ 核心 |
| `render` | 视频合成、导出 | ✅ 静态合成为主 |
| `task` | 异步任务调度 | ✅ 简化版 |
| `aigc` | Agnes AI 接入层 | ✅ 核心 |
| `resource` | 风格预设 | ✅ 硬编码几个 |
| `review` | 内容审核 | ⚠️ 仅文本敏感词 |
| `admin` | 后台管理 | ❌ 暂不做 |

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

### 6.1 ER 概览（简化）

```
user 1───* project 1───* storyboard 1───* frame_image
       │             │                └─* video_clip
       │             └─* character 1───* character_image
       └─* api_key（系统级 Key 池，不归属用户）
                project 1───* task_log
                (本期不建 episode / membership / quota_record 独立表)
```

> 简化说明：
> - **不建 episode 表**：本期单集，剧本/分镜直接挂在 project 下。
> - **保留 character 表**：用于角色一致性方案，存储角色描述 + 参考图（见 14 章）。
> - **保留 video_clip 表**：AI 视频片段需独立存储 URL 与状态，供 FFmpeg 拼接。
> - **新增 api_key 表**：Agnes AI 多 Key 轮询池（见 15 章）。
> - **不建 membership / quota_record 表**：本期不做付费与配额。
> - 风格预设硬编码在代码 / 配置文件，不建表。

### 6.2 核心表结构（共 7 张表）

#### 6.2.1 `user` 用户表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint PK | 主键 |
| username | varchar(64) | 用户名（唯一） |
| password_hash | varchar(128) | BCrypt 哈希 |
| created_at | datetime | |

> 不存 phone / email / avatar / status，最简。

#### 6.2.2 `project` 项目表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint PK | |
| user_id | bigint FK | |
| title | varchar(128) | 标题 |
| style_preset | varchar(32) | 画风预设 key |
| aspect_ratio | varchar(8) | 固定 9:16 |
| status | varchar(16) | DRAFT/SCRIPTING/STORYBOARDING/IMAGE_GENERATING/COMPOSING/READY |
| source_text | longtext | 原始小说文本 |
| script_content | longtext | 剧本 JSON |
| final_video_url | varchar(256) | 最终视频 URL |
| duration_sec | int | 视频时长 |
| created_at / updated_at | datetime | |

> 剧本直接存在 project 上，不单独建 script 表。

#### 6.2.3 `storyboard` 分镜表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint PK | |
| project_id | bigint FK | |
| seq | int | 分镜序号 |
| prompt | text | 画面描述 Prompt |
| dialogue | text | 台词（字幕） |
| narration | text | 旁白 |
| duration_sec | decimal(5,1) | 建议时长 |
| character_ids | varchar(256) | 关联角色 ID，逗号分隔 |
| scene_ref_id | bigint | 关联场景参考图 ID（可空，见 14 章） |
| created_at | datetime | |

#### 6.2.4 `character` 角色表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint PK | |
| project_id | bigint FK | |
| name | varchar(64) | 角色名 |
| aliases | varchar(256) | 别名 |
| appearance | text | 外貌描述（性别、年龄、发型、服饰、特征） |
| personality | text | 性格关键词 |
| reference_image_url | varchar(256) | 角色标准参考图 URL |
| status | varchar(16) | PENDING/READY |
| created_at / updated_at | datetime | |

> 用于角色一致性方案（见 14 章）。参考图由图像模型生成，作为后续分镜画面生成的 reference image 输入。

#### 6.2.5 `frame_image` 分镜画面表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint PK | |
| storyboard_id | bigint FK | |
| image_url | varchar(256) | |
| prompt | text | 实际使用的 Prompt |
| seed | bigint | |
| reference_image_urls | varchar(512) | 引用的角色/场景参考图 URL，逗号分隔 |
| status | varchar(16) | GENERATING/SUCCESS/FAILED |
| created_at | datetime | |

#### 6.2.6 `video_clip` 视频片段表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint PK | |
| storyboard_id | bigint FK | |
| frame_image_id | bigint FK | 输入图 |
| video_url | varchar(256) | AI 生成的片段 URL |
| duration_sec | decimal(5,1) | 片段时长 |
| status | varchar(16) | GENERATING/SUCCESS/FAILED |
| created_at | datetime | |

> 一个分镜一个片段，成功即用于最终拼接。

#### 6.2.7 `task_log` 任务日志表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint PK | |
| user_id | bigint FK | |
| project_id | bigint FK | |
| biz_type | varchar(32) | SCRIPT/STORYBOARD/CHARACTER_IMG/FRAME/VIDEO/COMPOSE |
| biz_id | bigint | 关联业务 ID |
| model | varchar(64) | 调用的 Agnes 模型 |
| api_key_id | bigint FK | 本次调用使用的 Key（见 15 章） |
| provider_task_id | varchar(128) | Agnes 返回的任务 ID |
| status | varchar(16) | PENDING/RUNNING/SUCCESS/FAILED |
| progress | int | 0-100 |
| input | longtext | 输入参数 JSON |
| output | longtext | 输出结果 JSON |
| error_msg | text | |
| started_at / finished_at | datetime | |
| created_at | datetime | |

#### 6.2.8 `api_key` Agnes AI Key 池表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint PK | |
| name | varchar(64) | Key 标识（如 agnes-key-01） |
| api_key | varchar(256) | 加密存储的 Key |
| status | varchar(16) | ACTIVE/COOLED/DISABLED |
| cooled_until | datetime | 冷却截止时间（限流时设置） |
| total_calls | bigint | 累计调用次数（统计用） |
| last_used_at | datetime | 最后使用时间 |
| created_at / updated_at | datetime | |

> Key 池为系统级，不归属用户。详见第 15 章。

### 6.3 索引设计要点

- `task_log`：`(user_id, status, created_at)`、`(biz_type, biz_id)`、`(api_key_id, created_at)`
- `storyboard`：`(project_id, seq)`
- `frame_image`：`(storyboard_id, status)`
- `video_clip`：`(storyboard_id, status)`
- `character`：`(project_id, status)`
- `api_key`：`(status, cooled_until)`
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

### 7.2 接口清单（本期保留）

#### 7.2.1 账户（极简）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/auth/register` | 注册（用户名+密码） |
| POST | `/auth/login` | 登录，返回 JWT |
| GET | `/users/me` | 当前用户信息（仅用户名） |

> 不做 logout 接口（前端丢弃 token）、不做个人资料更新、不做配额查询。

#### 7.2.2 项目

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/projects` | 创建项目 |
| GET | `/projects` | 项目列表（分页） |
| GET | `/projects/{id}` | 项目详情（含剧本、最终视频） |
| PUT | `/projects/{id}` | 更新项目（标题、画风、原文） |
| DELETE | `/projects/{id}` | 删除项目 |

> 不做 episode 接口（单集）。

#### 7.2.3 创作

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/projects/{id}/script/generate` | 生成剧本（返回 taskId） |
| PUT | `/projects/{id}/script` | 更新剧本 JSON |
| POST | `/projects/{id}/storyboard/generate` | 生成分镜（返回 taskId） |
| GET | `/projects/{id}/storyboard` | 获取分镜列表 |
| PUT | `/storyboard/{id}` | 更新单个分镜（Prompt/台词/时长/关联角色） |
| POST | `/projects/{id}/characters/extract` | 从剧本抽取角色（返回 taskId） |
| GET | `/projects/{id}/characters` | 角色列表 |
| PUT | `/characters/{id}` | 更新角色（外貌描述等） |
| POST | `/characters/{id}/image/generate` | 生成角色参考图（返回 taskId） |

> 角色一致性方案依赖角色参考图，详见第 14 章。

#### 7.2.4 媒体生成

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/projects/{id}/frames/generate` | 整批生成分镜画面（返回 taskId） |
| POST | `/storyboard/{id}/frame/regenerate` | 单张画面重新生成 |
| GET | `/projects/{id}/frames` | 获取全部分镜画面 |
| POST | `/projects/{id}/videos/generate` | 整批生成视频片段（返回 taskId，依赖画面完成） |
| POST | `/storyboard/{id}/video/regenerate` | 单个片段重新生成 |
| GET | `/projects/{id}/videos` | 获取全部视频片段 |

> 视频片段依赖对应画面图，未生成画面的分镜会被跳过并提示。

#### 7.2.5 渲染导出

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/projects/{id}/compose` | 合成视频（返回 taskId） |
| GET | `/projects/{id}/export` | 获取下载链接 |

> 不做 publish（无社区）。

#### 7.2.6 任务

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/tasks/{taskId}` | 任务状态与进度 |
| GET | `/tasks` | 任务列表（分页，按项目过滤） |
| POST | `/tasks/{taskId}/retry` | 重试任务 |
| WS | `/ws/task/{taskId}` | 任务进度 WebSocket 推送（可选） |

> 不做 cancel。WebSocket 可用前端轮询替代以进一步简化。

#### 7.2.7 资源库

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/resources/styles` | 风格预设列表（硬编码几个） |

> 不做音乐 / 音效库接口。

#### 7.2.8 后台管理

> **本期不做。** Agnes AI 密钥等配置写在 `application.yml`，需要时改配置重启即可。

### 7.3 接口示例：生成剧本

**请求**
```
POST /api/v1/projects/12345/script/generate
Authorization: Bearer <token>
Content-Type: application/json

{
  "style": "热血少年"
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
    "result": { "projectId": 12345 }
  }
}
```

---

## 8. 前端页面设计

### 8.1 页面清单（本期保留 5 个页面）

| 页面 | 路由 | 说明 |
| --- | --- | --- |
| 登录 / 注册 | `/login` | 一个页面切换 Tab |
| 项目列表 | `/` | 我的项目 + 新建按钮 |
| 创作工作台 | `/studio/:projectId` | **核心页面**：原文→剧本→分镜→画面→视频→下载 |
| 任务中心 | `/tasks` | 任务进度（可合并到工作台底部，独立页面可选） |
| 风格预设 | （弹窗或下拉） | 选择画风，不单独建页 |

> 不做：首页落地页、项目详情页（合并进工作台）、角色管理、资源库页、预览页（合并进工作台）、个人中心、社区广场、后台管理。

### 8.2 核心页面：创作工作台布局（简化）

```
┌────────────────────────────────────────────────────────────┐
│  顶部栏：项目标题（可编辑）| 画风选择 | 合成视频 | 下载     │
├──────┬─────────────────────────────────────────────────────┤
│      │                                                     │
│ 左侧 │       中间主区域（按步骤切换）                       │
│ 步骤 │                                                     │
│ 导航 │  ① 原文输入框（textarea）                           │
│      │  ② 剧本展示（JSON 结构化卡片，可编辑）              │
│ 1原文│  ③ 分镜列表（卡片：Prompt / 台词 / 时长）           │
│ 2剧本│  ④ 画面网格（每分镜一张图 + 重新生成按钮）          │
│ 3分镜│  ⑤ 视频片段网格（每分镜一段 mp4 + 重新生成按钮）    │
│ 4画面│  ⑥ 最终视频预览（video 标签播放拼接后 mp4）         │
│ 5视频│                                                     │
│ 6导出│                                                     │
│      │                                                     │
├──────┴─────────────────────────────────────────────────────┤
│  底部：当前任务进度条（轮询 /tasks/{id}）                   │
└────────────────────────────────────────────────────────────┘
```

### 8.3 关键交互

- **原文→剧本**：粘贴文本 → 点"生成剧本" → 步骤推进 → 剧本以卡片形式展示，可编辑后保存。
- **剧本→分镜**：点"生成分镜" → 中间切换为分镜卡片列表，每张卡可编辑 Prompt、台词、时长。
- **分镜→画面**：点"批量生成画面" → 网格视图显示每分镜一张图，单张可"重新生成"。
- **画面→视频片段**：点"批量生成视频" → 网格视图显示每分镜一段 mp4（可在线预览），单个可"重新生成"。
- **视频片段→最终视频**：点"合成视频" → 后端 FFmpeg 拼接所有片段 + 字幕 → 完成后显示 video 播放器 + 下载按钮。
- **进度**：底部固定任务进度条，前端每 2s 轮询 `/tasks/{id}`（视频批量任务进度 = 已完成片段数 / 总片段数）。

### 8.4 前端技术要点（精简）

- Vue 3 + Composition API + TypeScript。
- 路由：Vue Router 4。
- 状态：Pinia。
- UI：Element Plus（推荐，组件齐全）。
- 视频：原生 `<video>` 标签即可，无需复杂播放器。
- HTTP：Axios。
- 构建：Vite。

> 不引入：富文本编辑器、Fabric.js、时间轴组件、西瓜播放器（本期都不需要）。

---

## 9. 技术选型（精简）

### 9.1 后端

| 类别 | 选型 | 说明 |
| --- | --- | --- |
| 语言/框架 | Java 17 + Spring Boot 3 | 单体应用 |
| ORM | MyBatis-Plus | 开发效率高 |
| 数据库 | MySQL 8 | 仅 4 张业务表 |
| 缓存 | **本期不引入 Redis** | 任务用 DB 状态字段 + 内存线程池即可 |
| 对象存储 | MinIO（自建，docker 一键起） | 图片视频存储；或本地磁盘 |
| 任务调度 | Spring `@Async` + 线程池 | 异步执行 AI 任务 |
| 鉴权 | Spring Security + JWT | 极简 |
| 视频处理 | FFmpeg | Ken Burns + 字幕 + 拼接 |
| 接口文档 | SpringDoc OpenAPI 3 | 可选 |

> 不引入：XXL-JOB、RabbitMQ、WebSocket（前端轮询替代）、ELK。

### 9.2 AI 服务

| 能力 | 来源 | 本期必选 |
| --- | --- | --- |
| 文本生成（剧本/分镜） | Agnes AI 文本模型 | ✅ |
| 图像生成（分镜画面） | Agnes AI 图像模型 | ✅ |
| 视频生成（动态片段） | Agnes AI 视频模型 | ✅ |

> Agnes AI 密钥、模型版本、base URL 写在 `application.yml`。
> ⚠️ 由于暂无完整文档，开发前需先按第 13 章清单做接口调研/探活。

### 9.3 前端

见 8.4。

### 9.4 部署（极简）

| 类别 | 选型 |
| --- | --- |
| 容器 | Docker Compose（springboot + mysql + minio + ffmpeg） |
| 反向代理 | Nginx（可选，单机可直接暴露端口） |
| 监控 | 暂不做 |

> 一台 4C8G 服务器即可跑起整套验证环境。

---

## 10. 非功能性需求（本期降级）

| 维度 | 要求 |
| --- | --- |
| 性能 | 普通接口 P95 < 500ms 即可；AI 任务提交 < 2s |
| 可用性 | 单机部署，挂了重启即可，不追求高可用 |
| 并发 | 支撑 10 并发用户、5 并发 AI 任务即可验证 |
| 安全 | JWT、密码 BCrypt、最简敏感词过滤；不做 HTTPS（内网验证） |
| 合规 | 暂不接入正式审核，依赖 Agnes AI 自身过滤 |
| 成本 | 后端做全局并发上限（如同时最多 3 个图像生成任务），防误打爆账单 |

---

## 11. 项目规划与里程碑

| 阶段 | 范围 | 状态 |
| --- | --- | --- |
| **M0：快速验证（本文档对应）** | 极简登录 + 单集 + 静态合成 + 下载 | 设计中 |
| M1：MVP | 多张候选图、AI 视频生成、内容审核、多分辨率 | 待启动 |
| M2：编辑增强 | 画面重绘、局部修改、时间轴、角色一致性 | 待规划 |
| M3：多集与角色库 | 多集管理、风格库、音乐库 | 待规划 |
| M4：社区与商业化 | 作品广场、会员套餐、付费导出 | 待规划 |
| M5：开放生态 | 模板市场、API 开放、多模型供应商 | 待规划 |

> M0 验证目标：**一个用户能在 10 分钟内完成"粘贴小说 → 拿到一段 mp4"**，且成本可控。

---

## 12. 风险与对策（本期聚焦）

| 风险 | 影响 | 对策 |
| --- | --- | --- |
| **Agnes AI 无完整文档** | 无法对接，阻塞开发 | 开发前先按第 13 章做接口调研/探活，必要时联系对方拿文档 |
| **视频模型不支持"图生视频"** | 链路断 | 调研确认；若仅支持"文生视频"，则视频任务只用 Prompt 不用图 |
| **图像模型不支持 reference image** | 角色一致性差 | 走方案 A（纯文本描述固化）；见第 14 章 |
| **单 Key 触发限流（429）** | 任务失败率高 | 多 Key 轮询池 + 限流冷却 + 自动切换；见第 15 章 |
| Agnes AI 视频模型成本高/速度慢 | 验证成本高、体验差 | 全局并发上限 ≤3；单集分镜数限制（如 ≤10）；前端明确进度反馈 |
| 角色一致性不达标 | 产出质量低 | 优先走方案 B（参考图）；失败降级方案 A；验收标准见 14.6 |
| AI 结果不可控 | 用户流失 | 全流程可重新生成，分镜 Prompt 可手动编辑 |
| 并发误操作打爆账单 | 成本风险 | 双层限流（全局 + Key 级）+ 单用户串行提交 |
| 内容合规 | 法律风险 | 本期内网验证不公开，依赖 Agnes AI 过滤；上线前补审核 |
| 模型供应商锁定 | 切换成本 | AIGC 接入层抽象（接口预留），后续可换 |

---

## 附录 A：剧本 JSON 结构示例（简化）

```json
{
  "title": "第一章 觉醒",
  "scenes": [
    {
      "heading": "外景 山顶 黄昏",
      "description": "夕阳染红云海，少年林风独立山巅，风卷衣角。",
      "dialogues": [
        { "character": "林风", "line": "终于到了。" }
      ],
      "narration": "十年苦修，只为今日。"
    }
  ]
}
```

> 去掉 shotSuggestion（合并进分镜 Prompt）。

## 附录 B：分镜 JSON 结构示例（简化）

```json
{
  "storyboards": [
    {
      "seq": 1,
      "prompt": "epic anime landscape, mountain peak at sunset, golden clouds, a young swordsman in white robe standing on the cliff, wind blowing his robe, cinematic lighting, vertical 9:16",
      "dialogue": "终于到了。",
      "narration": "十年苦修，只为今日。",
      "durationSec": 4.0
    }
  ]
}
```

> 去掉 shotType、characterIds（角色描述直接写在 prompt 里）。

---

## 附录 C：M0 验收清单

- [ ] 用户能注册、登录（用户名 + 密码）
- [ ] 能创建项目（选画风），粘贴小说文本
- [ ] 一键生成剧本（调用 Agnes AI 文本模型）
- [ ] 一键生成分镜 + 抽取角色（含 Prompt、台词、时长、character_ids）
- [ ] 角色参考图自动生成（若走方案 B）
- [ ] 一键批量生成分镜画面（调用 Agnes AI 图像模型，引用角色参考图）
- [ ] 单张画面可重新生成
- [ ] 一键批量生成视频片段（调用 Agnes AI 视频模型）
- [ ] 单个视频片段可重新生成
- [ ] 一键合成最终视频（FFmpeg 拼接 + 字幕）
- [ ] 能下载最终 mp4
- [ ] 任务进度有可视化展示
- [ ] 全流程在一个页面内完成
- [ ] **一致性验收**：同一角色在 5 个分镜中发型/服饰/瞳色人眼可识别为同一角色（见 14.6）
- [ ] **多 Key 验收**：配置 ≥2 个 Key，人为打满单 Key 触发 429，系统自动切换到下一个 Key 继续完成任务
- [ ] **限流降级**：所有 Key 同时冷却时，任务标记为 FAILED 并提示「服务繁忙」，不崩溃

---

## 13. Agnes AI 接入调研清单（开发前必须完成）

> 现状：已有 Agnes AI 密钥，但**没有完整接口文档**。以下为开发前必须探明的项，建议用一个独立 Spring Boot 测试工程或 Postman 集合逐项验证，确认后再进入正式开发。

### 13.1 通用信息

| 项 | 待确认 | 备注 |
| --- | --- | --- |
| Base URL | `https://?` | 文本/图像/视频是否同一域名 |
| 鉴权方式 | Header `Authorization: Bearer <key>`？自定义 header？ | |
| 是否兼容 OpenAI 协议 | 是 / 否 | 若兼容，可直接用 OpenAI SDK |
| **单 Key 限流策略** | **QPS / 并发上限 / 日配额** | **决定 Key 池规模与并发上限**（见 15 章） |
| **多 Key 是否独立计费** | **同一账号下多 Key 是否各自独立配额** | **决定多 Key 轮询是否真的有效** |
| **限流响应格式** | **429 状态码？响应体字段？Retry-After 头？** | 用于精确判断冷却时长 |
| 计费方式 | 按次 / 按时长 / 按字符 / 按token | 影响成本预估 |
| 错误码规范 | 4xx/5xx 含义、限流错误码 | 用于重试判断 |

### 13.2 文本模型

| 项 | 待确认 |
| --- | --- |
| 模型名 / model 参数值 | 如 `agnes-text-pro` |
| 接口路径 | `/v1/chat/completions`？自定义？ |
| 是否流式 | SSE 支持？本期建议非流式 |
| 最大输入 token | 影响小说原文长度上限（当前 5000 字） |
| 最大输出 token | 影响剧本/分镜 JSON 长度 |
| 是否支持 JSON 模式 | `response_format=json_object`？保证剧本结构化输出 |
| 输入示例 / 输出示例 | 各取一条真实样本存档 |

### 13.3 图像模型

| 项 | 待确认 |
| --- | --- |
| 模型名 | |
| 接口路径 | `/v1/images/generations`？自定义？ |
| 调用模式 | 文生图 / 图生图 / 图+文生图 |
| 是否支持 reference image | 决定角色一致性方案 |
| 输入参数 | prompt、negative_prompt、width、height、seed、num_inference_steps |
| 宽高比支持 | 9:16 竖屏是否原生支持 |
| 输出格式 | 返回 URL 还是 base64？是否需要下载存储 |
| 同步 / 异步 | 同步返回？还是提交任务+轮询？ |
| 单次生成耗时 | 用于估算批量任务总时长 |

### 13.4 视频模型（最关键，风险最高）

| 项 | 待确认 | 影响 |
| --- | --- | --- |
| 模型名 | | |
| 接口路径 | | |
| **调用模式** | **图生视频（image+prompt→video）/ 文生视频（prompt→video）** | **决定 video_clip 是否需要 frame_image_id** |
| 输入参数 | image_url、prompt、duration、resolution、fps | |
| 输出时长 | 单段 3s / 5s / 10s？ | 影响分镜建议时长 |
| 输出分辨率 | 1080p？720p？竖屏？ | |
| 调用方式 | **同步 / 异步（提交+轮询）** | 异步需设计轮询逻辑 |
| 单段生成耗时 | 30s？2min？更长？ | 影响用户等待体验与并发上限 |
| 输出格式 | mp4 / gif / webp | 影响 FFmpeg 拼接命令 |
| 输出获取方式 | URL 还是 base64 | |
| 是否有内容审核拦截 | 违规内容如何返回 | |

### 13.5 调研产出物

调研完成后，需输出以下内容供正式开发使用：

1. `agnes-ai-api-spec.md`：三类模型的接口规格（路径、参数、响应、错误码）。
2. `agnes-ai-postman-collection.json`：Postman 集合，含可运行的样例请求。
3. `agnes-ai-cost-estimate.md`：单集（按 10 分镜）的 token/次数/时长成本估算。
4. `application.yml` 模板：base_url、key、各模型名、超时、并发上限的配置项。

> **结论**：13.4 视频模型的"调用模式"和"同步/异步"是本次调研的两个决定性问题，答案直接决定 `video_clip` 表结构和 `VideoClipJob` 的实现方式。建议优先验证视频模型。

---

## 14. 角色与场景一致性方案

### 14.1 问题定义

AI 生成漫画的核心痛点：同一角色在不同分镜中长相/服饰不一致，同一场景在多个镜头中环境不一致，导致最终视频割裂感强。

一致性包含两个维度：
- **角色一致性**：林风在第 1 镜是黑发白衣少年，第 5 镜不能变成金发红衣。
- **场景一致性**：山顶场景在多个镜头中地形、天空、光线风格统一。

### 14.2 三档方案（按调研结果选型）

#### 方案 A：纯文本描述固化（兜底，最低保障）

- 适用：Agnes AI 图像模型**不支持** reference image。
- 做法：
  - 角色抽取阶段，为每个角色生成**固定外貌描述片段**（如「林风：18 岁少年，黑色长发束高马尾，白色武侠长袍，腰悬长剑，剑眉星目」）。
  - 生成分镜画面时，把分镜中出现的所有角色的描述片段**强制拼接**到 Prompt 开头。
  - 全项目固定 seed 范围、固定画风关键词、固定 negative prompt。
- 效果：**风格统一，但角色长相只能"近似"，无法严格一致**。
- 成本：无额外图像调用。

#### 方案 B：角色参考图 + reference image（推荐，标准方案）

- 适用：Agnes AI 图像模型**支持**图生图 / reference image / IP-Adapter。
- 做法：
  1. 角色抽取后，为每个角色调用图像模型生成一张**标准参考图**（半身像、正面、中性表情、纯背景），存入 `character.reference_image_url`。
  2. 生成分镜画面时，把分镜涉及角色的参考图 URL 作为 `reference_image`（或 `image` + `denoising_strength`）传入图像模型。
  3. Prompt 仍包含角色描述与场景描述，参考图用于"锁长相"。
- 效果：**角色长相一致性显著提升**，是当前主流方案。
- 成本：每个角色多 1 次图像调用。
- 限制：参考图数量不宜过多，单张图引用 1~3 个角色参考图为宜（避免模型混乱）。

#### 方案 C：场景参考图 + 风格 LoRA（进阶，M0 不做）

- 适用：M2+ 阶段追求高一致性。
- 做法：
  - 为重要场景生成场景参考图，分镜引用。
  - 训练项目级 LoRA（角色 + 风格联合），所有分镜共用。
- 本期不做，预留接口。

### 14.3 M0 阶段实施策略

1. **优先调研 13.3 中的"是否支持 reference image"**：
   - 支持 → 走方案 B（推荐）。
   - 不支持 → 走方案 A（兜底）。
2. **场景一致性**统一用方案 A：
   - 画风预设固定（4 个预设，每个预设包含固定 prompt 片段 + negative prompt + 推荐种子范围）。
   - 同一场景的分镜共享场景关键词（如「外景 山顶 黄昏」→ 固定片段 `mountain peak, golden hour, sea of clouds`）。
3. **角色一致性**用方案 B：
   - 角色抽取后自动生成参考图（可由用户编辑外貌描述后重新生成）。
   - 分镜画面生成时，按 `character_ids` 自动注入对应参考图。
4. **失败降级**：若参考图生成失败或模型临时不支持，自动降级为方案 A，不阻塞主流程。

### 14.4 数据流

```
剧本生成
  └─> 角色抽取（文本AI）─> character 表（name + appearance）
        └─> 角色参考图生成（图像AI，方案B）─> character.reference_image_url
              └─> 分镜拆解（文本AI）─> storyboard.character_ids
                    └─> 分镜画面生成（图像AI）
                          ├─ Prompt = 画风片段 + 场景片段 + 角色描述片段 + 镜头描述
                          └─ reference_images = [角色参考图1, 角色参考图2, ...]
```

### 14.5 一致性相关 Prompt 模板

**画风预设片段**（硬编码，按 `style_preset` 选择）：

| 预设 | 正向片段 | 负向片段 |
| --- | --- | --- |
| 日漫 | `anime style, cel shading, vibrant color, detailed eyes, studio ghibli inspired` | `3d, realistic, photo, blurry` |
| 国漫 | `chinese anime style, ink wash painting influence, dynamic composition, the legend of hei inspired` | `3d, western cartoon, photo` |
| 美漫 | `american comic style, bold line art, high contrast, marvel inspired` | `anime, watercolor, photo` |
| 写实 | `semi-realistic illustration, cinematic lighting, detailed texture, artgerm inspired` | `chibi, low detail, sketch` |

**角色描述片段**（动态拼接）：

```
{character_name}: {age} years old, {gender}, {hair}, {clothing}, {distinctive_features}
```

例如：`Lin Feng: 18 years old, male, long black hair in high ponytail, white wuxia robe with silver embroidery, sword on waist, sharp eyes`

### 14.6 验收标准

- 同一角色在 5 个以上分镜中，**发型、服饰、瞳色**三项关键特征保持一致（人眼可识别为同一角色）。
- 同一场景在多个分镜中，**色调、构图风格**统一。
- 若走方案 A（兜底），验收标准放宽为"风格统一、角色描述无矛盾"。

---

## 15. 多 Key 轮询与限流方案

### 15.1 背景与目标

- Agnes AI 单 Key 存在调用频率限制（QPS / 并发 / 日配额），高频调用会触发 429 限流。
- 目标：通过 **多 Key 轮询池** + **限流冷却** + **失败自动切换**，在合规前提下提升整体吞吐。

### 15.2 Key 池架构

```
┌─────────────────────────────────────────────────────────┐
│                   AIGC 接入层（aigc-gateway）            │
│                                                         │
│   业务调用 ──> KeySelector ──> 选取可用 Key ──> 调用 AI │
│                    │                                    │
│                    ├─ 状态：ACTIVE / COOLED / DISABLED │
│                    ├─ 策略：轮询 / 最少使用 / 随机      │
│                    └─ 监控：429 → 冷却 → 切换下一个     │
│                                                         │
│   KeyPool（内存 + DB 持久化）                           │
│   ├─ key-01 [ACTIVE]   last_used: 10:00                │
│   ├─ key-02 [COOLED]   cooled_until: 10:05             │
│   ├─ key-03 [ACTIVE]   last_used: 10:01                │
│   └─ key-04 [DISABLED] 失效/欠费                        │
└─────────────────────────────────────────────────────────┘
```

### 15.3 Key 状态机

```
            ┌──────────────────────────────────────┐
            ▼                                      │
ACTIVE ──(429/限流)──> COOLED ──(冷却到期)──> ACTIVE
   │                                      │
   │                                      │
   └──(401/欠费/失效)──> DISABLED          │
                          ▲                │
                          └──(人工启用)────┘
```

| 状态 | 含义 | 触发条件 |
| --- | --- | --- |
| `ACTIVE` | 可用 | 默认；冷却到期自动恢复 |
| `COOLED` | 临时不可用，等待冷却 | 收到 429 / 超过 QPS |
| `DISABLED` | 永久不可用 | 401 鉴权失败 / 欠费 / 人工禁用 |

### 15.4 选 Key 策略

- **轮询（Round-Robin）**：在 `ACTIVE` 状态的 Key 中按顺序选，简单均衡，M0 默认采用。
- **最少使用（LFU）**：选 `total_calls` 最少的，避免单 Key 过载。
- **随机**：随机选一个 `ACTIVE` Key，避免抖动。

### 15.5 限流响应处理

调用 Agnes AI 返回 429 时：

1. 当前 Key 标记为 `COOLED`，`cooled_until = now + 冷却时长`（默认 60s，可配置）。
2. 自动从剩余 `ACTIVE` Key 中选一个**重试本次调用**（最多重试 N 次，N = Key 数 - 1）。
3. 若所有 Key 都进入 `COOLED`，本次调用直接失败，任务标记为 `FAILED`，提示「服务繁忙，请稍后重试」。
4. `task_log` 记录 `api_key_id`，便于追溯每个 Key 的使用情况。

### 15.6 并发控制（双层限流）

| 层级 | 维度 | 默认值 | 实现 |
| --- | --- | --- | --- |
| 全局 | 系统级并发上限 | 3（图像/视频各 3） | 后端信号量 / 线程池 |
| Key 级 | 单 Key 并发上限 | 按 Agnes AI 文档，未知则默认 2 | KeySelector 中按 Key 计数 |

> 单 Key 并发上限需在 13 章调研时确认 Agnes AI 的实际限制。

### 15.7 配置示例（application.yml）

```yaml
agnes:
  ai:
    base-url: https://api.agnes-ai.com
    timeout-sec: 120
    keys:
      - name: agnes-key-01
        api-key: ${AGNES_KEY_01}
      - name: agnes-key-02
        api-key: ${AGNES_KEY_02}
      - name: agnes-key-03
        api-key: ${AGNES_KEY_03}
    rate-limit:
      global-concurrency: 3      # 全局并发上限
      key-concurrency: 2         # 单 Key 并发上限（待调研确认）
      cool-down-sec: 60          # 限流冷却时长
      retry-on-429: true         # 收到 429 自动切换 Key 重试
    models:
      text: agnes-text-pro
      image: agnes-image-pro
      video: agnes-video-pro
```

### 15.8 Key 管理接口（可选，本期可仅靠配置文件）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/admin/keys` | Key 池状态列表（状态、最后使用、累计调用） |
| POST | `/admin/keys` | 新增 Key |
| PUT | `/admin/keys/{id}` | 启用/禁用 Key |
| DELETE | `/admin/keys/{id}` | 删除 Key |

> 本期不做后台管理页面的可只靠 `application.yml` + `api_key` 表（启动时加载到内存），通过改配置 + 重启生效；接口预留。

### 15.9 监控指标

| 指标 | 用途 |
| --- | --- |
| 各 Key 状态实时分布 | 运维查看 |
| 各 Key 累计调用次数 | 成本核算 |
| 429 触发次数 | 评估 Key 数是否足够 |
| 平均冷却时长 | 调整冷却参数 |
| 任务因"所有 Key 冷却"失败次数 | 扩容 Key 的信号 |

> 本期监控仅落库（`api_key` 表 + `task_log` 表），可视化看板放到 M2 后台管理。

---

> 本文档为 M0 快速验证阶段产品设计，后续将随开发迭代持续更新。
