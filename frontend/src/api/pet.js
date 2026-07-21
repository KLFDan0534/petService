import request from '@/utils/request'

export async function getPets() {
  const res = await request.get('/api/pets')
  return res.data
}

export async function getPet(id) {
  const res = await request.get(`/api/pets/${id}`)
  return res.data
}

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

export async function uploadPetAvatar(data) {
  const res = await request.post('/api/files/upload?directory=pets', data)
  return res.data
}

export async function getMerchantPets(page = 1, size = 100) {
  const res = await request.get(`/pets/merchant?page=${page}&size=${size}`)
  return res.data
}

export async function getAdminPets() {
  const res = await request.get('/api/pets/admin/all')
  return res.data
}



