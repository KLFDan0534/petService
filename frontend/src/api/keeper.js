import request from '@/utils/request'

// 宠托师列表，用户挑人时用，只出通过审核的
export async function getKeepers() {
  const res = await request.get('/api/keepers')
  return res.data
}

export async function getKeeper(id) {
  const res = await request.get(`/api/keepers/${id}`)
  return res.data
}

// 我自己作为宠托师的那份资料，工作台首页读它
export async function getMyKeeperInfo() {
  const res = await request.get('/api/keepers/me')
  return res.data
}

// 提交宠托师入驻申请，交完得等平台审
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

// 平台审核通过，通过后这人才能接单
export async function approveKeeper(id) {
  const res = await request.post(`/api/keepers/${id}/approve`, {})
  return res.data
}

// 平台驳回入驻申请，材料不合格时走这里
export async function rejectKeeper(id) {
  const res = await request.post(`/api/keepers/${id}/reject`, {})
  return res.data
}

// 切换接单开关，下线后就不再派新单过来
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

// 按经纬度找附近的宠托师，下单时选人用
export async function getNearbyKeepers(params) {
  const res = await request.get('/api/keepers/nearby', { params })
  return res.data
}

export async function getPendingKeepers() {
  const res = await request.get('/api/keepers/pending')
  return res.data
}

// 商家名下还没审的人，商家自己得先过一遍
export async function getMerchantPendingKeepers() {
  const res = await request.get('/api/keepers/merchant/pending')
  return res.data
}

export async function resignKeeper(id) {
  const res = await request.post(`/api/keepers/${id}/resign`, {})
  return res.data
}

// 商家同意收人，商家这边通过后平台那边才算数
export async function merchantApproveKeeper(id) {
  const res = await request.post(`/api/keepers/${id}/merchant-approve`, {})
  return res.data
}

export async function merchantRejectKeeper(id) {
  const res = await request.post(`/api/keepers/${id}/merchant-reject`, {})
  return res.data
}

// 商家跟人解约，解完这人在该商家就接不了单
export async function merchantTerminateKeeper(id) {
  const res = await request.post(`/api/keepers/${id}/merchant-terminate`, {})
  return res.data
}



