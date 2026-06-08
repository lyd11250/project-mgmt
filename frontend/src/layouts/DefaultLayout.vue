<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

interface MenuItem {
  index: string
  label: string
  show: boolean
}

const menus = computed<MenuItem[]>(() => [
  { index: '/home', label: '首页', show: true },
  { index: '/system/users', label: '用户管理', show: auth.hasPermission('user:list') },
  { index: '/system/tenants', label: '租户管理', show: auth.hasRole('SUPER_ADMIN') },
])

async function handleLogout() {
  await auth.logout()
  router.replace('/login')
}
</script>

<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">项目协作平台</div>
      <el-menu router :default-active="$route.path" class="menu" background-color="#001529"
        text-color="#fff" active-text-color="#409eff">
        <template v-for="m in menus" :key="m.index">
          <el-menu-item v-if="m.show" :index="m.index">{{ m.label }}</el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span>{{ auth.user?.username }}（{{ auth.roles.join(', ') }}）</span>
        <el-button link type="primary" @click="handleLogout">退出登录</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout {
  height: 100%;
}
.aside {
  background: #001529;
  color: #fff;
}
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  font-size: 18px;
  font-weight: 600;
  color: #fff;
}
.menu {
  border-right: none;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}
</style>
