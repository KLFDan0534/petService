import * as api from '@/api/merchant'
import { getBusinessHours as fetchHours } from '@/api/businessHours'
import { upsertBusinessHours as saveHours, deleteBusinessHour as removeHour } from '@/api/businessHours'
import { getServicesByMerchant as fetchServices } from '@/api/service'

// 接口统一返回 { code, message, data }，这里统一拆一层，出错直接抛，页面只管拿数据
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

// 附近商家要传经纬度，参数由页面自己组好直接往下传
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

// 营业时间不在商家接口里，走的是单独的营业时间模块，这里只做转发
export async function getBusinessHours(id) {
  return extract(await fetchHours(id))
}

export async function upsertBusinessHours(id, data) {
  return extract(await saveHours(id, data))
}

export async function deleteBusinessHour(id, hourId) {
  return extract(await removeHour(id, hourId))
}

// 商家挂的服务项目同样来自服务模块，按商家 id 查
export async function getServices(id) {
  return extract(await fetchServices(id))
}
