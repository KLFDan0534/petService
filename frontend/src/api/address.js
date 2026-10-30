import request from '@/utils/request'

// 只返回当前登录用户自己的收货地址，不用传用户 id
export async function getAddresses() {
  const res = await request.get('/api/addresses')
  return res.data
}

export async function getAddress(id) {
  const res = await request.get(`/api/addresses/${id}`)
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

// 设成默认收货地址，后端会把其它地址上的默认标记清掉
export async function setDefaultAddress(id) {
  const res = await request.post(`/api/addresses/${id}/default`, {})
  return res.data
}



