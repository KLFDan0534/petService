import request from '@/utils/request'

export async function getConversations(params) {
  const res = await request.get('/api/chat/conversation', { params })
  return res.data
}

export async function sendChatMessage(data) {
  const res = await request.post('/api/chat/send', data)
  return res.data
}

// 用户第一次找客服时调它，系统给分一个空闲客服，之后就固定在这个会话里
export async function assignCustomerServiceAgent() {
  const res = await request.post('/api/chat/assign-agent')
  return res.data
}

// 打开会话时把里面所有消息一次标已读；只标单条用下面那个
export async function markConversationRead(data) {
  const res = await request.post('/api/chat/read-conversation', data)
  return res.data
}

export async function markMessageRead(id) {
  const res = await request.post(`/api/chat/read/${id}`)
  return res.data
}



