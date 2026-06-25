import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import AdminCategory from '../admin/AdminCategory.vue'

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

describe('AdminCategory.vue', () => {
  it('renders empty state', async () => {
    const pinia = createMockSetup([])
    const wrapper = mount(AdminCategory, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('新增分类')
  })

  it('renders category list with fields', async () => {
    const mockCategories = [
      { id_wsh: 1, name_wsh: '狗', parent_id_wsh: 0, sort_order_wsh: 1 },
      { id_wsh: 2, name_wsh: '猫', parent_id_wsh: 0, sort_order_wsh: 2 },
      { id_wsh: 5, name_wsh: '泰迪', parent_id_wsh: 1, sort_order_wsh: 1 },
    ]
    const pinia = createMockSetup(mockCategories)
    const wrapper = mount(AdminCategory, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('狗')
    expect(wrapper.text()).toContain('猫')
    expect(wrapper.text()).toContain('泰迪')
  })

  it('shows create form on button click', async () => {
    const pinia = createMockSetup([])
    const wrapper = mount(AdminCategory, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const createBtn = wrapper.findAll('button').find(b => b.text() === '+ 新增分类')
    await createBtn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('新增分类')
    expect(wrapper.find('form').exists()).toBe(true)
    expect(wrapper.find('input').exists()).toBe(true)
  })

  it('calls create API on form submit', async () => {
    const pinia = createMockSetup([])
    const authStore = useAuthStore()
    authStore.apiPost = vi.fn().mockResolvedValue({ code: 200 })
    const wrapper = mount(AdminCategory, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const createBtn = wrapper.findAll('button').find(b => b.text() === '+ 新增分类')
    await createBtn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    const input = wrapper.find('input')
    await input.setValue('鸟类')
    const form = wrapper.find('form')
    await form.trigger('submit')
    await new Promise(r => setTimeout(r, 50))
    expect(authStore.apiPost).toHaveBeenCalledWith('/api/categories', { name: '鸟类', parentId: 0, sortOrder: 0 })
  })

  it('shows edit form with pre-filled data', async () => {
    const mockCategories = [{ id_wsh: 1, name_wsh: '狗', parent_id_wsh: 0, sort_order_wsh: 1 }]
    const pinia = createMockSetup(mockCategories)
    const wrapper = mount(AdminCategory, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const editBtn = wrapper.findAll('button').find(b => b.text() === '编辑')
    await editBtn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    const input = wrapper.find('input')
    expect(input.element.value).toBe('狗')
  })

  it('calls update API on edit form submit', async () => {
    const mockCategories = [{ id_wsh: 1, name_wsh: '狗', parent_id_wsh: 0, sort_order_wsh: 1 }]
    const pinia = createMockSetup(mockCategories)
    const authStore = useAuthStore()
    authStore.apiPut = vi.fn().mockResolvedValue({ code: 200 })
    const wrapper = mount(AdminCategory, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const editBtn = wrapper.findAll('button').find(b => b.text() === '编辑')
    await editBtn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    const input = wrapper.find('input')
    await input.setValue('狗狗')
    const form = wrapper.find('form')
    await form.trigger('submit')
    await new Promise(r => setTimeout(r, 50))
    expect(authStore.apiPut).toHaveBeenCalledWith('/api/categories/1', { name: '狗狗', parentId: 0, sortOrder: 1 })
  })

  it('calls delete API', async () => {
    const mockCategories = [{ id_wsh: 1, name_wsh: '狗', parent_id_wsh: 0, sort_order_wsh: 1 }]
    const pinia = createMockSetup(mockCategories)
    const authStore = useAuthStore()
    authStore.apiDelete = vi.fn().mockResolvedValue({ code: 200 })
    const wrapper = mount(AdminCategory, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    global.confirm = vi.fn(() => true)
    const deleteBtn = wrapper.findAll('button').find(b => b.text() === '删除')
    await deleteBtn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    expect(authStore.apiDelete).toHaveBeenCalledWith('/api/categories/1')
  })

  it('closes create form on cancel', async () => {
    const pinia = createMockSetup([])
    const wrapper = mount(AdminCategory, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const createBtn = wrapper.findAll('button').find(b => b.text() === '+ 新增分类')
    await createBtn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    const cancelBtn = wrapper.findAll('button').find(b => b.text() === '取消')
    await cancelBtn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.find('.modal').exists()).toBe(false)
  })
})
