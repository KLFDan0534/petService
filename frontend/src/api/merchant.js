import request from '@/utils/request'

// 商家列表，用户端首页和搜索页都读它
export async function getMerchants() {
  const res = await request.get('/api/merchants')
  return res.data
}

export async function getMerchant(id) {
  const res = await request.get(`/api/merchants/${id}`)
  return res.data
}

// 我自己经营的那家店，商家后台进来先拉这个
export async function getMyMerchant() {
  const res = await request.get('/api/merchants/my')
  return res.data
}

// 按位置找附近门店，带上经纬度才算得出距离
export async function getNearbyMerchants(params) {
  const res = await request.get('/api/merchants/nearby', { params })
  return res.data
}

// 提交开店申请，审核通过前用户是搜不到的
export async function createMerchant(data) {
  const res = await request.post('/api/merchants', data)
  return res.data
}

export async function updateMerchant(id, data) {
  const res = await request.put(`/api/merchants/${id}`, data)
  return res.data
}

export async function deleteMerchant(id) {
  const res = await request.delete(`/api/merchants/${id}`)
  return res.data
}

// 平台审核通过，通过后这家店才对外营业
export async function approveMerchant(id) {
  const res = await request.post(`/api/merchants/${id}/approve`, {})
  return res.data
}

export async function rejectMerchant(id) {
  const res = await request.post(`/api/merchants/${id}/reject`, {})
  return res.data
}

// 改营业状态，停业后前台不再派单
export async function updateMerchantStatus(id, status) {
  const res = await request.put(`/api/merchants/${id}/status`, { status_wsh: status })
  return res.data
}

// 切换门店的经营方式，到店寄养还是上门服务
export async function updateMerchantStoreMode(id, storeMode) {
  const res = await request.patch(`/api/merchants/${id}/store-mode`, { store_mode_wsh: storeMode })
  return res.data
}



