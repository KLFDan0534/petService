import request from '@/utils/request'

export async function getPublicQualifications(ownerType, ownerId) {
  const res = await request.get(`/api/qualifications/${ownerType}/${ownerId}`)
  return res.data
}

// 后台待审的资质列表，商家和护工上传的证照都进这里
export async function getPendingQualifications() {
  const res = await request.get('/api/qualifications/pending')
  return res.data
}

export async function approveQualification(id, reviewerId) {
  const res = await request.post(`/api/qualifications/${id}/approve?reviewerId=${reviewerId}`)
  return res.data
}

// 驳回要带理由，用户能看到为什么没过；remark 走 URL 编码免得中文乱掉
export async function rejectQualification(id, reviewerId, remark) {
  const res = await request.post(`/api/qualifications/${id}/reject?reviewerId=${reviewerId}${remark != null ? '&remark=' + encodeURIComponent(remark) : ''}`)
  return res.data
}
