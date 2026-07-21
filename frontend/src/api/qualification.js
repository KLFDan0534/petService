import request from '@/utils/request'

export async function getPendingQualifications() {
  const res = await request.get('/qualifications/pending')
  return res.data
}

export async function approveQualification(id, reviewerId) {
  const res = await request.post(`/qualifications/${id}/approve?reviewerId=${reviewerId}`)
  return res.data
}

export async function rejectQualification(id, reviewerId, remark) {
  const res = await request.post(`/qualifications/${id}/reject?reviewerId=${reviewerId}${remark != null ? '&remark=' + encodeURIComponent(remark) : ''}`)
  return res.data
}
