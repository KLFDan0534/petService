import request from '@/utils/request'

// 正在展示的公告，首页公告栏用，过期的不会出来
export async function getActiveNotices(params) {
  const res = await request.get('/api/notices/active', { params })
  return res.data
}

// 我还没读的公告，消息铃铛上那个数字靠它
export async function getUnreadNotices() {
  const res = await request.get('/api/notices/unread')
  return res.data
}

// 后台公告管理列表，草稿和下线的都能看到
export async function getNotices(params) {
  const res = await request.get('/api/notices', { params })
  return res.data
}

// 发一条公告，发完前台就能看到
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

// 进站要弹的公告，一般是大促或者停机通知
export async function getPopupNotices() {
  const res = await request.get('/api/notices/popup')
  return res.data
}

// 关掉弹窗要告诉后端，不然下次进站还弹同一个
export async function dismissPopup(id) {
  const res = await request.post(`/api/notices/${id}/dismiss-popup`, {})
  return res.data
}

