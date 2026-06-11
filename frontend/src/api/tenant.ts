import request from './request'
import type { PageResult } from './user'

export interface TenantItem {
  id: string
  name: string
  code: string
  status: number
  packageId?: string
  packageName?: string
  contact?: string
  /** 订阅到期时间；空表示永久。 */
  expireAt?: string
  /** 是否已过期（后端按 expireAt 现算）。 */
  expired?: boolean
  createdAt: string
}

export interface TenantCreateParams {
  name: string
  code: string
  packageId: string
  adminUsername: string
  adminPassword: string
  contact?: string
  expireAt?: string
}

export function pageTenants(params: { page?: number; size?: number }) {
  return request.get<unknown, PageResult<TenantItem>>('/system/tenants', { params })
}

export function createTenant(data: TenantCreateParams) {
  return request.post<unknown, string>('/system/tenants', data)
}

export interface TenantRenewParams {
  /** 新到期时间；空表示设为永久。 */
  expireAt?: string
  /** 变更套餐；空表示套餐不变。 */
  changePackageId?: string
}

/** 续费/调整到期时间，可同时变更套餐。 */
export function renewTenant(id: string, params: TenantRenewParams) {
  return request.put<unknown, void>(`/system/tenants/${id}/renew`, {
    expireAt: params.expireAt || null,
    changePackageId: params.changePackageId || null,
  })
}
