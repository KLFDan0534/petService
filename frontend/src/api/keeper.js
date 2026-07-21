import request from '@/utils/request'

export async function getKeepers() {
  const res = await request.get('/api/keepers')
  return res.data
}

export async function getKeeper(id) {
  const res = await request.get(`/api/keepers/${id}`)
  return res.data
}

export async function getMyKeeperInfo() {
  const res = await request.get('/api/keepers/me')
  return res.data
}

export async function createKeeper(data) {
  const res = await request.post('/api/keepers', data)
  return res.data
}

export async function updateKeeper(id, data) {
  const res = await request.put(`/api/keepers/${id}`, data)
  return res.data
}

export async function getKeepersByMerchant(merchantId) {
  const res = await request.get(`/api/keepers/merchant/${merchantId}`)
  return res.data
}

export async function approveKeeper(id) {
  const res = await request.post(`/api/keepers/${id}/approve`, {})
  return res.data
}

export async function rejectKeeper(id) {
  const res = await request.post(`/api/keepers/${id}/reject`, {})
  return res.data
}

export async function updateKeeperOnlineStatus(id, status) {
  const res = await request.patch(`/api/keepers/${id}/online-status`, { status_wsh: status })
  return res.data
}

export async function uploadKeeperAvatar(data) {
  const res = await request.post('/api/files/upload?directory=keepers', data)
  return res.data
}



