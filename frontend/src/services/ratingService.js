import * as api from '@/api/rating'

function extract(res) {
  if (!res) throw new Error('无响应')
  if (res.code !== 200) throw new Error(res.message || '请求失败')
  return res.data ?? res
}

export async function getByTarget(targetId, targetType) {
  return extract(await api.getRatings({ targetId, targetType }))
}

export async function getMine() {
  return extract(await api.getMyRatings())
}

export async function create(data) {
  return extract(await api.createRating(data))
}

export async function reply(id, reply) {
  return extract(await api.replyToRating(id, reply))
}
