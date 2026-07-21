import { setActivePinia, createPinia } from 'pinia'
import { useAppStore } from '../app'

describe('app store', () => {
  beforeEach(() => {
    localStorage.clear()
    document.documentElement.removeAttribute('data-theme')
    document.documentElement.classList.remove('dark')
    document.documentElement.removeAttribute('style')
    setActivePinia(createPinia())
  })

  it('starts with empty toasts and zero unread', () => {
    const store = useAppStore()
    expect(store.toasts).toEqual([])
    expect(store.unread).toBe(0)
    expect(store.theme).toBe('light')
  })

  it('addToast adds a toast and removes it after timeout', async () => {
    vi.useFakeTimers()
    const store = useAppStore()
    store.addToast('test message', 'success')
    expect(store.toasts).toHaveLength(1)
    expect(store.toasts[0].message).toBe('test message')
    expect(store.toasts[0].type).toBe('success')
    vi.advanceTimersByTime(3000)
    expect(store.toasts).toHaveLength(0)
    vi.useRealTimers()
  })

  it('removeToast removes a specific toast by id', () => {
    const store = useAppStore()
    store.toasts.push({ id: 1, message: 'first', type: 'info' })
    store.toasts.push({ id: 2, message: 'second', type: 'error' })
    expect(store.toasts).toHaveLength(2)
    store.removeToast(1)
    expect(store.toasts).toHaveLength(1)
    expect(store.toasts[0].message).toBe('second')
  })

  it('addToast sets default type to info', () => {
    const store = useAppStore()
    store.addToast('plain')
    expect(store.toasts[0].type).toBe('info')
  })

  it('setTheme persists and applies dark theme to the document root', () => {
    const store = useAppStore()

    store.setTheme('dark')

    expect(store.theme).toBe('dark')
    expect(store.isDarkTheme).toBe(true)
    expect(localStorage.getItem('pet-service-theme')).toBe('dark')
    expect(document.documentElement.dataset.theme).toBe('dark')
    expect(document.documentElement.classList.contains('dark')).toBe(true)
    expect(document.documentElement.style.colorScheme).toBe('dark')
    expect(document.documentElement.style.getPropertyValue('--color-background')).toBe('#101114')
  })

  it('toggleTheme switches between dark and light themes', () => {
    const store = useAppStore()

    store.toggleTheme()
    expect(store.theme).toBe('dark')

    store.toggleTheme()
    expect(store.theme).toBe('light')
    expect(localStorage.getItem('pet-service-theme')).toBe('light')
    expect(document.documentElement.dataset.theme).toBe('light')
    expect(document.documentElement.classList.contains('dark')).toBe(false)
    expect(document.documentElement.style.getPropertyValue('--color-background')).toBe('#F8FAFC')
  })

  it('uses a stored theme preference when the store is created', () => {
    localStorage.setItem('pet-service-theme', 'dark')
    const store = useAppStore()

    expect(store.theme).toBe('dark')

    store.applyTheme()
    expect(document.documentElement.dataset.theme).toBe('dark')
  })

  it('normalizes invalid theme values to light', () => {
    const store = useAppStore()

    store.setTheme('unexpected')

    expect(store.theme).toBe('light')
    expect(localStorage.getItem('pet-service-theme')).toBe('light')
    expect(document.documentElement.dataset.theme).toBe('light')
  })
})
