import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import AdminRoles from '../admin/AdminRoles.vue'

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

describe('AdminRoles.vue', () => {
  it('renders empty state with create button', async () => {
    const pinia = createMockSetup([])
    const wrapper = mount(AdminRoles, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.find('table').exists()).toBe(true)
    expect(wrapper.text()).toContain('+ 创建角色')
  })

  it('renders roles list', async () => {
    const mockRoles = [
      { id_wsh: 1, name_wsh: '管理员', code_wsh: 'ADMIN', description_wsh: '系统管理员' },
      { id_wsh: 2, name_wsh: '商家', code_wsh: 'MERCHANT', description_wsh: '商家用户' },
    ]
    const pinia = createMockSetup(mockRoles)
    const wrapper = mount(AdminRoles, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('管理员')
    expect(wrapper.text()).toContain('商家')
    expect(wrapper.text()).toContain('系统管理员')
  })

  it('shows create form on button click', async () => {
    const pinia = createMockSetup([])
    const wrapper = mount(AdminRoles, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const createBtn = wrapper.findAll('button').find(b => b.text() === '+ 创建角色')
    await createBtn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('创建角色')
  })

  it('calls create role API on form submit', async () => {
    const pinia = createMockSetup([])
    const authStore = useAuthStore()
    authStore.apiPost = vi.fn().mockResolvedValue({ code: 200 })
    const wrapper = mount(AdminRoles, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const createBtn = wrapper.findAll('button').find(b => b.text() === '+ 创建角色')
    await createBtn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('测试角色')
    await inputs[1].setValue('测试描述')
    const form = wrapper.find('form')
    await form.trigger('submit')
    await new Promise(r => setTimeout(r, 50))
    expect(authStore.apiPost).toHaveBeenCalledWith('/api/roles', { name_wsh: '测试角色', description_wsh: '测试描述' })
  })

  it('shows edit form with pre-filled data', async () => {
    const mockRoles = [
      { id_wsh: 1, name_wsh: '测试角色', code_wsh: 'TEST', description_wsh: '测试描述' },
    ]
    const pinia = createMockSetup(mockRoles)
    const wrapper = mount(AdminRoles, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const editBtn = wrapper.findAll('button').find(b => b.text() === '编辑')
    await editBtn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('编辑角色')
    const inputs = wrapper.findAll('input')
    expect(inputs[0].element.value).toBe('测试角色')
    expect(inputs[1].element.value).toBe('测试描述')
  })

  it('calls update role API on edit form submit', async () => {
    const mockRoles = [
      { id_wsh: 1, name_wsh: '测试角色', code_wsh: 'TEST', description_wsh: '测试描述' },
    ]
    const pinia = createMockSetup(mockRoles)
    const authStore = useAuthStore()
    authStore.apiPut = vi.fn().mockResolvedValue({ code: 200 })
    const wrapper = mount(AdminRoles, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const editBtn = wrapper.findAll('button').find(b => b.text() === '编辑')
    await editBtn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    const submitBtn = wrapper.findAll('button').find(b => b.text() === '保存')
    const form = wrapper.find('form')
    await form.trigger('submit')
    await new Promise(r => setTimeout(r, 50))
    expect(authStore.apiPut).toHaveBeenCalledWith('/api/roles/1', { name_wsh: '测试角色', description_wsh: '测试描述' })
  })

  it('calls delete role API', async () => {
    const mockRoles = [
      { id_wsh: 1, name_wsh: '测试角色', code_wsh: 'TEST', description_wsh: '测试描述' },
    ]
    const pinia = createMockSetup(mockRoles)
    const authStore = useAuthStore()
    authStore.apiDelete = vi.fn().mockResolvedValue({ code: 200 })
    global.confirm = vi.fn().mockReturnValue(true)
    const wrapper = mount(AdminRoles, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const deleteBtn = wrapper.findAll('button').find(b => b.text() === '删除')
    await deleteBtn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    expect(authStore.apiDelete).toHaveBeenCalledWith('/api/roles/1')
    delete global.confirm
  })

  it('does not delete when confirm is false', async () => {
    const mockRoles = [
      { id_wsh: 1, name_wsh: '测试角色', code_wsh: 'TEST', description_wsh: '测试描述' },
    ]
    const pinia = createMockSetup(mockRoles)
    const authStore = useAuthStore()
    authStore.apiDelete = vi.fn()
    global.confirm = vi.fn().mockReturnValue(false)
    const wrapper = mount(AdminRoles, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const deleteBtn = wrapper.findAll('button').find(b => b.text() === '删除')
    await deleteBtn.trigger('click')
    expect(authStore.apiDelete).not.toHaveBeenCalled()
    delete global.confirm
  })
})
