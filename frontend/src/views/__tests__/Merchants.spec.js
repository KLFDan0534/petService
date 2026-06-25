import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import Merchants from '../user/Merchants.vue'

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

describe('Merchants.vue', () => {
  it('renders loading state initially', () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const wrapper = mount(Merchants, { global: { plugins: [pinia] } })
    expect(wrapper.text()).toContain('加载中')
  })

  it('renders empty state when no merchants', async () => {
    const pinia = createMockSetup([])
    const wrapper = mount(Merchants, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).not.toContain('加载中')
  })

  it('renders merchant list with data', async () => {
    const mockMerchants = [
      { id_wsh: 1, name_wsh: '阳光宠物店', description_wsh: '专业寄养', rating_wsh: 4.5, order_count_wsh: 10 },
      { id_wsh: 2, name_wsh: '温馨喵屋', description_wsh: '猫咪寄养', rating_wsh: 4.8, order_count_wsh: 5 },
    ]
    const pinia = createMockSetup(mockMerchants)
    const wrapper = mount(Merchants, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('阳光宠物店')
    expect(wrapper.text()).toContain('温馨喵屋')
    expect(wrapper.text()).toContain('4.5')
    expect(wrapper.text()).toContain('4.8')
  })

  it('shows placeholder for missing description', async () => {
    const mockMerchants = [{ id_wsh: 1, name_wsh: '无介绍商家', rating_wsh: null }]
    const pinia = createMockSetup(mockMerchants)
    const wrapper = mount(Merchants, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('暂无介绍')
    expect(wrapper.text()).toContain('暂无评分')
  })

  it('has view service button for each merchant', async () => {
    const mockMerchants = [{ id_wsh: 1, name_wsh: '测试商家', rating_wsh: 4.0 }]
    const pinia = createMockSetup(mockMerchants)
    const wrapper = mount(Merchants, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const btns = wrapper.findAll('button')
    expect(btns.filter(b => b.text() === '查看服务').length).toBe(1)
  })
})
