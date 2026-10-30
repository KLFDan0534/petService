import * as api from '@/api/pet'
import { uploadFileToDirectory } from '@/api/file'

// 接口统一返回 { code, message, data }，这里统一拆一层，出错直接抛，页面只管拿数据
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

// 头像先传到 pets 目录拿到地址，再拿地址去更新宠物，两步分开做
export async function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return extract(await uploadFileToDirectory('pets', formData))
}

export async function updateAvatar(id, avatarUrl) {
  return update(id, { avatar_wsh: avatarUrl })
}
