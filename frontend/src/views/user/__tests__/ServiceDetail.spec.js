import { describe, it, expect, vi, beforeEach } from 'vitest'
import { shallowMount, flushPromises } from '@vue/test-utils'
import ServiceDetail from '@/views/user/ServiceDetail.vue'
import CreateOrderDialog from '@/components/order/CreateOrderDialog.vue'

const state = vi.hoisted(() => ({
  loggedIn: true,
  profileOk: true,
  route: { params: { id: '1' }, query: {}, path: '/services/1' },
  router: { push: vi.fn(), replace: vi.fn() },
  app: { showLoginPrompt: false, loginRedirectPath: '', addToast: vi.fn(), closeLoginPrompt: vi.fn() },
  bookable: true,
  bookableReason: '',
}))

const makeService = () => ({
  id_wsh: 1,
  name_wsh: '寄养服务',
  price_wsh: 88,
  unit_wsh: '天',
  description_wsh: '描述',
  category_name_wsh: '寄养',
  merchant_id_wsh: 10,
  merchant_name_wsh: 'M1',
  service_rating_wsh: 4.5,
  service_rating_count_wsh: 2,
  bookable_wsh: state.bookable,
  bookable_reason_wsh: state.bookableReason,
  media_wsh: [{ url_wsh: 'http://x/1.jpg', is_cover_wsh: 1 }],
  service_version_wsh: 'v1',
})

vi.mock('@/api/service', () => ({
  getServiceDetail: vi.fn(() => Promise.resolve({ code: 200, data: makeService() })),
}))
vi.mock('@/api/rating', () => ({
  getRatings: vi.fn(() => Promise.resolve({ code: 200, data: [] })),
}))
vi.mock('@/services/merchantService', () => ({
  getById: vi.fn(() => Promise.resolve({ id_wsh: 10, name_wsh: 'M1', address_wsh: '测试路1号' })),
}))
vi.mock('@/services/keeperService', () => ({
  getByMerchant: vi.fn(() => Promise.resolve([])),
}))
vi.mock('@/utils/profileRequirements', () => ({
  ensureProfileRequirement: vi.fn(() => Promise.resolve(state.profileOk)),
  PROFILE_ACTIONS: { CREATE_ORDER: 'create_order' },
}))
vi.mock('@/stores/app', () => ({
  useAppStore: () => state.app,
}))
vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({ get isLoggedIn() { return state.loggedIn } }),
}))
vi.mock('vue-router', () => ({
  useRoute: () => state.route,
  useRouter: () => state.router,
}))

async function mountDetail() {
  const wrapper = shallowMount(ServiceDetail)
  await flushPromises()
  await flushPromises()
  return wrapper
}

function bookButtons(wrapper) {
  return wrapper.findAll('button').filter(b => b.text() === '立即预约')
}

describe('ServiceDetail booking dialog', () => {
  beforeEach(() => {
    state.loggedIn = true
    state.profileOk = true
    state.bookable = true
    state.bookableReason = ''
    state.route.query = {}
    state.route.params = { id: '1' }
    state.route.path = '/services/1'
    state.app.showLoginPrompt = false
    state.app.loginRedirectPath = ''
    vi.clearAllMocks()
  })

  it('F-BKG-001: logged-in click opens dialog without route change', async () => {
    const wrapper = await mountDetail()
    await bookButtons(wrapper)[0].trigger('click')

    expect(wrapper.findComponent(CreateOrderDialog).props('visible')).toBe(true)
    expect(state.router.push).not.toHaveBeenCalled()
    expect(state.app.showLoginPrompt).toBe(false)
  })

  it('F-BKG-002: cancel closes dialog and restores focus to 立即预约', async () => {
    const wrapper = await mountDetail()
    const focusSpies = bookButtons(wrapper).map(b => vi.spyOn(b.element, 'focus').mockImplementation(() => {}))

    await bookButtons(wrapper)[0].trigger('click')
    expect(wrapper.findComponent(CreateOrderDialog).props('visible')).toBe(true)

    wrapper.findComponent(CreateOrderDialog).vm.$emit('close')
    await flushPromises()

    expect(wrapper.findComponent(CreateOrderDialog).props('visible')).toBe(false)
    expect(focusSpies.some(spy => spy.mock.calls.length > 0)).toBe(true)
  })

  it('F-BKG-003: anonymous click opens login prompt and stores /services/{id}?book=1 only', async () => {
    state.loggedIn = false
    const wrapper = await mountDetail()
    await bookButtons(wrapper)[0].trigger('click')

    expect(state.app.showLoginPrompt).toBe(true)
    expect(state.app.loginRedirectPath).toBe('/services/1?book=1')
    expect(wrapper.findComponent(CreateOrderDialog).props('visible')).toBe(false)
    expect(state.router.push).not.toHaveBeenCalled()
  })

  it('F-BKG-004: login return with book=1 opens dialog once and clears the query', async () => {
    state.loggedIn = true
    state.route.query = { book: '1' }
    const wrapper = await mountDetail()

    expect(wrapper.findComponent(CreateOrderDialog).props('visible')).toBe(true)
    expect(state.router.replace).toHaveBeenCalledWith({ path: '/services/1', query: {} })
  })

  it('F-BKG-013: anonymous arrival with book=1 opens login prompt and clears the query', async () => {
    state.loggedIn = false
    state.route.query = { book: '1' }
    const wrapper = await mountDetail()

    expect(wrapper.findComponent(CreateOrderDialog).props('visible')).toBe(false)
    expect(state.app.showLoginPrompt).toBe(true)
    expect(state.app.loginRedirectPath).toBe('/services/1?book=1')
    expect(state.router.replace).toHaveBeenCalledWith({ path: '/services/1', query: {} })
  })

  it('F-BKG-005: login return with book=1 does not open dialog when profile requirement fails and keeps the query', async () => {
    state.loggedIn = true
    state.profileOk = false
    state.route.query = { book: '1' }
    const wrapper = await mountDetail()

    expect(wrapper.findComponent(CreateOrderDialog).props('visible')).toBe(false)
    expect(state.router.replace).not.toHaveBeenCalled()
    expect(state.route.query).toEqual({ book: '1' })
  })

  it('F-BKG-012: login return with unknown query keys is rejected and falls back safely', async () => {
    state.loggedIn = true
    state.route.query = { book: '1', evil: 'x' }
    const wrapper = await mountDetail()

    expect(wrapper.findComponent(CreateOrderDialog).props('visible')).toBe(false)
    expect(state.router.replace).not.toHaveBeenCalled()
  })

  it('F-BKG-005: failed profile requirement does not open dialog', async () => {
    state.profileOk = false
    const wrapper = await mountDetail()
    await bookButtons(wrapper)[0].trigger('click')

    expect(wrapper.findComponent(CreateOrderDialog).props('visible')).toBe(false)
    expect(state.router.push).not.toHaveBeenCalled()
  })

  it('F-BKG-010: created order routes to /orders/{id}', async () => {
    const wrapper = await mountDetail()
    await bookButtons(wrapper)[0].trigger('click')

    wrapper.findComponent(CreateOrderDialog).vm.$emit('created', { id_wsh: 9 })
    await flushPromises()

    expect(state.router.push).toHaveBeenCalledWith('/orders/9')
  })

  it('F-BKG-011: unsupported-unit detail click warns with an explanatory reason instead of opening the dialog', async () => {
    state.bookable = false
    state.bookableReason = 'UNSUPPORTED_SERVICE_UNIT'
    const wrapper = await mountDetail()

    expect(wrapper.text()).toContain('该服务计费方式暂不支持在线预约，请线下联系商家')
    expect(state.app.addToast).toHaveBeenCalledWith('该服务计费方式暂不支持在线预约，请线下联系商家', 'warning')

    await bookButtons(wrapper)[0].trigger('click')
    expect(wrapper.findComponent(CreateOrderDialog).props('visible')).toBe(false)
    expect(state.app.addToast).toHaveBeenCalledWith('该服务计费方式暂不支持在线预约，请线下联系商家', 'warning')
  })
})
