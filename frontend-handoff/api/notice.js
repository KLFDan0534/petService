import request from '@/utils/request'

export async function getActiveNotices(params) {
  const res = await request.get('/api/notices/active', { params })
  return res.data
}

export async function getUnreadNotices() {
  const res = await request.get('/api/notices/unread')
  return res.data
}

export async function getNotices(params) {
  const res = await request.get('/api/notices', { params })
  return res.data
}

export async function createNotice(data) {
  const res = await request.post('/api/notices', data)
  return res.data
}

export async function updateNotice(id, data) {
  const res = await request.post(`/api/notices/${id}`, data)
  return res.data
}

export async function deleteNotice(id) {
  const res = await request.delete(`/api/notices/${id}`)
  return res.data
}

export async function markNoticeRead(id) {
  const res = await request.post(`/api/notices/${id}/read`, {})
  return res.data
}

export async function getNoticeById(id) {
  const res = await request.get(`/api/notices/${id}`)
  return res.data
}

export async function getPopupNotices() {
  const res = await request.get('/api/notices/popup')
  return res.data
}

export async function dismissPopup(id) {
  const res = await request.post(`/api/notices/${id}/dismiss-popup`, {})
  return res.data
}

