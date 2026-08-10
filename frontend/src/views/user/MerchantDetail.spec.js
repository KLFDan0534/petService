import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import MerchantDetail from './MerchantDetail.vue'
import { useAppStore } from '@/stores/app'

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '1' } }),
  useRouter: () => ({ push: vi.fn() }),
}))

vi.mock('@/domain/MerchantDomain', () => ({
  getFullProfile: vi.fn(),
}))
import { getFullProfile } from '@/domain/MerchantDomain'

let pinia

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
      stubs: { PageHero: true, LoadingSpinner: true, EmptyState: true, Star: true },
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

describe('MerchantDetail.vue future booking', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    pinia = createPinia()
    setActivePinia(pinia)
  })

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
})
