import axios, { type AxiosInstance } from 'axios'
import { ElMessage } from 'element-plus'

/** Sa-token 令牌在前端的存储键（与后端 token-name 对应）。 */
export const TOKEN_KEY = 'satoken'

/** 后端统一响应体。 */
export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
}

const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000,
})

// 请求拦截：附加令牌
request.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.set(TOKEN_KEY, token)
  }
  return config
})

// 响应拦截：解包 { code, message, data }，统一错误处理
request.interceptors.response.use(
  (response): any => {
    const body = response.data as ApiResult
    if (body.code === 0) {
      return body.data
    }
    if (body.code === 401) {
      localStorage.removeItem(TOKEN_KEY)
      window.location.href = '/login'
    }
    ElMessage.error(body.message || '请求失败')
    return Promise.reject(new Error(body.message || '请求失败'))
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      window.location.href = '/login'
    }
    ElMessage.error(error.message || '网络异常')
    return Promise.reject(error)
  },
)

export default request
