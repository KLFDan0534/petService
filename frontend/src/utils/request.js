import axios from 'axios'
import { useAppStore } from '@/stores/app'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

let isRefreshing = false
let pendingRequests = []

function onRefreshed(token) {
  pendingRequests.forEach(cb => cb(token))
  pendingRequests = []
}

function addPendingRequest(cb) {
  pendingRequests.push(cb)
}

request.interceptors.request.use(config => {
  // Normalize URL: strip duplicate /api/ prefix (baseURL already adds /api)
  if (config.url && config.url.startsWith('/api/')) {
    config.url = config.url.replace(/^\/api/, '')
  }
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 401) {
      return handle401Error(response.config)
    }
    if (res.code === 403) {
      return handle403Error(response.config)
    }
    if (res && typeof res.code === 'number' && res.code !== 200) {
      const appStore = useAppStore()
      appStore.addToast(res.message || '请求失败', 'error')
    }
    return response
  },
  async error => {
    if (!error.response) {
      return Promise.reject(error)
    }
    const { status, config } = error.response
    if (status === 401) {
      return handle401Error(config)
    }
    if (status === 403) {
      return handle403Error(config)
    }
    if (status === 500) {
      const appStore = useAppStore()
      appStore.addToast('服务器错误，请稍后重试', 'error')
    }
    return Promise.reject(error)
  }
)

async function tryRefresh(config) {
  const rt = localStorage.getItem('refreshToken')
  if (!rt) return false
  if (isRefreshing) {
    return new Promise(resolve => {
      addPendingRequest(newToken => {
        if (newToken) {
          config.headers.Authorization = `Bearer ${newToken}`
          resolve(true)
        } else {
          resolve(false)
        }
      })
    })
  }
  isRefreshing = true
  try {
    const r = await axios.post('/api/auth/refresh', { refresh_token_wsh: rt })
    if (r.data.code === 200) {
      const { access_token_wsh, refresh_token_wsh, roles_wsh } = r.data.data
      localStorage.setItem('token', access_token_wsh)
      if (refresh_token_wsh) localStorage.setItem('refreshToken', refresh_token_wsh)
      const userStr = localStorage.getItem('user')
      if (userStr) {
        const user = JSON.parse(userStr)
        if (roles_wsh) user.roles_wsh = roles_wsh
        localStorage.setItem('user', JSON.stringify(user))
      }
      onRefreshed(access_token_wsh)
      isRefreshing = false
      config.headers.Authorization = `Bearer ${access_token_wsh}`
      return true
    }
  } catch { /* ignore */ }
  isRefreshing = false
  onRefreshed(null)
  return false
}

async function handle401Error(config) {
  // 未登录（无 token）时的 401 直接拒绝，不强制跳转登录页：
  // 没有 token 可刷新，且不应打断公开页面（主页/服务浏览）的浏览体验
  if (!localStorage.getItem('token')) {
    return Promise.reject(new Error('未登录或登录已过期'))
  }
  const ok = await tryRefresh(config)
  if (ok) return request(config)
  clearAuthAndRedirect()
  return Promise.reject(new Error('Token刷新失败'))
}

async function handle403Error(config) {
  if (config._retry403) {
    const appStore = useAppStore()
    appStore.addToast('权限不足', 'error')
    return Promise.reject(new Error('权限不足'))
  }
  config._retry403 = true
  const ok = await tryRefresh(config)
  if (ok) return request(config)
  const appStore = useAppStore()
  appStore.addToast('权限不足', 'error')
  return Promise.reject(new Error('权限不足'))
}

function clearAuthAndRedirect() {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('user')
  window.location.href = '/login?redirect=' + encodeURIComponent(window.location.pathname)
}

export default request
