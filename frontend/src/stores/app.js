import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

const THEME_STORAGE_KEY = 'pet-service-theme'
const THEME_MODES = ['light', 'dark']

const THEME_VARIABLES = {
  light: {
    '--color-primary': '#F97316',
    '--color-on-primary': '#0F172A',
    '--color-secondary': '#FB923C',
    '--color-on-secondary': '#0F172A',
    '--color-accent': '#2563EB',
    '--color-on-accent': '#FFFFFF',
    '--color-background': '#F8FAFC',
    '--color-foreground': '#0F172A',
    '--color-card': '#FFFFFF',
    '--color-card-foreground': '#111827',
    '--color-muted': '#F1F5F9',
    '--color-muted-foreground': '#64748B',
    '--color-border': '#E2E8F0',
    '--color-destructive': '#DC2626',
    '--color-on-destructive': '#FFFFFF',
    '--color-danger': '#DC2626',
    '--color-error': '#DC2626',
    '--color-ring': '#F97316',
    '--color-success': '#10B981',
    '--color-warning': '#F59E0B',
    '--color-info': '#3B82F6',
    '--shadow-sm': '0 1px 2px rgba(0,0,0,0.05)',
    '--shadow-md': '0 8px 20px rgba(15,23,42,0.08)',
    '--shadow-lg': '0 14px 32px rgba(15,23,42,0.1)',
    '--shadow-xl': '0 24px 48px rgba(15,23,42,0.16)',
    '--shadow-inner': 'inset -2px -2px 8px rgba(0,0,0,0.04), 4px 4px 8px rgba(0,0,0,0.08)',
    '--el-color-primary': '#F97316',
    '--el-bg-color': '#FFFFFF',
    '--el-bg-color-page': '#F8FAFC',
    '--el-text-color-primary': '#0F172A',
    '--el-text-color-regular': '#111827',
    '--el-text-color-secondary': '#64748B',
    '--el-border-color': '#E2E8F0',
    '--el-fill-color-light': '#F1F5F9',
    '--el-mask-color': 'rgba(255, 255, 255, 0.9)',
  },
  dark: {
    '--color-primary': '#F59E0B',
    '--color-on-primary': '#111827',
    '--color-secondary': '#FBBF24',
    '--color-on-secondary': '#111827',
    '--color-accent': '#38BDF8',
    '--color-on-accent': '#07111F',
    '--color-background': '#101114',
    '--color-foreground': '#F4F5F7',
    '--color-card': '#181A1F',
    '--color-card-foreground': '#F4F5F7',
    '--color-muted': '#23262D',
    '--color-muted-foreground': '#A8B0BD',
    '--color-border': '#343946',
    '--color-destructive': '#F87171',
    '--color-on-destructive': '#1F1111',
    '--color-danger': '#F87171',
    '--color-error': '#F87171',
    '--color-ring': '#F59E0B',
    '--color-success': '#34D399',
    '--color-warning': '#FBBF24',
    '--color-info': '#60A5FA',
    '--shadow-sm': '0 1px 2px rgba(0,0,0,0.3)',
    '--shadow-md': '0 8px 20px rgba(0,0,0,0.35)',
    '--shadow-lg': '0 14px 32px rgba(0,0,0,0.42)',
    '--shadow-xl': '0 24px 48px rgba(0,0,0,0.5)',
    '--shadow-inner': 'inset -2px -2px 8px rgba(255,255,255,0.03), 4px 4px 10px rgba(0,0,0,0.35)',
    '--el-color-primary': '#F59E0B',
    '--el-bg-color': '#181A1F',
    '--el-bg-color-page': '#101114',
    '--el-text-color-primary': '#F4F5F7',
    '--el-text-color-regular': '#D9DEE7',
    '--el-text-color-secondary': '#A8B0BD',
    '--el-border-color': '#343946',
    '--el-fill-color-light': '#23262D',
    '--el-mask-color': 'rgba(0, 0, 0, 0.72)',
  },
}

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

      Object.entries(THEME_VARIABLES[normalizedTheme]).forEach(([name, value]) => {
        root.style.setProperty(name, value)
      })
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
