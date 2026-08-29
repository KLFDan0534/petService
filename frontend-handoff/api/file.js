import request from '@/utils/request'

export async function getFiles() {
  const res = await request.get('/api/files')
  return res.data
}

export async function uploadFile(data) {
  const res = await request.post('/api/files/upload', data)
  return res.data
}

export async function uploadFileToDirectory(directory, data) {
  const res = await request.post(`/api/files/upload?directory=${directory}`, data)
  return res.data
}

export async function downloadFile(id) {
  const res = await request.get(`/files/${id}/download`, { responseType: 'blob' })
  return res.data
}

export async function uploadProductImage(merchantId, data) {
  const params = merchantId != null ? { merchantId } : undefined
  const res = await request.post('/api/files/product-image', data, { params })
  return res.data
}

export async function getAdminFiles(params = {}) {
  const res = await request.get('/api/files/admin-list', { params })
  return res.data
}

export async function deleteFile(id) {
  const res = await request.delete(`/api/files/${id}`)
  return res.data
}



