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
    </div>

    <el-table
      v-loading="loading"
      :data="list"
      border
      :header-cell-style="{ textAlign: 'center' }"
      :cell-style="{ textAlign: 'center' }"
      stripe
    >
      <el-table-column prop="originalName" label="文件名" min-width="200" show-overflow-tooltip />
      <el-table-column prop="contentType" label="类型" min-width="160" show-overflow-tooltip />
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
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox, type UploadRequestOptions } from 'element-plus'
import {
  deleteFile,
  downloadFile,
  pageFiles,
  uploadFile,
  type FileItem,
} from '@/api/file'
import { formatDateTime } from '@/utils/time'

const loading = ref(false)
const uploading = ref(false)
const list = ref<FileItem[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(10)

async function load() {
  loading.value = true
  try {
    const page = await pageFiles({ current: current.value, size: size.value })
    list.value = page.records
    total.value = Number(page.total)
  } finally {
    loading.value = false
  }
}

onMounted(load)

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
}
.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
