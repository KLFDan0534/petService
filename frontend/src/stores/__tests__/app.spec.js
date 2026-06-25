import { setActivePinia, createPinia } from 'pinia'
import { useAppStore } from '../app'

describe('app store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('starts with empty toasts and zero unread', () => {
    const store = useAppStore()
    expect(store.toasts).toEqual([])
    expect(store.unread).toBe(0)
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
})
