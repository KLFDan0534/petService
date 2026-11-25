import request from '@/utils/request'

export async function getMyRefunds() {
  const res = await request.get('/api/refunds')
  return res.data
}

export async function getRefunds() {
  const res = await request.get('/api/refunds/all')
  return res.data
}

// 分页版：后台退款审核列表用。原 /all 是全量返回
export async function getRefundsPage(params = {}) {
  const res = await request.get('/api/refunds/all/page', { params })
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

// 驳回必须写原因：用户只能从站内信看到这次驳回，没有理由等于没有解释
export async function rejectRefund(id, reason) {
  const res = await request.post(`/api/refunds/${id}/reject`, { reject_reason_wsh: reason })
  return res.data
}

// 钱真的退回用户账户了，退款单到这一步才算走完
export async function completeRefund(id) {
  const res = await request.post(`/api/refunds/${id}/complete`, {})
  return res.data
}



