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

export async function executeAgent(data) {
  const res = await request.post('/api/agent/execute', data)
  return res.data
}

export async function getPetReports(petId) {
  const res = await request.get(`/api/ai/reports/pet/${petId}`)
  return res.data
}

export async function getAiConfig() {
  const res = await request.get('/ai/config')
  return res.data
}

export async function updateAiConfig(data) {
  const res = await request.put('/ai/config', data)
  return res.data
}

export async function testAiConnection() {
  const res = await request.post('/ai/config/test')
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



