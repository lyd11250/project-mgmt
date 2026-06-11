import axios from 'axios'
import request, { TOKEN_KEY } from './request'
import type { Role } from './role'

/**
 * MyBatis-Plus 分页结果。
 * 注意：后端雪花主键(Long)以字符串下发以避免 JS 精度丢失，故各实体 id 为 string；
 * total/current/size 为原始 long/int，仍是数字。
 */
export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

export interface UserItem {
  id: string
  username: string
  nickname?: string
  phone?: string
  status: number
  roles: Role[]
  createdAt: string
}

export interface UserCreateParams {
  username: string
  password: string
  nickname?: string
  phone?: string
  roleIds?: string[]
}

/** 个人中心资料。 */
export interface ProfileInfo {
  id: string
  username: string
  nickname?: string
  phone?: string
  tenantId: string
  avatarFileId?: string
  roles: Role[]
}

export function pageUsers(params: { page?: number; size?: number; username?: string }) {
  return request.get<unknown, PageResult<UserItem>>('/system/users', { params })
}

export function createUser(data: UserCreateParams) {
  return request.post<unknown, string>('/system/users', data)
}

export function updateUser(id: string, data: { status?: number }) {
  return request.put<unknown, void>(`/system/users/${id}`, data)
}

export function deleteUser(id: string) {
  return request.delete<unknown, void>(`/system/users/${id}`)
}

export function resetPassword(id: string, password: string) {
  return request.put<unknown, void>(`/system/users/${id}/password`, { password })
}

export function assignRoles(id: string, roleIds: string[]) {
  return request.put<unknown, void>(`/system/users/${id}/roles`, { roleIds })
}

// ---- 自助（当前登录用户）----

/** 查询个人中心资料。 */
export function getProfile() {
  return request.get<unknown, ProfileInfo>('/system/users/me/profile')
}

/** 更新个人资料（昵称、手机号）。 */
export function updateProfile(data: { nickname?: string; phone?: string }) {
  return request.put<unknown, void>('/system/users/me/profile', data)
}

/** 修改自身密码（需原密码）。 */
export function changePassword(data: { oldPassword: string; newPassword: string }) {
  return request.put<unknown, void>('/system/users/me/password', data)
}

/** 上传/更换当前用户头像，返回新头像文件 id。 */
export function uploadAvatar(file: File) {
  const fd = new FormData()
  fd.append('file', file)
  return request.put<unknown, string>('/system/users/me/avatar', fd)
}

// 头像读取专用裸实例：响应是图片二进制流，不能复用 request 的「按 code 解包」拦截器；
// 仅附加令牌（<img> 直链无法携带请求头，故必须 fetch 成 blob 再转 objectURL）。
const rawAvatar = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30000,
  responseType: 'blob',
})
rawAvatar.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) config.headers.set(TOKEN_KEY, token)
  return config
})

/**
 * 拉取指定用户头像并转为可直接用于 &lt;img src&gt; 的 objectURL。
 * 失败（如未设置头像 404）返回 null，由调用方回退默认头像。
 * 调用方在替换/卸载时应 URL.revokeObjectURL 释放。
 */
export async function fetchAvatarObjectUrl(userId: string): Promise<string | null> {
  try {
    const res = await rawAvatar.get(`/system/users/${userId}/avatar`)
    return URL.createObjectURL(res.data as Blob)
  } catch {
    return null
  }
}
