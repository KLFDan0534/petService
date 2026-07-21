import request from '@/utils/request'

export async function getMyWallet() {
  const res = await request.get('/api/wallet/me')
  return res.data
}

export async function getWallets() {
  const res = await request.get('/api/wallet')
  return res.data
}

export async function adjustWallet(data) {
  const res = await request.post('/api/wallet/admin/adjust', data)
  return res.data
}

export async function getMyTips() {
  const res = await request.get('/api/tips/me')
  return res.data
}

export async function createTip(data) {
  const res = await request.post('/api/tips', data)
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

export async function createWithdrawal(data) {
  const res = await request.post('/api/withdrawals', data)
  return res.data
}

export async function approveWithdrawal(id) {
  const res = await request.post(`/api/withdrawals/${id}/approve`, {})
  return res.data
}

export async function rejectWithdrawal(id) {
  const res = await request.post(`/api/withdrawals/${id}/reject`, {})
  return res.data
}

export async function completeWithdrawal(id) {
  const res = await request.post(`/api/withdrawals/${id}/complete`, {})
  return res.data
}

export async function getMyTransactions() {
  const res = await request.get('/api/transactions/me')
  return res.data
}

export async function getTransactions() {
  const res = await request.get('/api/transactions')
  return res.data
}



