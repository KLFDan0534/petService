import request from '@/utils/request'

// 登录，账号密码换回一个 token，之后所有请求都带着它
export async function login(data) {
  const res = await request.post('/api/auth/login', data)
  return res.data
}

export async function register(data) {
  const res = await request.post('/api/auth/register', data)
  return res.data
}

// 先要一条短信验证码，拿到了才能往下填注册表单
export async function requestRegisterCaptcha(data) {
  const res = await request.post('/api/auth/register/captcha', data)
  return res.data
}

// 拿旧 token 换新的，请求拦截器发现快过期时悄悄调
export async function refreshToken(data) {
  const res = await request.post('/api/auth/refresh', data)
  return res.data
}

// 忘记密码，发验证码到手机走重置流程
export async function forgotPassword(data) {
  const res = await request.post('/api/auth/forgot-password', data)
  return res.data
}

// 取当前登录人的资料，刷新页面后靠它恢复登录态
export async function getCurrentUser() {
  const res = await request.get('/api/users/me')
  return res.data
}

export async function updateCurrentUser(data) {
  const res = await request.put('/api/users/me', data)
  return res.data
}

export async function deleteCurrentUser() {
  const res = await request.delete('/api/users/me')
  return res.data
}



