import request from '@/utils/request'

// 我名下的宠物，下单选宠物时从这挑
export async function getPets() {
  const res = await request.get('/api/pets')
  return res.data
}

export async function getPet(id) {
  const res = await request.get(`/api/pets/${id}`)
  return res.data
}

// 添加一只宠物，品种体重这些填得越全后面越好派单
export async function createPet(data) {
  const res = await request.post('/api/pets', data)
  return res.data
}

export async function updatePet(id, data) {
  const res = await request.put(`/api/pets/${id}`, data)
  return res.data
}

export async function deletePet(id) {
  const res = await request.delete(`/api/pets/${id}`)
  return res.data
}

// 传宠物头像，借用通用上传接口，靠 directory 参数归到 pets 目录
export async function uploadPetAvatar(data) {
  const res = await request.post('/api/files/upload?directory=pets', data)
  return res.data
}

// 商家这边服务过的宠物档案，分页拉
export async function getMerchantPets(page = 1, size = 100) {
  const res = await request.get(`/pets/merchant?page=${page}&size=${size}`)
  return res.data
}

// 后台看全平台宠物，运营统计用
export async function getAdminPets() {
  const res = await request.get('/api/pets/admin/all')
  return res.data
}



