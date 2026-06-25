import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import AdminStatistics from '../admin/AdminStatistics.vue'

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

describe('AdminStatistics.vue', () => {
  it('renders stat cards with zero defaults', async () => {
    const pinia = createMockSetup({})
    const wrapper = mount(AdminStatistics, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('总订单')
    expect(wrapper.text()).toContain('总收入')
    expect(wrapper.text()).toContain('活跃用户')
    expect(wrapper.text()).toContain('完成率')
  })

  it('displays actual stat values', async () => {
    const mockStats = { totalOrders: 150, totalRevenue: 45000, activeUsers: 80, completionRate: 95 }
    const pinia = createMockSetup(mockStats)
    const wrapper = mount(AdminStatistics, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('150')
    expect(wrapper.text()).toContain('45000')
    expect(wrapper.text()).toContain('80')
    expect(wrapper.text()).toContain('95')
  })

  it('renders four stat cards', async () => {
    const mockStats = { totalOrders: 10, totalRevenue: 5000, activeUsers: 20, completionRate: 88 }
    const pinia = createMockSetup(mockStats)
    const wrapper = mount(AdminStatistics, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const statCards = wrapper.findAll('.stat-card')
    expect(statCards.length).toBe(4)
  })
})
