// 全局类型定义，与后端实体保持一致（字段均为 camelCase）

/** 统一响应结构 */
export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
}

/** 分页响应 */
export interface PageResult<T> {
  page: number
  size: number
  total: number
  list: T[]
}

/** 画风预设 key（与后端 StylePresets 对应） */
export type StylePreset = 'anime_jp' | 'anime_cn' | 'comic_us' | 'realistic'

/** 项目状态 */
export type ProjectStatus =
  | 'DRAFT'
  | 'SCRIPTING'
  | 'STORYBOARDING'
  | 'IMAGE_GENERATING'
  | 'VIDEO_GENERATING'
  | 'COMPOSING'
  | 'READY'

/** 任务状态 */
export type TaskStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED'

/** 业务类型 */
export type BizType =
  | 'SCRIPT'
  | 'STORYBOARD'
  | 'CHARACTER_IMG'
  | 'FRAME'
  | 'VIDEO'
  | 'COMPOSE'

/** 用户 */
export interface User {
  id: number
  username: string
}

/** 登录返回 */
export interface LoginResult {
  token: string
  userId: number
  username: string
}

/** 项目 */
export interface Project {
  id: number
  userId: number
  title: string
  stylePreset: string
  aspectRatio: string
  status: ProjectStatus
  sourceText: string | null
  scriptContent: string | null
  finalVideoUrl: string | null
  durationSec: number | null
  createdAt: string
  updatedAt: string
}

/** 分镜 */
export interface Storyboard {
  id: number
  projectId: number
  seq: number
  prompt: string
  dialogue: string | null
  narration: string | null
  durationSec: number | null
  characterIds: string | null
  sceneRefId: number | null
  createdAt: string
}

/** 角色 */
export interface Character {
  id: number
  projectId: number
  name: string
  aliases: string | null
  appearance: string | null
  personality: string | null
  referenceImageUrl: string | null
  status: 'PENDING' | 'READY'
  createdAt: string
  updatedAt: string
}

/** 分镜画面 */
export interface FrameImage {
  id: number
  storyboardId: number
  imageUrl: string | null
  prompt: string | null
  seed: number | null
  referenceImageUrls: string | null
  status: 'GENERATING' | 'SUCCESS' | 'FAILED'
  createdAt: string
}

/** 视频片段 */
export interface VideoClip {
  id: number
  storyboardId: number
  frameImageId: number | null
  videoUrl: string | null
  durationSec: number | null
  status: 'GENERATING' | 'SUCCESS' | 'FAILED'
  createdAt: string
}

/** 任务日志 */
export interface TaskLog {
  id: number
  userId: number
  projectId: number | null
  bizType: BizType
  bizId: number | null
  model: string | null
  apiKeyId: number | null
  providerTaskId: string | null
  status: TaskStatus
  progress: number
  input: string | null
  output: string | null
  errorMsg: string | null
  startedAt: string | null
  finishedAt: string | null
  createdAt: string
}

/** 创建/更新项目入参 */
export interface ProjectInput {
  title?: string
  stylePreset?: string
  aspectRatio?: string
  sourceText?: string
}

/** 异步任务提交后后端返回 */
export interface TaskSubmitResult {
  taskId: number
  status: TaskStatus
}

/** 导出接口返回 */
export interface ExportResult {
  videoUrl: string
  durationSec: number | null
  status: ProjectStatus
}

/** 剧本 JSON 结构（附录 A） */
export interface ScriptScene {
  heading: string
  description: string
  dialogues: { character: string; line: string }[]
  narration?: string
}

export interface ScriptContent {
  title?: string
  scenes: ScriptScene[]
}

/** 画风预设元数据（用于下拉展示） */
export interface StylePresetOption {
  key: StylePreset
  label: string
}
