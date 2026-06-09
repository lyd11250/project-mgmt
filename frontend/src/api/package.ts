import request from './request'

export interface PackageItem {
  id: string
  name: string
  code: string
  status: number
  remark?: string
}

export interface PackageParams {
  name: string
  code: string
  status?: number
  remark?: string
}

export function listPackages() {
  return request.get<unknown, PackageItem[]>('/system/packages')
}

export function getPackageMenus(id: string) {
  return request.get<unknown, string[]>(`/system/packages/${id}/menus`)
}

export function createPackage(data: PackageParams) {
  return request.post<unknown, string>('/system/packages', data)
}

export function updatePackage(id: string, data: PackageParams) {
  return request.put<unknown, void>(`/system/packages/${id}`, data)
}

export function deletePackage(id: string) {
  return request.delete<unknown, void>(`/system/packages/${id}`)
}

export function assignPackageMenus(id: string, menuIds: string[]) {
  return request.put<unknown, void>(`/system/packages/${id}/menus`, { menuIds })
}
