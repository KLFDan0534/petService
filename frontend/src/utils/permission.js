import { useAuthStore } from '@/stores/auth'

export const ROLES = {
  OWNER: 'OWNER',
  KEEPER: 'KEEPER',
  MERCHANT: 'MERCHANT',
  ADMIN: 'ADMIN',
  CUSTOMER_SERVICE: 'CUSTOMER_SERVICE',
}

// 判断有没有登录，只看本地有没有 token，token 过期与否这里不管
export function isLogin() {
  return !!localStorage.getItem('token')
}

export function hasRole(role) {
  const authStore = useAuthStore()
  return authStore.hasRole(role)
}

export function hasAnyRole(...roles) {
  const authStore = useAuthStore()
  return roles.some(r => authStore.hasRole(r))
}

// 一个角色都不要求就默认放行，这样页面上不写要求时不用额外判断
export function hasPermission(requiredRoles) {
  if (!requiredRoles || requiredRoles.length === 0) return true
  const authStore = useAuthStore()
  return requiredRoles.some(r => authStore.hasRole(r))
}

// 本地存的 user 可能被手动改坏，解析失败就当没登录
export function getCurrentUser() {
  try {
    return JSON.parse(localStorage.getItem('user') || 'null')
  } catch {
    return null
  }
}
