import * as api from '@/api/favorite'

function extract(res) {
  if (!res) throw new Error('无响应')
  if (res.code !== 200) throw new Error(res.message || '请求失败')
  return res.data ?? res
}

function normalizeFavoriteParams(params = {}) {
  const normalized = { ...params }
  if (normalized.targetId !== undefined && normalized.target_id_wsh === undefined) {
    normalized.target_id_wsh = normalized.targetId
  }
  if (normalized.targetType !== undefined && normalized.target_type_wsh === undefined) {
    normalized.target_type_wsh = normalized.targetType
  }
  delete normalized.targetId
  delete normalized.targetType
  return normalized
}

export async function getList(params = {}) {
  return extract(await api.getFavorites(normalizeFavoriteParams(params)))
}

export async function getPage(params = {}) {
  return extract(await api.getFavoritePage(normalizeFavoriteParams(params)))
}

export async function getTypes() {
  return extract(await api.getFavoriteTypes())
}

export async function check(targetId, targetType) {
  return extract(await api.checkFavorite({ target_id_wsh: targetId, target_type_wsh: targetType }))
}

export async function toggle(targetId, targetType) {
  return extract(await api.toggleFavorite({ target_id_wsh: targetId, target_type_wsh: targetType }))
}
