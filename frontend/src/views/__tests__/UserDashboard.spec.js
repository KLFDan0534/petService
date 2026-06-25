import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import Dashboard from '../user/Dashboard.vue'

vi.mock('@/stores/design', () => ({
  useDesignStore: () => ({ activeDesign: null }),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
}))

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
  },
}))

import request from '@/utils/request'

function createMockSetup(statsData, servicesData, bannerData) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const authStore = useAuthStore()
  authStore.setAuth({
    userId: 1, username: 'owner', nickname: '宠物主',
    roles: ['OWNER'], accessToken: 't', refreshToken: 'r',
  })
  request.get
    .mockResolvedValueOnce({ data: { code: 200, data: statsData } })
    .mockResolvedValueOnce({ data: { code: 200, data: servicesData } })
    .mockResolvedValueOnce({ data: { code: 200, data: bannerData || [] } })
  return pinia
}

describe('UserDashboard.vue', () => {
  it('renders welcome message when logged in', async () => {
    const pinia = createMockSetup({ pets_wsh: 0, active_orders_wsh: 0, completed_orders_wsh: 0, total_spent_wsh: 0 }, [])
    const wrapper = mount(Dashboard, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('欢迎回来')
  })

  it('renders empty state when no services', async () => {
    const pinia = createMockSetup({}, [])
    const wrapper = mount(Dashboard, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('全部服务')
    expect(wrapper.text()).toContain('暂无服务')
  })

  it('renders service cards', async () => {
    const mockServices = [
      { id_wsh: 1, name_wsh: '标准寄养', description_wsh: '每日定时喂食', price_wsh: 150, unit_wsh: '天', type_wsh: 'boarding' },
      { id_wsh: 2, name_wsh: '豪华寄养', description_wsh: '独立单间', price_wsh: 280, unit_wsh: '天', type_wsh: 'boarding' },
    ]
    const pinia = createMockSetup({ pets_wsh: 1, active_orders_wsh: 0, completed_orders_wsh: 0, total_spent_wsh: 0 }, mockServices)
    const wrapper = mount(Dashboard, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('标准寄养')
    expect(wrapper.text()).toContain('豪华寄养')
    expect(wrapper.text()).toContain('¥150')
    expect(wrapper.text()).toContain('¥280')
  })

  it('renders featured/pricing section for first 3 services', async () => {
    const mockServices = [
      { id_wsh: 1, name_wsh: '基础寄养', price_wsh: 100, unit_wsh: '天', type_wsh: 'boarding' },
      { id_wsh: 2, name_wsh: '标准寄养', price_wsh: 200, unit_wsh: '天', type_wsh: 'boarding' },
      { id_wsh: 3, name_wsh: '豪华寄养', price_wsh: 300, unit_wsh: '天', type_wsh: 'boarding' },
      { id_wsh: 4, name_wsh: '尊享寄养', price_wsh: 500, unit_wsh: '天', type_wsh: 'boarding' },
    ]
    const pinia = createMockSetup({}, mockServices)
    const wrapper = mount(Dashboard, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const pricingCards = wrapper.findAll('.pricing-card')
    expect(pricingCards.length).toBe(3)
    expect(wrapper.text()).toContain('基础寄养')
    expect(wrapper.text()).toContain('标准寄养')
    expect(wrapper.text()).toContain('豪华寄养')
    expect(wrapper.text()).toContain('尊享寄养')
    expect(wrapper.text()).toContain('全部服务')
  })
})
