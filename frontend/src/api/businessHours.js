import request from '@/utils/request'

export async function getBusinessHours(merchantId) {
  const res = await request.get(`/api/merchants/${merchantId}/hours`)
  return res.data
}

// 新增和修改共用一个接口：带 id 就是改那条，不带 id 就是新加
export async function upsertBusinessHours(merchantId, data) {
  const res = await request.post(`/api/merchants/${merchantId}/hours`, data)
  return res.data
}

export async function deleteBusinessHour(merchantId, id) {
  const res = await request.delete(`/api/merchants/${merchantId}/hours/${id}`)
  return res.data
}
