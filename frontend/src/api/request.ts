// axios 实例与统一拦截器
import axios, { AxiosError, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult } from '@/types'

// API 基础路径，默认 /api/v1（由 vite proxy 转发到 http://localhost:8080）
const API_BASE = import.meta.env.VITE_API_BASE || '/api/v1'

export const request = axios.create({
  baseURL: API_BASE,
  timeout: 60_000
})

// ====== 请求拦截器：自动加 Token ======
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ====== 响应拦截器：统一处理 code != 0 / 401 ======
request.interceptors.response.use(
  (response: AxiosResponse<ApiResult>) => {
    const res = response.data
    // 二进制流等非标准响应直接放行
    if (res === null || typeof res !== 'object' || typeof res.code === 'undefined') {
      return response
    }
    if (res.code !== 0) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || `code=${res.code}`))
    }
    return response
  },
  (error: AxiosError<ApiResult>) => {
    const status = error.response?.status
    const msg = error.response?.data?.message
    if (status === 401) {
      // 鉴权失败：清空 token 并跳登录
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      ElMessage.error(msg || '登录已失效，请重新登录')
      // 避免在 /login 页面无限跳转
      if (!location.pathname.startsWith('/login')) {
        location.href = '/login'
      }
    } else if (status !== undefined) {
      ElMessage.error(msg || `请求失败（HTTP ${status}）`)
    } else {
      ElMessage.error(error.message || '网络异常，请稍后重试')
    }
    return Promise.reject(error)
  }
)

/** 取出 data 字段，便于业务层直接拿结果 */
export function unwrap<T>(response: AxiosResponse<ApiResult<T>>): T {
  return response.data.data
}
