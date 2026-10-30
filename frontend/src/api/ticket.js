import request from '@/utils/request'

// 我提过的工单，能看到处理到哪一步了
export async function getMyTickets() {
  const res = await request.get('/api/tickets/me')
  return res.data
}

// 客服后台的工单列表，状态、时间这些条件拼在 filters 里
export async function getTickets(page = 1, size = 20, filters = {}) {
  const params = { page, size, ...filters }
  const res = await request.get('/api/tickets', { params })
  return res.data
}

// 用户提工单，把问题描述和截图一起交上去
export async function createTicket(data) {
  const res = await request.post('/api/tickets', data)
  return res.data
}

// 工单里的来回沟通，用户和客服在同一个串里
export async function getTicketMessages(id) {
  const res = await request.get(`/api/tickets/${id}/messages`)
  return res.data
}

export async function getTicket(id) {
  const res = await request.get(`/api/tickets/${id}`)
  return res.data
}

export async function sendTicketMessage(id, data) {
  const res = await request.post(`/api/tickets/${id}/messages`, data)
  return res.data
}

// 把工单派给某个客服，不传人就等于自己接
export async function assignTicket(id, assigneeId) {
  const data = assigneeId ? { assignee_id_wsh: assigneeId } : {}
  const res = await request.post(`/api/tickets/${id}/assign`, data)
  return res.data
}

// 工单处理完，写上结论回给用户
export async function resolveTicket(id, result = '工单已处理完成') {
  const res = await request.post(`/api/tickets/${id}/resolve`, { result_wsh: result })
  return res.data
}

export async function closeTicket(id) {
  const res = await request.post(`/api/tickets/${id}/close`, {})
  return res.data
}



