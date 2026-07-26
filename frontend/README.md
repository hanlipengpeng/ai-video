# AI 漫剧制作平台 — 前端

Vue 3 + TypeScript + Vite 实现的「AI 漫剧制作网站」前端，对接后端 `http://localhost:8080/api/v1`。

## 技术栈

- Vue 3（Composition API + `<script setup>`）
- TypeScript
- Vite 5
- Vue Router 4
- Pinia
- Axios
- Element Plus（全量引入）+ @element-plus/icons-vue

## 环境要求

- **Node.js 18+**（推荐 18.x / 20.x）
- npm 9+ 或 pnpm / yarn 均可

## 目录结构

```
frontend/
├── index.html              入口 HTML
├── vite.config.ts          Vite 配置（含 /api 与 /storage 代理）
├── tsconfig.json           TS 配置
├── .env.development        开发环境变量（VITE_API_BASE=/api/v1）
├── .env.production         生产环境变量
├── env.d.ts                Vite 环境变量类型
└── src/
    ├── main.ts             应用入口（注册 Element Plus、Pinia、Router）
    ├── App.vue             根组件
    ├── styles.css          全局样式
    ├── api/                接口封装
    │   ├── request.ts      axios 实例 + 拦截器（加 token / code!=0 报错 / 401 跳登录）
    │   ├── auth.ts         注册 / 登录 / 当前用户
    │   ├── project.ts      项目 CRUD
    │   ├── creation.ts     剧本 / 分镜 / 角色
    │   ├── media.ts        画面 / 视频片段
    │   ├── render.ts       合成 / 导出
    │   ├── task.ts         任务查询 / 重试
    │   └── index.ts        统一出口
    ├── router/index.ts     路由 + 登录守卫
    ├── stores/
    │   ├── user.ts         token / username / login / logout
    │   └── task.ts         当前任务进度（每 2s 轮询）
    ├── types/index.ts      全局类型定义
    ├── utils/index.ts      资源 URL 拼接 / 状态翻译
    ├── components/
    │   ├── TaskProgressBar.vue     底部固定任务进度条
    │   ├── StylePresetSelector.vue 画风下拉
    │   ├── StoryboardCard.vue      分镜卡片（可编辑 Prompt/台词/旁白/时长）
    │   ├── FrameImageCard.vue      画面卡片（图片 + 重新生成）
    │   └── VideoClipCard.vue       视频卡片（video + 重新生成）
    └── views/
        ├── LoginView.vue          登录 / 注册（Tab 切换）
        ├── ProjectListView.vue    项目列表 + 新建/删除/分页
        └── StudioView.vue         创作工作台（核心页面）
```

## 路由

| 路径                       | 页面          | 是否需登录 |
| -------------------------- | ------------- | ---------- |
| `/login`                   | 登录 / 注册   | 否         |
| `/`                        | 项目列表      | 是         |
| `/studio/:projectId`       | 创作工作台    | 是         |

未登录访问受保护路由会自动跳转 `/login?redirect=...`。

## 创作工作台 6 步流程

按文档 8.2 布局：顶部栏 + 左侧步骤导航 + 中间主区域 + 底部任务进度条。

1. **原文** — `textarea` 输入小说文本，保存到 `project.sourceText`
2. **剧本** — 显示剧本 JSON（结构化场景卡片），支持「生成剧本」「编辑/保存」
3. **分镜** — 分镜卡片列表（可编辑 Prompt/台词/旁白/时长）+ 「生成分镜」「抽取角色」+ 角色列表（含生成参考图）
4. **画面** — 网格显示每分镜一张图 + 「批量生成画面」+ 单张「重新生成」
5. **视频** — 网格显示每分镜一段 video + 「批量生成视频」+ 单个「重新生成」
6. **导出** — 「合成视频」按钮，完成后显示 video 预览 + 下载

底部 `TaskProgressBar` 每 2 秒轮询 `/tasks/{id}`，展示 status 与 progress，任务完成后自动刷新对应数据。

## 资源 URL 处理

后端返回的图片/视频是相对路径（如 `/storage/xxx.png`），前端通过 `src/utils/index.ts` 的 `resolveAssetUrl()` 拼接为 `http://localhost:8080 + path`。开发环境同时通过 Vite proxy 把 `/storage` 转发到后端。

## 开发与构建

```bash
# 1. 安装依赖
npm install

# 2. 启动开发服务器（默认 http://localhost:5173）
npm run dev

# 3. 类型检查 + 生产构建
npm run build

# 4. 预览生产构建产物
npm run preview
```

## 联调后端

1. 先启动后端（监听 `http://localhost:8080`，接口前缀 `/api/v1`）。
2. 启动本项目 `npm run dev`，Vite 会把：
   - `/api/**` → `http://localhost:8080`
   - `/storage/**` → `http://localhost:8080`
3. 注册账号 → 登录 → 新建项目 → 进入工作台开始创作。

## 环境变量

| 变量                | 默认值                  | 说明                                  |
| ------------------- | ----------------------- | ------------------------------------- |
| `VITE_API_BASE`     | `/api/v1`               | 接口基础路径（开发由 proxy 转发）     |
| `VITE_ASSET_ORIGIN` | `http://localhost:8080` | 拼接资源完整 URL 用的后端 origin      |

生产部署时把这两个变量改成真实后端地址即可。
