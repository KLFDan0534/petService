import { defineStore } from 'pinia'
import { ref, onUnmounted } from 'vue'
import { useAuthStore } from './auth'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  const notifications = ref([])
  let pollingInterval = null

  async function fetchUnreadCount() {
    const authStore = useAuthStore()
    if (!authStore.isLoggedIn) return
    try {
      const r = await authStore.apiGet('/api/notifications/unread-count')
      if (r.code === 200) unreadCount.value = r.data.count
    } catch (e) {}
  }

  async function fetchNotifications() {
    const authStore = useAuthStore()
    if (!authStore.isLoggedIn) return
    try {
      const r = await authStore.apiGet('/api/notifications')
      if (r.code === 200) notifications.value = r.data
    } catch (e) {}
  }

  async function markAsRead(id) {
    const authStore = useAuthStore()
    try {
      const r = await authStore.apiPost(`/api/notifications/${id}/read`, {})
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
      const r = await authStore.apiPost('/api/notifications/read-all', {})
      if (r.code === 200) {
        notifications.value.forEach(n => { n.is_read_wsh = 1 })
        unreadCount.value = 0
      }
    } catch (e) {}
  }

  function startPolling(intervalMs = 30000) {
    stopPolling()
    fetchUnreadCount()
    pollingInterval = setInterval(fetchUnreadCount, intervalMs)
  }

  function stopPolling() {
    if (pollingInterval) {
      clearInterval(pollingInterval)
      pollingInterval = null
    }
  }

  return { unreadCount, notifications, fetchUnreadCount, fetchNotifications, markAsRead, markAllAsRead, startPolling, stopPolling }
})
