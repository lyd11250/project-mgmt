import request from './request'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  token: string
}

/**
 * 登录（第 1 期实现后端接口，本期仅占位定义）。
 */
export function login(params: LoginParams) {
  return request.post<unknown, LoginResult>('/auth/login', params)
}

/**
 * 健康检查（开放接口，用于联调验证）。
 */
export function ping() {
  return request.get<unknown, string>('/ping')
}
