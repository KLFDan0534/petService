import { useAuthStore } from '@/stores/auth'

const permission = {
  // 只在元素挂到页面上时判断一次，用户角色中途变了不会重新判断
  mounted(el, binding) {
    const authStore = useAuthStore()
    const { value } = binding
    if (value && Array.isArray(value) && value.length > 0) {
      const hasPermission = value.some(role => authStore.hasRole(role))
      if (!hasPermission) {
        // 没权限就把元素整个从页面上摘掉，不是藏起来，藏起来还能被翻出来改样式
        el.parentNode && el.parentNode.removeChild(el)
      }
    } else {
      throw new Error('v-permission requires an array of roles, e.g. v-permission="[\'ADMIN\']"')
    }
  },
}

export default permission
