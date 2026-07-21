import request from '@/utils/request'

export async function getMyComplaints() {
  const res = await request.get('/api/complaints')
  return res.data
}

export async function getComplaints() {
  const res = await request.get('/api/complaints/all')
  return res.data
}

export async function createComplaint(data) {
  const res = await request.post('/api/complaints', data)
  return res.data
}

export async function getComplaintEvidence(id) {
  const res = await request.get(`/api/complaints/${id}/evidence`)
  return res.data
}

export async function resolveComplaint(id, result = '投诉已处理') {
  const res = await request.post(`/api/complaints/${id}/resolve`, { result_wsh: result })
  return res.data
}

export async function rejectComplaint(id, result = '投诉证据不足，已驳回') {
  const res = await request.post(`/api/complaints/${id}/reject`, { result_wsh: result })
  return res.data
}



