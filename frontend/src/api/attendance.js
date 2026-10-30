import request from '@/utils/request'

// 看我现在打没打卡，按钮显示"上班"还是"下班"靠它
export async function getCurrentAttendance() {
  const res = await request.get('/api/keeper-attendance/me/current')
  return res.data
}

// 今天的考勤小结，打了几次卡、一共待了多久
export async function getTodayAttendance() {
  const res = await request.get('/api/keeper-attendance/me/today')
  return res.data
}

// 上班打卡，要带当前位置，商家那边能看到
export async function checkIn(data) {
  const res = await request.post('/api/keeper-attendance/check-in', data)
  return res.data
}

// 下班打卡，打完这段工时就算出来了
export async function checkOut(data) {
  const res = await request.post('/api/keeper-attendance/check-out', data)
  return res.data
}

// 商家看今天店里谁到了、谁还没来
export async function getMerchantTodayAttendance() {
  const res = await request.get('/api/keeper-attendance/merchant/today')
  return res.data
}

export async function getMerchantLeaves() {
  const res = await request.get('/api/keeper-leaves/merchant')
  return res.data
}

// 商家替员工补录一条请假，员工没手机时用得上
export async function createMerchantLeave(data) {
  const res = await request.post('/api/keeper-leaves/merchant', data)
  return res.data
}

export async function deleteMerchantLeave(id) {
  const res = await request.delete(`/api/keeper-leaves/merchant/${id}`)
  return res.data
}

export async function getAdminAttendance(params = {}) {
  const res = await request.get('/api/keeper-attendance/admin-list', { params })
  return res.data
}

export async function getAdminLeaves(params = {}) {
  const res = await request.get('/api/keeper-leaves/admin-list', { params })
  return res.data
}

// 批请假，批了这段时间就不算出勤
export async function approveLeave(id, reason = '') {
  const res = await request.post(`/api/keeper-leaves/${id}/approve`, { reason })
  return res.data
}

export async function rejectLeave(id, reason = '') {
  const res = await request.post(`/api/keeper-leaves/${id}/reject`, { reason })
  return res.data
}
