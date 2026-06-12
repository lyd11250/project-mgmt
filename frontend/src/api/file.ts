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

/** 业务类型选项：{ value: 技术串, label: 中文名 }。仅含当前租户实有文件的业务类型。 */
export interface BizTypeOption {
  value: string
  label: string
}

/** 文件分页（按 bizType 可选过滤）。 */
export function pageFiles(params: { current?: number; size?: number; bizType?: string }) {
  return request.get<unknown, PageResult<FileItem>>('/system/files', { params })
}

/** 拉取当前租户的业务类型选项，驱动列翻译与筛选下拉。 */
export function getFileBizTypes() {
  return request.get<unknown, BizTypeOption[]>('/system/files/biz-types')
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

/** 浏览器可内联渲染的 MIME（图片/PDF/纯文本）才提供预览。 */
export function isPreviewable(contentType?: string): boolean {
  if (!contentType) return false
  return (
    contentType.startsWith('image/') ||
    contentType === 'application/pdf' ||
    contentType.startsWith('text/')
  )
}

/** 是否为图片类型（用于列表缩略图渲染）。 */
export function isImage(contentType?: string): boolean {
  return !!contentType && contentType.startsWith('image/')
}

/**
 * 取文件的 blob 对象 URL，供 el-image 缩略图/放大预览使用。
 * 下载接口需令牌头，img 标签直链无法携带，故先取 blob 再生成本地 URL（用后需 revoke）。
 */
export async function fetchFileObjectUrl(id: string): Promise<string> {
  const res = await rawDownload.get(`/system/files/${id}`)
  return URL.createObjectURL(res.data as Blob)
}

/**
 * 在新标签页预览文件。先同步开窗（避开浏览器弹窗拦截），再把取到的 blob 链接喂给该窗口。
 * blob: 链接为本地资源，下载接口的 attachment 头不影响其内联渲染，浏览器按 MIME 展示。
 */
export async function previewFile(id: string) {
  const win = window.open('', '_blank')
  try {
    const res = await rawDownload.get(`/system/files/${id}`)
    const url = URL.createObjectURL(res.data as Blob)
    if (win) {
      win.location.href = url
    } else {
      window.open(url, '_blank')
    }
    setTimeout(() => URL.revokeObjectURL(url), 60000)
  } catch {
    win?.close()
    ElMessage.error('预览失败')
  }
}
