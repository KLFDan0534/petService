import request from '@/utils/request'

// 后台看会员套餐列表，下架的也能看到
export async function getAdminMemberPlans(params) {
  const res = await request.get('/api/membership/admin/plans', { params })
  return res.data
}

// 用户端能买的套餐，只返回在售的那些
export async function getActiveMemberPlans() {
  const res = await request.get('/api/membership/plans/active')
  return res.data
}

// 买会员，先出一笔待付款的会员订单
export async function createMembershipOrder(data) {
  const res = await request.post('/api/membership/orders', data)
  return res.data
}

// 我买过的会员记录，含没付款和已过期的
export async function getMyMembershipOrders() {
  const res = await request.get('/api/membership/orders')
  return res.data
}

export async function getMembershipOrder(orderNo) {
  const res = await request.get(`/api/membership/orders/${orderNo}`)
  return res.data
}

// 付这笔会员订单的钱，付完会员立刻生效
export async function payMembershipOrder(orderNo) {
  const res = await request.post(`/api/membership/orders/${orderNo}/pay`)
  return res.data
}

export async function cancelMembershipOrder(orderNo) {
  const res = await request.post(`/api/membership/orders/${orderNo}/cancel`)
  return res.data
}

// 后台的会员订单列表，可按用户和时间筛
export async function getAdminMembershipOrders(params) {
  const res = await request.get('/api/membership/orders/admin/list', { params })
  return res.data
}

// 后台手工标记成已付款，线下转账收不到回执时用
export async function confirmMembershipOrderPaid(orderNo) {
  const res = await request.post(`/api/membership/orders/admin/${orderNo}/pay`)
  return res.data
}

// 看我现在是什么等级会员、什么时候到期
export async function getMyMembership() {
  const res = await request.get('/api/membership/me')
  return res.data
}

// 算一下这单会员能便宜多少，结算页下单前先问一次
export async function quoteMembershipOrderDiscount(amount) {
  const res = await request.get('/api/membership/benefits/order/quote', {
    params: { amount_wsh: amount },
  })
  return res.data
}

// 我的权益用了几次，比如洗护折扣还剩多少回
export async function getMembershipUsages() {
  const res = await request.get('/api/membership/usages')
  return res.data
}

export async function getAdminMembershipUsers(params) {
  const res = await request.get('/api/membership/admin/users', { params })
  return res.data
}

export async function getAdminMembershipUsages(params) {
  const res = await request.get('/api/membership/admin/usages', { params })
  return res.data
}

export async function expireMembershipsForAdmin() {
  const res = await request.post('/api/membership/admin/users/expire')
  return res.data
}

export async function createMemberPlan(data) {
  const res = await request.post('/api/membership/admin/plans', data)
  return res.data
}

export async function updateMemberPlan(id, data) {
  const res = await request.put(`/api/membership/admin/plans/${id}`, data)
  return res.data
}

// 改套餐上下架状态，下架后用户就买不到了
export async function updateMemberPlanStatus(id, status) {
  const res = await request.post(`/api/membership/admin/plans/${id}/status`, { status_wsh: status })
  return res.data
}

export async function deleteMemberPlan(id) {
  const res = await request.delete(`/api/membership/admin/plans/${id}`)
  return res.data
}

export async function getAdminMemberPlan(id) {
  const res = await request.get(`/api/membership/admin/plans/${id}`)
  return res.data
}
