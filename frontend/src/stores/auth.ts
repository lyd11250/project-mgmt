import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  login as loginApi,
  logout as logoutApi,
  getMe,
  type LoginParams,
  type MeInfo,
} from '@/api/auth'
import { TOKEN_KEY } from '@/api/request'

/**
 * 鉴权状态：令牌 + 当前用户（角色/权限）。令牌持久化到 localStorage，
 * 用户信息刷新后由 `/auth/me` 重新拉取。
 */
export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) ?? '')
  const user = ref<MeInfo | null>(null)

  const roles = computed(() => user.value?.roles ?? [])
  const permissions = computed(() => user.value?.permissions ?? [])

  function setToken(value: string) {
    token.value = value
    localStorage.setItem(TOKEN_KEY, value)
  }

  async function login(params: LoginParams) {
    const result = await loginApi(params)
    setToken(result.token)
    await fetchMe()
  }

  async function fetchMe() {
    user.value = await getMe()
  }

  async function logout() {
    try {
      await logoutApi()
    } catch {
      // 忽略登出接口异常，本地照常清理
    }
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
  }

  function hasRole(role: string) {
    return roles.value.includes(role)
  }

  function hasPermission(permission: string) {
    return permissions.value.includes('*') || permissions.value.includes(permission)
  }

  return { token, user, roles, permissions, setToken, login, fetchMe, logout, hasRole, hasPermission }
})
