import request from '@/utils/request'

// 我投诉过的单子，带处理进度
export async function getMyComplaints() {
  const res = await request.get('/api/complaints')
  return res.data
}

// 先拉到可投诉的对象，投诉单要挂在这上面，不然提交会失败
export async function getComplaintTargets() {
  const res = await request.get('/api/complaints/targets')
  return res.data
}

export async function getComplaints(filters = {}) {
  const res = await request.get('/api/complaints/all', { params: filters })
  return res.data
}

// 提交一条投诉，可以带截图当证据
export async function createComplaint(data) {
  const res = await request.post('/api/complaints', data)
  return res.data
}

export async function getComplaintEvidence(id) {
  const res = await request.get(`/api/complaints/${id}/evidence`)
  return res.data
}

// 投诉里的聊天记录，用户和商家各说各的
export async function getComplaintMessages(id) {
  const res = await request.get(`/api/complaints/${id}/messages`)
  return res.data
}

export async function sendComplaintMessage(id, content, fileUrl) {
  const res = await request.post(`/api/complaints/${id}/messages`, { content_wsh: content, file_url_wsh: fileUrl || null })
  return res.data
}

// 客服点"我来处理"，接手后这条就归他了
export async function acceptComplaint(id) {
  const res = await request.post(`/api/complaints/${id}/accept`, {})
  return res.data
}

// 处理完收尾，得写一句结论给用户看
export async function resolveComplaint(id, result = '投诉已处理') {
  const res = await request.post(`/api/complaints/${id}/resolve`, { result_wsh: result })
  return res.data
}

export async function rejectComplaint(id, result = '投诉证据不足，已驳回') {
  const res = await request.post(`/api/complaints/${id}/reject`, { result_wsh: result })
  return res.data
}



