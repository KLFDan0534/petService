import * as api from '@/api/merchant'
import { getBusinessHours as fetchHours } from '@/api/businessHours'
import { upsertBusinessHours as saveHours, deleteBusinessHour as removeHour } from '@/api/businessHours'
import { getServicesByMerchant as fetchServices } from '@/api/service'
import { unwrapResult } from '@/utils/result'

export async function getById(id) {
  return unwrapResult(await api.getMerchant(id))
}

export async function getList() {
  return unwrapResult(await api.getMerchants())
}

export async function getMy() {
  return unwrapResult(await api.getMyMerchant())
}

// 附近商家要传经纬度，参数由页面自己组好直接往下传
export async function getNearby(params) {
  return unwrapResult(await api.getNearbyMerchants(params))
}

export async function create(data) {
  return unwrapResult(await api.createMerchant(data))
}

export async function update(id, data) {
  return unwrapResult(await api.updateMerchant(id, data))
}

export async function remove(id) {
  return unwrapResult(await api.deleteMerchant(id))
}

export async function approve(id) {
  return unwrapResult(await api.approveMerchant(id))
}

export async function reject(id) {
  return unwrapResult(await api.rejectMerchant(id))
}

export async function updateStatus(id, status) {
  return unwrapResult(await api.updateMerchantStatus(id, status))
}

export async function updateStoreMode(id, storeMode) {
  return unwrapResult(await api.updateMerchantStoreMode(id, storeMode))
}

// 营业时间不在商家接口里，走的是单独的营业时间模块，这里只做转发
export async function getBusinessHours(id) {
  return unwrapResult(await fetchHours(id))
}

export async function upsertBusinessHours(id, data) {
  return unwrapResult(await saveHours(id, data))
}

export async function deleteBusinessHour(id, hourId) {
  return unwrapResult(await removeHour(id, hourId))
}

// 商家挂的服务项目同样来自服务模块，按商家 id 查
export async function getServices(id) {
  return unwrapResult(await fetchServices(id))
}
