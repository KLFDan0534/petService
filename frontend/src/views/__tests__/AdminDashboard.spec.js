import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import AdminDashboard from '../admin/AdminDashboard.vue'

vi.mock('@/stores/design', () => ({
  useDesignStore: () => ({ activeDesign: null }),
}))

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
  },
}))

import request from '@/utils/request'

function createMockSetup(data) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const authStore = useAuthStore()
  authStore.setAuth({
    userId: 1, username: 'admin', nickname: '管理员',
    roles: ['ADMIN'], accessToken: 't', refreshToken: 'r',
  })
  request.get.mockResolvedValue({ data: { code: 200, data } })
  return pinia
}

describe('AdminDashboard.vue', () => {
  it('renders stat cards with zero defaults', async () => {
    const pinia = createMockSetup({})
    const wrapper = mount(AdminDashboard, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('总用户')
    expect(wrapper.text()).toContain('商家')
    expect(wrapper.text()).toContain('寄养员')
    expect(wrapper.text()).toContain('宠物')
    expect(wrapper.text()).toContain('订单')
    expect(wrapper.text()).toContain('已完成')
    expect(wrapper.text()).toContain('总收入')
    expect(wrapper.text()).toContain('待处理')
  })

  it('displays actual stat values', async () => {
    const mockStats = { totalUsers: 100, totalMerchants: 10, totalKeepers: 25, totalPets: 80, totalOrders: 200, completedOrders: 150, totalRevenue: 50000, pendingOrders: 10 }
    const pinia = createMockSetup(mockStats)
    const wrapper = mount(AdminDashboard, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('100')
    expect(wrapper.text()).toContain('10')
    expect(wrapper.text()).toContain('25')
    expect(wrapper.text()).toContain('80')
    expect(wrapper.text()).toContain('200')
    expect(wrapper.text()).toContain('150')
    expect(wrapper.text()).toContain('50000')
    expect(wrapper.text()).toContain('10')
  })

  it('renders eight stat cards', async () => {
    const mockStats = { totalUsers: 1, totalMerchants: 1, totalKeepers: 1, totalPets: 1, totalOrders: 1, completedOrders: 1, totalRevenue: 1, pendingOrders: 1 }
    const pinia = createMockSetup(mockStats)
    const wrapper = mount(AdminDashboard, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const statCards = wrapper.findAll('.stat-card')
    expect(statCards.length).toBe(8)
  })
})
