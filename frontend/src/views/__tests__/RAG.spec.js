import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import RAG from '../user/RAG.vue'

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
  return pinia
}

describe('RAG.vue', () => {
  it('renders knowledge base search', async () => {
    const pinia = createMockSetup()
    const wrapper = mount(RAG, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('RAG 知识库')
    expect(wrapper.text()).toContain('宠物护理智能检索')
    expect(wrapper.find('input').exists()).toBe(true)
    expect(wrapper.find('button').exists()).toBe(true)
  })

  it('shows empty state when no results', async () => {
    const pinia = createMockSetup()
    const authStore = useAuthStore()
    authStore.apiGet = vi.fn().mockResolvedValue({ code: 200, data: [] })
    const wrapper = mount(RAG, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const input = wrapper.find('input')
    await input.setValue('xyz123nonexistent')
    const btn = wrapper.find('button')
    await btn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('未找到结果')
  })

  it('displays search results', async () => {
    const pinia = createMockSetup()
    const authStore = useAuthStore()
    const results = [
      { id_wsh: 1, title_wsh: '宠物疫苗接种指南', content_wsh: '幼犬应在出生后6-8周接种疫苗', score_wsh: 0.95, category_wsh: 'health' },
      { id_wsh: 2, title_wsh: '宠物饮食注意事项', content_wsh: '避免喂食巧克力等有害食物', score_wsh: 0.87, category_wsh: 'diet' },
    ]
    authStore.apiGet = vi.fn().mockResolvedValue({ code: 200, data: results })
    const wrapper = mount(RAG, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const input = wrapper.find('input')
    await input.setValue('疫苗')
    const btn = wrapper.find('button')
    await btn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('宠物疫苗接种指南')
    expect(wrapper.text()).toContain('宠物饮食注意事项')
    expect(wrapper.text()).toContain('95%')
    expect(wrapper.text()).toContain('87%')
  })

  it('shows loading state during search', async () => {
    const pinia = createMockSetup()
    const authStore = useAuthStore()
    authStore.apiGet = vi.fn().mockImplementation(() => new Promise(r => setTimeout(() => r({ code: 200, data: [] }), 1000)))
    const wrapper = mount(RAG, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const input = wrapper.find('input')
    await input.setValue('疫苗')
    const btn = wrapper.find('button')
    await btn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('搜索中')
  })
})
