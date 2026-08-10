import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import CreateOrderDialog from './CreateOrderDialog.vue'
import { getPets } from '@/api/pet'
import { createOrder } from '@/api/order'
import { getMerchants } from '@/api/merchant'
import { getKeepersByMerchant } from '@/api/keeper'
import { getService } from '@/api/service'
import { getAvailableCoupons, quoteCoupon } from '@/api/coupon'
import { quoteMembershipOrderDiscount } from '@/api/membership'
import { useAppStore } from '@/stores/app'

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
    pinia = createPinia()
    setActivePinia(pinia)
    getPets.mockResolvedValue({ code: 200, data: [{ id_wsh: 1, name_wsh: '小白' }] })
    getService.mockResolvedValue({ code: 200, data: null })
    getAvailableCoupons.mockResolvedValue({ code: 200, data: [] })
    quoteCoupon.mockResolvedValue({ code: 200, data: null })
    quoteMembershipOrderDiscount.mockResolvedValue({ code: 200, data: null })
  })

  it('lists merchants that have an approved in-service keeper regardless of presence', async () => {
    mockMerchants([
      merchant(1, '营业商家'),
      merchant(2, '无资质商家'),
      merchant(3, '休息商家'),
      merchant(4, '离职商家'),
    ])
    getKeepersByMerchant.mockImplementation(async (merchantId) => ({
      code: 200,
      data: {
        1: [keeper(11, { status: 1, qualificationStatus: 'approved' })],
        2: [keeper(22, { status: 1, qualificationStatus: 'pending' })],
        3: [keeper(33, { status: 3, qualificationStatus: 'approved' })],
        4: [keeper(44, { status: 5, qualificationStatus: 'approved' })],
      }[merchantId] || [],
    }))

    const wrapper = mountDialog()
    await flushPromises()

    const merchantOptions = wrapper.findAll('select').at(1).findAll('option').map(option => option.text())
    expect(merchantOptions).toContain('营业商家')
    expect(merchantOptions).toContain('休息商家')
    expect(merchantOptions).not.toContain('无资质商家')
    expect(merchantOptions).not.toContain('离职商家')
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
      data: [keeper(11, { status: 6, qualificationStatus: 'approved' })],
    })

    const wrapper = mountDialog()
    await flushPromises()

    const merchantSelect = wrapper.findAll('select').at(1)
    expect(merchantSelect.element.disabled).toBe(true)
    expect(merchantSelect.text()).toContain('暂无可接单商家')
    expect(wrapper.text()).toContain('当前没有满足接单条件的商家，请稍后再试')
  })

  it('maps a stable FUTURE_BOOKING_DISABLED error code to an actionable toast', async () => {
    mockMerchants([merchant(1, '可接单商家')])
    getKeepersByMerchant.mockResolvedValue({
      code: 200,
      data: [keeper(11, { status: 3, qualificationStatus: 'approved' })],
    })
    createOrder.mockResolvedValue({
      code: 400,
      errorCode: 'FUTURE_BOOKING_DISABLED',
      message: '商家未开放未来预约',
      data: null,
    })

    const wrapper = mountDialog()
    await flushPromises()
    await submitValidForm(wrapper)
    await flushPromises()

    const appStore = useAppStore()
    expect(appStore.toasts.some(t => t.message === '该商家当前未开放未来预约')).toBe(true)
    expect(appStore.toasts.some(t => t.message === '商家未开放未来预约')).toBe(false)
  })

  it('maps a stable CAPACITY_EXCEEDED error code to an actionable toast', async () => {
    mockMerchants([merchant(1, '可接单商家')])
    getKeepersByMerchant.mockResolvedValue({
      code: 200,
      data: [keeper(11, { status: 1, qualificationStatus: 'approved' })],
    })
    createOrder.mockResolvedValue({
      code: 400,
      errorCode: 'CAPACITY_EXCEEDED',
      message: '看护者排班与已有订单冲突',
      data: null,
    })

    const wrapper = mountDialog()
    await flushPromises()
    await submitValidForm(wrapper)
    await flushPromises()

    const appStore = useAppStore()
    expect(appStore.toasts.some(t => t.message === '该时段看护人已预约满，请更换时段')).toBe(true)
  })
})

let pinia

function mountDialog(props = {}) {
  return mount(CreateOrderDialog, {
    props: { visible: true, ...props },
    global: {
      plugins: [pinia],
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

async function submitValidForm(wrapper) {
  const selects = wrapper.findAll('select')
  const petSelect = selects.at(0)
  const merchantSelect = selects.at(1)
  const keeperSelect = selects.at(2)
  await petSelect.setValue(1)
  await merchantSelect.setValue(1)
  await flushPromises()
  await keeperSelect.setValue(11)
  await flushPromises()

  const inputs = wrapper.findAll('input')
  const datetime = inputs.filter(i => i.attributes('type') === 'datetime-local')
  const tomorrow = toLocalDateTime(new Date(Date.now() + 86400000))
  const dayAfter = toLocalDateTime(new Date(Date.now() + 2 * 86400000))
  await datetime.at(0).setValue(tomorrow)
  await datetime.at(1).setValue(dayAfter)

  const addresses = wrapper.findAll('input.address-stub')
  await addresses.at(0).setValue('测试地址')

  const emergencyName = wrapper.findAll('input').filter(i => i.attributes('placeholder') === '联系人姓名').at(0)
  await emergencyName.setValue('张三')
  const emergencyPhone = wrapper.findAll('input').filter(i => i.attributes('placeholder') === '联系人手机号').at(0)
  await emergencyPhone.setValue('13800000000')

  await flushPromises()
  const submitButton = wrapper.findAll('button[type="button"]').find(b => b.text().includes('确认下单'))
  if (!submitButton) return
  await submitButton.trigger('click')
  await flushPromises()
}

function toLocalDateTime(date) {
  const pad = value => String(value).padStart(2, '0')
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate()),
  ].join('-') + `T${pad(date.getHours())}:${pad(date.getMinutes())}`
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
