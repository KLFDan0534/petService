import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(loadUser())
  const token = ref(localStorage.getItem('token') || null)
  const refreshToken = ref(localStorage.getItem('refreshToken') || null)

  function loadUser() {
    try {
      return JSON.parse(localStorage.getItem('user') || 'null')
    } catch {
      return null
    }
  }

  const isLoggedIn = computed(() => !!token.value)

  const normalizedRoles = computed(() =>
    (user.value?.roles_wsh || []).map(r => r.replace('ROLE_', ''))
  )

  const isAdmin = computed(() => normalizedRoles.value.includes('ADMIN'))
  const isMerchant = computed(() => normalizedRoles.value.includes('MERCHANT'))
  const isUser = computed(() => normalizedRoles.value.includes('USER'))
  const isCs = computed(() => normalizedRoles.value.includes('CUSTOMER_SERVICE'))

  function hasRole(role) {
    const cleanRole = role.replace('ROLE_', '')
    if (normalizedRoles.value.includes(cleanRole)) return true
    const roleMap = { 'OWNER': 'USER', 'KEEPER': 'USER' }
    const mapped = normalizedRoles.value.map(r => roleMap[r] || r)
    return mapped.includes(cleanRole)
  }

  function setAuth(data) {
    const roles = (data.roles_wsh || data.roles || []).map(r => r.replace('ROLE_', ''))
    const accessToken = data.access_token_wsh || data.access_token || data.accessToken
    const refresh = data.refresh_token_wsh || data.refresh_token || data.refreshToken || null
    user.value = {
      id_wsh: data.user_id_wsh || data.user_id || data.userId,
      username_wsh: data.username_wsh || data.username,
      nickname_wsh: data.nickname_wsh || data.nickname,
      roles_wsh: roles,
    }
    token.value = accessToken
    refreshToken.value = refresh
    localStorage.setItem('token', accessToken)
    if (refresh) localStorage.setItem('refreshToken', refresh)
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  function clearAuth() {
    user.value = null
    token.value = null
    refreshToken.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
  }

  function refetchUser() {
    user.value = loadUser()
  }

  async function apiGet(url, params) {
    const r = await request.get(url, { params })
    return r.data
  }

  async function apiPost(url, data) {
    const r = await request.post(url, data)
    return r.data
  }

  async function apiPut(url, data) {
    const r = await request.put(url, data)
    return r.data
  }

  async function apiDelete(url) {
    const r = await request.delete(url)
    return r.data
  }

  async function apiPatch(url, data) {
    const r = await request.patch(url, data)
    return r.data
  }

  return { user, token, refreshToken, isLoggedIn, isAdmin, isMerchant, isUser, isCs, hasRole, setAuth, clearAuth, refetchUser, apiGet, apiPost, apiPut, apiDelete, apiPatch }
})
