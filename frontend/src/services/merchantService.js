import * as api from '@/api/merchant'
import { getBusinessHours as fetchHours } from '@/api/businessHours'
import { upsertBusinessHours as saveHours, deleteBusinessHour as removeHour } from '@/api/businessHours'
import { getServicesByMerchant as fetchServices } from '@/api/service'

function extract(res) {
  if (!res) throw new Error('无响应')
  if (res.code !== 200) throw new Error(res.message || '请求失败')
  return res.data ?? res
}

export async function getById(id) {
  return extract(await api.getMerchant(id))
}

export async function getList() {
  return extract(await api.getMerchants())
}

export async function getMy() {
  return extract(await api.getMyMerchant())
}

export async function getNearby(params) {
  return extract(await api.getNearbyMerchants(params))
}

export async function create(data) {
  return extract(await api.createMerchant(data))
}

export async function update(id, data) {
  return extract(await api.updateMerchant(id, data))
}

export async function remove(id) {
  return extract(await api.deleteMerchant(id))
}

export async function approve(id) {
  return extract(await api.approveMerchant(id))
}

export async function reject(id) {
  return extract(await api.rejectMerchant(id))
}

export async function updateStatus(id, status) {
  return extract(await api.updateMerchantStatus(id, status))
}

export async function updateStoreMode(id, storeMode) {
  return extract(await api.updateMerchantStoreMode(id, storeMode))
}

export async function getBusinessHours(id) {
  return extract(await fetchHours(id))
}

export async function upsertBusinessHours(id, data) {
  return extract(await saveHours(id, data))
}

export async function deleteBusinessHour(id, hourId) {
  return extract(await removeHour(id, hourId))
}

export async function getServices(id) {
  return extract(await fetchServices(id))
}
