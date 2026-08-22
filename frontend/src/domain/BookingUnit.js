const NORMALIZE_ALIASES = {
  day: 'day',
  days: 'day',
  '天': 'day',
  session: 'session',
  sessions: 'session',
  '次': 'session',
  hour: 'hour',
  hours: 'hour',
  '小时': 'hour',
}

export const DAY = 'day'
export const SESSION = 'session'
export const HOUR = 'hour'

export function normalizeUnit(value) {
  const key = String(value || '').trim().toLowerCase()
  return NORMALIZE_ALIASES[key] || null
}

export function isSupportedUnit(value) {
  return normalizeUnit(value) != null
}

export function unitLabel(value) {
  const unit = normalizeUnit(value)
  if (unit === DAY) return '天'
  if (unit === SESSION) return '次'
  if (unit === HOUR) return '小时'
  return unit || ''
}

export function bookingMode(value, unitValue) {
  const unit = normalizeUnit(unitValue)
  if (value) return value
  if (unit === DAY) return 'date_range'
  if (unit === SESSION || unit === HOUR) return 'slot'
  return 'date_range'
}

export function defaultDurationMinutes(unitValue) {
  const unit = normalizeUnit(unitValue) || DAY
  if (unit === DAY) return 1440
  if (unit === HOUR) return 60
  return 60
}

export function durationText(minutes) {
  const value = Number(minutes || 0)
  if (value <= 0) return ''
  if (value % 1440 === 0) return `${value / 1440} 天`
  if (value % 60 === 0) return `${value / 60} 小时`
  return `${value} 分钟`
}

/**
 * Builds a human-readable billing description for an order, e.g.
 *  "3 天"、"1 次（60 分钟）"、"4 小时 × ￥30.00"。
 * Falls back to legacy day projection when unit fields are missing.
 */
export function billingText(order) {
  if (!order) return ''
  const unit = normalizeUnit(order.billing_unit_wsh)
  if (unit === null) {
    const days = Number(order.days_wsh || 0)
    return days > 0 ? `${days} 天` : ''
  }
  const quantity = Number(order.quantity_wsh || 0)
  if (quantity <= 0) {
    return unitLabel(unit)
  }
  if (unit === DAY) {
    return `${quantity} 天`
  }
  if (unit === SESSION) {
    const duration = durationText(order.duration_minutes_wsh)
    return duration ? `1 次（${duration}）` : `1 次`
  }
  const duration = durationText(order.duration_minutes_wsh)
  return quantity > 1 ? `${quantity} ${unitLabel(unit)}（${duration}）` : `1 ${unitLabel(unit)}`
}