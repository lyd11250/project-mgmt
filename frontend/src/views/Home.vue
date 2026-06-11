<template>
  <el-card>
    <h2>欢迎，{{ auth.user?.username }}</h2>
    <p>当前角色：{{ auth.roles.join(', ') || '无' }}</p>
    <p>第 1 期-A 认证权限基座已就绪：登录、RBAC、用户/租户管理。后续将实现主数据与项目进度。</p>
    <el-button type="primary" @click="handlePing">测试后端连通 (/api/v1/ping)</el-button>
    <p v-if="result">返回：{{ result }}</p>
  </el-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ping } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
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
