/**
 * 计算两个经纬度坐标之间的大圆距离（米），使用 Haversine 公式。
 * 参考后端 GeoDistanceUtils.calculateDistance 的球面距离口径。
 */
export function haversineMeters(latitude1, longitude1, latitude2, longitude2) {
  const toRadians = value => (Number(value) * Math.PI) / 180
  const radiusMeters = 6371000
  const dLat = toRadians(latitude2) - toRadians(latitude1)
  const dLng = toRadians(longitude2) - toRadians(longitude1)
  const a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
    + Math.cos(toRadians(latitude1)) * Math.cos(toRadians(latitude2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2)
  return 2 * radiusMeters * Math.asin(Math.sqrt(a))
}

/** 把米数格式化为人性化的距离文案：1 公里内显示米，以上显示公里。 */
export function formatDistanceMeters(meters) {
  if (meters == null || meters === '') return ''
  const value = Number(meters)
  if (!Number.isFinite(value) || value < 0) return ''
  if (value < 1000) return `${Math.max(1, Math.round(value))} 米`
  return `${(value / 1000).toFixed(1)} 公里`
}
