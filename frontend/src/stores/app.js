import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

const THEME_STORAGE_KEY = 'pet-service-theme'
const THEME_MODES = ['light', 'dark']

// 设计令牌（--color-* / --ref-* / --shadow-* 等）的唯一权威来源是
// src/assets/css/design-tokens.css（:root 与 html[data-theme="dark"]）。
// 本 store 只负责“主题状态”，通过 data-theme / .dark / color-scheme 触发 CSS 级联，
// 绝不在运行时重复注入令牌，避免与 CSS 发生双轨覆盖。

function normalizeTheme(value) {
  return THEME_MODES.includes(value) ? value : 'light'
}

function getStoredTheme() {
  if (typeof localStorage === 'undefined') return ''

  try {
    const storedTheme = localStorage.getItem(THEME_STORAGE_KEY)
    return THEME_MODES.includes(storedTheme) ? storedTheme : ''
  } catch (_) {
    return ''
  }
}

function getSystemTheme() {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
    return 'light'
  }

  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

function getInitialTheme() {
  return getStoredTheme() || getSystemTheme()
}

export const useAppStore = defineStore('app', () => {
  const toasts = ref([])
  const unread = ref(0)
  const showLoginPrompt = ref(false)
  const loginRedirectPath = ref('')
  const theme = ref(getInitialTheme())
  const isDarkTheme = computed(() => theme.value === 'dark')

  function addToast(message, type = 'info') {
    const id = Date.now()
    toasts.value.push({ id, message, type })
    setTimeout(() => removeToast(id), 3000)
  }

  function removeToast(id) {
    toasts.value = toasts.value.filter(t => t.id !== id)
  }

  function closeLoginPrompt() {
    showLoginPrompt.value = false
  }

  function applyTheme(nextTheme = theme.value) {
    const normalizedTheme = normalizeTheme(nextTheme)
    theme.value = normalizedTheme

    if (typeof document !== 'undefined') {
      const root = document.documentElement
      root.dataset.theme = normalizedTheme
      root.classList.toggle('dark', normalizedTheme === 'dark')
      root.style.colorScheme = normalizedTheme
      // 注意：不在此写入任何设计令牌，令牌分级由 design-tokens.css 处理。
    }

    return normalizedTheme
  }

  function setTheme(nextTheme) {
    const normalizedTheme = applyTheme(nextTheme)

    if (typeof localStorage !== 'undefined') {
      try {
        localStorage.setItem(THEME_STORAGE_KEY, normalizedTheme)
      } catch (_) {}
    }

    return normalizedTheme
  }

  function toggleTheme() {
    return setTheme(isDarkTheme.value ? 'light' : 'dark')
  }

  return {
    toasts,
    unread,
    showLoginPrompt,
    loginRedirectPath,
    theme,
    isDarkTheme,
    addToast,
    removeToast,
    closeLoginPrompt,
    applyTheme,
    setTheme,
    toggleTheme,
  }
})
