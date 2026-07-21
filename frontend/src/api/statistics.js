import request from '@/utils/request'

export async function getUserStatistics() {
  const res = await request.get('/api/statistics/user')
  return res.data
}

export async function getMerchantStatistics() {
  const res = await request.get('/api/statistics/merchant')
  return res.data
}

export async function getAdminStatistics() {
  const res = await request.get('/api/statistics/admin')
  return res.data
}

export async function getReputationStatistics(params) {
  const res = await request.get('/api/statistics/reputation', { params })
  return res.data
}


