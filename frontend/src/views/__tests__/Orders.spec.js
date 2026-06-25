import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import Orders from '../user/Orders.vue'

vi.mock('@/stores/design', () => ({
  useDesignStore: () => ({ activeDesign: null }),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
  useRoute: () => ({ query: {} }),
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

describe('Orders.vue', () => {
  it('renders loading state initially', () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const wrapper = mount(Orders, { global: { plugins: [pinia] } })
    expect(wrapper.text()).toContain('加载中')
  })

  it('renders empty state when no orders', async () => {
    const pinia = createMockSetup([])
    const wrapper = mount(Orders, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('暂无订单')
  })

  it('renders order list with status badges', async () => {
    const mockOrders = [
      { id_wsh: 1, service_name_wsh: '金毛寄养', status_wsh: 'pending', total_amount_wsh: 450, created_at_wsh: '2026-06-01T00:00:00Z' },
      { id_wsh: 2, service_name_wsh: '猫咪照看', status_wsh: 'completed', total_amount_wsh: 300, created_at_wsh: '2026-06-10T00:00:00Z' },
    ]
    const pinia = createMockSetup(mockOrders)
    const wrapper = mount(Orders, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('金毛寄养')
    expect(wrapper.text()).toContain('猫咪照看')
    expect(wrapper.text()).toContain('¥450')
    expect(wrapper.text()).toContain('¥300')
  })

  it('shows pay/cancel buttons only for pending orders', async () => {
    const mockOrders = [
      { id_wsh: 1, service_name_wsh: '金毛寄养', status_wsh: 'pending', total_amount_wsh: 450, created_at_wsh: '2026-06-01T00:00:00Z' },
      { id_wsh: 2, service_name_wsh: '猫咪照看', status_wsh: 'completed', total_amount_wsh: 300, created_at_wsh: '2026-06-10T00:00:00Z' },
    ]
    const pinia = createMockSetup(mockOrders)
    const wrapper = mount(Orders, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const buttons = wrapper.findAll('button')
    expect(buttons.filter(b => b.text() === '付款').length).toBe(1)
    expect(buttons.filter(b => b.text() === '取消').length).toBe(1)
  })

  it('filters orders by tab', async () => {
    const mockOrders = [
      { id_wsh: 1, service_name_wsh: '金毛寄养', status_wsh: 'pending', total_amount_wsh: 450, created_at_wsh: '2026-06-01T00:00:00Z' },
      { id_wsh: 2, service_name_wsh: '猫咪照看', status_wsh: 'completed', total_amount_wsh: 300, created_at_wsh: '2026-06-10T00:00:00Z' },
    ]
    const pinia = createMockSetup(mockOrders)
    const wrapper = mount(Orders, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const completedBtn = wrapper.findAll('button').find(b => b.text() === '已完成')
    await completedBtn.trigger('click')
    expect(wrapper.text()).toContain('猫咪照看')
    expect(wrapper.text()).not.toContain('金毛寄养')
  })

  it('cancel button calls cancel API', async () => {
    const mockOrders = [
      { id_wsh: 1, service_name_wsh: '金毛寄养', status_wsh: 'pending', total_amount_wsh: 450, created_at_wsh: '2026-06-01T00:00:00Z' },
      { id_wsh: 2, service_name_wsh: '已完成单', status_wsh: 'completed', total_amount_wsh: 300, created_at_wsh: '2026-06-10T00:00:00Z' },
    ]
    const pinia = createMockSetup(mockOrders)
    const authStore = useAuthStore()
    authStore.apiPut = vi.fn().mockResolvedValue({ code: 200 })
    const wrapper = mount(Orders, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const cancelBtn = wrapper.findAll('button').find(b => b.text() === '取消')
    await cancelBtn.trigger('click')
    expect(authStore.apiPut).toHaveBeenCalledWith('/api/orders/1/status', { status: 'cancelled' })
  })
})
