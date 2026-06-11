<template>
  <el-breadcrumb class="breadcrumb" separator="/">
    <el-breadcrumb-item :to="{ path: '/home' }">首页</el-breadcrumb-item>
    <el-breadcrumb-item v-for="(item, i) in trail" :key="item.id">
      <span :class="{ current: i === trail.length - 1 }">{{ item.name }}</span>
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import type { MenuNode } from '@/api/menu'

const route = useRoute()
const auth = useAuthStore()

/** 在菜单树中定位当前路径节点，回溯祖先链（含目录），生成面包屑层级。 */
const trail = computed<MenuNode[]>(() => {
  const path = route.path
  const chain: MenuNode[] = []
  const dfs = (nodes: MenuNode[], ancestors: MenuNode[]): boolean => {
    for (const n of nodes) {
      const next = [...ancestors, n]
      if (n.type === 'C' && n.path === path) {
        chain.push(...next)
        return true
      }
      if (n.children?.length && dfs(n.children, next)) return true
    }
    return false
  }
  dfs(auth.menus, [])
  // 未命中菜单（如首页/个人中心等静态页）则用路由 meta.title 兜底
  if (chain.length === 0 && route.meta.title && path !== '/home') {
    return [{ id: path, type: 'C', name: route.meta.title as string }] as MenuNode[]
  }
  return chain
})
</script>

<style scoped>
.breadcrumb {
  font-size: 14px;
}
.current {
  color: #303133;
}
</style>
