import * as api from '@/api/pet'
import { uploadFileToDirectory } from '@/api/file'

function extract(res) {
  if (!res) throw new Error('无响应')
  if (res.code !== 200) throw new Error(res.message || '请求失败')
  return res.data ?? res
}

export async function getById(id) {
  return extract(await api.getPet(id))
}

export async function getList() {
  return extract(await api.getPets())
}

export async function create(data) {
  return extract(await api.createPet(data))
}

export async function update(id, data) {
  return extract(await api.updatePet(id, data))
}

export async function remove(id) {
  return extract(await api.deletePet(id))
}

export async function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return extract(await uploadFileToDirectory('pets', formData))
}

export async function updateAvatar(id, avatarUrl) {
  return update(id, { avatar_wsh: avatarUrl })
}
