const MAX_REDIRECT_LENGTH = 2048

function safeDecode(value) {
  try {
    return decodeURIComponent(value)
  } catch {
    return null
  }
}

function containsControlChars(value) {
  return /[\u0000-\u001f\u007f]/.test(value)
}

// 登录后要跳回的地址是用户自己带过来的，不能拿到就跳，否则人家塞个外站链接就把人骗走了。
// 这里只放行站内路径，任何可能是外站的写法一律返回 null，让调用方退回首页。
export function safeRedirect(target) {
  if (typeof target !== 'string' || target.length === 0 || target.length > MAX_REDIRECT_LENGTH) {
    return null
  }
  const trimmed = target.trim()
  if (trimmed !== target) return null
  // 必须以单个 / 开头，http:// 这类绝对地址直接挡在外面
  if (!trimmed.startsWith('/')) return null
  // 为什么 // 也要挡？浏览器把 //evil.com 当成协议相对的完整网址，一样会跳去外站
  if (trimmed.startsWith('//') || trimmed.startsWith('/\\') || trimmed.startsWith('\\')) return null

  let decoded = safeDecode(trimmed)
  if (decoded === null || decoded.length > MAX_REDIRECT_LENGTH) return null
  // 为什么解两次？有人会把 %252F%252F 这种套两层编码，只解一次看不出真面目
  let doubleDecoded = safeDecode(decoded)
  if (doubleDecoded === null || doubleDecoded.length > MAX_REDIRECT_LENGTH) return null

  // 三种写法都要查一遍，光看原文很容易被编码绕过
  const candidates = [trimmed, decoded, doubleDecoded]
  if (candidates.some(containsControlChars)) return null
  if (candidates.some(value => value.includes('#'))) return null
  if (candidates.some(value => value.includes('\\'))) return null

  const path = decoded.split(/[?#]/, 1)[0]
  // 路径里出现冒号基本就是 http: 或者 javascript:，直接拒
  if (path.includes(':')) return null

  const query = decoded.includes('?') ? decoded.slice(decoded.indexOf('?') + 1) : ''
  // 参数里再套一个 redirect 就是典型的跳转套娃，防止一层层把人带到别处
  if (query && /(^|&)redirect=/i.test(query)) return null

  return decoded
}
