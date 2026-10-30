// 下单后 15 分钟内必须付款，过了这个点订单会被系统自动取消
export const ORDER_PAYMENT_TIMEOUT_MS = 15 * 60 * 1000
export const PAYMENT_TIMEOUT_REFRESH_INTERVAL_MS = 15 * 1000

// 超时时间点 = 下单时间 + 15 分钟。注意算的是下单时间，不是打开页面的时间
export function getPaymentDeadlineMs(order) {
  const createdAt = parseOrderTime(order?.created_at_wsh)
  return createdAt ? createdAt + ORDER_PAYMENT_TIMEOUT_MS : null
}

// 剩余时间最小给 0，别让页面显示负数
export function getPaymentTimeoutRemaining(order, now = Date.now()) {
  const deadline = getPaymentDeadlineMs(order)
  if (!deadline) return null
  return Math.max(0, deadline - now)
}

export function isPaymentTimeoutExpired(order, now = Date.now()) {
  const remaining = getPaymentTimeoutRemaining(order, now)
  return remaining === 0
}

export function hasExpiredPaymentTimeout(orders, now = Date.now()) {
  return Array.isArray(orders)
    && orders.some(order => order?.status_wsh === 'pending' && isPaymentTimeoutExpired(order, now))
}

// 倒计时按分:秒显示，秒数向上取整，这样最后一秒还会显示 00:01 而不是提前归零
export function formatPaymentTimeoutRemaining(ms) {
  if (ms == null) return '--:--'
  const totalSeconds = Math.max(0, Math.ceil(ms / 1000))
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

// 后端给的是 "2026-06-01 10:00:00" 这种带空格的格式，部分浏览器不认，得先把空格换成 T
function parseOrderTime(value) {
  if (!value) return null
  const text = String(value).trim()
  const normalized = /^\d{4}-\d{2}-\d{2} \d{2}/.test(text) ? text.replace(' ', 'T') : text
  const date = new Date(normalized)
  const time = date.getTime()
  return Number.isNaN(time) ? null : time
}
