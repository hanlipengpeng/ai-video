// 账户接口（注册 / 登录 / 当前用户）
import { request, unwrap } from './request'
import type { LoginResult, User } from '@/types'

/** 注册 */
export function register(username: string, password: string) {
  return request
    .post('/auth/register', { username, password })
    .then(unwrap) as Promise<{ id: number; username: string }>
}

/** 登录，返回 JWT */
export function login(username: string, password: string) {
  return request
    .post('/auth/login', { username, password })
    .then(unwrap) as Promise<LoginResult>
}

/** 当前用户信息 */
export function fetchMe() {
  return request.get('/users/me').then(unwrap) as Promise<User>
}
