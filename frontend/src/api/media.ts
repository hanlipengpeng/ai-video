// 媒体接口：分镜画面 / 视频片段
import { request, unwrap } from './request'
import type { FrameImage, TaskSubmitResult, VideoClip } from '@/types'

// ====== 分镜画面 ======

/** 整批生成分镜画面（异步） */
export function generateFrames(projectId: number | string) {
  return request
    .post(`/projects/${projectId}/frames/generate`)
    .then(unwrap) as Promise<TaskSubmitResult>
}

/** 单张画面重新生成 */
export function regenerateFrame(storyboardId: number | string) {
  return request
    .post(`/storyboard/${storyboardId}/frame/regenerate`)
    .then(unwrap) as Promise<TaskSubmitResult>
}

/** 获取全部分镜画面 */
export function listFrames(projectId: number | string) {
  return request
    .get(`/projects/${projectId}/frames`)
    .then(unwrap) as Promise<FrameImage[]>
}

// ====== 视频片段 ======

/** 整批生成视频片段（异步） */
export function generateVideos(projectId: number | string) {
  return request
    .post(`/projects/${projectId}/videos/generate`)
    .then(unwrap) as Promise<TaskSubmitResult>
}

/** 单个视频片段重新生成 */
export function regenerateVideo(storyboardId: number | string) {
  return request
    .post(`/storyboard/${storyboardId}/video/regenerate`)
    .then(unwrap) as Promise<TaskSubmitResult>
}

/** 获取全部视频片段 */
export function listVideos(projectId: number | string) {
  return request
    .get(`/projects/${projectId}/videos`)
    .then(unwrap) as Promise<VideoClip[]>
}
