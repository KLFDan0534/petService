import { ref } from 'vue'
import { getCurrentAddress, searchAmapAddress } from '@/composables/useAmapLocation'
import { haversineMeters } from '@/utils/geo'

/**
 * 服务卡片距离组合式函数。
 *
 * 距离计算全部走高德：
 * - 用户位置：高德浏览器定位（getCurrentAddress）
 * - 商家位置：对商家地址做高德地理编码（searchAmapAddress / geocode）
 * - 距离：球面距离（haversine），与后端口径一致
 *
 * 不依赖本地库表的经纬度字段，方便后续下线本地经纬度、统一使用高德数据。
 * 用户位置与商家坐标按会话缓存，避免重复定位/地理编码。
 */

let userLocationPromise = null
const merchantPointCache = new Map()

/** 距离文案：只显示数字 + 单位，<1km 用 m，否则用 km（如 850m、1.2km） */
export function formatServiceDistance(meters) {
  if (meters == null || meters === '') return ''
  const value = Number(meters)
  if (!Number.isFinite(value) || value < 0) return ''
  if (value < 1000) return `${Math.max(1, Math.round(value))}m`
  return `${(value / 1000).toFixed(1)}km`
}

function resolveUserLocation() {
  if (!userLocationPromise) {
    userLocationPromise = getCurrentAddress().catch(error => {
      userLocationPromise = null
      throw error
    })
  }
  return userLocationPromise
}

function resolveMerchantPoint(merchant) {
  const key = `merchant:${String(merchant?.id_wsh ?? '')}`
  if (merchantPointCache.has(key)) return merchantPointCache.get(key)
  const promise = (async () => {
    const address = String(merchant?.address_wsh || '').trim()
    if (!address) return null
    const results = await searchAmapAddress(address, { withNearby: false, limit: 1 })
    const first = Array.isArray(results) ? results[0] : null
    const latitude = Number(first?.latitude_wsh)
    const longitude = Number(first?.longitude_wsh)
    if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) return null
    return { latitude_wsh: latitude, longitude_wsh: longitude }
  })().catch(() => null)
  merchantPointCache.set(key, promise)
  return promise
}

export function useServiceDistance() {
  const locating = ref(false)

  /** 获取用户位置（高德定位）；失败静默返回 null，不打扰用户 */
  async function locateUser() {
    locating.value = true
    try {
      return await resolveUserLocation()
    } catch {
      return null
    } finally {
      locating.value = false
    }
  }

  /**
   * 为服务列表附加距离（原地写 service.distance_m_wsh，单位：米）。
   * @param services 服务数组（含 merchant_id_wsh）
   * @param merchants 商家数组（含 id_wsh / address_wsh），商家坐标由高德地理编码得到
   */
  async function attachDistances(services, merchants) {
    const user = await locateUser()
    if (!user) return services
    const merchantById = new Map((merchants || []).map(merchant => [String(merchant?.id_wsh), merchant]))
    await Promise.all((services || []).map(async service => {
      const merchant = merchantById.get(String(service?.merchant_id_wsh))
      if (!merchant) return
      const point = await resolveMerchantPoint(merchant)
      if (!point) return
      service.distance_m_wsh = haversineMeters(
        user.latitude_wsh,
        user.longitude_wsh,
        point.latitude_wsh,
        point.longitude_wsh,
      )
    }))
    return services
  }

  return { locateUser, attachDistances, locating }
}
