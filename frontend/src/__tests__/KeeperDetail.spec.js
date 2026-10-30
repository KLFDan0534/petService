import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import KeeperDetail from '@/views/user/KeeperDetail.vue'

// 路由、store、接口都先换成假对象，页面不用连后端也能跑起来
const state = vi.hoisted(() => ({
  route: { params: { id: '1' } },
  router: { push: vi.fn() },
  app: { addToast: vi.fn() },
  auth: { isLoggedIn: true },
  keeper: {
    code: 200,
    data: {
      id_wsh: 1,
      name_wsh: '小王',
      merchant_id_wsh: 2,
      merchant_name_wsh: '测试商家',
      avatar_wsh: '',
      price_per_day_wsh: 50,
      experience_years_wsh: 3,
      current_pets_wsh: 1,
      max_pets_wsh: 3,
      completion_rate_wsh: 0.9,
      qualifications: [],
    },
  },
  ratings: { code: 200, data: [] },
}))

vi.mock('vue-router', () => ({
  useRoute: () => state.route,
  useRouter: () => state.router,
}))

vi.mock('@/stores/app', () => ({
  useAppStore: () => state.app,
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({ get isLoggedIn() { return state.auth.isLoggedIn } }),
}))

vi.mock('@/composables/useKeeperDetail', () => ({
  useKeeperDetail: () => ({
    keeper: state.keeper.data,
    loading: false,
    loadKeeper: vi.fn(),
    ratingSummary: vi.fn(() => ({})),
    ratings: [],
    qualifications: [],
    avgScore: 0,
    onlineLabel: '',
    onlineBadge: '',
    load: vi.fn(),
  }),
}))

vi.mock('@/composables/useFavoriteState', () => ({
  useFavoriteState: () => ({
    isFavorited: { value: false },
    favoriteLoading: { value: false },
    favoriteToggling: { value: false },
    toggleFavoriteState: vi.fn(),
  }),
}))

vi.mock('@/api/rating', () => ({
  getRatings: vi.fn(() => Promise.resolve(state.ratings)),
}))

function mountDetail() {
  return mount(KeeperDetail, {
    global: {
      stubs: {
        LoadingSpinner: true,
        EmptyState: true,
        'el-icon': { template: '<span class="el-icon-stub"><slot /></span>' },
      },
    },
  })
}

// 看护员页面只负责把人介绍清楚，真下单要去他所在的商家页，这组就是在守这条线
describe('KeeperDetail page (F-NAV-004)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // 页面上所有"选择服务"按钮都该跳到这位看护员所在的商家，一个都不能漏到下单页去
  it('routes 选择服务 buttons to the keeper merchant page, never to /orders', async () => {
    const wrapper = mountDetail()
    await flushPromises()

    const buttons = wrapper.findAll('button').filter(b => b.text().includes('选择服务'))
    expect(buttons.length).toBeGreaterThan(0)
    for (const button of buttons) {
      await button.trigger('click')
    }

    expect(state.router.push).toHaveBeenCalledWith('/merchants/2')
    expect(state.router.push.mock.calls.join('|')).not.toContain('/orders')
    expect(state.router.push.mock.calls.join('|')).not.toContain('create')
  })

  it('routes 查看商家 to the merchant page', async () => {
    const wrapper = mountDetail()
    await flushPromises()

    const button = wrapper.findAll('button').find(b => b.text().includes('查看商家'))
    await button.trigger('click')

    expect(state.router.push).toHaveBeenCalledWith('/merchants/2')
  })

  // 页面上不该冒出"联系"这类私下沟通的入口，不然用户就绕过平台直接找人下单了
  it('does not offer a direct contact action that creates orders', async () => {
    const wrapper = mountDetail()
    await flushPromises()

    expect(wrapper.text()).not.toContain('联系')
  })
})
