import * as api from '@/api/file'
import { unwrapResult } from '@/utils/result'

// 上传要传 FormData，不能直接传 File 对象，浏览器才认这个格式
export async function upload(file) {
  const formData = new FormData()
  formData.append('file', file)
  return unwrapResult(await api.uploadFile(formData))
}

export async function uploadToDirectory(directory, file) {
  const formData = new FormData()
  formData.append('file', file)
  return unwrapResult(await api.uploadFileToDirectory(directory, formData))
}

export async function getList() {
  return unwrapResult(await api.getFiles())
}

export async function download(id) {
  return unwrapResult(await api.downloadFile(id))
}
