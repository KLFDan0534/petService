import { useAuthStore } from '@/stores/auth'

export const ROLES = {
  USER: 'USER',
  MERCHANT: 'MERCHANT',
  ADMIN: 'ADMIN',
  CUSTOMER_SERVICE: 'CUSTOMER_SERVICE',
}

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

export function hasPermission(requiredRoles) {
  if (!requiredRoles || requiredRoles.length === 0) return true
  const authStore = useAuthStore()
  return requiredRoles.some(r => authStore.hasRole(r))
}

export function getCurrentUser() {
  try {
    return JSON.parse(localStorage.getItem('user') || 'null')
  } catch {
    return null
  }
}
