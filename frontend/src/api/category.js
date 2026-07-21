import request from '@/utils/request'

export async function getCategories() {
  const res = await request.get('/api/categories')
  return res.data
}

export async function getCategory(id) {
  const res = await request.get(`/api/categories/${id}`)
  return res.data
}

export async function createCategory(data) {
  const res = await request.post('/api/categories', data)
  return res.data
}

export async function updateCategory(id, data) {
  const res = await request.put(`/api/categories/${id}`, data)
  return res.data
}

export async function deleteCategory(id) {
  const res = await request.delete(`/api/categories/${id}`)
  return res.data
}



