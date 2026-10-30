import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useAuthStore } from './auth'
import { getUnreadCount, getNotifications, markAsRead as apiMarkRead, markAllAsRead as apiMarkAllAsRead } from '@/api/notification'
import socketManager from '@/utils/SocketManager'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  const notifications = ref([])
  let cleanup = null

  // 没登录就别查了，接口只会返回 401
  async function fetchUnreadCount() {
    const authStore = useAuthStore()
    if (!authStore.isLoggedIn) return
    try {
      const r = await getUnreadCount()
      if (r.code === 200) unreadCount.value = r.data.count
    } catch (e) {}
  }

  async function fetchNotifications() {
    const authStore = useAuthStore()
    if (!authStore.isLoggedIn) return
    try {
      const r = await getNotifications()
      if (r.code === 200) notifications.value = r.data
    } catch (e) {}
  }

  // 标已读成功后就地改这一条，再减一个未读数，不用重拉整个列表
  async function markAsRead(id) {
    const authStore = useAuthStore()
    try {
      const r = await apiMarkRead(id)
      if (r.code === 200) {
        const notif = notifications.value.find(n => n.id_wsh === id)
        if (notif) notif.is_read_wsh = 1
        unreadCount.value = Math.max(0, unreadCount.value - 1)
      }
    } catch (e) {}
  }

  async function markAllAsRead() {
    const authStore = useAuthStore()
    try {
      const r = await apiMarkAllAsRead()
      if (r.code === 200) {
        notifications.value.forEach(n => { n.is_read_wsh = 1 })
        unreadCount.value = 0
      }
    } catch (e) {}
  }

  // 定时查一次未读数，同时挂个长连接，有新消息马上刷
  function startPolling(intervalMs = 60000) {
    stopPolling()
    fetchUnreadCount()
    const authStore = useAuthStore()
    if (authStore.token) {
      socketManager.connectSSE('/api/notification-events/stream', authStore.token)
    }
    const timer = setInterval(fetchUnreadCount, intervalMs)
    const unsub = socketManager.on('notification', () => {
      fetchUnreadCount()
      fetchNotifications()
    })
    // 把定时器和订阅一起收掉，不然来回切页面会越积越多
    cleanup = () => {
      clearInterval(timer)
      unsub()
      socketManager.disconnect()
    }
  }

  function stopPolling() {
    if (cleanup) {
      cleanup()
      cleanup = null
    }
  }

  return { unreadCount, notifications, fetchUnreadCount, fetchNotifications, markAsRead, markAllAsRead, startPolling, stopPolling }
})
