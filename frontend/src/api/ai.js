import request from '@/utils/request'

// 问 AI 要一份照护建议，按宠物品种年龄这些信息生成
export async function getCareSuggestion(data) {
  const res = await request.post('/api/ai/care-suggestion', data)
  return res.data
}

// 生成一份寄养总结报告，把这段时间的吃喝拉撒整理成文字
export async function getBoardingReport(data) {
  const res = await request.post('/api/ai/boarding-report', data)
  return res.data
}

// 按订单号取这份寄养报告，用户端详情页展示用
export async function getAIReprot(orderId) {
  const res = await request.get(`/ai/reports/order/${orderId}`)
  return res.data
}

// 拿问题去知识库问答，回答里会带上引用的原文
export async function askRag(data) {
  const res = await request.post('/api/rag/ask', data)
  return res.data
}

// 只在知识库里搜相关片段，不生成答案，用来做搜索框联想
export async function searchRag(params) {
  const res = await request.get('/api/rag/search', { params })
  return res.data
}

export async function getRagDocuments() {
  const res = await request.get('/api/rag/documents')
  return res.data
}

export async function createRagDocument(data) {
  const res = await request.post('/api/rag/documents', data)
  return res.data
}

// 上传文件进知识库，后端会自己切块存好，不用前端处理
export async function uploadRagDocument(fd) {
  const res = await request.post('/api/rag/documents/upload', fd)
  return res.data
}

export async function deleteRagDocument(id) {
  const res = await request.delete(`/api/rag/documents/${id}`)
  return res.data
}

export async function getRagDocument(id) {
  const res = await request.get(`/api/rag/documents/${id}`)
  return res.data
}

export async function updateRagDocument(id, data) {
  const res = await request.put(`/api/rag/documents/${id}`, data)
  return res.data
}

// 跟 AI 对话，一轮一问一答，聊天记录挂在会话下
export async function aiChat(data) {
  const res = await request.post('/api/ai/chat', data)
  return res.data
}

// 让 AI 先出一份办事计划，这一步只是商量，还不会真动手
export async function executeAgentPlan(data) {
  const res = await request.post('/api/agent/plan', data)
  return res.data
}

// 用户点了确认才走这个，上一步那份计划这时才真的执行
export async function executeAgentConfirm(data) {
  const res = await request.post('/api/agent/confirm', data)
  return res.data
}

// 一只宠物历次寄养的全部报告，按时间倒着排
export async function getPetReports(petId) {
  const res = await request.get(`/api/ai/reports/pet/${petId}`)
  return res.data
}

export async function listAiConfigs() {
  const res = await request.get('/api/ai/configs')
  return res.data
}

export async function createAiConfig(data) {
  const res = await request.post('/api/ai/configs', data)
  return res.data
}

export async function updateAiConfigById(id, data) {
  const res = await request.put(`/api/ai/configs/${id}`, data)
  return res.data
}

export async function deleteAiConfig(id) {
  const res = await request.delete(`/api/ai/configs/${id}`)
  return res.data
}

export async function enableAiConfig(id, enabled) {
  const res = await request.post(`/api/ai/configs/${id}/enable`, { enabled })
  return res.data
}

// 试跑一下这组配置，看地址和密钥能不能通
export async function testAiConfigById(id) {
  const res = await request.post(`/api/ai/configs/${id}/test`)
  return res.data
}

// 按用途取当前生效的配置，同一个用途只会有一套在跑
export async function getEffectiveAiConfig(usage) {
  const res = await request.get('/api/ai/configs/effective', { params: { usage } })
  return res.data
}

// 我聊过的会话列表，侧边栏那串历史就是它
export async function getAiChatSessions() {
  const res = await request.get('/ai/history/sessions')
  return res.data
}

export async function getAiChatSessionMessages(sessionId) {
  const res = await request.get(`/ai/history/sessions/${sessionId}`)
  return res.data
}

// 删掉一整个会话，连同里面的消息一起清
export async function clearAiChatSession(sessionId) {
  const res = await request.delete(`/ai/history/sessions/${sessionId}`)
  return res.data
}



