import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import AdminKeepers from '../admin/AdminKeepers.vue'

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
  authStore.apiGet = vi.fn().mockResolvedValue({ code: 200, data })
  return pinia
}

describe('AdminKeepers.vue', () => {
  it('renders keeper list with action buttons', async () => {
    const mockKeepers = [
      { id_wsh: 1, name_wsh: 'KeeperA', status_wsh: 0, online_status_wsh: 'offline' },
      { id_wsh: 2, name_wsh: 'KeeperB', status_wsh: 1, online_status_wsh: 'online' },
    ]
    const pinia = createMockSetup(mockKeepers)
    const wrapper = mount(AdminKeepers, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const btns = wrapper.findAll('button')
    expect(btns.filter(b => b.text() === '通过').length).toBe(1)
    expect(btns.filter(b => b.text() === '拒绝').length).toBe(1)
  })

  it('shows approve/reject buttons for keepers with status 0', async () => {
    const mockKeepers = [
      { id_wsh: 1, name_wsh: '待审核寄养员', status_wsh: 0, online_status_wsh: 'offline' },
      { id_wsh: 2, name_wsh: '已通过寄养员', status_wsh: 1, online_status_wsh: 'online' },
    ]
    const pinia = createMockSetup(mockKeepers)
    const wrapper = mount(AdminKeepers, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const btns = wrapper.findAll('button')
    expect(btns.filter(b => b.text() === '通过').length).toBe(1)
    expect(btns.filter(b => b.text() === '拒绝').length).toBe(1)
  })

  it('shows badge for approved/rejected keepers', async () => {
    const mockKeepers = [
      { id_wsh: 1, name_wsh: '已通过', status_wsh: 1, online_status_wsh: 'online' },
      { id_wsh: 2, name_wsh: '已拒绝', status_wsh: 2, online_status_wsh: 'offline' },
    ]
    const pinia = createMockSetup(mockKeepers)
    const wrapper = mount(AdminKeepers, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('已通过')
    expect(wrapper.text()).toContain('已拒绝')
  })
})
