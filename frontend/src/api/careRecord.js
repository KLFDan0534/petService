import request from '@/utils/request'

export async function getCareRecordsByOrder(orderId) {
  const res = await request.get(`/care-records/order/${orderId}`)
  return res.data
}

export async function createCareRecord(data) {
  const res = await request.post('/care-records', data)
  return res.data
}



