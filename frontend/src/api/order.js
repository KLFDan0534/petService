import request from '@/utils/request'

export async function getOrders() {
  const res = await request.get('/api/orders')
  return res.data
}

export async function getOrder(id) {
  const res = await request.get(`/api/orders/${id}`)
  return res.data
}

export async function createOrder(data) {
  const res = await request.post('/api/orders', data)
  return res.data
}

export async function createOrdersBatch(data) {
  const res = await request.post('/api/orders/batch', data)
  return res.data
}

export async function cancelOrder(data) {
  const res = await request.post('/api/orders/cancel', data)
  return res.data
}

export async function confirmDelivered(data) {
  const res = await request.post('/api/orders/delivered', data)
  return res.data
}

export async function acceptOrder(data) {
  const res = await request.post('/api/orders/accept', data)
  return res.data
}

export async function rejectOrder(data) {
  const res = await request.post('/api/orders/reject', data)
  return res.data
}

export async function markReceived(data) {
  const res = await request.post('/api/orders/received', data)
  return res.data
}

export async function startService(data) {
  const res = await request.post('/api/orders/start/upload', data)
  return res.data
}

export async function completeOrder(data) {
  const res = await request.post('/api/orders/complete', data)
  return res.data
}

export async function updateOrderStatus(id, data) {
  const res = await request.put(`/api/orders/${id}/status`, data)
  return res.data
}

export async function deleteOrder(id) {
  const res = await request.delete(`/api/orders/${id}`)
  return res.data
}

export async function getPendingOrders() {
  const res = await request.get('/api/orders/pending')
  return res.data
}

export async function getMyKeeperOrders() {
  const res = await request.get('/api/orders/my-keeper')
  return res.data
}

export async function getMerchantOrders(page = 1, size = 100) {
  const res = await request.get(`/orders/merchant?page=${page}&size=${size}`)
  return res.data
}

export async function getTimeline(orderId) {
  const res = await request.get(`/api/order-fulfillments/${orderId}/timeline`)
  return res.data
}

export async function getDailyStatus(orderId) {
  const res = await request.get(`/api/order-fulfillments/${orderId}/daily-status`)
  return res.data
}

export async function getConversation(orderId) {
  const res = await request.get(`/api/order-fulfillments/${orderId}/conversation`)
  return res.data
}

export async function sendMessage(orderId, data) {
  const res = await request.post(`/api/order-fulfillments/${orderId}/conversation`, data)
  return res.data
}

export async function sendMessageWithFile(orderId, formData) {
  const res = await request.post(`/api/order-fulfillments/${orderId}/conversation/upload`, formData)
  return res.data
}

export async function uploadTimeline(orderId, formData) {
  const res = await request.post(`/api/order-fulfillments/${orderId}/timeline/upload`, formData)
  return res.data
}



