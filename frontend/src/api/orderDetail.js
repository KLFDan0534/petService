import request from '@/utils/request'

// 订单详情页的主数据，跟 order.js 里那条是同一个接口
export async function getOrderDetail(id) {
  const res = await request.get(`/api/orders/${id}`)
  return res.data
}

// 时间线按页往下翻，详情页滑动加载更多
export async function getOrderTimeline(orderId, page = 1) {
  const res = await request.get(`/api/order-fulfillments/${orderId}/timeline`, { params: { page } })
  return res.data
}

export async function getDailyStatus(orderId) {
  const res = await request.get(`/api/order-fulfillments/${orderId}/daily-status`)
  return res.data
}

// 这单的聊天记录，用户和宠托师都看这一份
export async function getOrderMessages(orderId, params = {}) {
  const res = await request.get(`/api/order-fulfillments/${orderId}/conversation`, { params })
  return res.data
}

export async function sendMessage(orderId, data) {
  const res = await request.post(`/api/order-fulfillments/${orderId}/conversation`, data)
  return res.data
}

// 聊天发图片，得自己写上 multipart 头，不然传不上去
export async function sendMessageWithFile(orderId, formData) {
  const res = await request.post(`/api/order-fulfillments/${orderId}/conversation/upload`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return res.data
}

// 把聊天标成已读，那个小红点就没了
export async function markOrderConversationRead(orderId, params = {}) {
  const res = await request.post(`/api/order-fulfillments/${orderId}/conversation/read`, null, { params })
  return res.data
}

// 这单的寄养报告，详情页里单独一块展示
export async function getOrderReport(orderId) {
  const res = await request.get(`/api/ai/reports/order/${orderId}`)
  return res.data
}

// 报告写得不行就重出一份，会覆盖原来那份
export async function regenerateReport(data) {
  const res = await request.post('/api/ai/boarding-report', data)
  return res.data
}

// 发起支付，后端返回拉起支付要用的参数
export async function createPayment(data) {
  const res = await request.post('/api/payments/create', data)
  return res.data
}

// 用户付完钱回传结果，后端核对后订单才算付了
export async function executePayment(data) {
  const res = await request.post('/api/payments/pay', data)
  return res.data
}

export async function cancelOrder(data) {
  const res = await request.post('/api/orders/cancel', data)
  return res.data
}


