import axios from 'axios'
import { ElMessage } from 'element-plus'
import request, { TOKEN_KEY } from './request'
import type { PageResult } from './user'

export interface FileItem {
  id: string
  originalName: string
  contentType?: string
  sizeBytes: number
  bizType?: string
  createdAt: string
}

/** 文件分页（按 bizType 可选过滤）。 */
export function pageFiles(params: { current?: number; size?: number; bizType?: string }) {
  return request.get<unknown, PageResult<FileItem>>('/system/files', { params })
}

/** 上传文件，返回文件信息。 */
export function uploadFile(file: File, bizType?: string) {
  const fd = new FormData()
  fd.append('file', file)
  if (bizType) fd.append('bizType', bizType)
  return request.post<unknown, FileItem>('/system/files', fd)
}

export function deleteFile(id: string) {
  return request.delete<unknown, void>(`/system/files/${id}`)
}

// 下载专用裸实例：响应是二进制流而非 { code, message, data }，
// 故不能复用 request 的「按 code 解包」拦截器；这里仅附加令牌与 401 处理。
const rawDownload = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 60000,
  responseType: 'blob',
})
rawDownload.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) config.headers.set(TOKEN_KEY, token)
  return config
})

/** 下载文件并触发浏览器保存。令牌随请求头发送（a.href 直链无法携带头）。 */
export async function downloadFile(id: string, filename: string) {
  try {
    const res = await rawDownload.get(`/system/files/${id}`)
    const url = URL.createObjectURL(res.data as Blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('下载失败')
  }
}
