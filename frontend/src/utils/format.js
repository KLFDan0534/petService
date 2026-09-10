/**
 * 统一的格式化工具（Design System Phase B）。
 *
 * 各页面原先各自内联定义 formatDate / formatDateTime / formatMoney / formatNumber，
 * 且输出规则存在细微差异。这里把常用语义集中为一份实现，通过参数保留各页面的
 * 既有输出规则，避免引入行为变化（仅收敛重复代码）。
 */

function hasValue(value) {
  return value != null && value !== ''
}

/** 截断 ISO 日期为 YYYY-MM-DD（等价于原 `String(v).slice(0, 10)`）。 */
export function formatDate(value, fallback = '-') {
  return hasValue(value) ? String(value).slice(0, 10) : fallback
}

/**
 * 日期 → zh-CN 本地化日期字符串，如 2026/1/1。
 * 原 Dashboard / Favorites 使用 toLocaleDateString('zh-CN')。
 */
export function formatLocalDate(value, fallback = '-') {
  if (!hasValue(value)) return fallback
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? fallback : d.toLocaleDateString('zh-CN')
}

/**
 * 日期时间 → 本地化字符串。
 *  - format='full'（默认）: toLocaleString()，如 2026/1/1 12:00:00
 *  - format='compact'    : "MM-DD HH:mm"，如 01-01 12:00（原 Orders / OrderCard）
 *  - format='cdatetime'  : 无效输入时原样返回字符串（原 MerchantOrders 行为，fallback 视为字符串）
 */
export function formatDateTime(value, { fallback = '—', format = 'full' } = {}) {
  if (!hasValue(value)) return fallback
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) {
    return format === 'full' ? String(value) : fallback
  }
  if (format === 'compact') {
    return d.toLocaleString('zh-CN', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
  }
  return d.toLocaleString()
}

/** 金额：保留两位小数（等价于原 `Number(v || 0).toFixed(2)`）。不附带货币符号，符号留在模板。 */
export function formatMoney(value, fallback = '0.00') {
  const num = Number(value)
  return Number.isFinite(num) ? num.toFixed(2) : String(fallback)
}

/**
 * 数字：千分位 + 指定小数位（原 Admin 面板使用 toLocaleString('zh-CN', {2 位})）。
 */
export function formatNumber(value, { decimals = 2 } = {}) {
  const num = Number(value || 0)
  if (!Number.isFinite(num)) return '--'
  return num.toLocaleString('zh-CN', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  })
}