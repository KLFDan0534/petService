import * as api from '@/api/file'

function extract(res) {
  if (!res) throw new Error('无响应')
  if (res.code !== 200) throw new Error(res.message || '请求失败')
  return res.data ?? res
}

// 上传要传 FormData，不能直接传 File 对象，浏览器才认这个格式
export async function upload(file) {
  const formData = new FormData()
  formData.append('file', file)
  return extract(await api.uploadFile(formData))
}

export async function uploadToDirectory(directory, file) {
  const formData = new FormData()
  formData.append('file', file)
  return extract(await api.uploadFileToDirectory(directory, formData))
}

export async function getList() {
  return extract(await api.getFiles())
}

export async function download(id) {
  return extract(await api.downloadFile(id))
}
