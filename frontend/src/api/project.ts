// 项目接口
import { request, unwrap } from './request'
import type { PageResult, Project, ProjectInput } from '@/types'

/** 创建项目 */
export function createProject(input: ProjectInput) {
  return request.post('/projects', input).then(unwrap) as Promise<Project>
}

/** 项目列表（分页） */
export function listProjects(page = 1, size = 10) {
  return request
    .get('/projects', { params: { page, size } })
    .then(unwrap) as Promise<PageResult<Project>>
}

/** 项目详情 */
export function getProject(id: number | string) {
  return request.get(`/projects/${id}`).then(unwrap) as Promise<Project>
}

/** 更新项目 */
export function updateProject(id: number | string, input: ProjectInput) {
  return request.put(`/projects/${id}`, input).then(unwrap) as Promise<Project>
}

/** 删除项目 */
export function deleteProject(id: number | string) {
  return request.delete(`/projects/${id}`).then(unwrap) as Promise<void>
}
