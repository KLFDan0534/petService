import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import Complaints from '../user/Complaints.vue'

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

describe('Complaints.vue', () => {
  it('renders empty state when no complaints', async () => {
    const pinia = createMockSetup([])
    const wrapper = mount(Complaints, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('暂无投诉')
  })

  it('renders complaint list with status badges', async () => {
    const pinia = createMockSetup([
      { id_wsh: 1, title_wsh: '服务态度问题', content_wsh: '沟通不畅', status_wsh: 'pending' },
      { id_wsh: 2, title_wsh: '环境脏乱', content_wsh: '寄养环境差', status_wsh: 'resolved' },
    ])
    const wrapper = mount(Complaints, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('服务态度问题')
    expect(wrapper.text()).toContain('沟通不畅')
    expect(wrapper.text()).toContain('环境脏乱')
    expect(wrapper.text()).toContain('待处理')
    expect(wrapper.text()).toContain('已处理')
  })

  it('opens complaint form modal on button click', async () => {
    const pinia = createMockSetup([])
    const wrapper = mount(Complaints, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const submitBtn = wrapper.findAll('button').find(b => b.text().includes('提交投诉'))
    await submitBtn.trigger('click')
    expect(wrapper.text()).toContain('提交投诉')
    expect(wrapper.find('form').exists()).toBe(true)
  })

  it('submits complaint and refreshes list', async () => {
    const pinia = createMockSetup([])
    const authStore = useAuthStore()
    const newComplaint = { id_wsh: 3, title_wsh: '食物问题', content_wsh: '提供的食物过期', status_wsh: 'pending' }
    authStore.apiPost = vi.fn().mockResolvedValue({ code: 200, data: newComplaint })
    const wrapper = mount(Complaints, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const submitBtn = wrapper.findAll('button').find(b => b.text().includes('提交投诉'))
    await submitBtn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    const form = wrapper.find('form')
    await form.trigger('submit')
    expect(authStore.apiPost).toHaveBeenCalledWith('/api/complaints', {
      title_wsh: '', content_wsh: '',
    })
  })
})
