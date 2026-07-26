# AI 漫剧制作网站后端

基于 Spring Boot 3 + Java 17 的 AI 漫剧制作网站后端，对接 Agnes AI 多模态模型，实现「小说 → 剧本 → 分镜 → 画面 → 视频」全链路自动化生成。

> 对应产品设计文档 v1.3（M0 快速验证版）。

## 技术栈

| 类别 | 选型 |
| --- | --- |
| 语言/框架 | Java 17 + Spring Boot 3.3.4 |
| ORM | MyBatis-Plus 3.5.7 |
| 数据库 | MySQL 8 |
| 鉴权 | Spring Security + JWT (jjwt 0.12.6) |
| AI 调用 | Spring WebFlux WebClient（OpenAI 兼容协议） |
| 视频处理 | FFmpeg（ProcessBuilder 调用） |
| 接口文档 | SpringDoc OpenAPI 3 (swagger-ui) |
| 存储 | 本地磁盘（M0 替代 OSS） |

## 环境要求

- **JDK 17**（推荐 Eclipse Temurin 17）
- **MySQL 8**（字符集 utf8mb4）
- **FFmpeg**（需在 PATH 中，或通过 `FFMPEG_PATH` 环境变量指定路径）
- **Maven 3.8+**

## 目录结构

```
backend/
├── pom.xml
├── README.md
└── src/main/
    ├── java/com/ai/comic/
    │   ├── ComicApplication.java          # 启动类（@EnableAsync + @EnableScheduling）
    │   ├── account/                       # 账户模块（注册/登录/JWT）
    │   ├── project/                       # 项目管理（CRUD + 状态机）
    │   ├── creation/                      # 创作模块（剧本/分镜/角色 + Prompt 拼接）
    │   ├── media/                         # 媒体生成（分镜画面 + 视频片段）
    │   ├── render/                        # 渲染合成（FFmpeg + SRT 字幕）
    │   ├── task/                          # 任务中心（异步任务日志）
    │   ├── aigc/                          # AIGC 接入层（KeyPool + Text/Image/Video Client + VideoPoller）
    │   ├── common/                        # 通用（Result/异常/分页/存储）
    │   └── config/                        # 配置（Security/Async/Web/MyBatis/JWT/Storage）
    └── resources/
        ├── application.yml                # 主配置
        └── db/
            └── schema.sql                 # 数据库 DDL（7 张表）
```

## 快速开始

### 1. 创建数据库

登录 MySQL，执行 DDL：

```bash
mysql -u root -p < src/main/resources/db/schema.sql
```

或手动在 MySQL 客户端中执行 `src/main/resources/db/schema.sql`，会自动创建 `comic_db` 数据库及 7 张表（user / project / storyboard / character / frame_image / video_clip / task_log / api_key）。

> 画风预设（日漫/国漫/美漫/写实）硬编码在 `StylePresets.java`，无需建表。

### 2. 配置 Agnes AI Key

在 `application.yml` 中已预置 3 个 Key 槽位，通过环境变量注入：

```bash
export AGNES_KEY_01="你的 Agnes API Key 1"   # token-plan
export AGNES_KEY_02="你的 Agnes API Key 2"   # token-plan
export AGNES_KEY_03="你的 Agnes API Key 3"   # free（仅文本/图像）
```

> 也可直接在 `application.yml` 的 `agnes.ai.keys[].api-key` 中填写。
> 启动时若 `api_key` 表为空，会自动将配置中的 Key 写入表中；若表中已有数据则优先使用表数据。
> **建议至少配置 2 个 Token Plan Key**（视频 5 RPM/Key），见文档第 15.7 章。

### 3. 配置数据库连接

通过环境变量或修改 `application.yml`：

```bash
export MYSQL_HOST=127.0.0.1
export MYSQL_PORT=3306
export MYSQL_DB=comic_db
export MYSQL_USER=root
export MYSQL_PASSWORD=your_password
```

### 4. 配置存储路径

```bash
export COMIC_STORAGE_PATH=/data/comic-storage   # 默认值
export FFMPEG_PATH=ffmpeg                        # 若 ffmpeg 不在 PATH
```

确保存储目录存在且有写权限：

```bash
mkdir -p /data/comic-storage
```

### 5. 编译与启动

```bash
# 编译
mvn -DskipTests compile

# 打包
mvn -DskipTests package

# 启动
java -jar target/comic-backend-0.0.1-SNAPSHOT.jar
```

或开发模式直接运行：

```bash
mvn spring-boot:run
```

启动后服务监听 `http://localhost:8080`。

### 6. 查看接口文档

启动后访问 Swagger UI：

```
http://localhost:8080/swagger-ui.html
```

## API 接口概览

所有接口前缀 `/api/v1`，需 Bearer Token 鉴权（除 `/auth/**`）。

| 模块 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 认证 | POST | `/auth/register` | 注册 |
| 认证 | POST | `/auth/login` | 登录，返回 JWT |
| 认证 | GET | `/users/me` | 当前用户 |
| 项目 | POST/GET/PUT/DELETE | `/projects` | 项目 CRUD |
| 剧本 | POST | `/projects/{id}/script/generate` | 生成剧本（异步） |
| 剧本 | PUT | `/projects/{id}/script` | 更新剧本 |
| 分镜 | POST | `/projects/{id}/storyboard/generate` | 生成分镜（异步） |
| 分镜 | GET | `/projects/{id}/storyboard` | 分镜列表 |
| 分镜 | PUT | `/storyboard/{id}` | 更新分镜 |
| 角色 | POST | `/projects/{id}/characters/extract` | 抽取角色（异步） |
| 角色 | GET | `/projects/{id}/characters` | 角色列表 |
| 角色 | PUT | `/characters/{id}` | 更新角色 |
| 角色 | POST | `/characters/{id}/image/generate` | 生成角色参考图（异步） |
| 画面 | POST | `/projects/{id}/frames/generate` | 批量生成画面（异步） |
| 画面 | POST | `/storyboard/{id}/frame/regenerate` | 单张重新生成 |
| 画面 | GET | `/projects/{id}/frames` | 画面列表 |
| 视频 | POST | `/projects/{id}/videos/generate` | 批量生成视频（异步） |
| 视频 | POST | `/storyboard/{id}/video/regenerate` | 单个重新生成 |
| 视频 | GET | `/projects/{id}/videos` | 视频列表 |
| 渲染 | POST | `/projects/{id}/compose` | 合成最终视频（异步） |
| 渲染 | GET | `/projects/{id}/export` | 获取下载链接 |
| 任务 | GET | `/tasks/{id}` | 任务详情 |
| 任务 | GET | `/tasks` | 任务列表（分页） |
| 任务 | POST | `/tasks/{id}/retry` | 重试任务 |
| 资源 | GET | `/resources/styles` | 画风预设列表 |

## 核心设计说明

### Agnes AI 接入层（文档第 13~16 章）

- **KeyPool**：多 Key 轮询池，按模型类型（TEXT/IMAGE/VIDEO）分别计数 RPM，429 自动冷却 60s 并切换 Key 重试。
- **AgnesTextClient**：调用 `/v1/chat/completions`（同步），用于剧本/分镜/角色抽取。
- **AgnesImageClient**：调用 `/v1/images/generations`（同步），支持文生图与图生图（角色一致性方案 B）。
- **AgnesVideoClient**：调用 `/v1/videos` 创建异步任务，`GET /agnesapi?video_id=xxx` 轮询结果。
- **VideoPoller**：`@Scheduled(fixedDelay=10000)` 扫描 RUNNING 视频任务轮询，超时 5min 标记 FAILED，服务重启自动恢复。

### 角色一致性方案（文档第 14 章）

1. 角色抽取 → 生成标准参考图（半身/正面/纯色背景，1K/1:1）
2. 分镜画面生成时，将角色参考图作为 `image` 数组传入（图生图），prompt 同时写入角色描述（双保险）
3. 单张分镜引用不超过 3 个角色参考图

### 任务编排

所有 AI 任务异步执行（`@Async` + 线程池，核心 3 / 最大 5），进度记录到 `task_log` 表，前端轮询 `/tasks/{id}` 获取进度。

项目状态机：`DRAFT → SCRIPTING → STORYBOARDING → IMAGE_GENERATING → VIDEO_GENERATING → COMPOSING → READY`

## 典型使用流程

```
1. POST /auth/register → 注册
2. POST /auth/login    → 获取 JWT
3. POST /projects      → 创建项目（选画风 anime_jp/anime_cn/comic_us/realistic）
4. PUT  /projects/{id} → 粘贴小说原文
5. POST /projects/{id}/script/generate      → 生成剧本（轮询 tasks/{id}）
6. POST /projects/{id}/storyboard/generate   → 生成分镜
7. POST /projects/{id}/characters/extract    → 抽取角色
8. POST /characters/{id}/image/generate      → 生成角色参考图（每个角色）
9. POST /projects/{id}/frames/generate       → 批量生成分镜画面
10. POST /projects/{id}/videos/generate      → 批量生成视频片段
11. POST /projects/{id}/compose              → FFmpeg 合成最终视频
12. GET  /projects/{id}/export               → 获取下载链接
```
