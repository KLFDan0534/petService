import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import Anomaly from '../user/Anomaly.vue'

vi.mock('@/stores/design', () => ({
  useDesignStore: () => ({ activeDesign: null }),
}))

function createMockSetup(records) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const authStore = useAuthStore()
  authStore.setAuth({
    userId: 1, username: 'keeper', nickname: '看护人',
    roles: ['KEEPER'], accessToken: 't', refreshToken: 'r',
  })
  authStore.apiGet = vi.fn().mockResolvedValue({ code: 200, data: records })
  authStore.apiPost = vi.fn().mockResolvedValue({ code: 200, data: { id: 999 } })
  return pinia
}

describe('Anomaly.vue', () => {
  it('renders empty state when no records', async () => {
    const pinia = createMockSetup([])
    const wrapper = mount(Anomaly, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('暂无异常记录')
  })

  it('renders type buttons', async () => {
    const pinia = createMockSetup([])
    const wrapper = mount(Anomaly, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('健康异常上报')
    expect(wrapper.text()).toContain('饮食异常上报')
    expect(wrapper.text()).toContain('行为异常上报')
    expect(wrapper.text()).toContain('紧急情况上报')
  })

  it('renders record list with all four types', async () => {
    const records = [
      { id_wsh: 1, type_wsh: 'health', content_wsh: '体温升高', record_time_wsh: '2026-01-01T10:00:00' },
      { id_wsh: 2, type_wsh: 'diet', content_wsh: '食欲不振', record_time_wsh: '2026-01-01T11:00:00' },
      { id_wsh: 3, type_wsh: 'behavior', content_wsh: '异常焦虑', record_time_wsh: '2026-01-01T12:00:00' },
      { id_wsh: 4, type_wsh: 'emergency', content_wsh: '误食异物', record_time_wsh: '2026-01-01T13:00:00' },
    ]
    const pinia = createMockSetup(records)
    const wrapper = mount(Anomaly, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('健康异常')
    expect(wrapper.text()).toContain('饮食异常')
    expect(wrapper.text()).toContain('行为异常')
    expect(wrapper.text()).toContain('紧急情况')
    expect(wrapper.text()).toContain('体温升高')
    expect(wrapper.text()).toContain('食欲不振')
    expect(wrapper.text()).toContain('异常焦虑')
    expect(wrapper.text()).toContain('误食异物')
  })

  it('opens form modal when clicking health button', async () => {
    const pinia = createMockSetup([])
    const wrapper = mount(Anomaly, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const btn = wrapper.findAll('button').find(b => b.text().includes('健康异常上报'))
    await btn.trigger('click')
    expect(wrapper.text()).toContain('健康异常上报')
    expect(wrapper.find('form').exists()).toBe(true)
  })

  it('opens form modal with emergency type', async () => {
    const pinia = createMockSetup([])
    const wrapper = mount(Anomaly, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const btn = wrapper.findAll('button').find(b => b.text().includes('紧急情况上报'))
    await btn.trigger('click')
    expect(wrapper.text()).toContain('紧急情况上报')
  })

  it('submits record successfully', async () => {
    const pinia = createMockSetup([])
    const authStore = useAuthStore()
    authStore.apiPost = vi.fn().mockResolvedValue({ code: 200, data: { id_wsh: 999, type_wsh: 'health', content_wsh: '测试内容' } })
    const wrapper = mount(Anomaly, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const btn = wrapper.findAll('button').find(b => b.text().includes('健康异常上报'))
    await btn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    const form = wrapper.find('form')
    await form.trigger('submit')
    expect(authStore.apiPost).toHaveBeenCalled()
  })
})
