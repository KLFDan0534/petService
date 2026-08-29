import request from '@/utils/request'

export async function getConversations(params) {
  const res = await request.get('/api/chat/conversation', { params })
  return res.data
}

export async function sendChatMessage(data) {
  const res = await request.post('/api/chat/send', data)
  return res.data
}

export async function assignCustomerServiceAgent() {
  const res = await request.post('/api/chat/assign-agent')
  return res.data
}

export async function markConversationRead(data) {
  const res = await request.post('/api/chat/read-conversation', data)
  return res.data
}

export async function markMessageRead(id) {
  const res = await request.post(`/api/chat/read/${id}`)
  return res.data
}



