import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import AI from '../user/AI.vue'

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
  authStore.apiGet = vi.fn().mockResolvedValue({ code: 200, data: [] })
  authStore.apiPost = vi.fn().mockResolvedValue({ code: 200, data: { reply_wsh: '建议定期给宠物进行健康检查，保持良好的饮食和运动习惯。' } })
  return pinia
}

describe('AI.vue', () => {
  it('renders AI chat interface', async () => {
    const pinia = createMockSetup()
    const wrapper = mount(AI, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('AI 助手')
    expect(wrapper.text()).toContain('您好！我是宠物护理 AI 助手')
  })

  it('sends message and receives AI reply', async () => {
    const pinia = createMockSetup()
    const authStore = useAuthStore()
    const wrapper = mount(AI, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const input = wrapper.find('input[placeholder="输入您的问题..."]')
    await input.setValue('如何照顾老年宠物？')
    const sendBtn = wrapper.findAll('button').find(b => b.text().includes('发送'))
    await sendBtn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    expect(authStore.apiPost).toHaveBeenCalledWith('/api/rag/ask', { question: '如何照顾老年宠物？' })
    expect(wrapper.text()).toContain('如何照顾老年宠物？')
  })

  it('shows thinking state while loading', async () => {
    const pinia = createMockSetup()
    const authStore = useAuthStore()
    authStore.apiPost = vi.fn().mockImplementation(() => new Promise(r => setTimeout(() => r({ code: 200, data: { reply_wsh: '回复' } }), 1000)))
    const wrapper = mount(AI, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const input = wrapper.find('input[placeholder="输入您的问题..."]')
    await input.setValue('测试问题')
    const sendBtn = wrapper.findAll('button').find(b => b.text().includes('发送'))
    await sendBtn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('AI 思考中')
  })
})
