import * as api from '@/api/keeper'

function extract(res) {
  if (!res) throw new Error('无响应')
  if (res.code !== 200) throw new Error(res.message || '请求失败')
  return res.data ?? res
}

export async function getById(id) {
  return extract(await api.getKeeper(id))
}

export async function getList() {
  return extract(await api.getKeepers())
}

export async function getMy() {
  return extract(await api.getMyKeeperInfo())
}

export async function getByMerchant(merchantId) {
  return extract(await api.getKeepersByMerchant(merchantId))
}

export async function create(data) {
  return extract(await api.createKeeper(data))
}

export async function update(id, data) {
  return extract(await api.updateKeeper(id, data))
}

export async function approve(id) {
  return extract(await api.approveKeeper(id))
}

export async function reject(id) {
  return extract(await api.rejectKeeper(id))
}

export async function updateOnlineStatus(id, status) {
  return extract(await api.updateKeeperOnlineStatus(id, status))
}
