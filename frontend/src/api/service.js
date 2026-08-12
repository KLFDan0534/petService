import request from '@/utils/request'

export async function getServices() {
  const res = await request.get('/services')
  return res.data
}

export async function getService(id) {
  const res = await request.get(`/services/${id}`)
  return res.data
}

export async function getServicesByMerchant(merchantId) {
  const res = await request.get(`/api/services/merchant/${merchantId}`)
  return res.data
}

export async function getMerchantServices(merchantId) {
  const res = await request.get(`/api/services/merchant/${merchantId}/manage`)
  return res.data
}

export async function createService(data) {
  const res = await request.post('/api/services', data)
  return res.data
}

export async function updateService(id, data) {
  const res = await request.put(`/api/services/${id}`, data)
  return res.data
}

export async function deleteService(id) {
  const res = await request.delete(`/api/services/${id}`)
  return res.data
}

export async function toggleServiceStatus(id) {
  const res = await request.post(`/api/services/${id}/toggle-status`)
  return res.data
}

export async function getServiceAvailability(serviceId, from, to, keeperId) {
  const params = { from, to }
  if (keeperId) params.keeperId = keeperId
  const res = await request.get(`/api/services/${serviceId}/availability`, { params })
  return res.data
}




