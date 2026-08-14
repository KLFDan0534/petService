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

export function safeRedirect(target) {
  if (typeof target !== 'string' || target.length === 0 || target.length > MAX_REDIRECT_LENGTH) {
    return null
  }
  const trimmed = target.trim()
  if (trimmed !== target) return null
  if (!trimmed.startsWith('/')) return null
  if (trimmed.startsWith('//') || trimmed.startsWith('/\\') || trimmed.startsWith('\\')) return null

  let decoded = safeDecode(trimmed)
  if (decoded === null || decoded.length > MAX_REDIRECT_LENGTH) return null
  let doubleDecoded = safeDecode(decoded)
  if (doubleDecoded === null || doubleDecoded.length > MAX_REDIRECT_LENGTH) return null

  const candidates = [trimmed, decoded, doubleDecoded]
  if (candidates.some(containsControlChars)) return null
  if (candidates.some(value => value.includes('#'))) return null
  if (candidates.some(value => value.includes('\\'))) return null

  const path = decoded.split(/[?#]/, 1)[0]
  if (path.includes(':')) return null

  const query = decoded.includes('?') ? decoded.slice(decoded.indexOf('?') + 1) : ''
  if (query && /(^|&)redirect=/i.test(query)) return null

  return decoded
}
