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

// 价格预览：与下单走同一套后端计价，返回的金额就是提交后的实付
export async function previewOrder(data) {
  const res = await request.post('/api/orders/preview', data)
  return res.data
}

// 指派：商家把待分配订单分给指定看护者
export async function assignOrder(data) {
  const res = await request.post('/api/orders/assign', data)
  return res.data
}

// 取消订单，用户和商家两边都调这一个
export async function cancelOrder(data) {
  const res = await request.post('/api/orders/cancel', data)
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

// 我自己接下的单，宠托师工作台用
export async function getMyKeeperOrders() {
  const res = await request.get('/api/orders/my-keeper')
  return res.data
}

// 待分配订单池：本商家名下已支付但还没有负责人的单，宠托师工作台「待抢单」用
export async function getPoolOrders() {
  const res = await request.get('/api/orders/pool')
  return res.data
}

// 抢单：把池子里的单接到自己名下。body 传 { order_no_wsh }
export async function claimOrder(data) {
  const res = await request.post('/api/orders/claim', data)
  return res.data
}

// @deprecated 后端 /orders/merchant 是假分页（total 取 list.size，page/size 只回显）。
// 商家订单列表请用 scrollMerchantOrders。
export async function getMerchantOrders(page = 1, size = 100) {
  const res = await request.get(`/orders/merchant?page=${page}&size=${size}`)
  return res.data
}

// ===== 游标分页（C 端滚动加载）：首页不传 cursor，下一页传上次返回的 next_cursor_wsh =====

// 我的订单
export async function scrollMyOrders(params = {}) {
  const res = await request.get('/orders/scroll', { params })
  return res.data
}

// 商家订单
export async function scrollMerchantOrders(params = {}) {
  const res = await request.get('/orders/merchant/scroll', { params })
  return res.data
}

// 看护者任务
export async function scrollKeeperOrders(params = {}) {
  const res = await request.get('/orders/keeper/scroll', { params })
  return res.data
}

// 待分配抢单池（按创建时间升序，先到先得）
export async function scrollPoolOrders(params = {}) {
  const res = await request.get('/orders/pool/scroll', { params })
  return res.data
}

// 订单各状态计数（标签数字用）：返回 { status_wsh: count }
// 列表分页后前端只持有已加载的一页，标签数字必须由服务端按全量算
export async function getMyOrderStats() {
  const res = await request.get('/orders/stats')
  return res.data
}

export async function getMerchantOrderStats() {
  const res = await request.get('/orders/merchant/stats')
  return res.data
}

// 后台全平台订单（页码分页，可跳页/对账）
export async function pageAdminOrders(params = {}) {
  const res = await request.get('/orders/admin/page', { params })
  return res.data
}

// 服务时间线，一天一条记录，用户能看见每天干了啥
export async function getTimeline(orderId) {
  const res = await request.get(`/api/order-fulfillments/${orderId}/timeline`)
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

// 把某笔订单的会话标已读：工作区点开订单后调用，红点清零
export async function markConversationRead(orderId, params = {}) {
  const res = await request.post(`/api/order-fulfillments/${orderId}/conversation/read`, null, { params })
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
