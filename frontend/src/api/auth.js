import request from '@/utils/request'

export async function login(data) {
  const res = await request.post('/api/auth/login', data)
  return res.data
}

export async function register(data) {
  const res = await request.post('/api/auth/register', data)
  return res.data
}

export async function requestRegisterCaptcha(data) {
  const res = await request.post('/api/auth/register/captcha', data)
  return res.data
}

export async function refreshToken(data) {
  const res = await request.post('/api/auth/refresh', data)
  return res.data
}

export async function forgotPassword(data) {
  const res = await request.post('/auth/forgot-password', data)
  return res.data
}

export async function getCurrentUser() {
  const res = await request.get('/users/me')
  return res.data
}

export async function updateCurrentUser(data) {
  const res = await request.put('/users/me', data)
  return res.data
}

export async function deleteCurrentUser() {
  const res = await request.delete('/users/me')
  return res.data
}



