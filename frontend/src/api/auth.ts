import request from './request'

export interface LoginParams {
  tenantCode: string
  username: string
  password: string
}

export interface LoginResult {
  tokenName: string
  token: string
}

export interface MeInfo {
  userId: string
  username: string
  nickname?: string
  tenantId: string
  roles: string[]
  permissions: string[]
}

/** 登录：租户编码 + 用户名 + 密码。 */
export function login(params: LoginParams) {
  return request.post<unknown, LoginResult>('/system/auth/login', params)
}

/** 登出。 */
export function logout() {
  return request.post<unknown, void>('/system/auth/logout')
}

/** 当前登录用户信息（含角色与权限）。 */
export function getMe() {
  return request.get<unknown, MeInfo>('/system/auth/me')
}

/** 健康检查（开放接口）。 */
export function ping() {
  return request.get<unknown, string>('/ping')
}
