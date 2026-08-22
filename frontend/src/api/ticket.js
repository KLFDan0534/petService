import request from '@/utils/request'

export async function getMyTickets() {
  const res = await request.get('/api/tickets/me')
  return res.data
}

export async function getTickets(page = 1, size = 20, filters = {}) {
  const params = { page, size, ...filters }
  const res = await request.get('/api/tickets', { params })
  return res.data
}

export async function createTicket(data) {
  const res = await request.post('/api/tickets', data)
  return res.data
}

export async function getTicketMessages(id) {
  const res = await request.get(`/api/tickets/${id}/messages`)
  return res.data
}

export async function sendTicketMessage(id, data) {
  const res = await request.post(`/api/tickets/${id}/messages`, data)
  return res.data
}

export async function assignTicket(id, assigneeId) {
  const data = assigneeId ? { assignee_id_wsh: assigneeId } : {}
  const res = await request.post(`/api/tickets/${id}/assign`, data)
  return res.data
}

export async function resolveTicket(id, result = '工单已处理完成') {
  const res = await request.post(`/api/tickets/${id}/resolve`, { result_wsh: result })
  return res.data
}

export async function closeTicket(id) {
  const res = await request.post(`/api/tickets/${id}/close`, {})
  return res.data
}



