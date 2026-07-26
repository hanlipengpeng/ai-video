// 渲染导出接口
import { request, unwrap } from './request'
import type { ExportResult, TaskSubmitResult } from '@/types'

/** 合成最终视频（异步） */
export function composeVideo(projectId: number | string) {
  return request
    .post(`/projects/${projectId}/compose`)
    .then(unwrap) as Promise<TaskSubmitResult>
}

/** 获取最终视频下载链接 */
export function exportVideo(projectId: number | string) {
  return request
    .get(`/projects/${projectId}/export`)
    .then(unwrap) as Promise<ExportResult>
}
