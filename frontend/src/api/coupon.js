import request from '@/utils/request'

export async function getCouponTemplates() {
  const res = await request.get('/api/coupons/templates')
  return res.data
}

// 用户能领的券，券中心列表就是它
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

// 定向发券，客服安抚用户时常用
export async function grantCouponToUser(id, data) {
  const res = await request.post(`/api/coupons/templates/${id}/grant`, data)
  return res.data
}

export async function grantCouponToAll(id, data) {
  const res = await request.post(`/api/coupons/templates/${id}/grant-all`, data)
  return res.data
}

// 按条件圈一批人发券，比如只发给新注册的
export async function grantCouponByCondition(id, data) {
  const res = await request.post(`/api/coupons/templates/${id}/grant-condition`, data)
  return res.data
}

// 用户自己点"领取"，领完进我的券包
export async function claimCoupon(id) {
  const res = await request.post(`/api/coupons/templates/${id}/claim`, {})
  return res.data
}

// 我券包里所有的券，用过没过期都列出来
export async function getMyCoupons() {
  const res = await request.get('/api/coupons/my')
  return res.data
}

export async function getAvailableCoupons(params) {
  const res = await request.get('/api/coupons/available', { params })
  return res.data
}

// 算一下用券能减多少钱，结算页选券时调
export async function quoteCoupon(data) {
  const res = await request.post('/api/coupons/quote', data)
  return res.data
}
