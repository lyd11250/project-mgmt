import request from './request'
import type { PageResult } from './user'

/** 相关方类型。 */
export type PartyType = 'PERSON' | 'ORGANIZATION'

// ---- 人员 ----

export interface PersonItem {
  id: string
  name: string
  gender: number
  idCard?: string
  contact?: string
  status: number
  remark?: string
  createdAt: string
}

export interface PersonParams {
  name: string
  gender?: number
  idCard?: string
  contact?: string
  status?: number
  remark?: string
}

export function pagePersons(params: { page?: number; size?: number; keyword?: string }) {
  return request.get<unknown, PageResult<PersonItem>>('/party/persons', { params })
}

export function createPerson(data: PersonParams) {
  return request.post<unknown, string>('/party/persons', data)
}

export function updatePerson(id: string, data: PersonParams) {
  return request.put<unknown, void>(`/party/persons/${id}`, data)
}

export function deletePerson(id: string) {
  return request.delete<unknown, void>(`/party/persons/${id}`)
}

// ---- 组织/单位 ----

export interface OrganizationItem {
  id: string
  name: string
  orgType?: string
  taxNo?: string
  registeredCapital?: string
  establishedDate?: string
  legalPerson?: string
  regAddress?: string
  businessScope?: string
  status: number
  remark?: string
  createdAt: string
}

export interface OrganizationParams {
  name: string
  orgType?: string
  taxNo?: string
  registeredCapital?: string
  establishedDate?: string
  legalPerson?: string
  regAddress?: string
  businessScope?: string
  status?: number
  remark?: string
}

export function pageOrganizations(params: { page?: number; size?: number; keyword?: string }) {
  return request.get<unknown, PageResult<OrganizationItem>>('/party/organizations', { params })
}

/** 已入库的组织类型（去重），供输入补全。 */
export function listOrganizationTypes() {
  return request.get<unknown, string[]>('/party/organizations/types')
}

export function createOrganization(data: OrganizationParams) {
  return request.post<unknown, string>('/party/organizations', data)
}

export function updateOrganization(id: string, data: OrganizationParams) {
  return request.put<unknown, void>(`/party/organizations/${id}`, data)
}

export function deleteOrganization(id: string) {
  return request.delete<unknown, void>(`/party/organizations/${id}`)
}
