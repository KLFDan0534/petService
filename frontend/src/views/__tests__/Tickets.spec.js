import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import Tickets from '../user/Tickets.vue'

vi.mock('@/stores/design', () => ({
  useDesignStore: () => ({ activeDesign: null }),
}))

function createMockSetup(tickets, messages) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const authStore = useAuthStore()
  authStore.setAuth({
    userId: 1, username: 'owner', nickname: '宠物主',
    roles: ['OWNER'], accessToken: 't', refreshToken: 'r',
  })
  authStore.apiGet = vi.fn((url) => {
    if (url.includes('/messages')) return Promise.resolve({ code: 200, data: messages || [] })
    return Promise.resolve({ code: 200, data: tickets })
  })
  authStore.apiPost = vi.fn().mockResolvedValue({ code: 200, data: {} })
  return pinia
}

describe('Tickets.vue', () => {
  it('renders empty state when no tickets', async () => {
    const pinia = createMockSetup([])
    const wrapper = mount(Tickets, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('暂无工单')
  })

  it('renders ticket list with category/priority/status badges', async () => {
    const tickets = [
      { id_wsh: 1, title_wsh: '费用申诉', content_wsh: '费用计算错误', category_wsh: 'appeal', priority_wsh: 'high', status_wsh: 'pending', created_at_wsh: '2026-01-01T00:00:00' },
      { id_wsh: 2, title_wsh: '服务投诉', content_wsh: '服务质量问题', category_wsh: 'complaint', priority_wsh: 'medium', status_wsh: 'resolved', result_wsh: '已退款', created_at_wsh: '2026-01-02T00:00:00' },
      { id_wsh: 3, title_wsh: '其他问题', content_wsh: '咨询', category_wsh: 'other', priority_wsh: 'low', status_wsh: 'closed', created_at_wsh: '2026-01-03T00:00:00' },
    ]
    const pinia = createMockSetup(tickets)
    const wrapper = mount(Tickets, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('费用申诉')
    expect(wrapper.text()).toContain('服务投诉')
    expect(wrapper.text()).toContain('其他问题')
    expect(wrapper.text()).toContain('申诉')
    expect(wrapper.text()).toContain('投诉')
    expect(wrapper.text()).toContain('紧急')
    expect(wrapper.text()).toContain('待处理')
    expect(wrapper.text()).toContain('已解决')
    expect(wrapper.text()).toContain('已关闭')
  })

  it('opens create form modal with category and priority fields', async () => {
    const pinia = createMockSetup([])
    const wrapper = mount(Tickets, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const btn = wrapper.findAll('button').find(b => b.text().includes('新建申诉'))
    await btn.trigger('click')
    expect(wrapper.text()).toContain('新建申诉')
    expect(wrapper.find('form').exists()).toBe(true)
    expect(wrapper.find('select').exists()).toBe(true)
  })

  it('creates ticket with appeal category', async () => {
    const pinia = createMockSetup([])
    const authStore = useAuthStore()
    const newTicket = { id_wsh: 3, title_wsh: '订单申诉', content_wsh: '费用问题', category_wsh: 'appeal', priority_wsh: 'high', status_wsh: 'pending' }
    authStore.apiPost = vi.fn().mockResolvedValue({ code: 200, data: newTicket })
    const wrapper = mount(Tickets, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const btn = wrapper.findAll('button').find(b => b.text().includes('新建申诉'))
    await btn.trigger('click')
    const form = wrapper.find('form')
    await form.trigger('submit')
    expect(authStore.apiPost).toHaveBeenCalledWith('/api/tickets', { title_wsh: '', content_wsh: '', category_wsh: 'appeal', priority_wsh: 'medium' })
  })

  it('opens ticket detail modal when clicked', async () => {
    const ticket = { id_wsh: 1, title_wsh: '费用申诉', content_wsh: '费用计算错误', category_wsh: 'appeal', priority_wsh: 'high', status_wsh: 'pending', created_at_wsh: '2026-01-01T00:00:00' }
  })

  it('shows evidence upload button in detail', async () => {
    const ticket = { id_wsh: 1, title_wsh: '费用申诉', content_wsh: '费用错误', category_wsh: 'appeal', priority_wsh: 'high', status_wsh: 'pending', created_at_wsh: '2026-01-01T00:00:00' }
    const pinia = createMockSetup([ticket], [])
    const wrapper = mount(Tickets, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const card = wrapper.find('.card')
    await card.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('上传证据')
  })

  it('submits evidence successfully', async () => {
    const ticket = { id_wsh: 1, title_wsh: '费用申诉', content_wsh: '费用错误', category_wsh: 'appeal', priority_wsh: 'high', status_wsh: 'pending', created_at_wsh: '2026-01-01T00:00:00' }
    const pinia = createMockSetup([ticket], [])
    const authStore = useAuthStore()
    authStore.apiPost = vi.fn().mockResolvedValue({ code: 200, data: { id_wsh: 10, ticket_id_wsh: 1, user_id_wsh: 1, content_wsh: '这是支付截图证据', created_at_wsh: '2026-01-02T00:00:00' } })
    const wrapper = mount(Tickets, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const card = wrapper.find('.card')
    await card.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    const uploadBtn = wrapper.findAll('button').find(b => b.text().includes('上传证据'))
    await uploadBtn.trigger('click')
    const textarea = wrapper.find('textarea')
    await textarea.setValue('这是支付截图证据')
    const submitBtn = wrapper.findAll('button').find(b => b.text() === '提交')
    await submitBtn.trigger('click')
    expect(authStore.apiPost).toHaveBeenCalledWith('/api/tickets/1/messages', { content: '这是支付截图证据' })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('这是支付截图证据')
  })

  it('shows processing result for resolved tickets', async () => {
    const tickets = [
      { id_wsh: 1, title_wsh: '费用申诉', content_wsh: '费用错误', category_wsh: 'appeal', priority_wsh: 'high', status_wsh: 'resolved', result_wsh: '经核查，已退还50元', created_at_wsh: '2026-01-01T00:00:00' },
    ]
    const pinia = createMockSetup(tickets)
    const wrapper = mount(Tickets, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const card = wrapper.find('.card')
    await card.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('处理结果')
    expect(wrapper.text()).toContain('经核查，已退还50元')
  })

  it('displays messages from admin and user', async () => {
    const ticket = { id_wsh: 1, title_wsh: '费用申诉', content_wsh: '费用错误', category_wsh: 'appeal', priority_wsh: 'high', status_wsh: 'pending', created_at_wsh: '2026-01-01T00:00:00' }
    const messages = [
      { id_wsh: 1, ticket_id_wsh: 1, user_id_wsh: 1, content_wsh: '这是我的证据截图', created_at_wsh: '2026-01-02T00:00:00' },
      { id_wsh: 2, ticket_id_wsh: 1, user_id_wsh: 2, content_wsh: '已收到，正在核查', created_at_wsh: '2026-01-02T01:00:00' },
    ]
    const pinia = createPinia()
    setActivePinia(pinia)
    const authStore = useAuthStore()
    authStore.setAuth({
      userId: 1, username: 'owner', nickname: '宠物主',
      roles: ['OWNER'], accessToken: 't', refreshToken: 'r',
    })
    authStore.apiGet = vi.fn()
      .mockResolvedValueOnce({ code: 200, data: [ticket] })
      .mockResolvedValueOnce({ code: 200, data: messages })
    authStore.apiPost = vi.fn().mockResolvedValue({ code: 200, data: {} })
    const wrapper = mount(Tickets, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const card = wrapper.find('.card')
    await card.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('这是我的证据截图')
    expect(wrapper.text()).toContain('已收到，正在核查')
    expect(wrapper.text()).toContain('我')
    expect(wrapper.text()).toContain('客服')
  })
})
