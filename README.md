# AI 漫剧制作网站

> 将小说一键转化为 AI 漫剧视频的全栈应用。基于 Spring Boot 3 + Vue 3，对接 Agnes AI 多模态模型（文本 / 图像 / 视频）。
>
> 对应产品设计文档：[AI漫剧制作网站产品设计文档.md](./AI漫剧制作网站产品设计文档.md)（v1.3，M0 快速验证版）

## 功能一览

- 用户名 + 密码登录注册（M0 简化）
- 创建项目，选择画风（日漫 / 国漫 / 美漫 / 写实）
- 粘贴小说原文 → AI 生成结构化剧本
- AI 拆解分镜 + 抽取角色清单
- 生成角色标准参考图（保证角色一致性）
- 批量生成分镜画面（图生图，引用角色参考图）
- 批量生成视频片段（Agnes Video V2.0 图生视频，原生音画同步）
- FFmpeg 拼接 + 字幕烧录，输出最终 mp4

## 技术栈

| 层 | 技术 |
| --- | --- |
| 前端 | Vue 3 + TypeScript + Vite + Element Plus + Pinia |
| 后端 | Spring Boot 3.3 + Java 17 + MyBatis-Plus + Spring Security + JWT |
| 数据库 | MySQL 8 |
| AI 模型 | Agnes AI（兼容 OpenAI 协议）：agnes-2.0-flash / agnes-image-2.1-flash / agnes-video-v2.0 |
| 视频处理 | FFmpeg |
| 部署 | Docker Compose |

## 目录结构

```
.
├── AI漫剧制作网站产品设计文档.md   # 完整产品设计（16 章 + 附录）
├── docker-compose.yml              # 一键部署
├── .env.example                    # 环境变量模板（Agnes Key 等）
├── backend/                        # Spring Boot 后端
│   ├── Dockerfile
│   ├── pom.xml
│   ├── README.md
│   └── src/main/
│       ├── java/com/ai/comic/      # 9 个模块（account/project/creation/media/render/task/aigc/common/config）
│       └── resources/
│           ├── application.yml
│           └── db/schema.sql       # 8 张表 DDL
└── frontend/                       # Vue 3 前端
    ├── Dockerfile
    ├── package.json
    ├── vite.config.ts
    └── src/
        ├── api/                    # 接口封装
        ├── views/                  # 3 个页面（登录/项目列表/创作工作台）
        ├── components/             # 5 个通用组件
        ├── stores/                 # Pinia（user/task）
        └── router/
```

## 快速开始

### 方式一：Docker Compose 一键启动（推荐）

**前置**：安装 Docker 与 Docker Compose。

1. 复制环境变量模板并填入 Agnes AI Key：

   ```bash
   cp .env.example .env
   # 编辑 .env，填入你的 AGNES_KEY_01 / AGNES_KEY_02
   ```

   > Agnes AI Key 申请：https://platform.agnes-ai.com/
   > 建议至少配置 2 个 Token Plan Key（视频 5 RPM/Key），见文档第 15.7 章。

2. 一键启动：

   ```bash
   docker compose up -d --build
   ```

3. 访问：
   - 前端：http://localhost:5173
   - 后端 Swagger：http://localhost:8080/swagger-ui.html

4. 查看日志：

   ```bash
   docker compose logs -f backend
   ```

5. 停止：

   ```bash
   docker compose down
   ```

### 方式二：本地开发模式

**环境要求**：JDK 17、Node 18+、MySQL 8、FFmpeg。

1. **启动 MySQL 并建库**：

   ```bash
   mysql -u root -p < backend/src/main/resources/db/schema.sql
   ```

2. **启动后端**：

   ```bash
   cd backend
   export MYSQL_HOST=127.0.0.1 MYSQL_USER=root MYSQL_PASSWORD=your_password
   export AGNES_KEY_01=sk-xxxx
   mvn spring-boot:run
   ```

   后端监听 http://localhost:8080，详见 [backend/README.md](./backend/README.md)。

3. **启动前端**：

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

   前端监听 http://localhost:5173，详见 [frontend/README.md](./frontend/README.md)。

## 核心设计要点

### Agnes AI 多 Key 轮询（文档第 15 章）

- 按模型类型（TEXT/IMAGE/VIDEO）分别维护 RPM 计数
- 收到 429 自动冷却 60s 并切换 Key 重试
- 容量规划：2 个 Token Plan Key 可让 10 分镜的视频步骤在 1 分钟内完成

### 角色一致性方案 B（文档第 14 章）

1. 角色抽取 → 生成标准参考图（半身/正面/纯色背景，1K/1:1）
2. 分镜画面生成时，将角色参考图作为 `image` 数组传入 Agnes Image 2.1 Flash（图生图）
3. Prompt 同时写入角色描述（双保险），单张分镜引用不超过 3 个角色参考图

### 视频异步轮询（文档第 16 章）

- Agnes Video V2.0 为异步 API，调用 `POST /v1/videos` 返回 video_id
- `VideoPoller` 每 10s 扫描 RUNNING 状态的视频任务，调用 `GET /agnesapi?video_id=xxx` 查询
- 单片段超 5min 标记 FAILED，服务重启自动恢复轮询

## 典型使用流程

```
1. 注册/登录
2. 新建项目（选画风）
3. 粘贴小说原文
4. 生成剧本（AI）
5. 生成分镜 + 抽取角色（AI）
6. 生成角色参考图（AI）
7. 批量生成分镜画面（AI 图生图）
8. 批量生成视频片段（AI 图生视频）
9. 合成最终视频（FFmpeg）
10. 下载 mp4
```

整个流程在创作工作台一个页面内完成（左侧 6 步导航 + 中间主区域 + 底部任务进度条）。

## 验收清单

详见产品设计文档 [附录 C](./AI漫剧制作网站产品设计文档.md)，共 19 条验收项，覆盖登录、创作全流程、一致性、多 Key、异步轮询、超时处理等。

## License

MIT
