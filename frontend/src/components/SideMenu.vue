<template>
  <template v-for="m in items" :key="m.id">
    <el-sub-menu v-if="m.children && m.children.length" :index="String(m.id)">
      <template #title>
        <el-icon v-if="m.icon"><component :is="m.icon" /></el-icon>
        <span>{{ m.name }}</span>
      </template>
      <SideMenu :items="m.children" />
    </el-sub-menu>
    <el-menu-item v-else-if="m.path" :index="m.path">
      <el-icon v-if="m.icon"><component :is="m.icon" /></el-icon>
      <span>{{ m.name }}</span>
    </el-menu-item>
  </template>
</template>

<script setup lang="ts">
import type { MenuNode } from '@/api/menu'

// 递归渲染导航菜单树：M 目录 → el-sub-menu，C 页面 → el-menu-item
defineProps<{ items: MenuNode[] }>()
</script>
