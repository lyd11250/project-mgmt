import request from './request'

export interface PackageItem {
  id: number
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

export function getPackageMenus(id: number) {
  return request.get<unknown, number[]>(`/system/packages/${id}/menus`)
}

export function createPackage(data: PackageParams) {
  return request.post<unknown, number>('/system/packages', data)
}

export function updatePackage(id: number, data: PackageParams) {
  return request.put<unknown, void>(`/system/packages/${id}`, data)
}

export function deletePackage(id: number) {
  return request.delete<unknown, void>(`/system/packages/${id}`)
}

export function assignPackageMenus(id: number, menuIds: number[]) {
  return request.put<unknown, void>(`/system/packages/${id}/menus`, { menuIds })
}
