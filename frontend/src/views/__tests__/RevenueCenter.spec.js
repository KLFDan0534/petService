import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import RevenueCenter from '../user/RevenueCenter.vue'

vi.mock('@/stores/design', () => ({
  useDesignStore: () => ({ activeDesign: null }),
}))

function createMockSetup(wallet, tips, stats, transactions, withdrawals) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const authStore = useAuthStore()
  authStore.setAuth({
    userId: 1, username: 'owner', nickname: '测试用户',
    roles: ['OWNER'], accessToken: 't', refreshToken: 'r',
  })
  authStore.apiGet = vi.fn()
    .mockResolvedValueOnce({ code: 200, data: wallet })
    .mockResolvedValueOnce({ code: 200, data: tips })
    .mockResolvedValueOnce({ code: 200, data: transactions || [] })
    .mockResolvedValueOnce({ code: 200, data: withdrawals || [] })
    .mockResolvedValueOnce({ code: 200, data: stats })
  return pinia
}

describe('RevenueCenter.vue', () => {
  it('renders revenue stat cards', async () => {
    const pinia = createMockSetup({}, [], {})
    const wrapper = mount(RevenueCenter, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('总收入')
    expect(wrapper.text()).toContain('本月收入')
    expect(wrapper.text()).toContain('打赏次数')
    expect(wrapper.text()).toContain('打赏总金额')
  })

  it('displays actual revenue values', async () => {
    const stats = { totalRevenue: 15000, monthRevenue: 2500, totalTips: 5, tipsAmount: 200 }
    const pinia = createMockSetup({ balance_wsh: 3000 }, [], stats)
    const wrapper = mount(RevenueCenter, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('15000')
    expect(wrapper.text()).toContain('2500')
    expect(wrapper.text()).toContain('5')
    expect(wrapper.text()).toContain('200')
  })

  it('renders wallet balance', async () => {
    const pinia = createMockSetup({ balance_wsh: 5000 }, [], {})
    const wrapper = mount(RevenueCenter, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('5000')
    expect(wrapper.text()).toContain('可用余额')
  })

  it('renders empty state when no tips', async () => {
    const pinia = createMockSetup({}, [], {})
    const wrapper = mount(RevenueCenter, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('暂无打赏记录')
  })

  it('renders tip records', async () => {
    const tips = [
      { id_wsh: 1, amount_wsh: 50, message_wsh: '谢谢照顾', created_at_wsh: '2026-01-01T00:00:00' },
      { id_wsh: 2, amount_wsh: 30, message_wsh: '额外感谢', created_at_wsh: '2026-01-02T00:00:00' },
    ]
    const pinia = createMockSetup({}, tips, {})
    const wrapper = mount(RevenueCenter, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('50')
    expect(wrapper.text()).toContain('30')
    expect(wrapper.text()).toContain('谢谢照顾')
    expect(wrapper.text()).toContain('额外感谢')
  })

  it('renders revenue statistics', async () => {
    const stats = { totalOrders: 50, completedOrders: 40, pendingOrders: 10 }
    const pinia = createMockSetup({}, [], stats)
    const wrapper = mount(RevenueCenter, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('50')
    expect(wrapper.text()).toContain('40')
    expect(wrapper.text()).toContain('10')
    expect(wrapper.text()).toContain('总订单')
    expect(wrapper.text()).toContain('已完成')
    expect(wrapper.text()).toContain('待处理')
  })
})
