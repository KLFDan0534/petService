import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../auth'

describe('auth store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('starts logged out when no token in localStorage', () => {
    const store = useAuthStore()
    expect(store.isLoggedIn).toBe(false)
    expect(store.token).toBeNull()
    expect(store.user).toBeNull()
  })

  it('setAuth sets user, token, and persists to localStorage', () => {
    const store = useAuthStore()
    store.setAuth({
      userId: 1,
      username: 'testuser',
      nickname: '测试用户',
      roles: ['OWNER'],
      accessToken: 'test-token-123',
      refreshToken: 'refresh-456',
    })
    expect(store.isLoggedIn).toBe(true)
    expect(store.token).toBe('test-token-123')
    expect(store.user.id_wsh).toBe(1)
    expect(store.user.username_wsh).toBe('testuser')
    expect(store.user.roles_wsh).toEqual(['OWNER'])
    expect(localStorage.getItem('token')).toBe('test-token-123')
    expect(JSON.parse(localStorage.getItem('user'))).toEqual({
      id_wsh: 1, username_wsh: 'testuser', nickname_wsh: '测试用户', roles_wsh: ['OWNER'],
    })
  })

  it('hasRole checks if user has a role', () => {
    const store = useAuthStore()
    store.setAuth({
      userId: 1, username: 'admin', nickname: '管理员',
      roles: ['ADMIN', 'OWNER'],
      accessToken: 't', refreshToken: 'r',
    })
    expect(store.hasRole('ADMIN')).toBe(true)
    expect(store.hasRole('OWNER')).toBe(true)
    expect(store.hasRole('KEEPER')).toBe(false)
  })

  it('isAdmin returns true only for ADMIN role', () => {
    const store = useAuthStore()
    store.setAuth({
      userId: 1, username: 'admin', nickname: '管理员',
      roles: ['ADMIN'], accessToken: 't', refreshToken: 'r',
    })
    expect(store.isAdmin).toBe(true)

    const store2 = useAuthStore()
    store2.setAuth({
      userId: 2, username: 'owner', nickname: '主人',
      roles: ['OWNER'], accessToken: 't', refreshToken: 'r',
    })
    expect(store2.isAdmin).toBe(false)
  })

  it('clearAuth resets state and clears localStorage', () => {
    const store = useAuthStore()
    store.setAuth({
      userId: 1, username: 'u', nickname: 'n',
      roles: ['OWNER'], accessToken: 't', refreshToken: 'r',
    })
    expect(store.isLoggedIn).toBe(true)
    store.clearAuth()
    expect(store.isLoggedIn).toBe(false)
    expect(store.token).toBeNull()
    expect(store.user).toBeNull()
    expect(localStorage.getItem('token')).toBeNull()
  })
})
