<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ping } from '@/api/auth'

const result = ref('')

async function handlePing() {
  try {
    result.value = await ping()
    ElMessage.success('后端连通：' + result.value)
  } catch {
    // 错误已在拦截器统一提示
  }
}
</script>

<template>
  <el-card>
    <h2>欢迎使用项目协作平台</h2>
    <p>第 0 期脚手架已就绪。后续模块（用户与权限 / 主数据 / 项目进度）将分期实现。</p>
    <el-button type="primary" @click="handlePing">测试后端连通 (/api/v1/ping)</el-button>
    <p v-if="result">返回：{{ result }}</p>
  </el-card>
</template>
