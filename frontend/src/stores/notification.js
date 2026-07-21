import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useAuthStore } from './auth'
import { getUnreadCount, getNotifications, markAsRead as apiMarkRead, markAllAsRead as apiMarkAllAsRead } from '@/api/notification'
import socketManager from '@/utils/SocketManager'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  const notifications = ref([])
  let cleanup = null

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
