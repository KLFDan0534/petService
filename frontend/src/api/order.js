import request from '@/utils/request'

// 查我自己的订单列表，用户端"我的订单"用这个
export async function getOrders() {
  const res = await request.get('/api/orders')
  return res.data
}

export async function getOrder(id) {
  const res = await request.get(`/api/orders/${id}`)
  return res.data
}

// 下单，选好服务、时间和宠物后提交，生成一笔待付款订单
export async function createOrder(data) {
  const res = await request.post('/api/orders', data)
  return res.data
}

// 一次下好几只宠物的单，合在一起只付一次钱
export async function createOrdersBatch(data) {
  const res = await request.post('/api/orders/batch', data)
  return res.data
}

// 取消订单，用户和商家两边都调这一个
export async function cancelOrder(data) {
  const res = await request.post('/api/orders/cancel', data)
  return res.data
}

export async function confirmDelivered(data) {
  const res = await request.post('/api/orders/delivered', data)
  return res.data
}

// 接单，商家或宠托师点了同意才会进服务流程
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

// 开始服务，顺手把到店照片传上去当凭证
export async function startService(data) {
  const res = await request.post('/api/orders/start/upload', data)
  return res.data
}

// 服务做完了提交完成，等用户那边确认收货
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

// 待接的单，接单大厅那页刷的就是它
export async function getPendingOrders() {
  const res = await request.get('/api/orders/pending')
  return res.data
}

// 我自己接下的单，宠托师工作台用
export async function getMyKeeperOrders() {
  const res = await request.get('/api/orders/my-keeper')
  return res.data
}

// 商家后台的订单列表，分页拉，一次默认 100 条
export async function getMerchantOrders(page = 1, size = 100) {
  const res = await request.get(`/orders/merchant?page=${page}&size=${size}`)
  return res.data
}

// 服务时间线，一天一条记录，用户能看见每天干了啥
export async function getTimeline(orderId) {
  const res = await request.get(`/api/order-fulfillments/${orderId}/timeline`)
  return res.data
}

export async function getFulfillmentOverview(orderId) {
  const res = await request.get(`/api/order-fulfillments/${orderId}`)
  return res.data
}

export async function createTimeline(orderId, data) {
  const res = await request.post(`/api/order-fulfillments/${orderId}/timeline`, data)
  return res.data
}

// 每天的状态小结，吃饭、遛弯、排便这些，给用户报平安
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

// 订单里的聊天发图，表单里塞文件所以不设 JSON 头
export async function sendMessageWithFile(orderId, formData) {
  const res = await request.post(`/api/order-fulfillments/${orderId}/conversation/upload`, formData)
  return res.data
}

export async function uploadTimeline(orderId, formData) {
  const res = await request.post(`/api/order-fulfillments/${orderId}/timeline/upload`, formData)
  return res.data
}



