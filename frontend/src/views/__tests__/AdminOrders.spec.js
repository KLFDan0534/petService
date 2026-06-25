import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import AdminOrders from '../admin/AdminOrders.vue'

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

describe('AdminOrders.vue', () => {
  it('renders tab filters', async () => {
    const pinia = createMockSetup([])
    const wrapper = mount(AdminOrders, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('全部')
    expect(wrapper.text()).toContain('待处理')
    expect(wrapper.text()).toContain('已确认')
    expect(wrapper.text()).toContain('已完成')
  })

  it('renders order list with buttons', async () => {
    const mockOrders = [
      { id_wsh: 1, status_wsh: 'pending', name_wsh: 'pending订单' },
      { id_wsh: 2, status_wsh: 'confirmed', name_wsh: 'confirmed订单' },
      { id_wsh: 3, status_wsh: 'completed', name_wsh: 'completed订单' },
    ]
    const pinia = createMockSetup(mockOrders)
    const wrapper = mount(AdminOrders, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const btns = wrapper.findAll('button')
    expect(btns.filter(b => b.text() === '确认').length).toBe(1)
    expect(btns.filter(b => b.text() === '完成').length).toBe(1)
    expect(btns.filter(b => b.text() === '取消').length).toBe(2)
  })

  it('filters orders by tab selection', async () => {
    const mockOrders = [
      { id_wsh: 1, status_wsh: 'pending', name_wsh: '待处理' },
      { id_wsh: 2, status_wsh: 'completed', name_wsh: '已完成' },
    ]
    const pinia = createMockSetup(mockOrders)
    const wrapper = mount(AdminOrders, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const pendingBtn = wrapper.findAll('button').find(b => b.text() === '待处理')
    await pendingBtn.trigger('click')
    expect(wrapper.text()).toContain('待处理')
  })

  it('complete button calls complete API', async () => {
    const mockOrders = [{ id_wsh: 1, status_wsh: 'confirmed', name_wsh: 'test', order_no_wsh: 'ORD001' }]
    const pinia = createMockSetup(mockOrders)
    const authStore = useAuthStore()
    authStore.apiPost = vi.fn().mockResolvedValue({ code: 200 })
    const wrapper = mount(AdminOrders, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const completeBtn = wrapper.findAll('button').find(b => b.text() === '完成')
    await completeBtn.trigger('click')
    expect(authStore.apiPost).toHaveBeenCalledWith('/api/orders/complete', { orderNo: 'ORD001' })
  })
})
