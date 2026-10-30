import request from '@/utils/request'

// 用户端下单选服务时用的类目，只返回能正常下单的那些
export async function getServiceCategoryList() {
  const res = await request.get('/api/service-categories/list')
  return res.data
}

// 后台版能看到已下架的类目，前台版只给正常在用的，两个别混
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


