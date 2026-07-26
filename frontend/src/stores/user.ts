// 用户状态
import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as authApi from '@/api/auth'
import type { User } from '@/types'

const TOKEN_KEY = 'token'
const USERNAME_KEY = 'username'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
  const username = ref<string>(localStorage.getItem(USERNAME_KEY) || '')
  const userId = ref<number | null>(null)

  /** 是否已登录 */
  const isLogin = () => !!token.value

  /** 登录 */
  async function login(user: string, password: string) {
    const res = await authApi.login(user, password)
    token.value = res.token
    username.value = res.username
    userId.value = res.userId
    localStorage.setItem(TOKEN_KEY, res.token)
    localStorage.setItem(USERNAME_KEY, res.username)
  }

  /** 注册 */
  async function register(user: string, password: string) {
    return authApi.register(user, password)
  }

  /** 退出：仅丢弃本地 token */
  function logout() {
    token.value = ''
    username.value = ''
    userId.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USERNAME_KEY)
  }

  /** 拉取当前用户信息（用于刷新页面后恢复） */
  async function fetchMe() {
    try {
      const me: User = await authApi.fetchMe()
      username.value = me.username
      userId.value = me.id
      localStorage.setItem(USERNAME_KEY, me.username)
      return me
    } catch {
      logout()
      return null
    }
  }

  return { token, username, userId, isLogin, login, register, logout, fetchMe }
})
