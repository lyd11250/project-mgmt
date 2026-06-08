import request from './request'
import type { Role } from './role'

/** MyBatis-Plus 分页结果。 */
export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

export interface UserItem {
  id: number
  username: string
  status: number
  personId?: number
  roles: Role[]
  createdAt: string
}

export interface UserCreateParams {
  username: string
  password: string
  personId?: number
  roleIds?: number[]
}

export function pageUsers(params: { page?: number; size?: number; username?: string }) {
  return request.get<unknown, PageResult<UserItem>>('/users', { params })
}

export function createUser(data: UserCreateParams) {
  return request.post<unknown, number>('/users', data)
}

export function updateUser(id: number, data: { status?: number; personId?: number }) {
  return request.put<unknown, void>(`/users/${id}`, data)
}

export function deleteUser(id: number) {
  return request.delete<unknown, void>(`/users/${id}`)
}

export function resetPassword(id: number, password: string) {
  return request.put<unknown, void>(`/users/${id}/password`, { password })
}

export function assignRoles(id: number, roleIds: number[]) {
  return request.put<unknown, void>(`/users/${id}/roles`, { roleIds })
}
