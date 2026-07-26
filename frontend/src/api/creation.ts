// 创作接口：剧本 / 分镜 / 角色
import { request, unwrap } from './request'
import type {
  Character,
  Storyboard,
  TaskSubmitResult
} from '@/types'

// ====== 剧本 ======

/** 生成剧本（异步，返回 taskId） */
export function generateScript(projectId: number | string) {
  return request
    .post(`/projects/${projectId}/script/generate`)
    .then(unwrap) as Promise<TaskSubmitResult>
}

/** 更新剧本 JSON（scriptContent 为字符串） */
export function saveScript(projectId: number | string, scriptContent: string) {
  return request
    .put(`/projects/${projectId}/script`, { script: scriptContent })
    .then(unwrap) as Promise<void>
}

// ====== 分镜 ======

/** 生成分镜（异步，返回 taskId） */
export function generateStoryboard(projectId: number | string) {
  return request
    .post(`/projects/${projectId}/storyboard/generate`)
    .then(unwrap) as Promise<TaskSubmitResult>
}

/** 获取分镜列表 */
export function listStoryboards(projectId: number | string) {
  return request
    .get(`/projects/${projectId}/storyboard`)
    .then(unwrap) as Promise<Storyboard[]>
}

/** 更新单个分镜 */
export function updateStoryboard(
  id: number | string,
  payload: {
    prompt?: string
    dialogue?: string
    narration?: string
    durationSec?: number
    characterIds?: string
  }
) {
  return request
    .put(`/storyboard/${id}`, payload)
    .then(unwrap) as Promise<Storyboard>
}

// ====== 角色 ======

/** 抽取角色（异步，返回 taskId） */
export function extractCharacters(projectId: number | string) {
  return request
    .post(`/projects/${projectId}/characters/extract`)
    .then(unwrap) as Promise<TaskSubmitResult>
}

/** 角色列表 */
export function listCharacters(projectId: number | string) {
  return request
    .get(`/projects/${projectId}/characters`)
    .then(unwrap) as Promise<Character[]>
}

/** 更新角色 */
export function updateCharacter(
  id: number | string,
  payload: {
    name?: string
    aliases?: string
    appearance?: string
    personality?: string
  }
) {
  return request.put(`/characters/${id}`, payload).then(unwrap) as Promise<Character>
}

/** 生成角色参考图（异步，返回 taskId） */
export function generateCharacterImage(characterId: number | string) {
  return request
    .post(`/characters/${characterId}/image/generate`)
    .then(unwrap) as Promise<TaskSubmitResult>
}
