const NORMALIZE_ALIASES = {
  night: 'night',
  nights: 'night',
  '晚': 'night',
  '夜': 'night',
  '按晚': 'night',
  '按夜': 'night',
  '过夜': 'night',
  session: 'session',
  sessions: 'session',
  '次': 'session',
  '课时': 'session',
  '节': 'session',
  hour: 'hour',
  hours: 'hour',
  '小时': 'hour',
  '按时': 'hour',
  '按小时': 'hour',
}

/** 过夜寄养：按夜计价，一夜 24 小时，14:00 入住锚点 */
export const NIGHT = 'night'
export const SESSION = 'session'
export const HOUR = 'hour'

/** 固定锚点/宽限/提前门槛（与后端 OrderPricing 同源，前端只展示、不计算金额） */
export const CHECKIN_ANCHOR_TIME = '14:00'
export const CHECKOUT_GRACE_MINUTES = 30
export const EARLY_LIMIT_MINUTES = 180

export function normalizeUnit(value) {
  const key = String(value || '').trim().toLowerCase()
  return NORMALIZE_ALIASES[key] || null
}

export function isSupportedUnit(value) {
  return normalizeUnit(value) != null
}

export function unitLabel(value) {
  const unit = normalizeUnit(value)
  if (unit === NIGHT) return '夜'
  if (unit === SESSION) return '次'
  if (unit === HOUR) return '小时'
  return unit || ''
}

export function bookingMode(value, unitValue) {
  const unit = normalizeUnit(unitValue)
  if (value) return value
  if (unit === SESSION || unit === HOUR) return 'slot'
  if (unit === NIGHT) return 'interval'
  return 'interval'
}

export function defaultDurationMinutes(unitValue) {
  const unit = normalizeUnit(unitValue)
  // night 的 1440 只是单位元数据，不要求实际住满 24 小时
  if (unit === NIGHT) return 1440
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
 * 订单的计费说明文案，例如「3 夜」「1 次（8 小时）」「4 小时」。
 * 旧订单缺少 unit 字段时只做历史投影读取，不参与任何计算。
 */
export function billingText(order) {
  if (!order) return ''
  const unit = normalizeUnit(order.billing_unit_wsh)
  if (unit === null) {
    const days = Number(order.days_wsh || 0)
    return days > 0 ? `${days} 天` : ''
  }
  const quantity = Number(order.quantity_wsh || 0)
  if (unit === NIGHT) {
    return quantity > 0 ? `${quantity} 夜` : '按夜计费'
  }
  if (quantity <= 0) {
    return unitLabel(unit)
  }
  if (unit === SESSION) {
    const duration = durationText(order.duration_minutes_wsh)
    return duration ? `1 次（${duration}）` : '1 次'
  }
  const duration = durationText(order.duration_minutes_wsh)
  return quantity > 1 ? `${quantity} ${unitLabel(unit)}（${duration}）` : `1 ${unitLabel(unit)}`
}

/**
 * 按夜订单的离店日期 = 入住日 + N 天（只用来推导表单日期，金额一律用后端报价）。
 */
export function standardCheckoutDate(checkinDate, nights) {
  if (!checkinDate) return ''
  const base = new Date(`${checkinDate}T00:00:00`)
  if (Number.isNaN(base.getTime())) return ''
  base.setDate(base.getDate() + Math.max(1, Number(nights) || 1))
  const y = base.getFullYear()
  const m = String(base.getMonth() + 1).padStart(2, '0')
  const d = String(base.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}
