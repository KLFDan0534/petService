import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import Payments from '../user/Payments.vue'

vi.mock('@/stores/design', () => ({
  useDesignStore: () => ({ activeDesign: null }),
}))

function createMockSetup(data) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const authStore = useAuthStore()
  authStore.setAuth({
    userId: 1, username: 'owner', nickname: '主人',
    roles: ['OWNER'], accessToken: 't', refreshToken: 'r',
  })
  authStore.apiGet = vi.fn().mockResolvedValue({ code: 200, data })
  return pinia
}

describe('Payments.vue', () => {
  it('renders loading state initially', () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const wrapper = mount(Payments, { global: { plugins: [pinia] } })
    expect(wrapper.text()).toContain('加载中')
  })

  it('renders empty state when no payments', async () => {
    const pinia = createMockSetup([])
    const wrapper = mount(Payments, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('暂无支付记录')
  })

  it('renders payment list with method labels', async () => {
    const pinia = createMockSetup([
      { id_wsh: 1, amount_wsh: 450, method_wsh: 'wechat', status_wsh: 'paid', created_at_wsh: '2026-06-01T00:00:00Z' },
      { id_wsh: 2, amount_wsh: 300, method_wsh: 'alipay', status_wsh: 'pending', created_at_wsh: '2026-06-10T00:00:00Z' },
    ])
    const wrapper = mount(Payments, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('¥450')
    expect(wrapper.text()).toContain('微信支付')
    expect(wrapper.text()).toContain('¥300')
    expect(wrapper.text()).toContain('支付宝')
    expect(wrapper.text()).toContain('已支付')
    expect(wrapper.text()).toContain('待支付')
  })
})
