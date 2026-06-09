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
  createdAt: string
}

export interface TenantCreateParams {
  name: string
  code: string
  packageId: string
  adminUsername: string
  adminPassword: string
  contact?: string
}

export function pageTenants(params: { page?: number; size?: number }) {
  return request.get<unknown, PageResult<TenantItem>>('/tenants', { params })
}

export function createTenant(data: TenantCreateParams) {
  return request.post<unknown, string>('/tenants', data)
}
