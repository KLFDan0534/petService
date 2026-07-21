import request from '@/utils/request'

export async function getOrderDetail(id) {
  const res = await request.get(`/orders/${id}`)
  return res.data
}

export async function getOrderTimeline(orderId, page = 1) {
  const res = await request.get(`/order-fulfillments/${orderId}/timeline`, { params: { page } })
  return res.data
}

export async function getDailyStatus(orderId) {
  const res = await request.get(`/order-fulfillments/${orderId}/daily-status`)
  return res.data
}

export async function getOrderMessages(orderId, params = {}) {
  const res = await request.get(`/order-fulfillments/${orderId}/conversation`, { params })
  return res.data
}

export async function sendMessage(orderId, data) {
  const res = await request.post(`/order-fulfillments/${orderId}/conversation`, data)
  return res.data
}

export async function sendMessageWithFile(orderId, formData) {
  const res = await request.post(`/order-fulfillments/${orderId}/conversation/upload`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return res.data
}

export async function markOrderConversationRead(orderId, params = {}) {
  const res = await request.post(`/order-fulfillments/${orderId}/conversation/read`, null, { params })
  return res.data
}

export async function getOrderReport(orderId) {
  const res = await request.get(`/ai/reports/order/${orderId}`)
  return res.data
}

export async function regenerateReport(data) {
  const res = await request.post('/ai/boarding-report', data)
  return res.data
}

export async function createPayment(data) {
  const res = await request.post('/payments/create', data)
  return res.data
}

export async function executePayment(data) {
  const res = await request.post('/payments/pay', data)
  return res.data
}

export async function cancelOrder(data) {
  const res = await request.post('/orders/cancel', data)
  return res.data
}


