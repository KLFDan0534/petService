export const FAVORITE_TARGET_TYPES = Object.freeze({
  MERCHANT: 'merchant',
  KEEPER: 'keeper',
  SERVICE: 'service',
})

export const FAVORITE_TARGET_TYPE_LABELS = Object.freeze({
  merchant: '商家',
  keeper: '寄养员',
  service: '服务',
})

export const DEFAULT_FAVORITE_PAGE_SIZE = 12

export function normalizeFavoriteTargetType(value) {
  return String(value ?? '').trim().toLowerCase()
}

export function isFavoriteTargetType(value) {
  const normalized = normalizeFavoriteTargetType(value)
  return Object.values(FAVORITE_TARGET_TYPES).includes(normalized)
}

export function getFavoriteTargetTypeLabel(value) {
  return FAVORITE_TARGET_TYPE_LABELS[normalizeFavoriteTargetType(value)] || '未知'
}
