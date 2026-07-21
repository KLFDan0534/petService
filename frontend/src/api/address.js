import request from '@/utils/request'

export async function getAddresses() {
  const res = await request.get('/api/addresses')
  return res.data
}

export async function createAddress(data) {
  const res = await request.post('/api/addresses', data)
  return res.data
}

export async function updateAddress(id, data) {
  const res = await request.put(`/api/addresses/${id}`, data)
  return res.data
}

export async function deleteAddress(id) {
  const res = await request.delete(`/api/addresses/${id}`)
  return res.data
}

export async function setDefaultAddress(id) {
  const res = await request.post(`/api/addresses/${id}/default`, {})
  return res.data
}



