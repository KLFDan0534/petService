import { onBeforeUnmount, onMounted, shallowRef } from 'vue'

// Turnstile 站点公钥（公开值，前端渲染用；可用 VITE_TURNSTILE_SITEKEY 覆盖）
const SITEKEY = import.meta.env.VITE_TURNSTILE_SITEKEY || '0x4AAAAAAEp_LyWMpApQP7Ci'
const SCRIPT_SRC = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit'

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
 * @param {import('vue').Ref<HTMLElement|null>} containerRef 挂载 widget 的容器元素（需在模板中存在）
 * @returns {{ token: import('vue').Ref<string>, error: import('vue').Ref<string>, reset: () => void }}
 */
export function useTurnstile(containerRef) {
  const token = shallowRef('')
  const error = shallowRef('')
  let widgetId = null

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
      },
      'error-callback': () => {
        token.value = ''
        error.value = '人机验证加载失败，请刷新页面重试'
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
    } else {
      error.value = '人机验证加载失败，请刷新页面重试'
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
    if (widgetId != null && window.turnstile) {
      try {
        window.turnstile.remove(widgetId)
      } catch (e) {
        /* noop */
      }
      widgetId = null
    }
  })

  return { token, error, reset }
}