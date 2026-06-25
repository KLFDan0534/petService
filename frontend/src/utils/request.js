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
      const appStore = useAppStore()
      appStore.addToast(res.message || '权限不足', 'error')
      return Promise.reject(new Error(res.message || '权限不足'))
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
      const appStore = useAppStore()
      appStore.addToast('权限不足，无法访问', 'error')
    } else if (status === 500) {
      const appStore = useAppStore()
      appStore.addToast('服务器错误，请稍后重试', 'error')
    }
    return Promise.reject(error)
  }
)

async function handle401Error(config) {
  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) {
    clearAuthAndRedirect()
    return Promise.reject(new Error('未登录'))
  }
  if (!isRefreshing) {
    isRefreshing = true
    try {
      const r = await axios.post('/api/auth/refresh', { refresh_token_wsh: refreshToken })
      if (r.data.code === 200) {
        const newToken = r.data.data.access_token_wsh
        const newRefresh = r.data.data.refresh_token_wsh
        localStorage.setItem('token', newToken)
        if (newRefresh) localStorage.setItem('refreshToken', newRefresh)
        const userStr = localStorage.getItem('user')
        if (userStr) {
          const user = JSON.parse(userStr)
          user.roles_wsh = r.data.data.roles_wsh || user.roles_wsh
          localStorage.setItem('user', JSON.stringify(user))
        }
        onRefreshed(newToken)
        isRefreshing = false
        config.headers.Authorization = `Bearer ${newToken}`
        return request(config)
      }
    } catch (refreshError) {
      isRefreshing = false
      pendingRequests = []
    }
    clearAuthAndRedirect()
    return Promise.reject(new Error('Token刷新失败'))
  }
  return new Promise(resolve => {
    addPendingRequest(newToken => {
      config.headers.Authorization = `Bearer ${newToken}`
      resolve(request(config))
    })
  })
}

function clearAuthAndRedirect() {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('user')
  window.location.href = '/login?redirect=' + encodeURIComponent(window.location.pathname)
}

export default request
