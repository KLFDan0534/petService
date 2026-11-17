import axios from 'axios'
import request from '@/utils/request'

// 登录，账号密码换回一个 token，之后所有请求都带着它。
// 必须绕开统一拦截器：request 实例在 401 时会尝试刷新 token，而「密码错误」本身就是 401，
// 走拦截器会把后端的「用户名或密码不正确」改写成「未登录或登录已过期」。
// 因此这里用裸 axios，返回值仍是 Result 信封，与其它 api 方法一致。
export async function login(data) {
  const res = await axios.post('/api/auth/login', data)
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

// 退出登录：通知后端把当前 token 加入黑名单（前端本地状态由 store 负责清理）
export async function logout() {
  const res = await request.post('/api/auth/logout')
  return res.data
}

// 更改绑定手机号
export async function updatePhone(data) {
  const res = await request.put('/api/users/me/phone', data)
  return res.data
}

// 更改绑定邮箱
export async function updateEmail(data) {
  const res = await request.put('/api/users/me/email', data)
  return res.data
}

// 提交实名认证
export async function updateRealName(data) {
  const res = await request.put('/api/users/me/real-name', data)
  return res.data
}

// 设置/重置支付密码
export async function updatePaymentPassword(data) {
  const res = await request.put('/api/users/me/payment-password', data)
  return res.data
}



