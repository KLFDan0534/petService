import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import AdminMerchants from '../admin/AdminMerchants.vue'

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
  authStore.apiPost = vi.fn().mockResolvedValue({ code: 200 })
  return pinia
}

describe('AdminMerchants.vue', () => {
  it('renders merchant list with approve/reject buttons for pending', async () => {
    const mockMerchants = [
      { id_wsh: 1, name_wsh: '待审核商家', status_wsh: 0 },
      { id_wsh: 2, name_wsh: '已通过商家', status_wsh: 1 },
      { id_wsh: 3, name_wsh: '已拒绝商家', status_wsh: 2 },
    ]
    const pinia = createMockSetup(mockMerchants)
    const wrapper = mount(AdminMerchants, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const btns = wrapper.findAll('button')
    const approveBtns = btns.filter(b => b.text() === '通过')
    const rejectBtns = btns.filter(b => b.text() === '拒绝')
    expect(approveBtns.length).toBe(1)
    expect(rejectBtns.length).toBe(1)
  })

  it('shows badge for approved/rejected merchants', async () => {
    const mockMerchants = [
      { id_wsh: 1, name_wsh: '待审核', status_wsh: 0 },
      { id_wsh: 2, name_wsh: '已通过', status_wsh: 1 },
    ]
    const pinia = createMockSetup(mockMerchants)
    const wrapper = mount(AdminMerchants, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('已通过')
  })

  it('approve button calls approve API', async () => {
    const mockMerchants = [{ id_wsh: 1, name_wsh: '测试商家', status_wsh: 0 }]
    const pinia = createMockSetup(mockMerchants)
    const appStore = useAppStore()
    appStore.addToast = vi.fn()
    const wrapper = mount(AdminMerchants, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const approveBtn = wrapper.findAll('button').find(b => b.text() === '通过')
    await approveBtn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    const authStore = useAuthStore()
    expect(authStore.apiPost).toHaveBeenCalledWith('/api/merchants/1/approve', {})
  })

  it('reject button calls reject API', async () => {
    const mockMerchants = [{ id_wsh: 2, name_wsh: '测试商家2', status_wsh: 0 }]
    const pinia = createMockSetup(mockMerchants)
    const appStore = useAppStore()
    appStore.addToast = vi.fn()
    const wrapper = mount(AdminMerchants, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const rejectBtn = wrapper.findAll('button').find(b => b.text() === '拒绝')
    await rejectBtn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    const authStore = useAuthStore()
    expect(authStore.apiPost).toHaveBeenCalledWith('/api/merchants/2/reject', {})
  })
})
