// 工具函数集合

/**
 * 把后端返回的相对资源路径拼成完整 URL。
 * 后端静态资源暴露在 /storage 下，origin 默认 http://localhost:8080。
 * - 已是 http/https 完整 URL：原样返回
 * - 以 / 开头：拼接 origin
 * - 其他：前面补 / 后拼接 origin
 */
export function resolveAssetUrl(path: string | null | undefined): string {
  if (!path) return ''
  if (/^https?:\/\//i.test(path)) return path
  const origin = (import.meta.env.VITE_ASSET_ORIGIN || 'http://localhost:8080').replace(/\/$/, '')
  const p = path.startsWith('/') ? path : `/${path}`
  return `${origin}${p}`
}

/** 格式化日期时间（后端返回 yyyy-MM-dd HH:mm:ss） */
export function formatDateTime(value: string | null | undefined): string {
  if (!value) return '-'
  return value
}

/** 画风预设选项（与后端 StylePresets.java 保持一致） */
export const STYLE_PRESETS: { key: string; label: string }[] = [
  { key: 'anime_jp', label: '日漫' },
  { key: 'anime_cn', label: '国漫' },
  { key: 'comic_us', label: '美漫' },
  { key: 'realistic', label: '写实' }
]

/** 把画风 key 翻译成中文标签 */
export function styleLabel(key: string | null | undefined): string {
  if (!key) return '-'
  return STYLE_PRESETS.find((s) => s.key === key)?.label ?? key
}

/** 项目状态翻译 */
export function statusLabel(status: string | null | undefined): string {
  switch (status) {
    case 'DRAFT':
      return '草稿'
    case 'SCRIPTING':
      return '剧本生成中'
    case 'STORYBOARDING':
      return '分镜生成中'
    case 'IMAGE_GENERATING':
      return '画面生成中'
    case 'VIDEO_GENERATING':
      return '视频生成中'
    case 'COMPOSING':
      return '合成中'
    case 'READY':
      return '已完成'
    default:
      return status ?? '-'
  }
}

/** 任务状态翻译 */
export function taskStatusLabel(status: string | null | undefined): string {
  switch (status) {
    case 'PENDING':
      return '排队中'
    case 'RUNNING':
      return '执行中'
    case 'SUCCESS':
      return '成功'
    case 'FAILED':
      return '失败'
    default:
      return status ?? '-'
  }
}

/** 业务类型翻译 */
export function bizTypeLabel(bizType: string | null | undefined): string {
  switch (bizType) {
    case 'SCRIPT':
      return '剧本生成'
    case 'STORYBOARD':
      return '分镜生成'
    case 'CHARACTER_IMG':
      return '角色参考图'
    case 'FRAME':
      return '画面生成'
    case 'VIDEO':
      return '视频生成'
    case 'COMPOSE':
      return '视频合成'
    default:
      return bizType ?? '-'
  }
}
