import request from '@/utils/request'

export async function getMyRefunds() {
  const res = await request.get('/api/refunds')
  return res.data
}

export async function getRefunds() {
  const res = await request.get('/api/refunds/all')
  return res.data
}

export async function createRefund(data) {
  const res = await request.post('/api/refunds', data)
  return res.data
}

export async function approveRefund(id) {
  const res = await request.post(`/api/refunds/${id}/approve`, {})
  return res.data
}

export async function rejectRefund(id) {
  const res = await request.post(`/api/refunds/${id}/reject`, {})
  return res.data
}

export async function completeRefund(id) {
  const res = await request.post(`/api/refunds/${id}/complete`, {})
  return res.data
}



