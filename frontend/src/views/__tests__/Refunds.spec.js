import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import Refunds from '../user/Refunds.vue'

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

describe('Refunds.vue', () => {
  it('renders empty state when no refunds', async () => {
    const pinia = createMockSetup([])
    const wrapper = mount(Refunds, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('暂无退款申请')
  })

  it('renders refund list with status badges', async () => {
    const pinia = createMockSetup([
      { id_wsh: 1, order_id_wsh: 100, amount_wsh: 450, reason_wsh: '行程变更', status_wsh: 'pending', created_at_wsh: '2026-06-01T00:00:00Z' },
      { id_wsh: 2, order_id_wsh: 101, amount_wsh: 300, reason_wsh: '服务不满意', status_wsh: 'approved', created_at_wsh: '2026-06-10T00:00:00Z' },
    ])
    const wrapper = mount(Refunds, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('¥450')
    expect(wrapper.text()).toContain('行程变更')
    expect(wrapper.text()).toContain('¥300')
    expect(wrapper.text()).toContain('服务不满意')
    expect(wrapper.text()).toContain('审核中')
    expect(wrapper.text()).toContain('已通过')
  })

  it('opens refund form modal on button click', async () => {
    const pinia = createMockSetup([])
    const wrapper = mount(Refunds, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const applyBtn = wrapper.findAll('button').find(b => b.text().includes('申请退款'))
    await applyBtn.trigger('click')
    expect(wrapper.text()).toContain('申请退款')
    expect(wrapper.find('form').exists()).toBe(true)
  })

  it('submits refund form and refreshes list', async () => {
    const pinia = createMockSetup([])
    const authStore = useAuthStore()
    const newRefund = { id_wsh: 3, order_id_wsh: 102, amount_wsh: 200, reason_wsh: '重复支付', status_wsh: 'pending' }
    authStore.apiPost = vi.fn().mockResolvedValue({ code: 200, data: newRefund })
    const wrapper = mount(Refunds, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const applyBtn = wrapper.findAll('button').find(b => b.text().includes('申请退款'))
    await applyBtn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    const form = wrapper.find('form')
    await form.trigger('submit')
    expect(authStore.apiPost).toHaveBeenCalled()
    expect(authStore.apiPost).toHaveBeenCalledWith('/api/refunds', {
      orderId: '', reason: '',
    })
  })
})
