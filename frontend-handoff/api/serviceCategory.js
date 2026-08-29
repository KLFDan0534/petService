import request from '@/utils/request'

export async function getServiceCategoryList() {
  const res = await request.get('/api/service-categories/list')
  return res.data
}

export async function getAdminServiceCategoryList() {
  const res = await request.get('/api/service-categories/admin/list')
  return res.data
}

export async function getServiceCategoryById(id) {
  const res = await request.get(`/api/service-categories/${id}`)
  return res.data
}

export async function createServiceCategory(data) {
  const res = await request.post('/api/service-categories', data)
  return res.data
}

export async function updateServiceCategory(id, data) {
  const res = await request.put(`/api/service-categories/${id}`, data)
  return res.data
}

export async function deleteServiceCategory(id) {
  const res = await request.delete(`/api/service-categories/${id}`)
  return res.data
}


