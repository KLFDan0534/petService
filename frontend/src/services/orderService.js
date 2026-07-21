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

export async function getByPetId(petId) {
  const orders = await getList()
  return (orders || []).filter(o => String(o.pet_id_wsh) === String(petId))
}
