import request from '@/utils/request'

export async function getCsStats() {
  const res = await request.get('/api/cs-workbench/stats')
  return res.data
}

export async function getCsMerchants() {
  const res = await request.get('/api/cs-workbench/merchants')
  return res.data
}

export async function getCsConversations() {
  const res = await request.get('/api/cs-workbench/conversations')
  return res.data
}

// threads 是客服自己的待办会话流，conversations 是按商家分组的，两个别搞混
export async function getCsThreads() {
  const res = await request.get('/api/cs-workbench/threads')
  return res.data
}

// 客服看过的会话标已读，type 用来区分是商家咨询还是用户咨询
export async function markCsThreadRead(type, bizId) {
  const res = await request.post('/api/cs-workbench/threads/read', null, { params: { type, bizId } })
  return res.data
}
