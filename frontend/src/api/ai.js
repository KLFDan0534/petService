import request from '@/utils/request'

export async function getCareSuggestion(data) {
  const res = await request.post('/api/ai/care-suggestion', data)
  return res.data
}

export async function getBoardingReport(data) {
  const res = await request.post('/api/ai/boarding-report', data)
  return res.data
}

export async function getAIReprot(orderId) {
  const res = await request.get(`/ai/reports/order/${orderId}`)
  return res.data
}

export async function askRag(data) {
  const res = await request.post('/api/rag/ask', data)
  return res.data
}

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

export async function aiChat(data) {
  const res = await request.post('/api/ai/chat', data)
  return res.data
}

export async function executeAgentPlan(data) {
  const res = await request.post('/api/agent/plan', data)
  return res.data
}

export async function executeAgentConfirm(data) {
  const res = await request.post('/api/agent/confirm', data)
  return res.data
}

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

export async function testAiConfigById(id) {
  const res = await request.post(`/api/ai/configs/${id}/test`)
  return res.data
}

export async function getEffectiveAiConfig(usage) {
  const res = await request.get('/api/ai/configs/effective', { params: { usage } })
  return res.data
}

export async function getAiChatSessions() {
  const res = await request.get('/ai/history/sessions')
  return res.data
}

export async function getAiChatSessionMessages(sessionId) {
  const res = await request.get(`/ai/history/sessions/${sessionId}`)
  return res.data
}

export async function clearAiChatSession(sessionId) {
  const res = await request.delete(`/ai/history/sessions/${sessionId}`)
  return res.data
}



