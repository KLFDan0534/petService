import * as api from '@/api/rating'
import { unwrapResult } from '@/utils/result'

// 评价挂在商家或陪护人身上，靠 targetType 区分，查的时候两个参数都要带
export async function getByTarget(targetId, targetType) {
  return unwrapResult(await api.getRatings({ targetId, targetType }))
}

export async function getMine() {
  return unwrapResult(await api.getMyRatings())
}

export async function create(data) {
  return unwrapResult(await api.createRating(data))
}

export async function reply(id, reply) {
  return unwrapResult(await api.replyToRating(id, reply))
}
