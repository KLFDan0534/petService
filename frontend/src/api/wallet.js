import request from '@/utils/request'

// 我的钱包余额，能提的和冻结的都在里面
export async function getMyWallet() {
  const res = await request.get('/api/wallet/me')
  return res.data
}

// 后台看所有人的钱包，可按用户或余额筛
export async function getWallets(params) {
  const res = await request.get('/api/wallet', { params })
  return res.data
}

// 后台手工加减余额，客服补偿走这个，必须写清楚原因
export async function adjustWallet(data) {
  const res = await request.post('/api/wallet/admin/adjust', data)
  return res.data
}

// 往钱包里充钱，充完直接能拿来付订单
export async function rechargeWallet(data) {
  const res = await request.post('/api/wallet/recharge', data)
  return res.data
}

export async function getMyTips() {
  const res = await request.get('/api/tips/me')
  return res.data
}

// 给宠托师打赏一笔，钱从钱包扣
export async function createTip(data) {
  const res = await request.post('/api/tips', data)
  return res.data
}

export async function getAdminTips(params = {}) {
  const res = await request.get('/api/tips/admin-list', { params })
  return res.data
}

export async function getTipsByOrder(orderId) {
  const res = await request.get(`/api/tips/order/${orderId}`)
  return res.data
}

export async function getMyWithdrawals() {
  const res = await request.get('/api/withdrawals/me')
  return res.data
}

export async function getWithdrawals() {
  const res = await request.get('/api/withdrawals')
  return res.data
}

// 提现申请，先扣下余额等财务打款
export async function createWithdrawal(data) {
  const res = await request.post('/api/withdrawals', data)
  return res.data
}

// 审批通过提现，通过后才轮到打款
export async function approveWithdrawal(id) {
  const res = await request.post(`/api/withdrawals/${id}/approve`, {})
  return res.data
}

export async function rejectWithdrawal(id) {
  const res = await request.post(`/api/withdrawals/${id}/reject`, {})
  return res.data
}

// 标记这笔钱已经打出去了，财务转完账点一下
export async function completeWithdrawal(id) {
  const res = await request.post(`/api/withdrawals/${id}/complete`, {})
  return res.data
}

export async function getMyTransactions() {
  const res = await request.get('/api/transactions/me')
  return res.data
}

// 后台查全部流水，对账时按时间和类型筛
export async function getTransactions(params) {
  const res = await request.get('/api/transactions', { params })
  return res.data
}



