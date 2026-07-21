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

export async function checkFavorite(params) {
  const res = await request.get('/api/favorites/check', { params })
  return res.data
}

export async function toggleFavorite(data) {
  const res = await request.post('/api/favorites/toggle', data)
  return res.data
}



