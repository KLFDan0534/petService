import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import AdminComplaints from '../admin/AdminComplaints.vue'

vi.mock('@/stores/design', () => ({
  useDesignStore: () => ({ activeDesign: null }),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
}))

function createMockSetup(complaints) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const authStore = useAuthStore()
  authStore.setAuth({
    userId: 1, username: 'admin', nickname: '管理员',
    roles: ['ADMIN'], accessToken: 't', refreshToken: 'r',
  })
  authStore.apiGet = vi.fn().mockResolvedValue({ code: 200, data: complaints })
  authStore.apiPost = vi.fn().mockResolvedValue({ code: 200, data: {} })
  return pinia
}

describe('AdminComplaints.vue', () => {
  it('renders complaint table with records', async () => {
    const complaints = [
      { id_wsh: 1, title_wsh: '服务质量差', status_wsh: 'pending', owner_name_wsh: '用户1' },
      { id_wsh: 2, title_wsh: '环境问题', status_wsh: 'resolved', owner_name_wsh: '用户2' },
    ]
    const pinia = createMockSetup(complaints)
    const wrapper = mount(AdminComplaints, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('服务质量差')
    expect(wrapper.text()).toContain('环境问题')
  })

  it('shows resolve button for pending complaints', async () => {
    const complaints = [
      { id_wsh: 1, title_wsh: '待处理投诉', status_wsh: 'pending', owner_name_wsh: '用户' },
    ]
    const pinia = createMockSetup(complaints)
    const wrapper = mount(AdminComplaints, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('处理')
  })

  it('shows resolved badge for resolved complaints', async () => {
    const complaints = [
      { id_wsh: 1, title_wsh: '已处理投诉', status_wsh: 'resolved', owner_name_wsh: '用户' },
    ]
    const pinia = createMockSetup(complaints)
    const wrapper = mount(AdminComplaints, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('已处理')
  })

  it('resolves complaint on button click', async () => {
    const complaints = [
      { id_wsh: 1, title_wsh: '投诉1', status_wsh: 'pending', owner_name_wsh: '用户' },
    ]
    const pinia = createMockSetup(complaints)
    const authStore = useAuthStore()
    authStore.apiPost = vi.fn().mockResolvedValue({ code: 200, data: {} })
    const wrapper = mount(AdminComplaints, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const resolveBtn = wrapper.findAll('button').find(b => b.text().includes('处理'))
    await resolveBtn.trigger('click')
    expect(authStore.apiPost).toHaveBeenCalledWith('/api/complaints/1/resolve', {})
  })
})
