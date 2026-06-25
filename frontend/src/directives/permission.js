import { useAuthStore } from '@/stores/auth'

const permission = {
  mounted(el, binding) {
    const authStore = useAuthStore()
    const { value } = binding
    if (value && Array.isArray(value) && value.length > 0) {
      const hasPermission = value.some(role => authStore.hasRole(role))
      if (!hasPermission) {
        el.parentNode && el.parentNode.removeChild(el)
      }
    } else {
      throw new Error('v-permission requires an array of roles, e.g. v-permission="[\'ADMIN\']"')
    }
  },
}

export default permission
