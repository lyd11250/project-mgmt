import request from './request'

export type MenuType = 'M' | 'C' | 'F'

export interface MenuNode {
  id: number
  parentId: number
  type: MenuType
  name: string
  path?: string
  component?: string
  icon?: string
  perm?: string
  sort?: number
  visible?: number
  status?: number
  children?: MenuNode[]
}

export interface MenuParams {
  parentId?: number
  type: MenuType
  name: string
  path?: string
  component?: string
  icon?: string
  perm?: string
  sort?: number
  visible?: number
  status?: number
}

/** 当前用户导航菜单树（动态导航/路由）。 */
export function getNav() {
  return request.get<unknown, MenuNode[]>('/system/menus/nav')
}

/** 角色分配菜单时的可选菜单树（限当前租户套餐边界）。 */
export function getAssignableMenus() {
  return request.get<unknown, MenuNode[]>('/system/menus/assignable')
}

/** 全量菜单树（菜单管理）。 */
export function getMenuTree() {
  return request.get<unknown, MenuNode[]>('/system/menus')
}

export function createMenu(data: MenuParams) {
  return request.post<unknown, number>('/system/menus', data)
}

export function updateMenu(id: number, data: MenuParams) {
  return request.put<unknown, void>(`/system/menus/${id}`, data)
}

export function deleteMenu(id: number) {
  return request.delete<unknown, void>(`/system/menus/${id}`)
}
