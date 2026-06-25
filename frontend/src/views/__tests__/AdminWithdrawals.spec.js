import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import AdminWithdrawals from '../admin/AdminWithdrawals.vue'

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

describe('AdminWithdrawals.vue', () => {
  it('renders empty state', async () => {
    const pinia = createMockSetup([])
    const wrapper = mount(AdminWithdrawals, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.find('table').exists()).toBe(true)
  })

  it('shows approve/reject for pending withdrawals', async () => {
    const mockWithdrawals = [
      { id_wsh: 1, amount_wsh: 500, status_wsh: 'pending', bank_name_wsh: '工商银行' },
      { id_wsh: 2, amount_wsh: 300, status_wsh: 'approved', bank_name_wsh: '建设银行' },
    ]
    const pinia = createMockSetup(mockWithdrawals)
    const wrapper = mount(AdminWithdrawals, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const btns = wrapper.findAll('button')
    expect(btns.filter(b => b.text() === '通过').length).toBe(1)
    expect(btns.filter(b => b.text() === '拒绝').length).toBe(1)
  })

  it('shows badges for processed withdrawals', async () => {
    const mockWithdrawals = [
      { id_wsh: 1, amount_wsh: 100, status_wsh: 'approved', bank_name_wsh: '中国银行' },
      { id_wsh: 2, amount_wsh: 200, status_wsh: 'rejected', bank_name_wsh: '农业银行' },
    ]
    const pinia = createMockSetup(mockWithdrawals)
    const wrapper = mount(AdminWithdrawals, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('已通过')
    expect(wrapper.text()).toContain('已拒绝')
  })

  it('approve button calls API', async () => {
    const mockWithdrawals = [{ id_wsh: 1, amount_wsh: 500, status_wsh: 'pending', bank_name_wsh: '工商银行' }]
    const pinia = createMockSetup(mockWithdrawals)
    const authStore = useAuthStore()
    authStore.apiPost = vi.fn().mockResolvedValue({ code: 200 })
    const wrapper = mount(AdminWithdrawals, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const approveBtn = wrapper.findAll('button').find(b => b.text() === '通过')
    await approveBtn.trigger('click')
    expect(authStore.apiPost).toHaveBeenCalledWith('/api/withdrawals/1/approve', {})
  })
})
