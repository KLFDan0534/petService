import request from '@/utils/request'

export async function getUnreadCount() {
  const res = await request.get('/api/notifications/unread-count')
  return res.data
}

export async function getNotifications() {
  const res = await request.get('/api/notifications')
  return res.data
}

export async function markAsRead(id) {
  const res = await request.post(`/api/notifications/${id}/read`, {})
  return res.data
}

export async function markAllAsRead() {
  const res = await request.post('/api/notifications/read-all', {})
  return res.data
}

export async function getAdminNotifications(params = {}) {
  const res = await request.get('/api/notifications/admin-list', { params })
  return res.data
}



