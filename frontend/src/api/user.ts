import request from './request'
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
  status: number
  personId?: string
  roles: Role[]
  createdAt: string
}

export interface UserCreateParams {
  username: string
  password: string
  personId?: string
  roleIds?: string[]
}

export function pageUsers(params: { page?: number; size?: number; username?: string }) {
  return request.get<unknown, PageResult<UserItem>>('/system/users', { params })
}

export function createUser(data: UserCreateParams) {
  return request.post<unknown, string>('/system/users', data)
}

export function updateUser(id: string, data: { status?: number; personId?: string }) {
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
