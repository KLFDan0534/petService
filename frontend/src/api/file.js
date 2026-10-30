import request from '@/utils/request'

// 文件列表，一般给后台文件管理页用
export async function getFiles() {
  const res = await request.get('/api/files')
  return res.data
}

export async function uploadFile(data) {
  const res = await request.post('/api/files/upload', data)
  return res.data
}

// 传文件时顺手指定放哪个目录，头像、证件各归各的
export async function uploadFileToDirectory(directory, data) {
  const res = await request.post(`/api/files/upload?directory=${directory}`, data)
  return res.data
}

// 下载文件，得声明 blob 才能拿到二进制，不然会被当文本解析
export async function downloadFile(id) {
  const res = await request.get(`/files/${id}/download`, { responseType: 'blob' })
  return res.data
}

// 传商品图，带商家 id 就归到那家店，不带就是平台公共图
export async function uploadProductImage(merchantId, data) {
  const params = merchantId != null ? { merchantId } : undefined
  const res = await request.post('/api/files/product-image', data, { params })
  return res.data
}

// 后台看所有上传的文件，可按类型和上传人筛
export async function getAdminFiles(params = {}) {
  const res = await request.get('/api/files/admin-list', { params })
  return res.data
}

export async function deleteFile(id) {
  const res = await request.delete(`/api/files/${id}`)
  return res.data
}



