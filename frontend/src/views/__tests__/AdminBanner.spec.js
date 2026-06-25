import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import AdminBanner from '../admin/AdminBanner.vue'

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

describe('AdminBanner.vue', () => {
  it('renders empty state', async () => {
    const pinia = createMockSetup([])
    const wrapper = mount(AdminBanner, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('投放广告')
  })

  it('renders banner list', async () => {
    const mockBanners = [
      { id_wsh: 1, title_wsh: 'Summer Sale', type_wsh: 'banner', image_url_wsh: 'https://example.com/banner1.jpg', link_url_wsh: 'https://example.com', sort_order_wsh: 1, status_wsh: 1 },
      { id_wsh: 2, title_wsh: 'New Service', type_wsh: 'banner', image_url_wsh: 'https://example.com/banner2.jpg', link_url_wsh: 'https://example.com', sort_order_wsh: 2, status_wsh: 0 },
    ]
    const pinia = createMockSetup(mockBanners)
    const wrapper = mount(AdminBanner, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('Summer Sale')
    expect(wrapper.text()).toContain('New Service')
  })

  it('shows up/down shelf buttons based on status', async () => {
    const mockBanners = [
      { id_wsh: 1, title_wsh: 'Active Banner', type_wsh: 'banner', image_url_wsh: '', link_url_wsh: '', sort_order_wsh: 1, status_wsh: 1 },
      { id_wsh: 2, title_wsh: 'Inactive Banner', type_wsh: 'banner', image_url_wsh: '', link_url_wsh: '', sort_order_wsh: 2, status_wsh: 0 },
    ]
    const pinia = createMockSetup(mockBanners)
    const wrapper = mount(AdminBanner, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const btns = wrapper.findAll('button')
    expect(btns.filter(b => b.text() === '下架').length).toBe(1)
    expect(btns.filter(b => b.text() === '上架').length).toBe(1)
  })

  it('shows create form on button click', async () => {
    const pinia = createMockSetup([])
    const wrapper = mount(AdminBanner, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const createBtn = wrapper.findAll('button').find(b => b.text() === '+ 投放广告')
    await createBtn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('投放广告')
    expect(wrapper.find('form').exists()).toBe(true)
  })

  it('calls create API on form submit', async () => {
    const pinia = createMockSetup([])
    const authStore = useAuthStore()
    authStore.apiPost = vi.fn().mockResolvedValue({ code: 200 })
    const wrapper = mount(AdminBanner, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const createBtn = wrapper.findAll('button').find(b => b.text() === '+ 投放广告')
    await createBtn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    const input = wrapper.find('input')
    await input.setValue('New Banner')
    const submitBtn = wrapper.findAll('button').find(b => b.text() === '投放')
    await submitBtn.trigger('submit')
    await new Promise(r => setTimeout(r, 50))
    expect(authStore.apiPost).toHaveBeenCalledWith('/api/notices', {
      title_wsh: 'New Banner', content_wsh: '', type_wsh: 'banner',
      image_url_wsh: '', link_url_wsh: '', sort_order_wsh: 0,
    })
  })

  it('calls toggle API on shelf button click', async () => {
    const mockBanners = [{ id_wsh: 1, title_wsh: 'Banner', type_wsh: 'banner', image_url_wsh: '', link_url_wsh: '', sort_order_wsh: 1, status_wsh: 1 }]
    const pinia = createMockSetup(mockBanners)
    const authStore = useAuthStore()
    authStore.apiPut = vi.fn().mockResolvedValue({ code: 200 })
    const wrapper = mount(AdminBanner, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const shelfBtn = wrapper.findAll('button').find(b => b.text() === '下架')
    await shelfBtn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    expect(authStore.apiPut).toHaveBeenCalledWith('/api/notices/1', { status_wsh: 0 })
  })

  it('calls delete API', async () => {
    const mockBanners = [{ id_wsh: 1, title_wsh: 'Banner', type_wsh: 'banner', image_url_wsh: '', link_url_wsh: '', sort_order_wsh: 1, status_wsh: 1 }]
    const pinia = createMockSetup(mockBanners)
    const authStore = useAuthStore()
    authStore.apiDelete = vi.fn().mockResolvedValue({ code: 200 })
    const wrapper = mount(AdminBanner, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    global.confirm = vi.fn(() => true)
    const deleteBtn = wrapper.findAll('button').find(b => b.text() === '删除')
    await deleteBtn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    expect(authStore.apiDelete).toHaveBeenCalledWith('/api/notices/1')
  })
})
