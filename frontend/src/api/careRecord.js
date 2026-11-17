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
