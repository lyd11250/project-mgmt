<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">Bedrock 平台</div>
      <el-menu
        router
        :default-active="$route.path"
        class="menu"
        background-color="#001529"
        text-color="#fff"
        active-text-color="#409eff"
      >
        <el-menu-item index="/home">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>
        <SideMenu :items="auth.menus" />
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <el-dropdown trigger="click" @command="handleCommand">
          <div class="user-trigger">
            <el-avatar :size="32" class="avatar">
              <el-icon><Avatar /></el-icon>
            </el-avatar>
            <span class="user-name">{{ auth.user?.nickname || auth.user?.username }}</span>
            <el-icon class="arrow"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="password">修改密码</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Avatar, ArrowDown } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { resetDynamicRoutes } from '@/router'
import SideMenu from '@/components/SideMenu.vue'

const router = useRouter()
const auth = useAuthStore()

function handleCommand(command: string) {
  if (command === 'profile') {
    router.push({ path: '/profile', query: { tab: 'basic' } })
  } else if (command === 'password') {
    router.push({ path: '/profile', query: { tab: 'password' } })
  } else if (command === 'logout') {
    handleLogout()
  }
}

async function handleLogout() {
  await auth.logout()
  resetDynamicRoutes()
  router.replace('/login')
}
</script>

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
  justify-content: flex-end;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}
.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}
.avatar {
  background: #409eff;
}
.user-name {
  font-size: 14px;
  color: #303133;
}
.arrow {
  color: #909399;
}
</style>
