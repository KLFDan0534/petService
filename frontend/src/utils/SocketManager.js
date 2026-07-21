import { logger } from '@/utils/logger'

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

  connectSSE(url, token) {
    this.disconnect()
    this.url = url
    this.token = token
    this.transport = 'sse'
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

  scheduleReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      logger.warn('[SocketManager] Max reconnect attempts reached, staying in polling mode')
      this.transport = 'polling'
      return
    }
    this.reconnectAttempts++
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

  on(event, callback) {
    if (!this.listeners[event]) this.listeners[event] = []
    this.listeners[event].push(callback)
    return () => this.off(event, callback)
  }

  off(event, callback) {
    if (!this.listeners[event]) return
    this.listeners[event] = this.listeners[event].filter(cb => cb !== callback)
  }

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
