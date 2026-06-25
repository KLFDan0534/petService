import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import Agent from '../user/Agent.vue'

vi.mock('@/stores/design', () => ({
  useDesignStore: () => ({ activeDesign: null }),
}))

function createMockSetup() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const authStore = useAuthStore()
  authStore.setAuth({
    userId: 1, username: 'owner', nickname: '宠物主',
    roles: ['OWNER'], accessToken: 't', refreshToken: 'r',
  })
  authStore.apiPost = vi.fn().mockResolvedValue({ code: 200, data: { result: '任务已执行成功，订单已为您创建。', steps: ['识别需求', '搜索商家', '创建订单'] } })
  authStore.apiGet = vi.fn().mockResolvedValue({ code: 200, data: [] })
  return pinia
}

describe('Agent.vue', () => {
  it('renders agent chat interface', async () => {
    const pinia = createMockSetup()
    const wrapper = mount(Agent, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('AI Agent')
    expect(wrapper.text()).toContain('智能任务执行代理')
    expect(wrapper.find('input').exists()).toBe(true)
    expect(wrapper.text()).toContain('执行')
  })

  it('executes task and displays steps', async () => {
    const pinia = createMockSetup()
    const authStore = useAuthStore()
    authStore.apiPost = vi.fn().mockResolvedValue({
      code: 200, data: {
        result: '任务已成功执行，订单 ORD001 已创建。',
        steps: ['识别需求', '搜索商家', '推荐寄养员', '创建订单', '支付完成'],
      },
    })
    const wrapper = mount(Agent, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const input = wrapper.find('input')
    await input.setValue('帮我寄养金毛3天')
    const btn = wrapper.findAll('button').find(b => b.text().includes('执行'))
    await btn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    expect(authStore.apiPost).toHaveBeenCalledWith('/api/agent/execute', {
      task: '帮我寄养金毛3天',
    })
    expect(wrapper.text()).toContain('帮我寄养金毛3天')
    expect(wrapper.text()).toContain('识别需求')
    expect(wrapper.text()).toContain('创建订单')
  })

  it('shows loading state while executing', async () => {
    const pinia = createMockSetup()
    const authStore = useAuthStore()
    authStore.apiPost = vi.fn().mockImplementation(() => new Promise(r => setTimeout(() => r({ code: 200, data: { result: '成功' } }), 1000)))
    const wrapper = mount(Agent, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const input = wrapper.find('input')
    await input.setValue('测试任务')
    const btn = wrapper.findAll('button').find(b => b.text().includes('执行'))
    await btn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('Agent 执行中')
  })
})
