// 任务接口
import { request, unwrap } from './request'
import type { PageResult, TaskLog } from '@/types'

/** 任务详情 */
export function getTask(taskId: number | string) {
  return request.get(`/tasks/${taskId}`).then(unwrap) as Promise<TaskLog>
}

/** 任务列表（分页，可按项目过滤） */
export function listTasks(page = 1, size = 10, projectId?: number | string) {
  return request
    .get('/tasks', { params: { page, size, projectId } })
    .then(unwrap) as Promise<PageResult<TaskLog>>
}

/** 重试任务 */
export function retryTask(taskId: number | string) {
  return request.post(`/tasks/${taskId}/retry`).then(unwrap) as Promise<TaskLog>
}
