import request from '@/utils/request'

export async function getPayments() {
  const res = await request.get('/api/payments')
  return res.data
}

export async function getPaymentByOrder(orderNo) {
  const res = await request.get(`/api/payments/order/${orderNo}`)
  return res.data
}

// 先建支付单拿到支付参数，这一步还没扣钱
export async function createPayment(data) {
  const res = await request.post('/api/payments/create', data)
  return res.data
}

// 用户点"确认支付"后才调这个，钱是从这一步开始扣的
export async function executePayment(data) {
  const res = await request.post('/api/payments/pay', data)
  return res.data
}

export async function getAdminPayments(params = {}) {
  const res = await request.get('/api/payments/admin-list', { params })
  return res.data
}



