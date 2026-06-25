import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import AdminUsers from '../admin/AdminUsers.vue'

vi.mock('@/stores/design', () => ({
  useDesignStore: () => ({ activeDesign: null }),
}))

function createMockSetup(data) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const authStore = useAuthStore()
  authStore.setAuth({
    userId: 1, username: 'admin', nickname: '管理员',
    roles: ['ADMIN'], accessToken: 't', refreshToken: 'r',
  })
  authStore.apiGet = vi.fn().mockResolvedValue({ code: 200, data: { list: data } })
  return pinia
}

describe('AdminUsers.vue', () => {
  it('renders search input', async () => {
    const pinia = createMockSetup([])
    const wrapper = mount(AdminUsers, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.find('input').exists()).toBe(true)
  })

  it('renders user list with fields', async () => {
    const mockUsers = [
      { id_wsh: 1, username_wsh: 'alice', nickname_wsh: 'Alice', roles_wsh: ['USER'], status_wsh: 1 },
      { id_wsh: 2, username_wsh: 'bob', nickname_wsh: 'Bob', roles_wsh: ['MERCHANT'], status_wsh: 0 },
    ]
    const pinia = createMockSetup(mockUsers)
    const wrapper = mount(AdminUsers, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('alice')
    expect(wrapper.text()).toContain('Alice')
    expect(wrapper.text()).toContain('bob')
    expect(wrapper.text()).toContain('Bob')
  })

  it('shows ban button for active users and enable for banned', async () => {
    const mockUsers = [
      { id_wsh: 1, username_wsh: 'active', nickname_wsh: 'Active User', roles_wsh: ['USER'], status_wsh: 1 },
      { id_wsh: 2, username_wsh: 'banned', nickname_wsh: 'Banned User', roles_wsh: ['USER'], status_wsh: 0 },
    ]
    const pinia = createMockSetup(mockUsers)
    const wrapper = mount(AdminUsers, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const buttons = wrapper.findAll('button')
    const btnTexts = buttons.map(b => b.text())
    expect(btnTexts.filter(t => t === '封禁').length).toBe(1)
    expect(btnTexts.filter(t => t === '启用').length).toBe(1)
  })

  it('calls toggle API on button click', async () => {
    const mockUsers = [{ id_wsh: 1, username_wsh: 'test', nickname_wsh: 'Test', roles_wsh: ['USER'], status_wsh: 1 }]
    const pinia = createMockSetup(mockUsers)
    const authStore = useAuthStore()
    authStore.apiPut = vi.fn().mockResolvedValue({ code: 200 })
    const wrapper = mount(AdminUsers, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const btn = wrapper.findAll('button').find(b => b.text() === '封禁')
    await btn.trigger('click')
    expect(authStore.apiPut).toHaveBeenCalledWith('/api/users/1/status', { status: 0 })
  })

  it('handles empty user list', async () => {
    const pinia = createMockSetup([])
    const wrapper = mount(AdminUsers, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.find('table').exists()).toBe(true)
  })

  it('searches users on input', async () => {
    const pinia = createMockSetup([])
    const authStore = useAuthStore()
    const wrapper = mount(AdminUsers, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const input = wrapper.find('input')
    await input.setValue('alice')
    await input.trigger('input')
    await new Promise(r => setTimeout(r, 100))
    expect(authStore.apiGet).toHaveBeenCalled()
  })
})
