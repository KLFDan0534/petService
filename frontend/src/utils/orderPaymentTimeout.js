export const ORDER_PAYMENT_TIMEOUT_MS = 15 * 60 * 1000
export const PAYMENT_TIMEOUT_REFRESH_INTERVAL_MS = 15 * 1000

export function getPaymentDeadlineMs(order) {
  const createdAt = parseOrderTime(order?.created_at_wsh)
  return createdAt ? createdAt + ORDER_PAYMENT_TIMEOUT_MS : null
}

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

export function formatPaymentTimeoutRemaining(ms) {
  if (ms == null) return '--:--'
  const totalSeconds = Math.max(0, Math.ceil(ms / 1000))
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

function parseOrderTime(value) {
  if (!value) return null
  const text = String(value).trim()
  const normalized = /^\d{4}-\d{2}-\d{2} \d{2}/.test(text) ? text.replace(' ', 'T') : text
  const date = new Date(normalized)
  const time = date.getTime()
  return Number.isNaN(time) ? null : time
}
