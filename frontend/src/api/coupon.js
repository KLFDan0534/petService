import request from '@/utils/request'

export async function getCouponTemplates() {
  const res = await request.get('/api/coupons/templates')
  return res.data
}

export async function getActiveCouponTemplates() {
  const res = await request.get('/api/coupons/templates/active')
  return res.data
}

export async function createCouponTemplate(data) {
  const res = await request.post('/api/coupons/templates', data)
  return res.data
}

export async function updateCouponTemplateStatus(id, status) {
  const res = await request.post(`/api/coupons/templates/${id}/status`, { status_wsh: status })
  return res.data
}

export async function grantCouponToUser(id, data) {
  const res = await request.post(`/api/coupons/templates/${id}/grant`, data)
  return res.data
}

export async function grantCouponToAll(id, data) {
  const res = await request.post(`/api/coupons/templates/${id}/grant-all`, data)
  return res.data
}

export async function grantCouponByCondition(id, data) {
  const res = await request.post(`/api/coupons/templates/${id}/grant-condition`, data)
  return res.data
}

export async function claimCoupon(id) {
  const res = await request.post(`/api/coupons/templates/${id}/claim`, {})
  return res.data
}

export async function getMyCoupons() {
  const res = await request.get('/api/coupons/my')
  return res.data
}

export async function getAvailableCoupons(params) {
  const res = await request.get('/api/coupons/available', { params })
  return res.data
}

export async function quoteCoupon(data) {
  const res = await request.post('/api/coupons/quote', data)
  return res.data
}
