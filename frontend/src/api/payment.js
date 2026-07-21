import request from '@/utils/request'

export async function getPayments() {
  const res = await request.get('/api/payments')
  return res.data
}

export async function getPaymentByOrder(orderNo) {
  const res = await request.get(`/api/payments/order/${orderNo}`)
  return res.data
}

export async function createPayment(data) {
  const res = await request.post('/api/payments/create', data)
  return res.data
}

export async function executePayment(data) {
  const res = await request.post('/api/payments/pay', data)
  return res.data
}



