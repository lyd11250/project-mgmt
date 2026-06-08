import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'home',
        component: () => import('@/views/Home.vue'),
        meta: { title: '首页' },
      },
      {
        path: 'system/users',
        name: 'users',
        component: () => import('@/views/system/UserList.vue'),
        meta: { title: '用户管理', permission: 'user:list' },
      },
      {
        path: 'system/tenants',
        name: 'tenants',
        component: () => import('@/views/system/TenantList.vue'),
        meta: { title: '租户管理', role: 'SUPER_ADMIN' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 全局前置守卫：登录校验 + 角色/权限校验
router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.meta.public) {
    return true
  }
  if (!auth.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  // 有令牌但用户信息未加载（如刷新页面）→ 先拉取
  if (!auth.user) {
    try {
      await auth.fetchMe()
    } catch {
      return { name: 'login', query: { redirect: to.fullPath } }
    }
  }
  if (to.meta.role && !auth.hasRole(to.meta.role as string)) {
    return { name: 'home' }
  }
  if (to.meta.permission && !auth.hasPermission(to.meta.permission as string)) {
    return { name: 'home' }
  }
  return true
})

export default router
