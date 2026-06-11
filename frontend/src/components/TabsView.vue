<template>
  <div class="tabs-view">
    <el-tabs
      v-model="activePath"
      type="card"
      class="tabs"
      @tab-click="onClick"
      @tab-remove="onRemove"
    >
      <el-tab-pane
        v-for="tab in tabs.visitedViews"
        :key="tab.path"
        :name="tab.path"
        :closable="!tab.affix"
      >
        <template #label>
          <span class="tab-label" @contextmenu.prevent="openMenu($event, tab)">{{ tab.title }}</span>
        </template>
      </el-tab-pane>
    </el-tabs>

    <!-- 右键上下文菜单 -->
    <ul v-show="menu.visible" class="ctx-menu" :style="{ left: menu.x + 'px', top: menu.y + 'px' }">
      <li @click="closeRight">关闭右侧</li>
      <li @click="closeOthers">关闭其他</li>
      <li @click="closeAll">关闭全部</li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { TabsPaneContext } from 'element-plus'
import { useTabsStore, type TabView } from '@/stores/tabs'

const route = useRoute()
const router = useRouter()
const tabs = useTabsStore()

// 当前激活页签跟随路由
const activePath = ref(route.path)
watch(
  () => route.path,
  (p) => {
    activePath.value = p
  },
)

const onClick = (pane: TabsPaneContext) => {
  const path = pane.paneName as string
  const tab = tabs.visitedViews.find((t) => t.path === path)
  if (tab && tab.path !== route.path) router.push(tab.fullPath)
}

const onRemove = (name: string | number) => {
  const path = String(name)
  const target = tabs.removeView(path)
  // 关闭的是当前页才需要跳转
  if (path === route.path) router.push(target.fullPath)
}

// ---- 右键菜单 ----
const menu = reactive({ visible: false, x: 0, y: 0 })
const ctxTab = ref<TabView | null>(null)

function openMenu(e: MouseEvent, tab: TabView) {
  ctxTab.value = tab
  menu.x = e.clientX
  menu.y = e.clientY
  menu.visible = true
}
function hideMenu() {
  menu.visible = false
}
watch(() => menu.visible, (v) => {
  if (v) document.addEventListener('click', hideMenu)
  else document.removeEventListener('click', hideMenu)
})
onBeforeUnmount(() => document.removeEventListener('click', hideMenu))

/** 关闭操作后，若当前页已被关掉则跳到剩余页签的最后一个。 */
function ensureActive() {
  if (!tabs.visitedViews.find((t) => t.path === route.path)) {
    const last = tabs.visitedViews[tabs.visitedViews.length - 1]
    if (last) router.push(last.fullPath)
  }
}
function closeRight() {
  if (ctxTab.value) tabs.closeRight(ctxTab.value.path)
  ensureActive()
}
function closeOthers() {
  if (ctxTab.value) {
    tabs.closeOthers(ctxTab.value.path)
    if (ctxTab.value.path !== route.path) router.push(ctxTab.value.fullPath)
  }
}
function closeAll() {
  tabs.closeAll()
  ensureActive()
}
</script>

<style scoped>
.tabs-view {
  background: #fff;
  padding: 6px 12px 0;
  border-bottom: 1px solid #e4e7ed;
}
.tabs :deep(.el-tabs__header) {
  margin: 0;
}
.tabs :deep(.el-tabs__nav-wrap)::after {
  display: none;
}
.tab-label {
  display: inline-block;
}
.ctx-menu {
  position: fixed;
  z-index: 3000;
  margin: 0;
  padding: 4px 0;
  list-style: none;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.12);
  font-size: 13px;
}
.ctx-menu li {
  padding: 6px 18px;
  cursor: pointer;
  white-space: nowrap;
}
.ctx-menu li:hover {
  background: #f5f7fa;
}
</style>
