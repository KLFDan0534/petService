import { logger } from '@/utils/logger'

// 全局只有一个实例，负责连服务端、断线重连、把收到的消息转给页面里注册的回调
class SocketManager {
  constructor() {
    this.eventSource = null
    this.url = null
    this.token = null
    this.listeners = {}
    this.connected = false
    this.transport = 'polling'
    this.reconnectTimer = null
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 10
    this.reconnectDelay = 2000
    this.subscriptions = new Set()
  }

  // 用浏览器自带的 EventSource 连服务端，连上后服务端推什么这里收什么
  connectSSE(url, token) {
    this.disconnect()
    this.url = url
    this.token = token
    this.transport = 'sse'
    // 为什么 token 拼在地址上而不是请求头？因为 EventSource 不让改请求头，只能走参数
    const fullUrl = `${url}?token=${encodeURIComponent(token)}`
    try {
      this.eventSource = new EventSource(fullUrl)
    } catch (e) {
      logger.error('[SocketManager] Failed to create EventSource', e)
      this.transport = 'polling'
      return
    }
    this.eventSource.onopen = () => {
      this.connected = true
      // 连上了就把失败次数清零，下次断线又从 2 秒开始
      this.reconnectAttempts = 0
      logger.info('[SocketManager] SSE connected')
    }
    this.eventSource.addEventListener('notification', (e) => {
      try {
        const data = JSON.parse(e.data)
        this.emit('notification', data)
      } catch (err) {
        logger.error('[SocketManager] Failed to parse SSE data', err)
      }
    })
    this.eventSource.onerror = () => {
      this.connected = false
      // 出错后浏览器自己的重连不靠谱，先手动关掉，再按自己的节奏重排
      if (this.eventSource) {
        this.eventSource.close()
        this.eventSource = null
      }
      this.scheduleReconnect()
    }
  }

  disconnect() {
    this.connected = false
    if (this.eventSource) {
      this.eventSource.close()
      this.eventSource = null
    }
    this.clearTimers()
    this.listeners = {}
    this.subscriptions.clear()
    logger.info('[SocketManager] Disconnected')
  }

  // 重连节奏：每失败一次等待时间乘 1.5，最长 30 秒，失败 10 次就不再试了
  scheduleReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      logger.warn('[SocketManager] Max reconnect attempts reached, staying in polling mode')
      this.transport = 'polling'
      return
    }
    this.reconnectAttempts++
    // 1 次 2s、2 次 3s、3 次 4.5s……这样一路涨上去，避免服务器挂了还被打
    const delay = Math.min(this.reconnectDelay * Math.pow(1.5, this.reconnectAttempts - 1), 30000)
    logger.info(`[SocketManager] Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts})`)
    this.reconnectTimer = setTimeout(() => {
      const token = localStorage.getItem('token')
      if (token && this.url) {
        this.connectSSE(this.url, token)
      } else {
        logger.warn('[SocketManager] No token available, staying in polling mode')
        this.transport = 'polling'
      }
    }, delay)
  }

  // 页面在这里挂回调，返回值一调用就取消，省得自己记着 off
  on(event, callback) {
    if (!this.listeners[event]) this.listeners[event] = []
    this.listeners[event].push(callback)
    return () => this.off(event, callback)
  }

  off(event, callback) {
    if (!this.listeners[event]) return
    this.listeners[event] = this.listeners[event].filter(cb => cb !== callback)
  }

  // 消息分发：按事件名把数据挨个交给注册过的回调，某个回调报错不影响其他回调
  emit(event, data) {
    this.listeners[event]?.forEach(cb => {
      try { cb(data) } catch (e) { logger.error('[SocketManager] Listener error', e) }
    })
  }

  subscribe(channel) {
    this.subscriptions.add(channel)
  }

  unsubscribe(channel) {
    this.subscriptions.delete(channel)
  }

  clearTimers() {
    if (this.reconnectTimer) { clearTimeout(this.reconnectTimer); this.reconnectTimer = null }
  }
}

const socketManager = new SocketManager()
export default socketManager
