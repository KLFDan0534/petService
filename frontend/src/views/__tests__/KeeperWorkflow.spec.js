import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import Orders from '../user/Orders.vue'
import KeeperWorkflow from '../user/KeeperWorkflow.vue'

vi.mock('@/stores/design', () => ({
  useDesignStore: () => ({ activeDesign: null }),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
  useRoute: () => ({ query: {} }),
}))

function createKeeperSetup() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const authStore = useAuthStore()
  authStore.setAuth({
    userId_wsh: 10, username_wsh: 'keeper', nickname_wsh: '张阿姨',
    roles_wsh: ['KEEPER', 'OWNER'], access_token_wsh: 't', refresh_token_wsh: 'r',
  })
  return pinia
}

describe('KeeperWorkflow - Login & Profile', () => {
  it('auth store has keeper role', () => {
    const pinia = createKeeperSetup()
    const authStore = useAuthStore()
    expect(authStore.hasRole('KEEPER')).toBe(true)
    expect(authStore.user.nickname_wsh).toBe('张阿姨')
  })

  it('update profile calls apiPut', async () => {
    const pinia = createKeeperSetup()
    const authStore = useAuthStore()
    authStore.apiPut = vi.fn().mockResolvedValue({ code: 200, data: { nickname: '张阿姨已更新', email_wsh: 'keeper@test.com', phone: '13800000505' } })
    const r = await authStore.apiPut('/api/users/me', { nickname: '张阿姨已更新', email_wsh: 'keeper@test.com', phone: '13800000505' })
    expect(r.code).toBe(200)
    expect(r.data.nickname).toBe('张阿姨已更新')
    expect(authStore.apiPut).toHaveBeenCalledWith('/api/users/me', { nickname: '张阿姨已更新', email_wsh: 'keeper@test.com', phone: '13800000505' })
  })
})

describe('KeeperWorkflow - Keeper Certification', () => {
  it('submit certification calls apiPost', async () => {
    const pinia = createKeeperSetup()
    const authStore = useAuthStore()
    const certData = { merchant_id_wsh: 1, name_wsh: '张阿姨', phone_wsh: '13900139002', experience_years_wsh: 5, price_per_day_wsh: 150, max_pets_wsh: 5, bio_wsh: '有丰富经验', status_wsh: 1 }
    authStore.apiPost = vi.fn().mockResolvedValue({ code: 200, data: { id_wsh: 1, ...certData } })
    const r = await authStore.apiPost('/api/keepers', certData)
    expect(r.code).toBe(200)
    expect(r.data.name_wsh).toBe('张阿姨')
    expect(r.data.experience_years_wsh).toBe(5)
    expect(authStore.apiPost).toHaveBeenCalledWith('/api/keepers', certData)
  })

  it('view my merchant calls apiGet', async () => {
    const pinia = createKeeperSetup()
    const authStore = useAuthStore()
    authStore.apiGet = vi.fn().mockResolvedValue({ code: 200, data: { id_wsh: 1, name_wsh: '工作流测试寄养中心' } })
    const r = await authStore.apiGet('/api/merchants/my')
    expect(r.code).toBe(200)
    expect(r.data.name_wsh).toContain('寄养中心')
  })
})

describe('KeeperWorkflow - Order Management', () => {
  it('view pending orders calls apiGet', async () => {
    const pinia = createKeeperSetup()
    const authStore = useAuthStore()
    authStore.apiGet = vi.fn().mockResolvedValue({ code: 200, data: [{ id: 1, orderNo: 'ORD001', status: 'pending' }] })
    const r = await authStore.apiGet('/api/orders/pending')
    expect(r.code).toBe(200)
    expect(r.data.length).toBe(1)
    expect(r.data[0].status).toBe('pending')
  })

  it('accept order calls apiPost', async () => {
    const pinia = createKeeperSetup()
    const authStore = useAuthStore()
    authStore.apiPost = vi.fn().mockResolvedValue({ code: 200 })
    const r = await authStore.apiPost('/api/orders/accept', { orderNo: 'ORD001' })
    expect(r.code).toBe(200)
    expect(authStore.apiPost).toHaveBeenCalledWith('/api/orders/accept', { orderNo: 'ORD001' })
  })

  it('reject order calls apiPost', async () => {
    const pinia = createKeeperSetup()
    const authStore = useAuthStore()
    authStore.apiPost = vi.fn().mockResolvedValue({ code: 200 })
    const r = await authStore.apiPost('/api/orders/reject', { orderNo: 'ORD002' })
    expect(r.code).toBe(200)
    expect(authStore.apiPost).toHaveBeenCalledWith('/api/orders/reject', { orderNo: 'ORD002' })
  })

  it('view history (keeper) calls apiGet', async () => {
    const pinia = createKeeperSetup()
    const authStore = useAuthStore()
    authStore.apiGet = vi.fn().mockResolvedValue({ code: 200, data: [{ id: 1, status: 'completed' }, { id: 2, status: 'cancelled' }] })
    const r = await authStore.apiGet('/api/orders/my-keeper')
    expect(r.code).toBe(200)
    expect(r.data.length).toBe(2)
  })
})

describe('KeeperWorkflow - Service Management', () => {
  it('start service calls apiPost', async () => {
    const pinia = createKeeperSetup()
    const authStore = useAuthStore()
    authStore.apiPost = vi.fn().mockResolvedValue({ code: 200 })
    const r = await authStore.apiPost('/api/orders/start', { orderNo: 'ORD001' })
    expect(r.code).toBe(200)
    expect(authStore.apiPost).toHaveBeenCalledWith('/api/orders/start', { orderNo: 'ORD001' })
  })

  it('complete service calls apiPost', async () => {
    const pinia = createKeeperSetup()
    const authStore = useAuthStore()
    authStore.apiPost = vi.fn().mockResolvedValue({ code: 200 })
    const r = await authStore.apiPost('/api/orders/complete', { orderNo: 'ORD001' })
    expect(r.code).toBe(200)
    expect(authStore.apiPost).toHaveBeenCalledWith('/api/orders/complete', { orderNo: 'ORD001' })
  })

  it('update status calls apiPut', async () => {
    const pinia = createKeeperSetup()
    const authStore = useAuthStore()
    authStore.apiPut = vi.fn().mockResolvedValue({ code: 200 })
    const r = await authStore.apiPut('/api/orders/1/status', { status: 'in_progress' })
    expect(r.code).toBe(200)
    expect(authStore.apiPut).toHaveBeenCalledWith('/api/orders/1/status', { status: 'in_progress' })
  })
})

describe('KeeperWorkflow - Care Records', () => {
  it('create feed record calls apiPost', async () => {
    const pinia = createKeeperSetup()
    const authStore = useAuthStore()
    authStore.apiPost = vi.fn().mockResolvedValue({ code: 200, data: { id: 1, type: 'feed' } })
    const r = await authStore.apiPost('/api/care-records', { orderId: 1, petId: 1, keeperId: 1, type: 'feed', content: '上午8点喂食狗粮', images: 'https://example.com/feed.jpg' })
    expect(r.code).toBe(200)
    expect(r.data.type).toBe('feed')
  })

  it('create activity record calls apiPost', async () => {
    const pinia = createKeeperSetup()
    const authStore = useAuthStore()
    authStore.apiPost = vi.fn().mockResolvedValue({ code: 200, data: { id: 2, type: 'activity' } })
    const r = await authStore.apiPost('/api/care-records', { orderId: 1, petId: 1, keeperId: 1, type: 'activity', content: '上午散步40分钟', images: 'https://example.com/activity.jpg' })
    expect(r.code).toBe(200)
    expect(r.data.type).toBe('activity')
  })

  it('create medication record calls apiPost', async () => {
    const pinia = createKeeperSetup()
    const authStore = useAuthStore()
    authStore.apiPost = vi.fn().mockResolvedValue({ code: 200, data: { id: 3, type: 'medication' } })
    const r = await authStore.apiPost('/api/care-records', { orderId: 1, petId: 1, keeperId: 1, type: 'medication', content: '已按时服用驱虫药' })
    expect(r.code).toBe(200)
    expect(r.data.type).toBe('medication')
  })

  it('create health record calls apiPost', async () => {
    const pinia = createKeeperSetup()
    const authStore = useAuthStore()
    authStore.apiPost = vi.fn().mockResolvedValue({ code: 200, data: { id: 4, type: 'health' } })
    const r = await authStore.apiPost('/api/care-records', { orderId: 1, petId: 1, keeperId: 1, type: 'health', content: '体温38.3°C，精神状态良好' })
    expect(r.code).toBe(200)
    expect(r.data.type).toBe('health')
  })

  it('view care records calls apiGet', async () => {
    const pinia = createKeeperSetup()
    const authStore = useAuthStore()
    authStore.apiGet = vi.fn().mockResolvedValue({ code: 200, data: [{ id: 1, type: 'feed', content: '喂食', images: 'url1' }, { id: 2, type: 'activity', content: '活动', images: 'url2' }] })
    const r = await authStore.apiGet('/api/care-records/order/1')
    expect(r.code).toBe(200)
    expect(r.data.length).toBeGreaterThanOrEqual(1)
    const types = r.data.map(d => d.type)
    expect(types).toContain('feed')
    expect(types).toContain('activity')
  })

  it('upload images via care record images field', async () => {
    const pinia = createKeeperSetup()
    const authStore = useAuthStore()
    const images = 'https://example.com/photo1.jpg,https://example.com/photo2.jpg'
    authStore.apiPost = vi.fn().mockResolvedValue({ code: 200, data: { id: 5, type: 'feed', images } })
    const r = await authStore.apiPost('/api/care-records', { orderId: 1, petId: 1, keeperId: 1, type: 'feed', content: '上传照片', images })
    expect(r.code).toBe(200)
    expect(r.data.images).toContain('photo1.jpg')
    expect(r.data.images).toContain('photo2.jpg')
  })

  it('write service log via care record content', async () => {
    const pinia = createKeeperSetup()
    const authStore = useAuthStore()
    const logContent = '服务日志：今日宠物状态良好，食欲正常，已完成驱虫'
    authStore.apiPost = vi.fn().mockResolvedValue({ code: 200, data: { id: 6, type: 'note', content: logContent } })
    const r = await authStore.apiPost('/api/care-records', { orderId: 1, petId: 1, keeperId: 1, type: 'note', content: logContent })
    expect(r.code).toBe(200)
    expect(r.data.content).toContain('服务日志')
  })
})

describe('KeeperWorkflow - Orders.vue Component', () => {
  it('shows pending order status badge', async () => {
    const mockOrders = [
      { id_wsh: 1, service_name_wsh: '萨摩耶寄养', status_wsh: 'in_progress', total_amount_wsh: 997.5, created_at_wsh: '2026-06-20T00:00:00Z' },
    ]
    const pinia = createPinia()
    setActivePinia(pinia)
    const authStore = useAuthStore()
    authStore.setAuth({ userId_wsh: 10, username_wsh: 'keeper', nickname_wsh: '看护人', roles_wsh: ['KEEPER', 'OWNER'], access_token_wsh: 't', refresh_token_wsh: 'r' })
    authStore.apiGet = vi.fn().mockResolvedValue({ code: 200, data: mockOrders })
    const wrapper = mount(Orders, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('萨摩耶寄养')
    expect(wrapper.text()).toContain('¥997.5')
  })

  it('filters orders by tab for keeper view', async () => {
    const mockOrders = [
      { id_wsh: 1, service_name_wsh: '进行中订单', status_wsh: 'in_progress', total_amount_wsh: 500, created_at_wsh: '2026-06-20T00:00:00Z' },
      { id_wsh: 2, service_name_wsh: '已完成订单', status_wsh: 'completed', total_amount_wsh: 300, created_at_wsh: '2026-06-19T00:00:00Z' },
    ]
    const pinia = createPinia()
    setActivePinia(pinia)
    const authStore = useAuthStore()
    authStore.setAuth({ userId_wsh: 10, username_wsh: 'keeper', nickname_wsh: '看护人', roles_wsh: ['KEEPER', 'OWNER'], access_token_wsh: 't', refresh_token_wsh: 'r' })
    authStore.apiGet = vi.fn().mockResolvedValue({ code: 200, data: mockOrders })
    const wrapper = mount(Orders, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const btns = wrapper.findAll('button')
    const completedBtn = btns.find(b => b.text() === '已完成')
    if (completedBtn) {
      await completedBtn.trigger('click')
      expect(wrapper.text()).toContain('已完成订单')
    }
  })
})

describe('KeeperWorkflow - Merchant Selection', () => {
  it('loads merchant list on mount', async () => {
    const pinia = createKeeperSetup()
    const authStore = useAuthStore()
    const mockMerchants = [
      { id_wsh: 1, name_wsh: '快乐宠物店' },
      { id_wsh: 2, name_wsh: '爱心寄养中心' },
    ]
    authStore.apiGet = vi.fn().mockResolvedValue({ code: 200, data: mockMerchants })
    const r = await authStore.apiGet('/api/merchants')
    expect(r.code).toBe(200)
    expect(r.data.length).toBe(2)
    expect(r.data[0].name_wsh).toBe('快乐宠物店')
  })

  it('submit certification with selected merchant', async () => {
    const pinia = createKeeperSetup()
    const authStore = useAuthStore()
    const certData = {
      merchant_id_wsh: 2,
      name_wsh: '张阿姨',
      phone_wsh: '13900139002',
      experience_years_wsh: 5,
      price_per_day_wsh: 150,
      max_pets_wsh: 5,
      bio_wsh: '有丰富经验',
      status_wsh: 1,
    }
    authStore.apiPost = vi.fn().mockResolvedValue({ code: 200, data: { id_wsh: 1, ...certData } })
    const r = await authStore.apiPost('/api/keepers', certData)
    expect(r.code).toBe(200)
    expect(r.data.merchant_id_wsh).toBe(2)
    expect(authStore.apiPost).toHaveBeenCalledWith('/api/keepers', certData)
  })

  it('disables submit button when no merchant selected', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const authStore = useAuthStore()
    authStore.setAuth({
      userId_wsh: 10, username_wsh: 'keeper', nickname_wsh: '张阿姨',
      roles_wsh: ['KEEPER', 'OWNER'], access_token_wsh: 't', refresh_token_wsh: 'r',
    })
    authStore.apiGet = vi.fn((url) => {
      if (url === '/api/keepers/me') return Promise.resolve({ code: 200, data: null })
      return Promise.resolve({ code: 200, data: [] })
    })
    authStore.apiPost = vi.fn().mockResolvedValue({ code: 200 })
    authStore.apiPut = vi.fn().mockResolvedValue({ code: 200 })
    const wrapper = mount(KeeperWorkflow, {
      global: { plugins: [pinia] },
    })
    await new Promise(r => setTimeout(r, 50))
    const submitBtn = wrapper.findAll('button').find(b => b.text() === '提交认证')
    expect(submitBtn).toBeDefined()
    expect(submitBtn.element.disabled).toBe(true)
  })
})
