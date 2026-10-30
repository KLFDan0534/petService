import request from '@/utils/request'

// 一个订单寄养期间的全部照护记录，用户看"今天喂了几次、遛没遛"就靠它
export async function getCareRecordsByOrder(orderId) {
  const res = await request.get(`/api/care-records/order/${orderId}`)
  return res.data
}

export async function createCareRecord(data) {
  const res = await request.post('/api/care-records', data)
  return res.data
}

export async function getCareRecord(id) {
  const res = await request.get(`/api/care-records/${id}`)
  return res.data
}

export async function updateCareRecord(id, data) {
  const res = await request.put(`/api/care-records/${id}`, data)
  return res.data
}

export async function deleteCareRecord(id) {
  const res = await request.delete(`/api/care-records/${id}`)
  return res.data
}

// 照护照片、视频先走这个上传，拿到文件地址再挂到记录上
export async function uploadCareRecord(formData) {
  const res = await request.post('/api/care-records/upload', formData)
  return res.data
}



