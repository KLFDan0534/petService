import request from '@/utils/request'

export async function getUserStatistics() {
  const res = await request.get('/api/statistics/user')
  return res.data
}

// 商家后台首页的经营数据，营业额、订单数这些
export async function getMerchantStatistics() {
  const res = await request.get('/api/statistics/merchant')
  return res.data
}

export async function getAdminStatistics() {
  const res = await request.get('/api/statistics/admin')
  return res.data
}

// 信誉分相关的统计，params 里传起止时间按区间查
export async function getReputationStatistics(params) {
  const res = await request.get('/api/statistics/reputation', { params })
  return res.data
}


