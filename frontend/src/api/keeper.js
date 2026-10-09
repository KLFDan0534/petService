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

export async function getMyKeeperApplication() {
  const res = await request.get('/api/keepers/my-application')
  return res.data
}

export async function getNearbyKeepers(params) {
  const res = await request.get('/api/keepers/nearby', { params })
  return res.data
}

export async function getPendingKeepers() {
  const res = await request.get('/api/keepers/pending')
  return res.data
}

export async function getMerchantPendingKeepers() {
  const res = await request.get('/api/keepers/merchant/pending')
  return res.data
}

export async function resignKeeper(id) {
  const res = await request.post(`/api/keepers/${id}/resign`, {})
  return res.data
}

export async function merchantApproveKeeper(id) {
  const res = await request.post(`/api/keepers/${id}/merchant-approve`, {})
  return res.data
}

export async function merchantRejectKeeper(id) {
  const res = await request.post(`/api/keepers/${id}/merchant-reject`, {})
  return res.data
}

export async function merchantTerminateKeeper(id) {
  const res = await request.post(`/api/keepers/${id}/merchant-terminate`, {})
  return res.data
}



