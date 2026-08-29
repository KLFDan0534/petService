import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Orders from '@/views/user/Orders.vue'
import CreateOrderDialog from '@/components/order/CreateOrderDialog.vue'

const state = vi.hoisted(() => ({
  route: { query: {} },
  router: { push: vi.fn(), replace: vi.fn() },
  app: { addToast: vi.fn() },
  orders: { code: 200, data: [] },
}))

vi.mock('vue-router', () => ({
  useRoute: () => state.route,
  useRouter: () => state.router,
}))

vi.mock('@/stores/app', () => ({
  useAppStore: () => state.app,
}))

vi.mock('@/api/order', () => ({
  getOrders: vi.fn(() => Promise.resolve(state.orders)),
  cancelOrder: vi.fn(),
  confirmDelivered: vi.fn(),
}))

vi.mock('@/api/payment', () => ({
  createPayment: vi.fn(),
  executePayment: vi.fn(),
}))

vi.mock('@/api/wallet', () => ({
  createTip: vi.fn(),
}))

vi.mock('@/api/rating', () => ({
  createRating: vi.fn(),
  getMyRatingsByOrder: vi.fn(() => Promise.resolve({ code: 200, data: [] })),
}))

vi.mock('@/composables/useAmapLocation', () => ({
  getCurrentAddress: vi.fn(),
}))

function mountOrders() {
  return mount(Orders, {
    global: {
      stubs: {
        TipDialog: true,
        ReviewDialog: true,
        OrderCard: true,
        'el-icon': { template: '<span class="el-icon-stub"><slot /></span>' },
      },
    },
  })
}

describe('Orders page (F-NAV-005)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    state.route.query = {}
  })

  it('renders the order list without a generic 创建订单 button', async () => {
    const wrapper = mountOrders()
    await flushPromises()
    await flushPromises()

    expect(wrapper.text()).not.toContain('创建订单')
    expect(wrapper.findComponent(CreateOrderDialog).exists()).toBe(false)
  })

  it('ignores legacy ?create=true query and never opens the create dialog', async () => {
    state.route.query = { create: 'true', serviceId: '5', serviceName: '寄养' }
    const wrapper = mountOrders()
    await flushPromises()
    await flushPromises()

    expect(wrapper.findComponent(CreateOrderDialog).exists()).toBe(false)
    expect(state.router.replace).not.toHaveBeenCalled()
  })
})
