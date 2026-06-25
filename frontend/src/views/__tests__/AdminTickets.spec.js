import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import AdminTickets from '../admin/AdminTickets.vue'

vi.mock('@/stores/design', () => ({
  useDesignStore: () => ({ activeDesign: null }),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
}))

function createMockSetup(tickets) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const authStore = useAuthStore()
  authStore.setAuth({
    userId: 1, username: 'admin', nickname: '管理员',
    roles: ['ADMIN'], accessToken: 't', refreshToken: 'r',
  })
  authStore.apiGet = vi.fn().mockResolvedValue({ code: 200, data: { list: tickets, total: tickets.length } })
  authStore.apiPost = vi.fn().mockResolvedValue({ code: 200, data: {} })
  return pinia
}

describe('AdminTickets.vue', () => {
  it('renders ticket table with status badges', async () => {
    const tickets = [
      { id_wsh: 1, title_wsh: '订单问题', status_wsh: 'pending', user_wsh: { nickname_wsh: '用户1' } },
      { id_wsh: 2, title_wsh: '服务投诉', status_wsh: 'processing', user_wsh: { nickname_wsh: '用户2' } },
      { id_wsh: 3, title_wsh: '费用咨询', status_wsh: 'resolved', user_wsh: { nickname_wsh: '用户3' } },
    ]
    const pinia = createMockSetup(tickets)
    const wrapper = mount(AdminTickets, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('订单问题')
    expect(wrapper.text()).toContain('服务投诉')
    expect(wrapper.text()).toContain('费用咨询')
  })

  it('shows resolve button for processing tickets', async () => {
    const tickets = [
      { id_wsh: 1, title_wsh: '待处理工单', status_wsh: 'processing', user_wsh: { nickname_wsh: '用户' } },
    ]
    const pinia = createMockSetup(tickets)
    const wrapper = mount(AdminTickets, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('解决')
  })

  it('shows close button for resolved tickets', async () => {
    const tickets = [
      { id_wsh: 1, title_wsh: '已解决工单', status_wsh: 'resolved', user_wsh: { nickname_wsh: '用户' } },
    ]
    const pinia = createMockSetup(tickets)
    const wrapper = mount(AdminTickets, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('关闭')
  })

  it('resolves ticket on button click', async () => {
    const tickets = [
      { id_wsh: 1, title_wsh: '工单1', status_wsh: 'processing', user_wsh: { nickname_wsh: '用户' } },
    ]
    const pinia = createMockSetup(tickets)
    const authStore = useAuthStore()
    authStore.apiPost = vi.fn().mockResolvedValue({ code: 200, data: {} })
    const wrapper = mount(AdminTickets, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const resolveBtn = wrapper.findAll('button').find(b => b.text().includes('解决'))
    await resolveBtn.trigger('click')
    expect(authStore.apiPost).toHaveBeenCalledWith('/api/tickets/1/resolve', {})
  })

  it('closes ticket on button click', async () => {
    const tickets = [
      { id_wsh: 1, title_wsh: '工单1', status_wsh: 'resolved', user_wsh: { nickname_wsh: '用户' } },
    ]
    const pinia = createMockSetup(tickets)
    const authStore = useAuthStore()
    authStore.apiPost = vi.fn().mockResolvedValue({ code: 200, data: {} })
    const wrapper = mount(AdminTickets, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const closeBtn = wrapper.findAll('button').find(b => b.text().includes('关闭'))
    await closeBtn.trigger('click')
    expect(authStore.apiPost).toHaveBeenCalledWith('/api/tickets/1/close', {})
  })
})
