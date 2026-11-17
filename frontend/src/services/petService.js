import * as api from '@/api/pet'
import { uploadFileToDirectory } from '@/api/file'
import { unwrapResult } from '@/utils/result'

export async function getById(id) {
  return unwrapResult(await api.getPet(id))
}

export async function getList() {
  return unwrapResult(await api.getPets())
}

export async function create(data) {
  return unwrapResult(await api.createPet(data))
}

export async function update(id, data) {
  return unwrapResult(await api.updatePet(id, data))
}

export async function remove(id) {
  return unwrapResult(await api.deletePet(id))
}

// 头像先传到 pets 目录拿到地址，再拿地址去更新宠物，两步分开做
export async function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return unwrapResult(await uploadFileToDirectory('pets', formData))
}

export async function updateAvatar(id, avatarUrl) {
  return update(id, { avatar_wsh: avatarUrl })
}
