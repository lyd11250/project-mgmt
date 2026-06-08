import type { Directive } from 'vue'
import { useAuthStore } from '@/stores/auth'

/**
 * 按钮级权限指令：`v-permission="'user:create'"`，无权限时移除元素。
 */
export const permission: Directive<HTMLElement, string> = {
  mounted(el, binding) {
    const auth = useAuthStore()
    if (binding.value && !auth.hasPermission(binding.value)) {
      el.remove()
    }
  },
}
