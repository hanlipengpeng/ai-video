# AI 漫剧制作网站产品设计文档

> 版本：v1.1（快速验证版 / MVP-Lite）
> 日期：2026-07-26
> 文档类型：产品设计文档（PRD + 系统设计）
> 状态：初稿
>
> **本版定位**：快速验证"小说→动漫"核心链路可行性，**砍掉登录复杂度与付费/会员/配额体系**，仅保留最小可用闭环。会员、计费、社区等放到验证通过后再做。
>
> **v1.2 更新**：补充角色/场景一致性方案、Agnes AI 多 Key 轮询限流方案。
> **v1.3 更新**：基于 [Agnes AI 官方文档](https://wiki.agnes-ai.com/zh-Hans/docs/overview) 真实信息重写——替换调研清单为接入信息汇总、确认图生图支持（方案 B 定稿）、基于真实 RPM 重写限流、新增异步任务轮询机制。

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
13. [Agnes AI 接入信息汇总](#13-agnes-ai-接入信息汇总)
14. [角色与场景一致性方案](#14-角色与场景一致性方案)
15. [多 Key 轮询与限流方案](#15-多-key-轮询与限流方案)
16. [异步任务轮询机制](#16-异步任务轮询机制)

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
                         │ HTTPS（轮询 tasks）
┌────────────────────────▼─────────────────────────────────┐
│                     Nginx                                │
└────────────────────────┬─────────────────────────────────┘
                         │
┌────────────────────────▼─────────────────────────────────┐
│              后端服务（Spring Boot 单体）                 │
│  ┌──────────┬──────────┬──────────┬──────────┐           │
│  │ account  │ project  │ creation │  render  │           │
│  ├──────────┼──────────┼──────────┼──────────┤           │
│  │  media   │   task   │  review  │ (其余暂略)│          │
│  └──────────┴──────────┴──────────┴──────────┘           │
│           │                                              │
│           ▼                                              │
│  ┌──────────────────────────────────────────────────┐    │
│  │     AIGC 接入层（aigc-gateway，见 13~16 章）      │    │
│  │  ├─ KeyPool（多 Key 轮询 + 限流冷却，见 15 章）  │    │
│  │  ├─ TextClient（同步）                            │    │
│  │  ├─ ImageClient（同步，图生图，见 14 章）         │    │
│  │  ├─ VideoClient（异步 + 轮询，见 16 章）          │    │
│  │  └─ VideoPoller（@Scheduled 扫描 RUNNING 任务）   │    │
│  └──────────────────────────────────────────────────┘    │
└───┬──────────────────┬──────────────────┬────────────────┘
    │                  │                  │
    ▼                  ▼                  ▼
┌────────┐      ┌────────────┐      ┌──────────────┐
│ MySQL  │      │   Redis    │      │ 对象存储 OSS │
│ 业务数据│      │ (可选,本期可省)  │  │ 图片/视频    │
└────────┘      └────────────┘      └──────────────┘
                         │
                         ▼
                ┌─────────────────────────────┐
                │   Agnes AI API（OpenAI 兼容）│
                │   https://apihub.agnes-ai.com│
                │   文本 / 图像（同步）/ 视频（异步）│
                └─────────────────────────────┘
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

| 能力 | 模型 | 端点 | 调用方式 | 本期必选 |
| --- | --- | --- | --- | --- |
| 文本生成（剧本/分镜/角色） | `agnes-2.0-flash` | `/v1/chat/completions` | 同步 | ✅ |
| 图像生成（角色图/分镜画面） | `agnes-image-2.1-flash` | `/v1/images/generations` | 同步（支持图生图） | ✅ |
| 视频生成（动态片段） | `agnes-video-v2.0` | `/v1/videos` | **异步**（需轮询） | ✅ |

> 详细接入信息见第 13 章。Base URL、Key、模型名写在 `application.yml`（见 15.8）。
> 推荐使用 **OpenAI Java SDK** 直接接入（Agnes AI 完全兼容 OpenAI 协议），视频异步 API 自行封装。

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
| **Free Key 视频仅 1 RPM** | 单 Key 一集 10 分镜要 10 分钟 | 多 Key 轮询池（见 15 章），建议 ≥2 个 Token Plan Key |
| **视频日配额 500 秒/Key** | 单 Key 每天约 100 片段 | 多 Key 分摊；监控日配额消耗（15.10） |
| 视频异步任务积压 | 用户等待久 | 轮询机制（16 章）+ 进度可视化 + 单片段超时 5min 失败 |
| 角色一致性不达标 | 产出质量低 | 走方案 B（图生图 + 参考图，14 章）；单张可重新生成 |
| 4K 图像灰度未全覆盖 | 高清画面生成失败 | M0 用 2K（已全量开放），不依赖 4K |
| 视频原生音画同步可能不符预期 | 需额外配音 | M0 先用原生音频；若不满意，M2 接 TTS |
| AI 结果不可控 | 用户流失 | 全流程可重新生成，分镜 Prompt 可手动编辑 |
| 内容合规 | 法律风险 | 本期内网验证不公开，依赖 Agnes AI 过滤；上线前补审核 |
| 模型供应商锁定 | 切换成本 | AIGC 接入层抽象（兼容 OpenAI 协议，可平替其他供应商） |

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
- [ ] 一键生成剧本（调用 `agnes-2.0-flash`）
- [ ] 一键生成分镜 + 抽取角色（含 Prompt、台词、时长、character_ids）
- [ ] 角色参考图自动生成（`agnes-image-2.1-flash` 文生图）
- [ ] 一键批量生成分镜画面（`agnes-image-2.1-flash` 图生图，引用角色参考图）
- [ ] 单张画面可重新生成
- [ ] 一键批量生成视频片段（`agnes-video-v2.0` 图生视频，异步）
- [ ] 视频片段轮询机制工作正常（16 章：创建→轮询→成功下载）
- [ ] 单个视频片段可重新生成
- [ ] 一键合成最终视频（FFmpeg 拼接 + 字幕，保留原生音画同步）
- [ ] 能下载最终 mp4
- [ ] 任务进度有可视化展示（含视频批量进度 3/10）
- [ ] 全流程在一个页面内完成
- [ ] **一致性验收**：同一角色在 5 个分镜中发型/服饰/瞳色人眼可识别为同一角色（见 14.7）
- [ ] **多 Key 验收**：配置 ≥2 个 Token Plan Key，人为打满单 Key 触发 429，系统自动切换到下一个 Key 继续完成任务
- [ ] **限流降级**：所有 Key 同时冷却时，任务进入排队，前端提示「排队中」，不崩溃
- [ ] **任务恢复**：服务重启后，RUNNING 状态的视频任务能恢复轮询（16.8）
- [ ] **超时处理**：单片段超 5min 未完成，标记 FAILED 并提示重试

---

## 13. Agnes AI 接入信息汇总

> 信息来源：[Agnes AI 官方文档](https://wiki.agnes-ai.com/zh-Hans/docs/overview)、[GitHub AgnesAI-Labs](https://github.com/AgnesAI-Labs/AgnesAI-Models)、[API 平台](https://platform.agnes-ai.com/)。
>
> 截至日期：2026-07-26（官方文档版本 2026.06.28）。

### 13.1 通用信息

| 项 | 值 | 备注 |
| --- | --- | --- |
| API Base URL | `https://apihub.agnes-ai.com/v1` | 兼容旧地址 `https://api.agnes-ai.com/v1` |
| 协议兼容 | **完全兼容 OpenAI** | 可直接用 OpenAI SDK，仅需改 base_url / key / model |
| 鉴权 | `Authorization: Bearer <API_KEY>` | Header |
| Content-Type | `application/json` | |
| 官网 | https://agnes-ai.com/ | |
| API 平台 | https://platform.agnes-ai.com/ | 申请 Key、查用量 |
| 开发者文档 | https://wiki.agnes-ai.com/zh-Hans/docs/overview | |

### 13.2 文本模型 Agnes-2.0-Flash

| 项 | 值 |
| --- | --- |
| 模型名 | `agnes-2.0-flash` |
| 端点 | `POST /v1/chat/completions` |
| 上下文 | **1M tokens** |
| Max Output | 65.5K tokens |
| 流式 | 支持 SSE |
| Function Calling | 支持（Agent 场景） |
| 适用 | 剧本生成、分镜拆解、角色抽取 |

**示例**：
```bash
curl https://apihub.agnes-ai.com/v1/chat/completions \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "agnes-2.0-flash",
    "messages": [{"role": "user", "content": "..."}]
  }'
```

> 5000 字小说原文 ≈ 7000 tokens，远小于 1M 上下文，可全量传入。

### 13.3 图像模型 Agnes-Image-2.1-Flash

| 项 | 值 |
| --- | --- |
| 模型名 | `agnes-image-2.1-flash` |
| 端点 | `POST /v1/images/generations` |
| 文生图 | ✅ 支持 |
| **图生图** | ✅ **支持**（`image: string[]` 传 URL 数组或 Base64） |
| 输出尺寸 | `1K` / `2K` / `3K` / `4K`（灰度开放 4K，最高 4096×4096） |
| 宽高比 | `1:1` / `3:4` / `4:3` / `16:9` / **`9:16`** / `2:3` / `3:2` / `21:9` |
| 输出格式 | URL 或 Base64（`return_base64` 或 `extra_body.response_format`） |
| 调用方式 | **同步**（直接返回结果） |
| 编辑模式 | 图改图、多图融合、局部修改、背景替换、风格转换、文字编辑、图像修复 |
| 价格 | **$0 / 张**（免费） |

**文生图示例**：
```bash
curl https://apihub.agnes-ai.com/v1/images/generations \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "agnes-image-2.1-flash",
    "prompt": "anime style, a young swordsman...",
    "size": "2K",
    "ratio": "9:16",
    "extra_body": {"response_format": "url"}
  }'
```

**图生图示例（角色一致性关键）**：
```bash
curl https://apihub.agnes-ai.com/v1/images/generations \
  -H "Authorization: Bearer $API_KEY" \
  -d '{
    "model": "agnes-image-2.1-flash",
    "prompt": "Lin Feng standing on the cliff...",
    "size": "2K",
    "ratio": "9:16",
    "image": ["https://oss.example.com/character-linfeng-ref.png"]
  }'
```

> ✅ **关键结论**：图像模型支持图生图 + 多图融合，**第 14 章角色一致性方案 B 可直接落地**。

### 13.4 视频模型 Agnes-Video-V2.0

| 项 | 值 |
| --- | --- |
| 模型名 | `agnes-video-v2.0` |
| 端点 | `POST /v1/videos`（创建任务） |
| 查询结果 | `GET /agnesapi?video_id=<VIDEO_ID>`（推荐）<br>`GET /v1/videos/<TASK_ID>`（兼容旧版） |
| **文生视频** | ✅ 支持 |
| **图生视频** | ✅ **支持**（image URL） |
| 关键帧动画 | 支持（多帧过渡） |
| **音画同步** | ✅ **原生支持**（视频自带音频，无需单独配音） |
| 分辨率 | 720P / 1080P |
| 调用方式 | **异步**（创建任务 → 轮询结果） |
| 价格 | **$0 / 秒**（免费） |

**创建任务示例**：
```bash
curl https://apihub.agnes-ai.com/v1/videos \
  -H "Authorization: Bearer $API_KEY" \
  -d '{
    "model": "agnes-video-v2.0",
    "prompt": "camera slowly zooms in, wind blowing robe",
    "image": "https://oss.example.com/frame-001.png"
  }'
```

**响应**（返回 task_id / video_id）：
```json
{ "task_id": "tsk_xxx", "video_id": "vid_xxx", "status": "processing" }
```

**轮询结果**：
```bash
curl "https://apihub.agnes-ai.com/agnesapi?video_id=vid_xxx" \
  -H "Authorization: Bearer $API_KEY"
```

> ✅ **关键结论**：
> 1. 视频模型是**异步 API**，必须设计轮询机制（见第 16 章）。
> 2. 支持**图生视频**，`video_clip.frame_image_id` 字段直接可用。
> 3. **原生音画同步**，最终视频自带音频，可省掉 TTS 配音环节。

### 13.5 限流信息（关键！）

来源：[AgnesAI-Models GitHub](https://github.com/AgnesAI-Labs/AgnesAI-Models)

| 用户计划 | 文本 RPM | 图像 RPM | **视频 RPM** | 视频日配额 |
| --- | --- | --- | --- | --- |
| Free / default | 20 | 按分辨率不同 | **1** | - |
| Enterprise | 40 | 更高 | 2 | - |
| **Token Plan** | **1000** | 1K/2K 更高 | **5** | **每天 500 秒** |

订阅配额（Starter/Plus/Pro）：

| 计划 | 文本请求 | 图像 | 视频 |
| --- | --- | --- | --- |
| Starter | 5h / 1500 次；周 / 15000 次 | 4000 张/天 | 500 秒/天 |
| Plus | 5h / 7500 次；周 / 75000 次 | 4000 张/天 | 500 秒/天 |
| Pro | 5h / 30000 次；周 / 300000 次 | 4000 张/天 | 500 秒/天 |

> ⚠️ **关键约束**：
> - **Free 视频仅 1 RPM**：单 Key 每分钟只能生成 1 个视频片段。一集 10 分镜 → 单 Key 至少 10 分钟才能跑完视频步骤。
> - **视频日配额 500 秒**：按单片段 5 秒算，单 Key 每天最多 100 个片段。
> - **多 Key 轮询几乎是必须的**（见第 15 章）。
> - 图像 4000 张/天 对 M0 验证足够（一集 10 分镜 + 几个角色图）。

### 13.6 错误码与重试

参考官方 [TROUBLESHOOTING](https://github.com/AgnesAI-Labs/AgnesAI-Models/blob/main/docs/TROUBLESHOOTING.md) 与 [ERROR_CODES](https://github.com/AgnesAI-Labs/AgnesAI-Models/blob/main/docs/ERROR_CODES.md)：

| HTTP 状态 | 含义 | 处理 |
| --- | --- | --- |
| 429 | 限流 | 切换 Key 重试（见 15 章） |
| 401 | 鉴权失败 | Key 标记 DISABLED |
| 5xx | 服务端错误 | 指数退避重试 |
| 400 | 参数错误 | 不重试，记录日志 |

---

## 14. 角色与场景一致性方案

### 14.1 方案定稿：方案 B（角色参考图 + 图生图）

> ✅ 基于第 13 章确认：Agnes Image 2.1 Flash **支持图生图**（`image: string[]`）与多图融合，方案 B 直接落地，无需兜底方案 A。

### 14.2 实施流程

```
剧本生成
  └─> 角色抽取（agnes-2.0-flash）─> character 表（name + appearance）
        └─> 角色参考图生成（agnes-image-2.1-flash 文生图）─> character.reference_image_url
              └─> 分镜拆解（agnes-2.0-flash）─> storyboard.character_ids
                    └─> 分镜画面生成（agnes-image-2.1-flash 图生图）
                          ├─ prompt = 画风片段 + 场景片段 + 角色描述 + 镜头描述
                          └─ image = [角色参考图1, 角色参考图2, ...]   ← 关键
```

### 14.3 角色参考图生成规范

为每个角色生成**标准参考图**（半身像、正面、中性表情、纯色背景），便于后续图生图引用：

- **Prompt 模板**：`character sheet, single character, half-body portrait, front view, neutral expression, plain background, {角色描述}, {画风片段}`
- **尺寸**：`1K` + `1:1`（参考图无需竖屏）
- **存储**：上传到 OSS，URL 存入 `character.reference_image_url`

### 14.4 分镜画面生成的图生图调用

```json
POST /v1/images/generations
{
  "model": "agnes-image-2.1-flash",
  "prompt": "<画风片段> <场景片段> <镜头描述>",
  "size": "2K",
  "ratio": "9:16",
  "image": [
    "https://oss.../character-linfeng-ref.png",
    "https://oss.../character-xiaoyue-ref.png"
  ]
}
```

**注意事项**：
- 单张分镜引用**不超过 3 个角色参考图**（避免模型混乱）。
- 角色描述同时写入 prompt（双保险）。
- 场景一致性靠固定场景关键词（如 `mountain peak, golden hour, sea of clouds`）。

### 14.5 画风预设片段（硬编码）

| 预设 | 正向片段 | 负向片段（写入 prompt 反向） |
| --- | --- | --- |
| 日漫 | `anime style, cel shading, vibrant color, detailed eyes, studio ghibli inspired` | `3d, realistic, photo, blurry` |
| 国漫 | `chinese anime style, ink wash painting influence, dynamic composition` | `3d, western cartoon, photo` |
| 美漫 | `american comic style, bold line art, high contrast, marvel inspired` | `anime, watercolor, photo` |
| 写实 | `semi-realistic illustration, cinematic lighting, detailed texture` | `chibi, low detail, sketch` |

### 14.6 角色描述片段格式

```
{角色名}: {age} years old, {gender}, {hair}, {clothing}, {features}
```

例：`Lin Feng: 18 years old, male, long black hair in high ponytail, white wuxia robe with silver embroidery, sword on waist, sharp eyes`

### 14.7 验收标准

- 同一角色在 5 个以上分镜中，**发型、服饰、瞳色**三项关键特征保持一致（人眼可识别为同一角色）。
- 同一场景在多个分镜中，**色调、构图风格**统一。
- 若个别分镜一致性不达标，用户可单张重新生成。

---

## 15. 多 Key 轮询与限流方案

### 15.1 背景与目标

基于第 13.5 章真实限流数据：

| 维度 | Free 单 Key | Token Plan 单 Key |
| --- | --- | --- |
| 文本 RPM | 20 | 1000 |
| 图像 RPM | 按分辨率 | 1K/2K 更高 |
| **视频 RPM** | **1** | **5** |
| 视频日配额 | - | 500 秒/天 |

**核心矛盾**：Free 单 Key 视频仅 1 RPM，一集 10 分镜要 10 分钟；视频日配额 500 秒（约 100 个 5s 片段）。

**目标**：通过 **多 Key 轮询池** 突破单 Key 限流，线性提升吞吐。

### 15.2 Key 池架构

```
┌─────────────────────────────────────────────────────────┐
│                   AIGC 接入层（aigc-gateway）            │
│                                                         │
│   业务调用 ──> KeySelector ──> 选取可用 Key ──> 调用 AI │
│                    │                                    │
│                    ├─ 状态：ACTIVE / COOLED / DISABLED │
│                    ├─ 策略：按模型类型选 Key            │
│                    └─ 监控：429 → 冷却 → 切换下一个     │
│                                                         │
│   KeyPool（内存 + DB 持久化）                           │
│   ├─ key-01 [ACTIVE]   text_rpm: 5/20  video_rpm: 0/1  │
│   ├─ key-02 [COOLED]   cooled_until: 10:05             │
│   └─ key-03 [ACTIVE]   ...                              │
└─────────────────────────────────────────────────────────┘
```

### 15.3 Key 状态机

```
            ┌──────────────────────────────────────┐
            ▼                                      │
ACTIVE ──(429/限流)──> COOLED ──(冷却到期)──> ACTIVE
   │
   └──(401/失效)──> DISABLED ──(人工启用)──> ACTIVE
```

| 状态 | 含义 | 触发条件 |
| --- | --- | --- |
| `ACTIVE` | 可用 | 默认；冷却到期自动恢复 |
| `COOLED` | 临时不可用 | 收到 429 / 触发 RPM 上限 |
| `DISABLED` | 永久不可用 | 401 鉴权失败 / 人工禁用 |

### 15.4 按模型分别计数（关键）

> ⚠️ 文本、图像、视频的 RPM 不同，必须**按模型类型分别计数**，不能混用。

| 模型类型 | 单 Key RPM 上限（Free） | 单 Key RPM 上限（Token Plan） |
| --- | --- | --- |
| 文本 | 20 | 1000 |
| 图像 | 按分辨率（约 30） | 1K/2K 更高（约 100） |
| 视频 | **1** | **5** |

`api_key` 表需扩展字段记录各模型类型的最近 1 分钟调用次数（内存滑动窗口计数）。

### 15.5 选 Key 策略

1. 过滤掉 `DISABLED` 和 `COOLED` 的 Key。
2. 过滤掉**当前模型类型**已达 RPM 上限的 Key。
3. 在剩余可用 Key 中**轮询**选取。
4. 若无可用 Key → 调用进入等待队列，等待最近一个 Key 的 RPM 窗口恢复或冷却到期。

### 15.6 限流响应处理（429）

1. 当前 Key 标记 `COOLED`，`cooled_until = now + 冷却时长`（默认 60s）。
2. 自动从剩余 `ACTIVE` Key 中选一个**重试本次调用**（最多重试 N 次，N = Key 数 - 1）。
3. 若所有 Key 都 `COOLED`，本次调用进入等待队列，任务状态保持 `RUNNING`，前端进度提示「排队中」。
4. `task_log` 记录 `api_key_id`，便于追溯。

### 15.7 容量规划（M0 验证）

**目标**：一集 10 分镜，视频步骤在 5 分钟内跑完。

| Key 数量（Token Plan） | 视频 RPM 合计 | 10 分镜耗时 |
| --- | --- | --- |
| 1 | 5 | 2 分钟 |
| 2 | 10 | 1 分钟 |
| 3 | 15 | < 1 分钟 |

> M0 建议至少准备 **2~3 个 Token Plan Key**，既能快速验证又留有余量。Free Key 仅适合文本/图像任务。

### 15.8 配置示例（application.yml）

```yaml
agnes:
  ai:
    base-url: https://apihub.agnes-ai.com/v1
    timeout-sec: 120
    keys:
      - name: agnes-key-01
        api-key: ${AGNES_KEY_01}
        plan: token-plan       # free / token-plan / enterprise
      - name: agnes-key-02
        api-key: ${AGNES_KEY_02}
        plan: token-plan
      - name: agnes-key-03
        api-key: ${AGNES_KEY_03}
        plan: free             # 仅用于文本/图像
    rate-limit:
      cool-down-sec: 60
      retry-on-429: true
      max-retries: 3
    rpm:
      free:
        text: 20
        image: 30
        video: 1
      token-plan:
        text: 1000
        image: 100
        video: 5
    models:
      text: agnes-2.0-flash
      image: agnes-image-2.1-flash
      video: agnes-video-v2.0
```

### 15.9 Key 管理接口（预留，本期靠配置文件）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/admin/keys` | Key 池状态（状态、各模型 RPM 用量、累计调用） |
| POST | `/admin/keys` | 新增 Key |
| PUT | `/admin/keys/{id}` | 启用/禁用 Key |
| DELETE | `/admin/keys/{id}` | 删除 Key |

> 本期通过 `application.yml` + `api_key` 表（启动时加载到内存）实现，改配置重启生效；接口留到 M2 后台管理。

### 15.10 监控指标

| 指标 | 用途 |
| --- | --- |
| 各 Key 状态实时分布 | 运维查看 |
| 各 Key 各模型类型 RPM 用量 | 是否接近上限 |
| 429 触发次数 | 评估 Key 数是否足够 |
| 任务排队等待时长 | 是否需要扩容 Key |
| 视频日配额消耗 | 防止超额 |

> 本期监控仅落库（`api_key` 表 + `task_log` 表），可视化看板放到 M2。

---

## 16. 异步任务轮询机制

### 16.1 背景

基于第 13.4 章确认：Agnes Video V2.0 是**异步 API**，调用流程为：

1. `POST /v1/videos` 创建任务 → 返回 `task_id` / `video_id`，状态 `processing`。
2. `GET /agnesapi?video_id=<VIDEO_ID>` 轮询查询结果，直到状态变为 `succeeded` / `failed`。
3. 成功后响应中包含视频 URL。

文本模型（同步）与图像模型（同步）无需轮询，但视频模型必须设计轮询机制。

### 16.2 任务类型与调用方式

| 任务类型 | 模型 | 调用方式 | 实现 |
| --- | --- | --- | --- |
| 剧本生成 | agnes-2.0-flash | 同步 | `@Async` 直接返回结果 |
| 分镜拆解 | agnes-2.0-flash | 同步 | `@Async` 直接返回结果 |
| 角色抽取 | agnes-2.0-flash | 同步 | `@Async` 直接返回结果 |
| 角色参考图 | agnes-image-2.1-flash | 同步 | `@Async` 直接返回结果 |
| 分镜画面 | agnes-image-2.1-flash | 同步 | `@Async` 直接返回结果 |
| **视频片段** | **agnes-video-v2.0** | **异步** | **创建任务 + 轮询** |
| 最终合成 | FFmpeg | 同步 | `@Async` 直接返回结果 |

### 16.3 视频片段生成流程

```
VideoClipJob（每个分镜一个）
  │
  ├─ 1. 选 Key（见第 15 章）
  ├─ 2. POST /v1/videos 创建任务
  │     body: { model, prompt, image: <frame_image_url> }
  │     响应: { task_id, video_id, status: "processing" }
  │     → 存入 video_clip.provider_task_id = video_id
  │     → task_log.status = RUNNING
  │
  ├─ 3. 轮询循环（Spring Scheduled 或延迟队列）
  │     while (status == "processing" && 未超时):
  │       sleep(pollInterval)         # 默认 10s
  │       GET /agnesapi?video_id=<vid>
  │       更新 task_log.progress
  │
  ├─ 4a. 成功：status == "succeeded"
  │     → 下载视频到 OSS（或直接存 URL）
  │     → video_clip.video_url = <oss_url>
  │     → video_clip.status = SUCCESS
  │     → task_log.status = SUCCESS
  │
  └─ 4b. 失败：status == "failed" 或超时
        → video_clip.status = FAILED
        → task_log.status = FAILED + error_msg
        → 前端可点"重新生成"
```

### 16.4 轮询参数

| 参数 | 默认值 | 说明 |
| --- | --- | --- |
| `pollInterval` | 10s | 轮询间隔 |
| `maxPollDuration` | 5min | 单片段最大等待时长，超时判失败 |
| `maxPollTimes` | 30 | 最大轮询次数（5min / 10s） |

### 16.5 轮询实现方式（M0 推荐）

**方案：Spring `@Scheduled` + 数据库扫描**

```java
// 每 10s 扫描一次 RUNNING 状态的视频任务
@Scheduled(fixedDelay = 10000)
public void pollRunningVideoTasks() {
    List<TaskLog> running = taskLogMapper.selectByBizTypeAndStatus("VIDEO", "RUNNING");
    for (TaskLog task : running) {
        try {
            VideoResult result = agnesClient.getVideoResult(task.getProviderTaskId());
            if ("succeeded".equals(result.getStatus())) {
                // 下载视频到 OSS，更新 video_clip 与 task_log
                handleVideoSuccess(task, result);
            } else if ("failed".equals(result.getStatus())) {
                handleVideoFailure(task, result);
            }
            // processing → 继续等待
        } catch (Exception e) {
            log.warn("轮询视频任务失败: {}", task.getId(), e);
        }
    }
}
```

**优点**：
- 实现简单，无需引入 MQ 或复杂调度。
- 单机部署足够。
- 易于调试。

**后续优化（M2+）**：
- 改用延迟队列（RabbitMQ TTL + DLX 或 Redis ZSet）。
- 改用 Webhook（若 Agnes AI 支持）。

### 16.6 批量视频任务的进度计算

一集 10 分镜 → 10 个 VideoClipJob 并行（受 Key RPM 限制，实际串行/少量并行）。

**前端进度展示**：
```
视频生成进度：3/10 已完成
当前片段：第 4 个（轮询中，已等待 30s）
预计剩余：约 6 分钟
```

进度计算：
- `已完成片段数 / 总片段数 × 100%`
- 单片段内部进度可映射到 0~10%（如轮询 30 次中的第 N 次）。

### 16.7 超时与失败处理

| 场景 | 处理 |
| --- | --- |
| 单片段超 5min 未完成 | 标记 FAILED，提示「生成超时，请重试」 |
| 轮询时 401 | Key DISABLED，切换 Key 重新创建任务 |
| 轮询时 429 | Key COOLED，等待冷却或切换 Key |
| 轮询时 5xx | 指数退避重试 3 次 |
| Agnes AI 返回 failed | 记录 error_msg，标记 FAILED |

### 16.8 任务恢复（服务重启）

服务重启后，`task_log` 中 `RUNNING` 状态的视频任务需要恢复轮询：

- 启动时扫描所有 `biz_type=VIDEO && status=RUNNING` 的任务。
- 重新加入轮询队列。
- 若 `created_at` 距今已超 `maxPollDuration`，直接标记 FAILED（避免无限轮询僵尸任务）。

---

> 本文档为 M0 快速验证阶段产品设计，后续将随开发迭代持续更新。
