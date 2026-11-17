import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import UserLayout from '@/components/layout/UserLayout.vue'
import Profile from '@/views/user/Profile.vue'
import { useAuthStore } from '@/stores/auth'

const mocks = vi.hoisted(() => ({
  deleteCurrentUser: vi.fn(),
  getCurrentUser: vi.fn(),
  updateCurrentUser: vi.fn(),
  getUserStatistics: vi.fn(),
  getMyMerchant: vi.fn(),
  getMyKeeperApplication: vi.fn(),
  uploadFileToDirectory: vi.fn(),
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

// 个人中心已改为走具名 API（不再直接调用 utils/request），
// 因此这里 mock 对应的 api 模块，而不是按 URL mock request。
vi.mock('@/api/auth', () => ({
  deleteCurrentUser: mocks.deleteCurrentUser,
  getCurrentUser: mocks.getCurrentUser,
  updateCurrentUser: mocks.updateCurrentUser,
}))

vi.mock('@/api/statistics', () => ({
  getUserStatistics: mocks.getUserStatistics,
}))

vi.mock('@/api/merchant', () => ({
  getMyMerchant: mocks.getMyMerchant,
}))

vi.mock('@/api/keeper', () => ({
  getMyKeeperApplication: mocks.getMyKeeperApplication,
}))

vi.mock('@/api/file', () => ({
  uploadFileToDirectory: mocks.uploadFileToDirectory,
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

// 优惠券和会员这类入口不往全局导航里塞，只放在个人中心，这组就是在确认它们待的位置对不对
describe('优惠券和会员入口', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
    // 个人中心一进来会同时拉好几个接口，这里按 api 方法各回一份固定数据，省得每个用例都重写一遍
    const currentUser = {
      id_wsh: 1,
      username_wsh: 'owner',
      nickname_wsh: '宠物主人',
      roles_wsh: ['OWNER'],
    }
    mocks.getCurrentUser.mockResolvedValue({ code: 200, data: currentUser })
    mocks.getUserStatistics.mockResolvedValue({ code: 200, data: {} })
    mocks.getMyMerchant.mockResolvedValue({ code: 200, data: null })
    mocks.getMyKeeperApplication.mockResolvedValue({ code: 200, data: null })
    mocks.requestGet.mockImplementation((url) => {
      const responses = {
        '/users/me': currentUser,
        '/statistics/user': {},
        '/merchants/my': null,
        '/api/keepers/my-application': null,
      }
      return Promise.resolve({ data: { code: 200, data: responses[url] ?? null } })
    })
    mocks.getCustomerServiceApplications.mockResolvedValue({ code: 200, data: [] })
  })

  // 顶部导航谁都看得见，权益入口放进去太吵，所以这里断言它压根不出现
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

    // 页面改版后入口改叫 quick-row，还多了订单/服务两个通用入口，
    // 这里按路由把四个权益入口挑出来，文案和跳转地址仍要一一对应
    const benefitTos = ['/coupons', '/membership', '/tickets', '/complaints']
    const benefitLinks = wrapper.findAllComponents(RouterLinkStub).filter(link => benefitTos.includes(link.props('to')))
    expect(benefitLinks.map(link => link.text())).toEqual([
      expect.stringContaining('我的优惠券'),
      expect.stringContaining('会员中心'),
      expect.stringContaining('我的工单'),
      expect.stringContaining('我的投诉'),
    ])
    expect(benefitLinks.map(link => link.props('to'))).toEqual(benefitTos)
  })
})
