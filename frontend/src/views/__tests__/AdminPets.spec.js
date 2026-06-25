import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import AdminPets from '../admin/AdminPets.vue'

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

describe('AdminPets.vue', () => {
  it('renders empty state', async () => {
    const pinia = createMockSetup([])
    const wrapper = mount(AdminPets, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.find('table').exists()).toBe(true)
  })

  it('renders pet list with fields', async () => {
    const mockPets = [
      { id_wsh: 1, name_wsh: '旺财', type_wsh: 'Dog', breed_wsh: '金毛', owner_id_wsh: 10 },
      { id_wsh: 2, name_wsh: '咪咪', type_wsh: 'Cat', breed_wsh: '英短', owner_id_wsh: 11 },
    ]
    const pinia = createMockSetup(mockPets)
    const wrapper = mount(AdminPets, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('旺财')
    expect(wrapper.text()).toContain('咪咪')
    expect(wrapper.text()).toContain('Dog')
    expect(wrapper.text()).toContain('Cat')
    expect(wrapper.text()).toContain('金毛')
    expect(wrapper.text()).toContain('英短')
  })

  it('shows detail modal on view button click', async () => {
    const mockPets = [
      { id_wsh: 1, name_wsh: '旺财', type_wsh: 'Dog', breed_wsh: '金毛', age_wsh: 3, weight_wsh: 25, owner_name_wsh: '张三' },
    ]
    const pinia = createMockSetup(mockPets)
    const wrapper = mount(AdminPets, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const viewBtn = wrapper.findAll('button').find(b => b.text() === '查看')
    await viewBtn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('宠物详情')
    expect(wrapper.text()).toContain('旺财')
    expect(wrapper.text()).toContain('金毛')
    expect(wrapper.text()).toContain('3岁')
    expect(wrapper.text()).toContain('25kg')
  })

  it('closes detail modal', async () => {
    const mockPets = [
      { id_wsh: 1, name_wsh: '旺财', type_wsh: 'Dog', breed_wsh: '金毛', age_wsh: 3, weight_wsh: 25, owner_name_wsh: '张三' },
    ]
    const pinia = createMockSetup(mockPets)
    const wrapper = mount(AdminPets, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const viewBtn = wrapper.findAll('button').find(b => b.text() === '查看')
    await viewBtn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    const closeBtn = wrapper.findAll('button').find(b => b.text() === '关闭')
    await closeBtn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).not.toContain('宠物详情')
  })

  it('handles missing optional fields in detail', async () => {
    const mockPets = [
      { id_wsh: 1, name_wsh: 'Test', type_wsh: 'Dog', breed_wsh: '未知', owner_id_wsh: 10 },
    ]
    const pinia = createMockSetup(mockPets)
    const wrapper = mount(AdminPets, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const viewBtn = wrapper.findAll('button').find(b => b.text() === '查看')
    await viewBtn.trigger('click')
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('-')
  })
})
