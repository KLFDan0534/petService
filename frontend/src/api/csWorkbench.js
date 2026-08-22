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

export async function getCsThreads() {
  const res = await request.get('/api/cs-workbench/threads')
  return res.data
}

export async function markCsThreadRead(type, bizId) {
  const res = await request.post('/api/cs-workbench/threads/read', null, { params: { type, bizId } })
  return res.data
}
