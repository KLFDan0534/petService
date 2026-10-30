import * as api from '@/api/keeper'

// 接口统一返回 { code, message, data }，这里统一拆一层，出错直接抛，页面只管拿数据
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

// 陪护人分两种在线状态，商家可以手动把人切成休息，接单列表会跟着变
export async function updateOnlineStatus(id, status) {
  return extract(await api.updateKeeperOnlineStatus(id, status))
}
