import request from '@/utils/request'

export async function getMerchants() {
  const res = await request.get('/api/merchants')
  return res.data
}

export async function getMerchant(id) {
  const res = await request.get(`/api/merchants/${id}`)
  return res.data
}

export async function getMyMerchant() {
  const res = await request.get('/api/merchants/my')
  return res.data
}

export async function getNearbyMerchants(params) {
  const res = await request.get('/api/merchants/nearby', { params })
  return res.data
}

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

export async function approveMerchant(id) {
  const res = await request.post(`/api/merchants/${id}/approve`, {})
  return res.data
}

export async function rejectMerchant(id) {
  const res = await request.post(`/api/merchants/${id}/reject`, {})
  return res.data
}

export async function updateMerchantStatus(id, status) {
  const res = await request.put(`/api/merchants/${id}/status`, { status_wsh: status })
  return res.data
}

export async function updateMerchantStoreMode(id, storeMode) {
  const res = await request.patch(`/api/merchants/${id}/store-mode`, { store_mode_wsh: storeMode })
  return res.data
}



