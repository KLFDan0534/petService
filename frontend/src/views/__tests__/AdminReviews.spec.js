import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import AdminReviews from '../admin/AdminReviews.vue'

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

describe('AdminReviews.vue', () => {
  it('renders empty state', async () => {
    const pinia = createMockSetup([])
    const wrapper = mount(AdminReviews, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.find('table').exists()).toBe(true)
  })

  it('shows approve/reject for pending reviews', async () => {
    const mockReviews = [
      { id_wsh: 1, target_type_wsh: 'rating', reporter_id_wsh: 2, status_wsh: 'pending' },
      { id_wsh: 2, target_type_wsh: 'comment', reporter_id_wsh: 3, status_wsh: 'approved' },
    ]
    const pinia = createMockSetup(mockReviews)
    const wrapper = mount(AdminReviews, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const btns = wrapper.findAll('button')
    expect(btns.filter(b => b.text() === '通过').length).toBe(1)
    expect(btns.filter(b => b.text() === '拒绝').length).toBe(1)
  })

  it('shows badges for processed reviews', async () => {
    const mockReviews = [
      { id_wsh: 1, target_type_wsh: 'rating', reporter_id_wsh: 2, status_wsh: 'approved' },
      { id_wsh: 2, target_type_wsh: 'comment', reporter_id_wsh: 3, status_wsh: 'rejected' },
    ]
    const pinia = createMockSetup(mockReviews)
    const wrapper = mount(AdminReviews, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('已通过')
    expect(wrapper.text()).toContain('已拒绝')
  })

  it('approve button calls API', async () => {
    const mockReviews = [{ id_wsh: 1, target_type_wsh: 'rating', reporter_id_wsh: 2, status_wsh: 'pending' }]
    const pinia = createMockSetup(mockReviews)
    const authStore = useAuthStore()
    authStore.apiPost = vi.fn().mockResolvedValue({ code: 200 })
    const wrapper = mount(AdminReviews, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const approveBtn = wrapper.findAll('button').find(b => b.text() === '通过')
    await approveBtn.trigger('click')
    expect(authStore.apiPost).toHaveBeenCalledWith('/api/reviews/1/approve', {})
  })
})
