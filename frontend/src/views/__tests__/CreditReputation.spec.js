import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import CreditReputation from '../user/CreditReputation.vue'

vi.mock('@/stores/design', () => ({
  useDesignStore: () => ({ activeDesign: null }),
}))

function createMockSetup(creditInfo, complaints) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const authStore = useAuthStore()
  authStore.setAuth({
    userId: 1, username: 'owner', nickname: '测试用户',
    roles: ['OWNER'], accessToken: 't', refreshToken: 'r',
  })
  authStore.apiGet = vi.fn()
    .mockResolvedValueOnce({ code: 200, data: creditInfo })
    .mockResolvedValueOnce({ code: 200, data: complaints })
  return pinia
}

describe('CreditReputation.vue', () => {
  it('renders credit level section', async () => {
    const pinia = createMockSetup({ level_wsh: '金牌', score_wsh: 850 }, [])
    const wrapper = mount(CreditReputation, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('信用等级')
    expect(wrapper.text()).toContain('金牌')
    expect(wrapper.text()).toContain('850')
  })

  it('shows loading state for ratings initially', async () => {
    const pinia = createMockSetup({}, [])
    const wrapper = mount(CreditReputation, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('加载中')
  })

  it('renders empty state when no complaints', async () => {
    const pinia = createMockSetup({}, [])
    const wrapper = mount(CreditReputation, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('暂无投诉记录')
  })

  it('renders complaint list with status badges', async () => {
    const complaints = [
      { id_wsh: 1, title_wsh: '服务问题', content_wsh: '沟通不畅', status_wsh: 'pending' },
      { id_wsh: 2, title_wsh: '环境问题', content_wsh: '环境脏乱', status_wsh: 'resolved', result_wsh: '已处理完毕' },
    ]
    const pinia = createMockSetup({ totalRatings: 0 }, complaints)
    const wrapper = mount(CreditReputation, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('服务问题')
    expect(wrapper.text()).toContain('环境问题')
    expect(wrapper.text()).toContain('待处理')
    expect(wrapper.text()).toContain('已处理')
    expect(wrapper.text()).toContain('已处理完毕')
  })

  it('renders empty state when no performance data', async () => {
    const pinia = createMockSetup({}, [])
    const wrapper = mount(CreditReputation, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('暂无绩效数据')
  })

  it('renders performance stats when data exists', async () => {
    const creditInfo = { totalCompleted: 50, completionRate: 95, complaintRate: 1, totalTips: 10 }
    const pinia = createMockSetup(creditInfo, [])
    const wrapper = mount(CreditReputation, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('50')
    expect(wrapper.text()).toContain('95%')
    expect(wrapper.text()).toContain('1%')
    expect(wrapper.text()).toContain('10')
    expect(wrapper.text()).toContain('完成订单')
    expect(wrapper.text()).toContain('完成率')
    expect(wrapper.text()).toContain('投诉率')
    expect(wrapper.text()).toContain('获得打赏')
  })
})
