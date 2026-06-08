import request from './request'
import type { PageResult } from './user'

export interface TenantItem {
  id: number
  name: string
  code: string
  status: number
  contact?: string
  createdAt: string
}

export interface TenantCreateParams {
  name: string
  code: string
  adminUsername: string
  adminPassword: string
  contact?: string
}

export function pageTenants(params: { page?: number; size?: number }) {
  return request.get<unknown, PageResult<TenantItem>>('/tenants', { params })
}

export function createTenant(data: TenantCreateParams) {
  return request.post<unknown, number>('/tenants', data)
}
