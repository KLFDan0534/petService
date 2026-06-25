import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import AdminRefunds from '../admin/AdminRefunds.vue'

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

describe('AdminRefunds.vue', () => {
  it('renders empty state', async () => {
    const pinia = createMockSetup([])
    const wrapper = mount(AdminRefunds, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.find('table').exists()).toBe(true)
  })

  it('shows approve/reject buttons for pending refunds', async () => {
    const mockRefunds = [
      { id_wsh: 1, amount_wsh: 200, reason_wsh: '行程变更', status_wsh: 'pending' },
      { id_wsh: 2, amount_wsh: 150, reason_wsh: '重复付款', status_wsh: 'approved' },
    ]
    const pinia = createMockSetup(mockRefunds)
    const wrapper = mount(AdminRefunds, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const btns = wrapper.findAll('button')
    expect(btns.filter(b => b.text() === '通过').length).toBe(1)
    expect(btns.filter(b => b.text() === '拒绝').length).toBe(1)
  })

  it('shows badges for non-pending refunds', async () => {
    const mockRefunds = [
      { id_wsh: 1, amount_wsh: 100, reason_wsh: '测试', status_wsh: 'approved' },
      { id_wsh: 2, amount_wsh: 200, reason_wsh: '测试2', status_wsh: 'rejected' },
    ]
    const pinia = createMockSetup(mockRefunds)
    const wrapper = mount(AdminRefunds, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('已通过')
    expect(wrapper.text()).toContain('已拒绝')
  })

  it('approve button calls API', async () => {
    const mockRefunds = [{ id_wsh: 1, amount_wsh: 300, reason_wsh: '测试', status_wsh: 'pending' }]
    const pinia = createMockSetup(mockRefunds)
    const authStore = useAuthStore()
    authStore.apiPost = vi.fn().mockResolvedValue({ code: 200 })
    const wrapper = mount(AdminRefunds, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const approveBtn = wrapper.findAll('button').find(b => b.text() === '通过')
    await approveBtn.trigger('click')
    expect(authStore.apiPost).toHaveBeenCalledWith('/api/refunds/1/approve', {})
  })
})
