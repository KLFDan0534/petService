import { onBeforeUnmount, onMounted, shallowRef } from 'vue'

// Turnstile 站点公钥（公开值，前端渲染用；可用 VITE_TURNSTILE_SITEKEY 覆盖）
const SITEKEY = import.meta.env.VITE_TURNSTILE_SITEKEY || '0x4AAAAAAEp_LyWMpApQP7Ci'
const SCRIPT_SRC = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit'

// 兜底超时：超过该时间既无 token 也无错误回调，即视为不可用并放行
const FALLBACK_TIMEOUT_MS = 12000

let scriptPromise = null

function ensureScript() {
  if (typeof window === 'undefined') return Promise.resolve(false)
  if (window.turnstile) return Promise.resolve(true)
  if (!scriptPromise) {
    scriptPromise = new Promise((resolve) => {
      const existing = document.querySelector(
        'script[src^="https://challenges.cloudflare.com/turnstile"]',
      )
      if (existing) {
        existing.addEventListener('load', () => resolve(true))
        existing.addEventListener('error', () => resolve(false))
        return
      }
      const script = document.createElement('script')
      script.src = SCRIPT_SRC
      script.async = true
      script.onload = () => resolve(true)
      script.onerror = () => resolve(false)
      document.head.appendChild(script)
    })
  }
  return scriptPromise
}

/**
 * Cloudflare Turnstile 组合式函数。
 *
 * 降级策略：Turnstile 依赖 challenges.cloudflare.com，在国内网络、代理环境、
 * 或 sitekey 域名白名单未覆盖当前访问域名（例如直接用 IP 访问）时可能无法渲染。
 * 此时将 unavailable 置为 true 并放行登录 —— 与后端 TURNSTILE_SECRET 未配置时的
 * fail-open 行为保持一致（后端才是真正的安全边界，配置密钥后仍会强制校验）。
 *
 * @param {import('vue').Ref<HTMLElement|null>} containerRef 挂载 widget 的容器元素（需在模板中存在）
 * @returns {{ token, error, unavailable, reset }}
 */
export function useTurnstile(containerRef) {
  const token = shallowRef('')
  const error = shallowRef('')
  // true 表示人机验证不可用，调用方应跳过强制校验，避免用户被完全挡在门外
  const unavailable = shallowRef(false)
  let widgetId = null
  let fallbackTimer = null

  function markUnavailable(message) {
    token.value = ''
    unavailable.value = true
    error.value = message
  }

  function render() {
    const el = containerRef.value
    if (!el || !window.turnstile) return
    if (widgetId != null) {
      window.turnstile.reset(widgetId)
      return
    }
    widgetId = window.turnstile.render(el, {
      sitekey: SITEKEY,
      theme: 'auto',
      callback: (t) => {
        token.value = t
        error.value = ''
        unavailable.value = false
      },
      'error-callback': () => {
        markUnavailable('人机验证不可用，已跳过校验（可直接登录）')
      },
      'expired-callback': () => {
        token.value = ''
      },
    })
  }

  async function init() {
    const ok = await ensureScript()
    if (ok) {
      // 等待 Vue 完成 DOM 更新，确保容器已插入
      await new Promise((resolve) => {
        requestAnimationFrame(() => setTimeout(resolve, 0))
      })
      render()
      // 兜底：超时仍无 token 且无错误回调（如域名不在 sitekey 白名单、代理异常），
      // 视为不可用并放行，避免用户彻底无法登录。
      fallbackTimer = setTimeout(() => {
        if (!token.value && !error.value) {
          markUnavailable('人机验证超时，已跳过校验（可直接登录）')
        }
      }, FALLBACK_TIMEOUT_MS)
    } else {
      markUnavailable('人机验证不可用，已跳过校验（可直接登录）')
    }
  }

  function reset() {
    token.value = ''
    if (widgetId != null && window.turnstile) {
      try {
        window.turnstile.reset(widgetId)
      } catch (e) {
        /* noop */
      }
    }
  }

  onMounted(init)
  onBeforeUnmount(() => {
    if (fallbackTimer) {
      clearTimeout(fallbackTimer)
      fallbackTimer = null
    }
    if (widgetId != null && window.turnstile) {
      try {
        window.turnstile.remove(widgetId)
      } catch (e) {
        /* noop */
      }
      widgetId = null
    }
  })

  return { token, error, unavailable, reset }
}
