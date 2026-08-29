import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Dashboard from '@/views/user/Dashboard.vue'

const state = vi.hoisted(() => ({
  router: { push: vi.fn() },
  app: { addToast: vi.fn(), showLoginPrompt: false, loginRedirectPath: '' },
  auth: { isLoggedIn: true },
  services: {
    data: {
      code: 200,
      data: [{ id_wsh: 7, name_wsh: '寄养服务', price_wsh: 88, merchant_id_wsh: 1 }],
    },
  },
  banners: { data: { code: 200, data: [] } },
  merchants: { data: [] },
  keepers: { data: [] },
  ratings: { data: { code: 200, data: [] } },
}))

vi.mock('vue-router', () => ({
  useRouter: () => state.router,
}))

vi.mock('@/stores/app', () => ({
  useAppStore: () => state.app,
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({ get isLoggedIn() { return state.auth.isLoggedIn } }),
}))

vi.mock('@/api/keeper', () => ({
  getKeepers: vi.fn(() => Promise.resolve(state.keepers)),
}))

vi.mock('@/api/merchant', () => ({
  getMerchants: vi.fn(() => Promise.resolve(state.merchants)),
}))

vi.mock('@/api/rating', () => ({
  getRatings: vi.fn(() => Promise.resolve(state.ratings)),
}))

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(url => {
      if (url === '/services') return Promise.resolve(state.services)
      if (url === '/api/notices/active') return Promise.resolve(state.banners)
      return Promise.resolve({ data: { code: 200, data: [] } })
    }),
  },
}))

function mountDashboard() {
  return mount(Dashboard, {
    global: {
      stubs: {
        BannerCarousel: true,
        MediaWithFallback: true,
        'el-icon': { template: '<span class="el-icon-stub"><slot /></span>' },
      },
    },
  })
}

describe('Dashboard booking CTA (F-NAV-002)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('routes 立即预约 to /services/{id}?book=1 without legacy order query', async () => {
    const wrapper = mountDashboard()
    await flushPromises()
    await flushPromises()

    const cta = wrapper.find('.cta-primary')
    expect(cta.exists()).toBe(true)
    await cta.trigger('click')

    expect(state.router.push).toHaveBeenCalledWith({ path: '/services/7', query: { book: '1' } })
    expect(JSON.stringify(state.router.push.mock.calls)).not.toContain('/orders')
    expect(JSON.stringify(state.router.push.mock.calls)).not.toContain('create')
  })
})
