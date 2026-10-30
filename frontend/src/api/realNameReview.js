import request from '@/utils/request'

// 后台实名审核列表，params 里可以按状态筛出待审的那批
export async function getRealNameReviews(params) {
  const res = await request.get('/api/admin/real-name-reviews', { params })
  return res.data
}

export async function approveRealNameReview(userId, data) {
  const res = await request.post(`/api/admin/real-name-reviews/${userId}/approve`, data)
  return res.data
}

export async function rejectRealNameReview(userId, data) {
  const res = await request.post(`/api/admin/real-name-reviews/${userId}/reject`, data)
  return res.data
}
