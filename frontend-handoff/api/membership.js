import request from '@/utils/request'

export async function getAdminMemberPlans(params) {
  const res = await request.get('/api/membership/admin/plans', { params })
  return res.data
}

export async function getActiveMemberPlans() {
  const res = await request.get('/api/membership/plans/active')
  return res.data
}

export async function createMembershipOrder(data) {
  const res = await request.post('/api/membership/orders', data)
  return res.data
}

export async function getMyMembershipOrders() {
  const res = await request.get('/api/membership/orders')
  return res.data
}

export async function getMembershipOrder(orderNo) {
  const res = await request.get(`/api/membership/orders/${orderNo}`)
  return res.data
}

export async function payMembershipOrder(orderNo) {
  const res = await request.post(`/api/membership/orders/${orderNo}/pay`)
  return res.data
}

export async function cancelMembershipOrder(orderNo) {
  const res = await request.post(`/api/membership/orders/${orderNo}/cancel`)
  return res.data
}

export async function getAdminMembershipOrders(params) {
  const res = await request.get('/api/membership/orders/admin/list', { params })
  return res.data
}

export async function confirmMembershipOrderPaid(orderNo) {
  const res = await request.post(`/api/membership/orders/admin/${orderNo}/pay`)
  return res.data
}

export async function getMyMembership() {
  const res = await request.get('/api/membership/me')
  return res.data
}

export async function quoteMembershipOrderDiscount(amount) {
  const res = await request.get('/api/membership/benefits/order/quote', {
    params: { amount_wsh: amount },
  })
  return res.data
}

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

export async function updateMemberPlanStatus(id, status) {
  const res = await request.post(`/api/membership/admin/plans/${id}/status`, { status_wsh: status })
  return res.data
}

export async function deleteMemberPlan(id) {
  const res = await request.delete(`/api/membership/admin/plans/${id}`)
  return res.data
}
