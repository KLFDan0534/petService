import request from '@/utils/request'

// 我给出的评价，用户中心里看自己写过啥
export async function getMyRatings() {
  const res = await request.get('/api/ratings/my')
  return res.data
}

export async function getMyRatingsByOrder(orderId) {
  const res = await request.get('/api/ratings/my', { params: { orderId } })
  return res.data
}

export async function getRatings(params) {
  const res = await request.get('/api/ratings', { params })
  return res.data
}

// 给这单打分，星星加文字，提交后算进商家的评分
export async function createRating(data) {
  const res = await request.post('/api/ratings', data)
  return res.data
}

// 商家回一句，用户能在评价下面看到
export async function replyToRating(id, reply) {
  const res = await request.put(`/api/ratings/${id}/reply`, { reply_wsh: reply })
  return res.data
}

// 晒单评价，跟评分是两套东西，这里带图和长文字
export async function getReviews() {
  const res = await request.get('/api/reviews')
  return res.data
}

// 待审的晒单，审核员从这里一条条过
export async function getPendingReviews() {
  const res = await request.get('/api/reviews/pending')
  return res.data
}

export async function createReview(data) {
  const res = await request.post('/api/reviews', data)
  return res.data
}

// 审核通过，通过了才会显示在商家页面上
export async function approveReview(id) {
  const res = await request.post(`/api/reviews/${id}/approve`, {})
  return res.data
}

export async function rejectReview(id) {
  const res = await request.post(`/api/reviews/${id}/reject`, {})
  return res.data
}



