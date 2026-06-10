import request from './request'

export interface Role {
  id: string
  code: string
  name: string
}

export interface RoleParams {
  code: string
  name: string
}

/** 角色列表（本租户）。 */
export function listRoles() {
  return request.get<unknown, Role[]>('/system/roles')
}

export function createRole(data: RoleParams) {
  return request.post<unknown, string>('/system/roles', data)
}

export function updateRole(id: string, data: RoleParams) {
  return request.put<unknown, void>(`/system/roles/${id}`, data)
}

export function deleteRole(id: string) {
  return request.delete<unknown, void>(`/system/roles/${id}`)
}

/** 角色已分配的菜单 id 列表。 */
export function getRoleMenus(id: string) {
  return request.get<unknown, string[]>(`/system/roles/${id}/menus`)
}

export function assignRoleMenus(id: string, menuIds: string[]) {
  return request.put<unknown, void>(`/system/roles/${id}/menus`, { menuIds })
}
