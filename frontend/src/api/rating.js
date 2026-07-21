import request from '@/utils/request'

export async function getMyRatings() {
  const res = await request.get('/api/ratings')
  return res.data
}

export async function getRatings(params) {
  const res = await request.get('/api/ratings', { params })
  return res.data
}

export async function createRating(data) {
  const res = await request.post('/api/ratings', data)
  return res.data
}

export async function replyToRating(id, reply) {
  const res = await request.put(`/api/ratings/${id}/reply`, { reply_wsh: reply })
  return res.data
}

export async function getReviews() {
  const res = await request.get('/api/reviews')
  return res.data
}

export async function createReview(data) {
  const res = await request.post('/api/reviews', data)
  return res.data
}

export async function approveReview(id) {
  const res = await request.post(`/api/reviews/${id}/approve`, {})
  return res.data
}

export async function rejectReview(id) {
  const res = await request.post(`/api/reviews/${id}/reject`, {})
  return res.data
}



