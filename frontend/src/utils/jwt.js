// JWT 是三段用点隔开的字符串，第二段才是用户信息，base64 解一下就能读
export function parseJwt(token) {
  try {
    const payload = token.split('.')[1]
    return JSON.parse(atob(payload))
  } catch { return null }
}

// 读不到 exp 也当过期处理，宁可多登一次也别拿着废 token 去请求
export function isTokenExpired(token) {
  const payload = parseJwt(token)
  if (!payload || !payload.exp) return true
  return Date.now() >= payload.exp * 1000
}
