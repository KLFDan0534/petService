import * as api from '@/api/order'
import { unwrapResult } from '@/utils/result'

export async function getList() {
  return unwrapResult(await api.getOrders())
}

export async function getById(id) {
  return unwrapResult(await api.getOrder(id))
}

// 接口没有按宠物查订单，只能把订单列表拉回来自己在页面这边筛
export async function getByPetId(petId) {
  const orders = await getList()
  return (orders || []).filter(o => String(o.pet_id_wsh) === String(petId))
}
