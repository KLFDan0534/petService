import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import MerchantDetail from '@/views/user/MerchantDetail.vue'
import { useAppStore } from '@/stores/app'

const state = vi.hoisted(() => ({ push: vi.fn() }))

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '1' } }),
  useRouter: () => ({ push: state.push }),
}))

vi.mock('@/domain/MerchantDomain', () => ({
  getFullProfile: vi.fn(),
}))
import { getFullProfile } from '@/domain/MerchantDomain'

let pinia

// domain 层本来会返回一大包数据，这里只留用例关心的几块，其余的给空值凑数
function mockProfile({ merchant, services = [], hours = [], ratings = [] }) {
  getFullProfile.mockResolvedValue({
    merchant,
    services,
    hours,
    ratings,
    qualifications: [],
    stats: {},
    dayLabels: {},
  })
}

function mountDetail() {
  return mount(MerchantDetail, {
    global: {
      plugins: [pinia],
      stubs: { LoadingSpinner: true, EmptyState: true, Star: true },
    },
  })
}

function merchant({ storeStatus, futureBooking = 1 }) {
  return {
    id_wsh: 1,
    name_wsh: '测试宠物店',
    address_wsh: '测试地址',
    status_wsh: 1,
    store_status_wsh: storeStatus,
    future_booking_enabled_wsh: futureBooking,
    owner_keeper_id_wsh: 2,
  }
}

// 这几条问的都是同一件事：门店当时开着还是歇着、允不允许提前约，按钮该不该点得动
describe('MerchantDetail.vue future booking', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    pinia = createPinia()
    setActivePinia(pinia)
  })

  // 门店这会儿关着门，但允许提前预约，按钮就得能点，文案上还要写明白"休息中·可预约"
  it('keeps the booking button enabled with closed-store wording when store is closed but future booking is on', async () => {
    mockProfile({
      merchant: merchant({ storeStatus: 0 }),
      services: [{ id_wsh: 1, name_wsh: '寄养', price_wsh: 100, unit_wsh: '天' }],
    })

    const wrapper = mountDetail()
    await flushPromises()

    const button = wrapper.find('.service-action button')
    expect(button.exists()).toBe(true)
    expect(button.attributes('disabled')).toBeUndefined()
    expect(button.text()).toContain('休息中·可预约')
  })

  it('shows 预约 wording when the store is open and future booking is on', async () => {
    mockProfile({
      merchant: merchant({ storeStatus: 1 }),
      services: [{ id_wsh: 1, name_wsh: '寄养', price_wsh: 100, unit_wsh: '天' }],
    })

    const wrapper = mountDetail()
    await flushPromises()

    const button = wrapper.find('.service-action button')
    expect(button.text()).toContain('预约')
    expect(button.attributes('disabled')).toBeUndefined()
  })

  // 不允许提前预约就一律锁死，不管门店当时开着还是关着，开关只看这一个字段
  it('disables the booking button when future booking is off, regardless of store status', async () => {
    mockProfile({
      merchant: merchant({ storeStatus: 1, futureBooking: 0 }),
      services: [{ id_wsh: 1, name_wsh: '寄养', price_wsh: 100, unit_wsh: '天' }],
    })

    const wrapper = mountDetail()
    await flushPromises()

    const button = wrapper.find('.service-action button')
    expect(button.attributes('disabled')).toBeDefined()
  })

  // 点预约要去产品详情页并带上 book=1，以前那套直接下单的老地址不能再用
  it('F-NAV-003: booking routes to the product detail /services/{id}?book=1 without legacy order query', async () => {
    mockProfile({
      merchant: merchant({ storeStatus: 1 }),
      services: [{ id_wsh: 7, name_wsh: '寄养', price_wsh: 100, unit_wsh: '天' }],
    })

    const wrapper = mountDetail()
    await flushPromises()

    const button = wrapper.find('.service-action button')
    await button.trigger('click')

    expect(state.push).toHaveBeenCalledWith({ path: '/services/7', query: { book: '1' } })
    expect(JSON.stringify(state.push.mock.calls)).not.toContain('/orders')
    expect(JSON.stringify(state.push.mock.calls)).not.toContain('create')
  })
})
