<template>
  <div class="error-page">
    <el-result :icon="icon" :title="title" :sub-title="subTitle">
      <template #extra>
        <el-button type="primary" @click="goHome">返回首页</el-button>
        <el-button @click="goBack">返回上一页</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

defineProps<{
  icon: 'error' | 'warning' | 'info' | 'success'
  title: string
  subTitle: string
}>()

const router = useRouter()

function goHome() {
  router.replace('/home')
}

function goBack() {
  router.back()
}
</script>

<style scoped>
.error-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
}
</style>
