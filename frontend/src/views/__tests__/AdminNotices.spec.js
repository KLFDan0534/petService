import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import AdminNotices from '../admin/AdminNotices.vue'

vi.mock('@/stores/design', () => ({
  useDesignStore: () => ({ activeDesign: null }),
}))

function createMockSetup(data) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const authStore = useAuthStore()
  authStore.setAuth({
    userId: 1, username: 'admin', nickname: '管理员',
    roles: ['ADMIN'], accessToken: 't', refreshToken: 'r',
  })
  authStore.apiGet = vi.fn().mockResolvedValue({ code: 200, data })
  return pinia
}

describe('AdminNotices.vue', () => {
  it('renders empty state', async () => {
    const pinia = createMockSetup([])
    const wrapper = mount(AdminNotices, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).not.toContain('card')
  })

  it('renders notice list', async () => {
    const mockNotices = [
      { id_wsh: 1, title_wsh: '系统升级公告', content_wsh: '系统将于本周六维护', created_at_wsh: '2025-06-01T00:00:00' },
      { id_wsh: 2, title_wsh: '春节放假通知', content_wsh: '春节期间暂停服务', created_at_wsh: '2025-06-02T00:00:00' },
    ]
    const pinia = createMockSetup(mockNotices)
    const wrapper = mount(AdminNotices, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('系统升级公告')
    expect(wrapper.text()).toContain('系统将于本周六维护')
    expect(wrapper.text()).toContain('春节放假通知')
    expect(wrapper.text()).toContain('春节期间暂停服务')
  })

  it('shows create notice form on button click', async () => {
    const pinia = createMockSetup([])
    const wrapper = mount(AdminNotices, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const createBtn = wrapper.findAll('button').find(b => b.text() === '+ 发布公告')
    await createBtn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('发布公告')
    expect(wrapper.find('form').exists()).toBe(true)
    expect(wrapper.find('input').exists()).toBe(true)
    expect(wrapper.find('textarea').exists()).toBe(true)
  })

  it('calls create notice API on form submit', async () => {
    const pinia = createMockSetup([])
    const authStore = useAuthStore()
    authStore.apiPost = vi.fn().mockResolvedValue({ code: 200, data: { id_wsh: 3, title_wsh: '新公告', content_wsh: '新内容', created_at_wsh: new Date().toISOString() } })
    const wrapper = mount(AdminNotices, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const createBtn = wrapper.findAll('button').find(b => b.text() === '+ 发布公告')
    await createBtn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    const input = wrapper.find('input')
    await input.setValue('新公告')
    const textarea = wrapper.find('textarea')
    await textarea.setValue('新内容')
    const submitBtn = wrapper.findAll('button').find(b => b.text() === '发布')
    await submitBtn.trigger('submit')
    await new Promise(r => setTimeout(r, 50))
    expect(authStore.apiPost).toHaveBeenCalledWith('/api/notices', { title_wsh: '新公告', content_wsh: '新内容' })
  })

  it('closes create form on cancel', async () => {
    const pinia = createMockSetup([])
    const wrapper = mount(AdminNotices, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const createBtn = wrapper.findAll('button').find(b => b.text() === '+ 发布公告')
    await createBtn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    const cancelBtn = wrapper.findAll('button').find(b => b.text() === '取消')
    await cancelBtn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.find('.modal').exists()).toBe(false)
  })

  it('handles API error gracefully', async () => {
    const pinia = createMockSetup([])
    const authStore = useAuthStore()
    authStore.apiGet = vi.fn().mockRejectedValue(new Error('Network error'))
    const wrapper = mount(AdminNotices, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.find('button').exists()).toBe(true)
  })
})
