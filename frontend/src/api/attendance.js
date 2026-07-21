import request from '@/utils/request'

export async function getCurrentAttendance() {
  const res = await request.get('/api/keeper-attendance/me/current')
  return res.data
}

export async function getTodayAttendance() {
  const res = await request.get('/api/keeper-attendance/me/today')
  return res.data
}

export async function checkIn(data) {
  const res = await request.post('/api/keeper-attendance/check-in', data)
  return res.data
}

export async function checkOut(data) {
  const res = await request.post('/api/keeper-attendance/check-out', data)
  return res.data
}

export async function getMerchantTodayAttendance() {
  const res = await request.get('/api/keeper-attendance/merchant/today')
  return res.data
}

export async function getMerchantLeaves() {
  const res = await request.get('/api/keeper-leaves/merchant')
  return res.data
}

export async function createMerchantLeave(data) {
  const res = await request.post('/api/keeper-leaves/merchant', data)
  return res.data
}

export async function deleteMerchantLeave(id) {
  const res = await request.delete(`/api/keeper-leaves/merchant/${id}`)
  return res.data
}
