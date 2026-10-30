import request from '@/utils/request'

export async function getFavorites(params = {}) {
  const res = await request.get('/api/favorites', { params })
  return res.data
}

export async function getFavoritePage(params = {}) {
  const res = await request.get('/api/favorites/page', { params })
  return res.data
}

export async function getFavoriteTypes() {
  const res = await request.get('/api/favorites/types')
  return res.data
}

// 查当前用户有没有收藏过这件东西，详情页靠它决定爱心是实心还是空心
export async function checkFavorite(params) {
  const res = await request.get('/api/favorites/check', { params })
  return res.data
}

// 收藏和取消收藏共用这一个接口，后端看当前状态自己切，前端别拆成两个调
export async function toggleFavorite(data) {
  const res = await request.post('/api/favorites/toggle', data)
  return res.data
}

export async function getAdminFavorites(params = {}) {
  const res = await request.get('/api/favorites/admin-list', { params })
  return res.data
}



