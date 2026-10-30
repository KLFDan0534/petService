import * as api from '@/api/order'

function extract(res) {
  if (!res) throw new Error('无响应')
  if (res.code !== 200) throw new Error(res.message || '请求失败')
  return res.data ?? res
}

export async function getList() {
  return extract(await api.getOrders())
}

export async function getById(id) {
  return extract(await api.getOrder(id))
}

// 接口没有按宠物查订单，只能把订单列表拉回来自己在页面这边筛
export async function getByPetId(petId) {
  const orders = await getList()
  return (orders || []).filter(o => String(o.pet_id_wsh) === String(petId))
}
