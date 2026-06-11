import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  login as loginApi,
  logout as logoutApi,
  getMe,
  type LoginParams,
  type MeInfo,
} from '@/api/auth'
import { getNav, type MenuNode } from '@/api/menu'
import { fetchAvatarObjectUrl } from '@/api/user'
import { TOKEN_KEY } from '@/api/request'

/**
 * 鉴权状态：令牌 + 当前用户（角色/权限）+ 导航菜单。令牌持久化到 localStorage，
 * 用户信息与菜单刷新后由 `/auth/me`、`/system/menus/nav` 重新拉取。
 */
export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) ?? '')
  const user = ref<MeInfo | null>(null)
  const menus = ref<MenuNode[]>([])
  // 顶栏头像的 objectURL（带令牌拉取后生成）；无头像或拉取失败为空。
  const avatarObjectUrl = ref<string>('')

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

  /** 拉取当前用户信息与导航菜单，并刷新顶栏头像。 */
  async function fetchMe() {
    user.value = await getMe()
    menus.value = await getNav()
    await loadAvatar()
  }

  /** 据当前用户的 avatarFileId 拉取头像 objectURL（替换旧的并释放）。 */
  async function loadAvatar() {
    revokeAvatar()
    if (user.value?.avatarFileId) {
      avatarObjectUrl.value = (await fetchAvatarObjectUrl(user.value.userId)) ?? ''
    }
  }

  function revokeAvatar() {
    if (avatarObjectUrl.value) {
      URL.revokeObjectURL(avatarObjectUrl.value)
      avatarObjectUrl.value = ''
    }
  }

  async function logout() {
    try {
      await logoutApi()
    } catch {
      // 忽略登出接口异常，本地照常清理
    }
    token.value = ''
    user.value = null
    menus.value = []
    revokeAvatar()
    localStorage.removeItem(TOKEN_KEY)
  }

  function hasRole(role: string) {
    return roles.value.includes(role)
  }

  function hasPermission(permission: string) {
    return permissions.value.includes('*') || permissions.value.includes(permission)
  }

  return {
    token,
    user,
    menus,
    avatarObjectUrl,
    roles,
    permissions,
    setToken,
    login,
    fetchMe,
    loadAvatar,
    logout,
    hasRole,
    hasPermission,
  }
})
