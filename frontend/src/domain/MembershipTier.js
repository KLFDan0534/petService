/**
 * 会员三档的视觉材质与展示口径（纯前端，不含请求）。
 *
 * 口径来源：Magic Patterns「会员中心」画板（4hqqYHPJeZBqesfp42Ednh.frame）。
 * 递进逻辑：相伴（陪着你）→ 守护（替你守护）→ 挚爱（视如挚爱）。
 * 等级只认后端下发的 level_wsh，不按套餐名猜；未知等级落到中性档，不伪造等级。
 *
 * 颜色一律以 CSS 变量注入（见 tierVars），组件里统一用 var(--t-*)，
 * 这样同一张卡换档时只是变量变了，DOM 结构不用重排。
 */

const DAY = 86400000

/** 徽记尺寸：身份卡 / 套餐卡 / 行内 */
export const EMBLEM = { hero: 56, card: 40, inline: 20 }

/** 三档共用的一张噪点：内联 SVG，不走图片文件 */
export const GRAIN_URL = `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='180' height='180'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='3' stitchTiles='stitch'/%3E%3CfeColorMatrix type='saturate' values='0'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E")`

export const TIERS = {
  1: {
    key: 'companion',
    en: 'COMPANION',
    lv: 'Lv1',
    fallbackName: '相伴会员',
    tagline: '每一次出门，都有它陪着',
    card: 'linear-gradient(128deg, #F7F8FA 0%, #E4E7EB 38%, #CDD2D9 72%, #E9ECEF 100%)',
    cardInk: '#1C2026',
    cardSub: '#4F5762',
    cardLine: 'rgba(28, 32, 38, 0.12)',
    cardGlass: 'rgba(255, 255, 255, 0.46)',
    grain: 'repeating-linear-gradient(0deg, rgba(255,255,255,0.42) 0 1px, rgba(40,48,58,0.035) 1px 2px)',
    edge: 'rgba(255, 255, 255, 0.9)',
    drop: 'rgba(40, 48, 58, 0.38)',
    surface: '#F3F5F7',
    border: '#C9CFD7',
    borderHover: '#8B929B',
    ink: '#101318',
    sub: '#20252B',
    accent: '#2A3038',
    ctaBg: '#262B32',
    ctaInk: '#F7F8FA',
    ctaHover: '#3A414B',
    metal: ['#FBFCFD', '#C3CAD3', '#8D97A3'],
    facet: 'rgba(60, 70, 82, 0.22)',
    brushed: 'linear-gradient(135deg, #F2F5F9 0%, #DCE1E8 32%, #B9C0CA 76%, #E2E7EE 100%)',
    light: {
      rgb: '255, 255, 255',
      contrast: 1,
      field: 1.3,
      glow: 0.55,
      glowR: 0.72,
      glowMid: 0.3,
      glowEnd: 50,
      tilt: 1.6,
      streakFrom: 96,
      streakTo: 134,
      streak: 0.28,
      band: 12,
      rim: 0.3,
      ambient: 0.08,
      grain: 0.03,
      plan: { contrast: 0.62, glow: 0.1, streak: 0.09, rim: 0.1 },
    },
  },
  2: {
    key: 'guardian',
    en: 'GUARDIAN',
    lv: 'Lv2',
    fallbackName: '守护会员',
    tagline: '你不在的时候，有人替你守着',
    card: 'linear-gradient(128deg, #FAF1DE 0%, #EBD3A2 40%, #D3AE6A 74%, #E9CF97 100%)',
    cardInk: '#2A1D0A',
    cardSub: '#5E4520',
    cardLine: 'rgba(60, 40, 10, 0.16)',
    cardGlass: 'rgba(255, 249, 236, 0.42)',
    grain: 'repeating-linear-gradient(90deg, rgba(255,244,214,0.30) 0 1px, rgba(110,76,22,0.04) 1px 3px)',
    edge: 'rgba(255, 246, 222, 0.95)',
    drop: 'rgba(110, 76, 22, 0.42)',
    surface: '#FBF4E4',
    border: '#E3C78E',
    borderHover: '#B98F45',
    ink: '#1F1506',
    sub: '#33240C',
    accent: '#4A3208',
    ctaBg: '#5C400F',
    ctaInk: '#FFF9EC',
    ctaHover: '#48320A',
    metal: ['#FFF4D8', '#DDB66E', '#9C7330'],
    facet: 'rgba(110, 76, 22, 0.28)',
    brushed: 'linear-gradient(135deg, #FDF7E9 0%, #EBDCBC 32%, #CDB27C 76%, #F2E6D0 100%)',
    light: {
      rgb: '255, 236, 196',
      contrast: 0.78,
      field: 1.3,
      glow: 0.55,
      glowR: 0.72,
      glowMid: 0.3,
      glowEnd: 50,
      tilt: 2.2,
      streakFrom: 96,
      streakTo: 134,
      streak: 0.28,
      band: 12,
      rim: 0.32,
      ambient: 0.1,
      grain: 0.04,
      plan: { contrast: 0.55, glow: 0.12, streak: 0.09, rim: 0.1 },
    },
  },
  3: {
    key: 'beloved',
    en: 'BELOVED',
    lv: 'Lv3',
    fallbackName: '挚爱会员',
    tagline: '把它当家人，待遇也一样',
    card: 'linear-gradient(140deg, #0C1A33 0%, #132A50 46%, #1D4374 82%, #16345E 100%)',
    cardInk: '#F0F6FD',
    cardSub: '#A8BEDB',
    cardLine: 'rgba(196, 222, 250, 0.18)',
    cardGlass: 'rgba(160, 200, 245, 0.10)',
    grain:
      'repeating-linear-gradient(60deg, rgba(190,225,255,0.07) 0 1px, transparent 1px 26px), repeating-linear-gradient(-60deg, rgba(190,225,255,0.07) 0 1px, transparent 1px 26px)',
    edge: 'rgba(170, 210, 250, 0.45)',
    drop: 'rgba(8, 20, 44, 0.6)',
    surface: '#11203D',
    border: '#22406B',
    borderHover: '#6FA3D6',
    ink: '#F0F6FD',
    sub: '#A8BEDB',
    accent: '#9CCBF2',
    ctaBg: '#DDECFB',
    ctaInk: '#0C1A33',
    ctaHover: '#FFFFFF',
    metal: ['#F4FAFF', '#9FCBF0', '#3F78B3'],
    facet: 'rgba(210, 234, 255, 0.34)',
    brushed:
      'linear-gradient(135deg, #0D1A32 0%, #11203D 14%, #163058 31%, #102240 52%, #142B50 73%, #0E1C36 100%)',
    light: {
      rgb: '200, 228, 255',
      contrast: 0.4,
      field: 1,
      glow: 0.35,
      glowR: 0.9,
      glowMid: 0.25,
      glowEnd: 60,
      tilt: 2.8,
      streakFrom: 96,
      streakTo: 134,
      streak: 0.3,
      band: 18,
      rim: 0.35,
      ambient: 0.12,
      grain: 0.05,
      plan: { contrast: 0.2, glow: 0.16, streak: 0.12, rim: 0.14 },
    },
  },
}

/** 未开通：中性档，沿用相伴的材质但不倾斜、不发光 */
const PLAIN = {
  ...TIERS[1],
  key: 'plain',
  en: 'MEMBER',
  lv: 'Lv',
  fallbackName: '',
  tagline: '',
  metal: ['#FBFCFD', '#D7DCE3', '#A6AEB9'],
  light: { ...TIERS[1].light, tilt: 0 },
}

export function tierOf(level) {
  const n = Number(level)
  return TIERS[n] || PLAIN
}

/** 会员名：优先用后端套餐名，缺失时按等级兜底，未开通返回空串（调用方据此不渲染） */
export function memberName(membership, plan) {
  const name = plan?.name_wsh || membership?.plan_name_wsh
  if (name) return name
  const tier = tierOf(membership?.level_wsh ?? plan?.level_wsh)
  return tier.fallbackName || ''
}

/** 注入 --t-* 变量：卡片底色、正文、按钮全部从这里取 */
export function tierVars(tier) {
  return {
    '--t-card': tier.card,
    '--t-card-ink': tier.cardInk,
    '--t-card-sub': tier.cardSub,
    '--t-card-line': tier.cardLine,
    '--t-card-glass': tier.cardGlass,
    '--t-grain': tier.grain,
    '--t-brushed': tier.brushed,
    '--t-edge': tier.edge,
    '--t-drop': tier.drop,
    '--t-surface': tier.surface,
    '--t-border': tier.border,
    '--t-border-hover': tier.borderHover,
    '--t-ink': tier.ink,
    '--t-sub': tier.sub,
    '--t-accent': tier.accent,
    '--t-cta': tier.ctaBg,
    '--t-cta-ink': tier.ctaInk,
    '--t-cta-hover': tier.ctaHover,
    '--t-facet': tier.facet,
  }
}

/**
 * 套餐卡提亮层的平滑包络（33 个色标）。
 * 单调三次 Hermite 过 4 个节点：半径 0 时 1、40% 处 0.32、60% 处 0.10、80% 归零，
 * 中心与 80% 处切线为 0，钟形、无硬核，只亮在指针附近。
 */
const PLAN_KNOTS = [
  [0, 1, 0],
  [0.4, 0.32, -1.4],
  [0.6, 0.1, -0.8],
  [0.8, 0, 0],
]

export function planEnvelope(rgb) {
  const stops = []
  for (let i = 0; i <= 32; i++) {
    const r = (i / 32) * 0.8
    let k = 0
    while (k < PLAN_KNOTS.length - 2 && r > PLAN_KNOTS[k + 1][0]) k++
    const [x0, y0, m0] = PLAN_KNOTS[k]
    const [x1, y1, m1] = PLAN_KNOTS[k + 1]
    const h = x1 - x0
    const t = (r - x0) / h
    const t2 = t * t
    const t3 = t2 * t
    const a =
      (2 * t3 - 3 * t2 + 1) * y0 + (t3 - 2 * t2 + t) * h * m0 + (-2 * t3 + 3 * t2) * y1 + (t3 - t2) * h * m1
    stops.push(`rgba(${rgb}, ${clamp(a).toFixed(3)}) ${(r * 100).toFixed(2)}%`)
  }
  return stops.join(', ')
}

/**
 * 光照层变量。scale=hero 用于身份卡，scale=plan 用于套餐卡：
 * 套餐卡的对比/提亮/条带/边缘光直接取 tier.light.plan 的更克制参数，且不加暗肩。
 */
export function lightVars(tier, scale = 'hero') {
  const l = tier.light
  const plan = scale === 'plan'
  const s = plan
    ? { contrast: [39, 50], dark: 0, streak: 0.7, band: 0.7, rest: 0.45, peak: 1.08 }
    : { contrast: [56, 72], dark: 0.6, streak: 1, band: 1, rest: 0.7, peak: 1 }
  const ellipse = `${+(s.contrast[0] * l.field).toFixed(1)}% ${+(s.contrast[1] * l.field).toFixed(1)}%`
  return {
    '--l-rgb': l.rgb,
    '--l-a0': l.streakFrom,
    '--l-a1': l.streakTo,
    '--l-streak': plan ? l.plan.streak : +(l.streak * s.streak * (0.6 + 0.8 * l.contrast)).toFixed(3),
    '--l-shoulder': plan ? 0 : +(l.streak * s.streak * l.contrast * 0.5).toFixed(3),
    '--l-band': `${+((l.band / 2) * s.band).toFixed(2)}%`,
    '--l-glow': plan ? l.plan.glow : +Math.min(1, l.glow / 0.7).toFixed(3),
    '--l-rim': plan ? l.plan.rim : Math.min(0.35, l.rim),
    '--l-rim-max': plan ? 0.15 : 0.35,
    '--l-rest': s.rest,
    '--l-peak': s.peak,
    '--l-contrast': plan ? l.plan.contrast : l.contrast,
    '--l-ellipse': ellipse,
    '--l-dark': s.dark,
    '--l-glow-r': plan ? 1.2 : l.glowR,
    '--l-ambient': l.ambient,
    '--l-grain': l.grain,
    '--l-glow-image': plan
      ? `radial-gradient(circle var(--l-r, 320px) at var(--ms-mx, 72%) var(--ms-my, 24%), ${planEnvelope(l.rgb)})`
      : `radial-gradient(circle var(--l-r, 240px) at var(--ms-mx, 72%) var(--ms-my, 24%), rgba(${l.rgb}, 1) 0%, rgba(${l.rgb}, ${l.glowMid}) 30%, rgba(${l.rgb}, 0) ${l.glowEnd}%)`,
  }
}

/* ── 展示口径：数字全部来自后端字段，缺失就显示「—」，不补造 ── */

export function isActiveMembership(membership) {
  if (!membership) return false
  if (membership.status_wsh && membership.status_wsh !== 'active') return false
  if (membership.active_wsh === false) return false
  const end = Date.parse(membership.expires_at_wsh ?? '')
  return Number.isFinite(end) ? end > Date.now() : Boolean(membership.active_wsh)
}

export function getValidity(membership, now = Date.now()) {
  const start = Date.parse(membership?.started_at_wsh ?? '')
  const end = Date.parse(membership?.expires_at_wsh ?? '')
  const hasStart = Number.isFinite(start)
  const hasEnd = Number.isFinite(end)
  const rangeOk = hasStart && hasEnd && end > start
  const fallbackRemaining = Number.isFinite(Number(membership?.remaining_days_wsh))
    ? Math.max(0, Number(membership.remaining_days_wsh))
    : null
  const remainingDays = hasEnd ? Math.max(0, Math.ceil((end - now) / DAY)) : fallbackRemaining
  const totalDays = rangeOk ? Math.round((end - start) / DAY) : null
  return {
    startedAt: hasStart ? String(membership?.started_at_wsh ?? '').slice(0, 10) : null,
    expiresAt: hasEnd ? String(membership?.expires_at_wsh ?? '').slice(0, 10) : null,
    totalDays,
    usedDays: rangeOk ? Math.min(totalDays ?? 0, Math.max(0, Math.floor((now - start) / DAY))) : null,
    remainingDays,
    usedFraction: rangeOk ? clamp((now - start) / (end - start)) : null,
    expired: hasEnd ? end <= now : membership?.status_wsh === 'expired',
  }
}

/** 折扣率 → 折数（0.98 → 9.8）。非法值返回 null */
export function discountTenths(rate) {
  const n = Number(rate)
  if (!Number.isFinite(n) || n <= 0 || n > 1) return null
  return Math.round(n * 100) / 10
}

export function discountLabel(rate) {
  const t = discountTenths(rate)
  if (t == null) return '—'
  return t >= 10 ? '无折扣' : `${t} 折`
}

/** 日均价格：price / days，用于横向比较，不是额外权益 */
export function perDay(plan) {
  const price = Number(plan?.price_wsh)
  const days = Number(plan?.duration_days_wsh)
  if (!Number.isFinite(price) || !Number.isFinite(days) || !(days > 0)) return null
  return price / days
}

/** 金额：整数不带小数，其余保留两位 */
export function money(value) {
  const n = Number(value)
  if (!Number.isFinite(n)) return '—'
  return `¥${n.toLocaleString('zh-CN', {
    minimumFractionDigits: Number.isInteger(n) ? 0 : 2,
    maximumFractionDigits: 2,
  })}`
}

/** 日期统一写成 2026/11/19；缺失时返回「—」 */
export function fmtDate(value) {
  return value ? String(value).slice(0, 10).replace(/-/g, '/') : '—'
}

export function fmtDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '—'
}

/**
 * 套餐权益文案：每月券 + 下单折扣，全部来自两个配置字段。
 * 券只按数量描述（模板名要另一张表，这里不猜），下单折扣只在配置开启且折扣 < 1 时成立。
 * 券配置兼容两种历史结构：{"coupon_template_ids":[15,13]} 与 {"coupons":[{...}]}。
 */
export function planExtras(plan) {
  const out = []
  const couponCount = monthlyCouponCount(plan)
  if (couponCount > 0) out.push(`每月 ${couponCount} 张会员优惠券`)
  const config = parseJsonObject(plan?.benefit_config_wsh)
  const tenths = discountTenths(plan?.discount_rate_wsh)
  if (config.order_discount === true && tenths != null && tenths < 10) {
    out.push(`全站照护服务 ${discountLabel(plan?.discount_rate_wsh)}`)
  }
  return out
}

/** 每月发放的券数量：按 id 去重，也认对象数组 */
function monthlyCouponCount(plan) {
  const config = parseJsonObject(plan?.monthly_coupon_config_wsh)
  if (Array.isArray(config.coupons)) {
    return config.coupons.filter(item => item && typeof item === 'object').length
  }
  const ids = Array.isArray(config.coupon_template_ids) ? config.coupon_template_ids : []
  const seen = new Set()
  for (const raw of ids) {
    const id = Number(raw)
    if (Number.isInteger(id) && id > 0) seen.add(id)
  }
  return seen.size
}

/** 只接受 JSON 对象；空串、数组、格式错误一律当空对象，不抛错 */
function parseJsonObject(raw) {
  if (!raw) return {}
  try {
    const value = JSON.parse(raw)
    return value && typeof value === 'object' && !Array.isArray(value) ? value : {}
  } catch (e) {
    return {}
  }
}

function clamp(value) {
  return Math.min(1, Math.max(0, value))
}
