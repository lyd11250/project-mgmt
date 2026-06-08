import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, type LoginParams } from '@/api/auth'
import { TOKEN_KEY } from '@/api/request'

/**
 * 鉴权状态。登录逻辑的后端接口将在第 1 期实现，本期为可用占位。
 */
export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) ?? '')

  function setToken(value: string) {
    token.value = value
    localStorage.setItem(TOKEN_KEY, value)
  }

  async function login(params: LoginParams) {
    const result = await loginApi(params)
    setToken(result.token)
  }

  function logout() {
    token.value = ''
    localStorage.removeItem(TOKEN_KEY)
  }

  return { token, setToken, login, logout }
})
