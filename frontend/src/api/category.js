import request from '@/utils/request'

// 分类由运营在后台维护，前端主要拿它做筛选和发布宠物时的选项
export async function getCategories() {
  const res = await request.get('/api/categories')
  return res.data
}

export async function getCategory(id) {
  const res = await request.get(`/api/categories/${id}`)
  return res.data
}

// 按父级拿子分类，两级下拉框联动时用
export async function getCategoriesByParent(parentId) {
  const res = await request.get(`/api/categories/parent/${parentId}`)
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



