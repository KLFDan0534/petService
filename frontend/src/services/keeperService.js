import * as api from '@/api/keeper'
import { unwrapResult } from '@/utils/result'

export async function getById(id) {
  return unwrapResult(await api.getKeeper(id))
}

export async function getList() {
  return unwrapResult(await api.getKeepers())
}

export async function getMy() {
  return unwrapResult(await api.getMyKeeperInfo())
}

export async function getByMerchant(merchantId) {
  return unwrapResult(await api.getKeepersByMerchant(merchantId))
}

export async function create(data) {
  return unwrapResult(await api.createKeeper(data))
}

export async function update(id, data) {
  return unwrapResult(await api.updateKeeper(id, data))
}

export async function approve(id) {
  return unwrapResult(await api.approveKeeper(id))
}

export async function reject(id) {
  return unwrapResult(await api.rejectKeeper(id))
}

// 陪护人分两种在线状态，商家可以手动把人切成休息，接单列表会跟着变
export async function updateOnlineStatus(id, status) {
  return unwrapResult(await api.updateKeeperOnlineStatus(id, status))
}
