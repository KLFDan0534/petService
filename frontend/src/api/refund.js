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

// 商家点头同意退款；同意不等于到账，后面还得走 completeRefund 才真退钱
export async function approveRefund(id) {
  const res = await request.post(`/api/refunds/${id}/approve`, {})
  return res.data
}

export async function rejectRefund(id) {
  const res = await request.post(`/api/refunds/${id}/reject`, {})
  return res.data
}

// 钱真的退回用户账户了，退款单到这一步才算走完
export async function completeRefund(id) {
  const res = await request.post(`/api/refunds/${id}/complete`, {})
  return res.data
}



