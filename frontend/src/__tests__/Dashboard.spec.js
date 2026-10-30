import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Dashboard from '@/views/user/Dashboard.vue'

// 首页依赖的路由、store 和各个接口都换成假数据，这样不用连后端也能把页面跑起来
const state = vi.hoisted(() => ({
  router: { push: vi.fn() },
  app: { addToast: vi.fn(), showLoginPrompt: false, loginRedirectPath: '' },
  auth: { isLoggedIn: true },
  services: {
    code: 200,
    data: {
      items_wsh: [{ id_wsh: 7, name_wsh: '寄养服务', price_wsh: 88, merchant_id_wsh: 1 }],
      total_wsh: 1,
      page_wsh: 1,
      size_wsh: 100,
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

vi.mock('@/api/service', () => ({
  getPublicServices: vi.fn(() => Promise.resolve(state.services)),
}))

// 公告接口返回的包装跟别的不一样，这里按 URL 单独给它一份，其余一律回空列表
vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(url => {
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

// 首页那个"立即预约"点了要跳到产品详情并带上 book=1，这组就盯着这一个行为
describe('Dashboard booking CTA (F-NAV-002)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('routes 立即预约 to /services/{id}?book=1 without legacy order query', async () => {
    const wrapper = mountDashboard()
    // 首页一进来会连着发好几个请求，一次 flush 只够推完第一轮，所以要等两轮才稳
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
