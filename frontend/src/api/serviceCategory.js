import request from '@/utils/request'

export async function getServiceCategoryList() {
  const res = await request.get('/service-categories/list')
  return res.data
}

export async function getAdminServiceCategoryList() {
  const res = await request.get('/service-categories/admin/list')
  return res.data
}

export async function getServiceCategoryById(id) {
  const res = await request.get(`/service-categories/${id}`)
  return res.data
}

export async function createServiceCategory(data) {
  const res = await request.post('/service-categories', data)
  return res.data
}

export async function updateServiceCategory(id, data) {
  const res = await request.put(`/service-categories/${id}`, data)
  return res.data
}

export async function deleteServiceCategory(id) {
  const res = await request.delete(`/service-categories/${id}`)
  return res.data
}


