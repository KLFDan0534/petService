import * as api from '@/api/favorite'

// 接口统一返回 { code, message, data }，这里统一拆一层，出错直接抛，页面只管拿数据
function extract(res) {
  if (!res) throw new Error('无响应')
  if (res.code !== 200) throw new Error(res.message || '请求失败')
  return res.data ?? res
}

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
