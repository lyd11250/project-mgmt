import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import type { RouteLocationNormalized } from 'vue-router'

/** 一个已打开的页签。 */
export interface TabView {
  /** 路由路径（唯一键，如 /system/users）。 */
  path: string
  /** 完整路径（含 query），点击页签时跳转用。 */
  fullPath: string
  /** 路由名（动态路由为 menu-{id}），即组件 name，用于 keep-alive 匹配。 */
  name: string
  /** 页签标题。 */
  title: string
  /** 是否固定（首页等，不可关闭、不参与「关闭其他/全部」）。 */
  affix?: boolean
  /** 是否启用 keep-alive 缓存。 */
  keepAlive?: boolean
}

const STORAGE_KEY = 'bedrock-tabs'

/** 固定首页页签，store 初始化与「关闭全部」后始终保留。 */
const HOME_TAB: TabView = {
  path: '/home',
  fullPath: '/home',
  name: 'home',
  title: '首页',
  affix: true,
  keepAlive: false,
}

function loadFromStorage(): TabView[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return [{ ...HOME_TAB }]
    const list = JSON.parse(raw) as TabView[]
    // 确保首页固定页签始终在首位且存在
    const rest = list.filter((t) => t.path !== HOME_TAB.path)
    return [{ ...HOME_TAB }, ...rest]
  } catch {
    return [{ ...HOME_TAB }]
  }
}

/**
 * 页签栏状态：维护已访问页签列表与需要 keep-alive 缓存的组件名单。
 * 页签列表持久化到 localStorage（刷新后保留）；keep-alive 为内存缓存，刷新必然丢失。
 */
export const useTabsStore = defineStore('tabs', () => {
  const visitedViews = ref<TabView[]>(loadFromStorage())
  /** 需 keep-alive 的组件 name（即路由名）列表。 */
  const cachedViews = ref<string[]>([])

  // 页签列表变化即写回 localStorage
  watch(
    visitedViews,
    (views) => {
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(views))
      } catch {
        // 忽略存储异常（如隐私模式/超额）
      }
    },
    { deep: true },
  )

  function addCache(name: string) {
    if (name && !cachedViews.value.includes(name)) {
      cachedViews.value.push(name)
    }
  }

  function removeCache(name: string) {
    const i = cachedViews.value.indexOf(name)
    if (i > -1) cachedViews.value.splice(i, 1)
  }

  /** 记录一次页面访问：新增页签（已存在则更新 fullPath），并按 keepAlive 维护缓存名单。 */
  function addView(route: RouteLocationNormalized) {
    const path = route.path
    const name = (route.name as string) ?? ''
    const title = (route.meta.title as string) ?? name ?? path
    const keepAlive = route.meta.keepAlive === true

    const existed = visitedViews.value.find((t) => t.path === path)
    if (existed) {
      existed.fullPath = route.fullPath
    } else {
      visitedViews.value.push({ path, fullPath: route.fullPath, name, title, keepAlive })
    }
    if (keepAlive) addCache(name)
    else removeCache(name)
  }

  /** 关闭指定页签，返回关闭后应停留/跳转的页签（供调用方决定是否需要导航）。 */
  function removeView(path: string): TabView {
    const idx = visitedViews.value.findIndex((t) => t.path === path)
    if (idx > -1) {
      const [removed] = visitedViews.value.splice(idx, 1)
      removeCache(removed.name)
    }
    // 返回相邻页签（优先右侧，否则左侧，再否则首页）
    return (
      visitedViews.value[idx] ?? visitedViews.value[idx - 1] ?? visitedViews.value[0] ?? HOME_TAB
    )
  }

  /** 关闭除指定页签外的所有非固定页签。 */
  function closeOthers(path: string) {
    visitedViews.value = visitedViews.value.filter((t) => t.affix || t.path === path)
    syncCache()
  }

  /** 关闭全部非固定页签（保留首页）。 */
  function closeAll() {
    visitedViews.value = visitedViews.value.filter((t) => t.affix)
    syncCache()
  }

  /** 关闭指定页签右侧的所有非固定页签。 */
  function closeRight(path: string) {
    const idx = visitedViews.value.findIndex((t) => t.path === path)
    if (idx < 0) return
    visitedViews.value = visitedViews.value.filter((t, i) => i <= idx || t.affix)
    syncCache()
  }

  /** 批量关闭后，按剩余页签重建缓存名单。 */
  function syncCache() {
    const names = new Set(visitedViews.value.filter((t) => t.keepAlive).map((t) => t.name))
    cachedViews.value = cachedViews.value.filter((n) => names.has(n))
  }

  /** 登出/切换账号时清空，仅保留首页固定页签。 */
  function reset() {
    visitedViews.value = [{ ...HOME_TAB }]
    cachedViews.value = []
  }

  return {
    visitedViews,
    cachedViews,
    addView,
    removeView,
    closeOthers,
    closeAll,
    closeRight,
    reset,
  }
})
