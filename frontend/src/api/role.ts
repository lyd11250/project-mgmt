import request from './request'

export interface Role {
  id: number
  code: string
  name: string
}

export interface RoleParams {
  code: string
  name: string
}

/** 角色列表（本租户）。 */
export function listRoles() {
  return request.get<unknown, Role[]>('/roles')
}

export function createRole(data: RoleParams) {
  return request.post<unknown, number>('/roles', data)
}

export function updateRole(id: number, data: RoleParams) {
  return request.put<unknown, void>(`/roles/${id}`, data)
}

export function deleteRole(id: number) {
  return request.delete<unknown, void>(`/roles/${id}`)
}

/** 角色已分配的菜单 id 列表。 */
export function getRoleMenus(id: number) {
  return request.get<unknown, number[]>(`/roles/${id}/menus`)
}

export function assignRoleMenus(id: number, menuIds: number[]) {
  return request.put<unknown, void>(`/roles/${id}/menus`, { menuIds })
}
