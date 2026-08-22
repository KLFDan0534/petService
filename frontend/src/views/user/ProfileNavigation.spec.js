import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import UserLayout from '@/components/layout/UserLayout.vue'
import Profile from './Profile.vue'
import { useAuthStore } from '@/stores/auth'

const mocks = vi.hoisted(() => ({
  deleteCurrentUser: vi.fn(),
  getCustomerServiceApplications: vi.fn(),
  requestGet: vi.fn(),
  requestPost: vi.fn(),
  requestPut: vi.fn(),
  routerPush: vi.fn(),
  routerReplace: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mocks.routerPush,
    replace: mocks.routerReplace,
  }),
}))

vi.mock('@/api/auth', () => ({
  deleteCurrentUser: mocks.deleteCurrentUser,
}))

vi.mock('@/api/merchantCustomerService', () => ({
  getMyCustomerServiceApplications: mocks.getCustomerServiceApplications,
}))

vi.mock('@/utils/request', () => ({
  default: {
    get: mocks.requestGet,
    post: mocks.requestPost,
    put: mocks.requestPut,
  },
}))

function createAuthenticatedPinia() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const authStore = useAuthStore()
  authStore.token = 'test-token'
  authStore.user = {
    id_wsh: 1,
    username_wsh: 'owner',
    nickname_wsh: '宠物主人',
    roles_wsh: ['OWNER'],
  }
  return pinia
}

describe('优惠券和会员入口', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
    mocks.requestGet.mockImplementation((url) => {
      const responses = {
        '/users/me': {
          id_wsh: 1,
          username_wsh: 'owner',
          nickname_wsh: '宠物主人',
          roles_wsh: ['OWNER'],
        },
        '/statistics/user': {},
        '/merchants/my': null,
        '/api/keepers/my-application': null,
      }
      return Promise.resolve({ data: { code: 200, data: responses[url] ?? null } })
    })
    mocks.getCustomerServiceApplications.mockResolvedValue({ code: 200, data: [] })
  })

  it('不在全局用户导航中展示入口', () => {
    const pinia = createAuthenticatedPinia()
    const wrapper = mount(UserLayout, {
      global: {
        plugins: [pinia],
        stubs: {
          MessageIndicator: true,
          RouterLink: RouterLinkStub,
          ThemeToggle: true,
        },
      },
    })

    const navigation = wrapper.get('.user-nav')
    expect(navigation.text()).not.toContain('优惠券')
    expect(navigation.text()).not.toContain('会员')
  })

  it('在个人中心展示两个权益入口并保留原路由', async () => {
    const pinia = createAuthenticatedPinia()
    const wrapper = mount(Profile, {
      global: {
        plugins: [pinia],
        stubs: {
          AvatarUpload: true,
          KeeperPanel: true,
          MerchantPanel: true,
          RouterLink: RouterLinkStub,
          StatisticsPanel: true,
          'el-icon': { template: '<span><slot /></span>' },
        },
      },
    })

    await flushPromises()

    const benefitLinks = wrapper.findAll('.benefit-entry')
    expect(benefitLinks.map(link => link.text())).toEqual([
      expect.stringContaining('我的优惠券'),
      expect.stringContaining('会员中心'),
      expect.stringContaining('我的工单'),
      expect.stringContaining('我的投诉'),
    ])
    expect(wrapper.findAllComponents(RouterLinkStub).map(link => link.props('to'))).toEqual([
      '/coupons',
      '/membership',
      '/tickets',
      '/complaints',
    ])
  })
})
