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
