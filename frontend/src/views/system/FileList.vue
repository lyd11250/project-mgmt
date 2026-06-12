<template>
  <el-card>
    <div class="toolbar">
      <el-upload
        v-permission="'system:file:upload'"
        :show-file-list="false"
        :http-request="customUpload"
        :disabled="uploading"
      >
        <el-button type="success" :loading="uploading">上传文件</el-button>
      </el-upload>
      <el-select
        v-model="bizTypeFilter"
        clearable
        placeholder="全部业务类型"
        class="biz-filter"
        @change="onFilterChange"
      >
        <el-option
          v-for="opt in bizTypeOptions"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
    </div>

    <el-table
      v-loading="loading"
      :data="list"
      border
      :header-cell-style="{ textAlign: 'center' }"
      :cell-style="{ textAlign: 'center' }"
      stripe
    >
      <el-table-column label="预览" width="90">
        <template #default="{ row }">
          <el-image
            v-if="isImage(row.contentType) && imageUrls[row.id]"
            :src="imageUrls[row.id]"
            :preview-src-list="[imageUrls[row.id]]"
            preview-teleported
            fit="cover"
            class="thumb"
          />
          <el-icon v-else class="type-icon"><component :is="fileIcon(row.contentType)" /></el-icon>
        </template>
      </el-table-column>
      <el-table-column prop="originalName" label="文件名" min-width="200" show-overflow-tooltip />
      <el-table-column prop="contentType" label="类型" min-width="160" show-overflow-tooltip />
      <el-table-column label="业务类型" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">{{ bizTypeLabel(row.bizType) }}</template>
      </el-table-column>
      <el-table-column label="大小" width="120">
        <template #default="{ row }">{{ formatSize(row.sizeBytes) }}</template>
      </el-table-column>
      <el-table-column label="上传时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button
            v-permission="'system:file:download'"
            link
            type="primary"
            @click="handleDownload(row)"
          >
            下载
          </el-button>
          <el-button
            v-permission="'system:file:delete'"
            link
            type="danger"
            @click="handleDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pager"
      layout="total, prev, pager, next"
      :total="total"
      :current-page="current"
      :page-size="size"
      @current-change="onPageChange"
    />
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { ElMessage, ElMessageBox, type UploadRequestOptions } from 'element-plus'
import {
  deleteFile,
  downloadFile,
  fetchFileObjectUrl,
  getFileBizTypes,
  isImage,
  pageFiles,
  uploadFile,
  type BizTypeOption,
  type FileItem,
} from '@/api/file'
import { formatDateTime } from '@/utils/time'

const loading = ref(false)
const uploading = ref(false)
const list = ref<FileItem[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(10)
const bizTypeFilter = ref('')
const bizTypeOptions = ref<BizTypeOption[]>([])
/** 图片缩略图的 blob 对象 URL，按文件 id 缓存（重载/卸载时统一 revoke）。 */
const imageUrls = ref<Record<string, string>>({})

/** 按 MIME 选择类型图标（全局已注册 Element Plus 图标，按名引用）。 */
function fileIcon(contentType?: string): string {
  if (!contentType) return 'Document'
  if (contentType.startsWith('video/')) return 'VideoCamera'
  if (contentType.startsWith('audio/')) return 'Headset'
  if (contentType === 'application/pdf' || contentType.startsWith('text/')) return 'Document'
  if (contentType.includes('zip') || contentType.includes('compressed')) return 'Files'
  return 'Document'
}

/** 释放已生成的缩略图对象 URL，避免内存泄漏。 */
function revokeThumbnails() {
  for (const url of Object.values(imageUrls.value)) URL.revokeObjectURL(url)
  imageUrls.value = {}
}

/** 为当前页的图片文件并发拉取缩略图 blob；单个失败回退为图标。 */
async function loadThumbnails() {
  revokeThumbnails()
  const images = list.value.filter((f) => isImage(f.contentType))
  await Promise.all(
    images.map(async (f) => {
      try {
        imageUrls.value[f.id] = await fetchFileObjectUrl(f.id)
      } catch {
        // 单个缩略图加载失败忽略，列内回退展示类型图标
      }
    }),
  )
}

/** 业务类型技术串 → 中文标签；未登记或空值回退展示。 */
function bizTypeLabel(value?: string): string {
  if (!value) return '-'
  return bizTypeOptions.value.find((o) => o.value === value)?.label ?? value
}

async function load() {
  loading.value = true
  try {
    const page = await pageFiles({
      current: current.value,
      size: size.value,
      bizType: bizTypeFilter.value || undefined,
    })
    list.value = page.records
    total.value = Number(page.total)
    await loadThumbnails()
  } finally {
    loading.value = false
  }
}

async function loadBizTypes() {
  bizTypeOptions.value = await getFileBizTypes()
}

onMounted(() => {
  load()
  loadBizTypes()
})

onUnmounted(() => {
  revokeThumbnails()
})

function onFilterChange() {
  current.value = 1
  load()
}

function onPageChange(p: number) {
  current.value = p
  load()
}

/** 接管 el-upload 的上传动作，走统一 api（带令牌、统一错误处理）。 */
async function customUpload(options: UploadRequestOptions) {
  uploading.value = true
  try {
    await uploadFile(options.file as File)
    ElMessage.success('上传成功')
    current.value = 1
    await load()
    await loadBizTypes()
  } finally {
    uploading.value = false
  }
}

function handleDownload(row: FileItem) {
  downloadFile(row.id, row.originalName)
}

async function handleDelete(row: FileItem) {
  await ElMessageBox.confirm(`确认删除文件「${row.originalName}」？`, '提示', { type: 'warning' })
  await deleteFile(row.id)
  ElMessage.success('删除成功')
  await load()
  await loadBizTypes()
}

/** 人类可读的文件大小。 */
function formatSize(bytes: number): string {
  if (bytes == null) return '-'
  if (bytes < 1024) return `${bytes} B`
  const units = ['KB', 'MB', 'GB', 'TB']
  let value = bytes / 1024
  let i = 0
  while (value >= 1024 && i < units.length - 1) {
    value /= 1024
    i++
  }
  return `${value.toFixed(2)} ${units[i]}`
}
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
}
.biz-filter {
  width: 200px;
}
.thumb {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  vertical-align: middle;
}
.type-icon {
  font-size: 28px;
  color: var(--el-text-color-secondary);
}
.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
