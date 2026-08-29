import request from '@/utils/request'

export async function getBusinessHours(merchantId) {
  const res = await request.get(`/api/merchants/${merchantId}/hours`)
  return res.data
}

export async function upsertBusinessHours(merchantId, data) {
  const res = await request.post(`/api/merchants/${merchantId}/hours`, data)
  return res.data
}

export async function deleteBusinessHour(merchantId, id) {
  const res = await request.delete(`/api/merchants/${merchantId}/hours/${id}`)
  return res.data
}
