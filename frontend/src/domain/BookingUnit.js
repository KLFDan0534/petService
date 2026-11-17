/**
 * 计价单位展示工具。
 *
 * 全项目只允许 night / session / hour 三种计价单位，唯一注册表在后端 BookingUnit.java。
 * 本文件只做展示映射与纯日期推导：不校验单位合法性、不做别名归一、不计算任何金额或优惠。
 * 价格一律由服务端 POST /api/orders/preview 返回。
 */

/** 仅作为展示键使用，不参与任何校验。 */
export const NIGHT = 'night'
export const SESSION = 'session'
export const HOUR = 'hour'

const UNIT_LABELS = {
  [NIGHT]: '按夜',
  [SESSION]: '按次',
  [HOUR]: '按小时',
}

/**
 * 单位展示名：标准单位返回中文名；历史旧单位（day/boarding/time 等）
 * 只按原始值展示并明确标注为历史计价单位，绝不解释成「按24小时」、绝不映射成标准单位。
 */
export function unitLabel(value) {
  const raw = String(value ?? '').trim()
  if (!raw) return ''
  const label = UNIT_LABELS[raw.toLowerCase()]
  if (label) return label
  return `${raw}（历史计价单位）`
}

/** 是否标准计价单位（night / session / hour）：与上方唯一注册表同源。 */
export function isStandardUnit(value) {
  return Boolean(UNIT_LABELS[String(value ?? '').trim().toLowerCase()])
}

/** 时长文案：不含「天」口径，1440 分钟展示为「24 小时」。 */
export function durationText(minutes) {
  const value = Number(minutes || 0)
  if (value <= 0) return ''
  if (value % 60 === 0) return `${value / 60} 小时`
  return `${value} 分钟`
}

/**
 * 订单计费说明：一律按订单自身 billing_unit_wsh 快照展示，不含任何金额计算。
 * 标准单位用 unitLabel 的中文名；历史单位按原始值标注；缺失时明确提示。
 */
export function billingText(order) {
  if (!order) return ''
  const raw = String(order.billing_unit_wsh ?? '').trim()
  if (!raw) return '历史计价单位缺失'
  const label = unitLabel(raw)
  // 历史单位（unitLabel 已带标注）：只展示标注，不套用数量/时长，避免解释旧口径
  if (!UNIT_LABELS[raw.toLowerCase()]) return label
  const quantity = Number(order.quantity_wsh || 0)
  if (quantity <= 0) return label
  const duration = durationText(order.duration_minutes_wsh)
  return duration ? `${quantity} ${label}（${duration}）` : `${quantity} ${label}`
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
