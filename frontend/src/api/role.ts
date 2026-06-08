import request from './request'

export interface Role {
  id: number
  code: string
  name: string
}

/** 角色列表（本租户，供分配角色使用）。 */
export function listRoles() {
  return request.get<unknown, Role[]>('/roles')
}
