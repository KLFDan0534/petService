import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import CreateOrderDialog from './CreateOrderDialog.vue'
import { getPets } from '@/api/pet'
import { getMerchants } from '@/api/merchant'
import { getKeepersByMerchant } from '@/api/keeper'
import { getService } from '@/api/service'
import { getAvailableCoupons, quoteCoupon } from '@/api/coupon'
import { quoteMembershipOrderDiscount } from '@/api/membership'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
}))

vi.mock('@/utils/profileRequirements', () => ({
  PROFILE_ACTIONS: { CREATE_ORDER: 'create-order' },
  ensureProfileRequirement: vi.fn(() => Promise.resolve(true)),
}))

vi.mock('@/api/pet', () => ({ getPets: vi.fn() }))
vi.mock('@/api/merchant', () => ({ getMerchants: vi.fn() }))
vi.mock('@/api/keeper', () => ({ getKeepersByMerchant: vi.fn() }))
vi.mock('@/api/service', () => ({ getService: vi.fn() }))
vi.mock('@/api/order', () => ({ createOrder: vi.fn() }))
vi.mock('@/api/coupon', () => ({
  getAvailableCoupons: vi.fn(),
  quoteCoupon: vi.fn(),
}))
vi.mock('@/api/membership', () => ({
  quoteMembershipOrderDiscount: vi.fn(),
}))

describe('CreateOrderDialog.vue', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
    setActivePinia(createPinia())
    getPets.mockResolvedValue({ code: 200, data: [{ id_wsh: 1, name_wsh: '小白' }] })
    getService.mockResolvedValue({ code: 200, data: null })
    getAvailableCoupons.mockResolvedValue({ code: 200, data: [] })
    quoteCoupon.mockResolvedValue({ code: 200, data: null })
    quoteMembershipOrderDiscount.mockResolvedValue({ code: 200, data: null })
  })

  it('only lists merchants that have an approved online keeper', async () => {
    mockMerchants([
      merchant(1, '合格商家'),
      merchant(2, '无资质商家'),
      merchant(3, '离线商家'),
    ])
    getKeepersByMerchant.mockImplementation(async (merchantId) => ({
      code: 200,
      data: {
        1: [keeper(11, { status: 1, qualificationStatus: 'approved' })],
        2: [keeper(22, { status: 1, qualificationStatus: 'pending' })],
        3: [keeper(33, { status: 3, qualificationStatus: 'approved' })],
      }[merchantId] || [],
    }))

    const wrapper = mountDialog()
    await flushPromises()

    const merchantOptions = wrapper.findAll('select').at(1).findAll('option').map(option => option.text())
    expect(merchantOptions).toContain('合格商家')
    expect(merchantOptions).not.toContain('无资质商家')
    expect(merchantOptions).not.toContain('离线商家')
    expect(wrapper.text()).toContain('仅展示有合格看护人的商家')
  })

  it('clears an initial merchant when that merchant has no orderable keeper', async () => {
    mockMerchants([
      merchant(1, '可接单商家'),
      merchant(2, '不可接单商家'),
    ])
    getKeepersByMerchant.mockImplementation(async (merchantId) => ({
      code: 200,
      data: merchantId === 1
        ? [keeper(11, { status: 1, qualificationStatus: 'approved' })]
        : [keeper(22, { status: 1, qualificationStatus: 'rejected' })],
    }))

    const wrapper = mountDialog({ initialMerchantId: '2' })
    await flushPromises()

    const merchantSelect = wrapper.findAll('select').at(1)
    expect(merchantSelect.element.value).toBe('')
    expect(merchantSelect.text()).toContain('可接单商家')
    expect(merchantSelect.text()).not.toContain('不可接单商家')
  })

  it('shows an actionable empty state when no merchant can accept orders', async () => {
    mockMerchants([merchant(1, '暂不可接单商家')])
    getKeepersByMerchant.mockResolvedValue({
      code: 200,
      data: [keeper(11, { status: 3, qualificationStatus: 'approved' })],
    })

    const wrapper = mountDialog()
    await flushPromises()

    const merchantSelect = wrapper.findAll('select').at(1)
    expect(merchantSelect.element.disabled).toBe(true)
    expect(merchantSelect.text()).toContain('暂无可接单商家')
    expect(wrapper.text()).toContain('当前没有满足接单条件的商家，请稍后再试')
  })
})

function mountDialog(props = {}) {
  return mount(CreateOrderDialog, {
    props: { visible: true, ...props },
    global: {
      plugins: [createPinia()],
      stubs: {
        AmapAddressPicker: {
          props: ['modelValue'],
          emits: ['update:modelValue', 'update:source'],
          template: '<input class="address-stub" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
        },
      },
    },
  })
}

function mockMerchants(data) {
  getMerchants.mockResolvedValue({ code: 200, data })
}

function merchant(id, name) {
  return { id_wsh: id, name_wsh: name, address_wsh: `${name}地址` }
}

function keeper(id, { status, qualificationStatus }) {
  return {
    id_wsh: id,
    name_wsh: `看护人${id}`,
    status_wsh: status,
    price_per_day_wsh: 120,
    qualifications_wsh: [{ status_wsh: qualificationStatus }],
  }
}
