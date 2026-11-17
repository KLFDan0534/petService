import * as api from '@/api/favorite'
import { unwrapResult } from '@/utils/result'

// 页面习惯写驼峰，后端只认下划线，这里做一次转换再发出去，不然收藏会存错对象
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
  return unwrapResult(await api.getFavorites(normalizeFavoriteParams(params)))
}

export async function getPage(params = {}) {
  return unwrapResult(await api.getFavoritePage(normalizeFavoriteParams(params)))
}

export async function getTypes() {
  return unwrapResult(await api.getFavoriteTypes())
}

export async function check(targetId, targetType) {
  return unwrapResult(await api.checkFavorite({ target_id_wsh: targetId, target_type_wsh: targetType }))
}

export async function toggle(targetId, targetType) {
  return unwrapResult(await api.toggleFavorite({ target_id_wsh: targetId, target_type_wsh: targetType }))
}
