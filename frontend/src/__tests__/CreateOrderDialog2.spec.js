import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import CreateOrderDialog from '@/components/order/CreateOrderDialog.vue'
import { getPets } from '@/api/pet'
import { createOrder } from '@/api/order'
import { getMerchants } from '@/api/merchant'
import { getKeepersByMerchant } from '@/api/keeper'
import { getServiceDetail, getServiceAvailability } from '@/api/service'
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
vi.mock('@/api/service', () => ({ getServiceDetail: vi.fn(), getServiceAvailability: vi.fn() }))
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
    getServiceDetail.mockResolvedValue({ code: 200, data: null })
    getServiceAvailability.mockResolvedValue({ code: 200, data: { days_wsh: [] } })
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

  it('sends service_id_wsh and service_version_wsh when entering from service detail', async () => {
    mockMerchants([merchant(1, '服务商家')])
    getServiceDetail.mockResolvedValue({
      code: 200,
      data: {
        id_wsh: 7,
        name_wsh: '上门寄养',
        price_wsh: 199,
        merchant_id_wsh: 1,
        service_version_wsh: '2026-08-11T10:00:00',
      },
    })
    getKeepersByMerchant.mockResolvedValue({
      code: 200,
      data: [keeper(11, { status: 1, qualificationStatus: 'approved' })],
    })
    const tomorrow = toDateOnly(new Date(Date.now() + 86400000))
    const dayAfter = toDateOnly(new Date(Date.now() + 2 * 86400000))
    getServiceAvailability.mockResolvedValue({
      code: 200,
      data: {
        days_wsh: [
          { date_wsh: tomorrow, bookable_wsh: true, windows_wsh: [{ slots_wsh: [makeSlot(new Date(Date.now() + 86400000))] }] },
          { date_wsh: dayAfter, bookable_wsh: true, windows_wsh: [{ slots_wsh: [makeSlot(new Date(Date.now() + 2 * 86400000))] }] },
        ],
      },
    })

    const wrapper = mountDialog({ initialServiceId: '7' })
    await flushPromises()

    const merchantSelect = wrapper.findAll('select').at(1)
    expect(wrapper.find('.readonly-field').exists()).toBe(true)
    expect(wrapper.text()).toContain('由当前服务指定')

    await submitValidForm(wrapper)
    await flushPromises()

    expect(createOrder).toHaveBeenCalledWith(expect.objectContaining({
      service_id_wsh: 7,
      service_version_wsh: '2026-08-11T10:00:00',
      delivery_time_wsh: `${toLocalDateTime(new Date(Date.now() + 86400000)).slice(0, 16)}:00`,
      pickup_time_wsh: `${toLocalDateTime(new Date(Date.now() + 2 * 86400000)).slice(0, 16)}:00`,
    }))
  })

  it('only offers bookable dates and their slots from availability', async () => {
    mockMerchants([merchant(1, '服务商家')])
    getServiceDetail.mockResolvedValue({
      code: 200,
      data: {
        id_wsh: 7,
        name_wsh: '上门寄养',
        price_wsh: 199,
        merchant_id_wsh: 1,
        service_version_wsh: '2026-08-11T10:00:00',
      },
    })
    getKeepersByMerchant.mockResolvedValue({
      code: 200,
      data: [keeper(11, { status: 1, qualificationStatus: 'approved' })],
    })
    const tomorrow = toDateOnly(new Date(Date.now() + 86400000))
    const restDay = toDateOnly(new Date(Date.now() + 2 * 86400000))
    getServiceAvailability.mockResolvedValue({
      code: 200,
      data: {
        days_wsh: [
          { date_wsh: tomorrow, bookable_wsh: true, windows_wsh: [{ slots_wsh: [makeSlot(new Date(Date.now() + 86400000))] }] },
          { date_wsh: restDay, bookable_wsh: false, reason_code_wsh: 'MERCHANT_REST_DAY', windows_wsh: [] },
        ],
      },
    })

    const wrapper = mountDialog({ initialServiceId: '7' })
    await flushPromises()
    await findSelectByText(wrapper, '看护人').setValue(11)
    await flushPromises()

    expect(wrapper.text()).toContain('仅展示可预约日期（1 天可约）')
    const dateInput = wrapper.findAll('input').filter(i => i.attributes('type') === 'date').at(0)
    await dateInput.setValue(tomorrow)
    await flushPromises()
    const slotSelect = findSelectWithOption(wrapper, makeSlot(new Date(Date.now() + 86400000)))
    expect(Array.from(slotSelect.element.options).map(o => o.text)).toContain(displayTime(new Date(Date.now() + 86400000)))

    await dateInput.setValue(restDay)
    await flushPromises()
    expect(findSelectByText(wrapper, '该日暂无可约时间').text()).toContain('该日暂无可约时间')
  })

  it('refetches availability and clears chosen times when keeper changes', async () => {
    mockMerchants([merchant(1, '服务商家')])
    getServiceDetail.mockResolvedValue({
      code: 200,
      data: {
        id_wsh: 7,
        name_wsh: '上门寄养',
        price_wsh: 199,
        merchant_id_wsh: 1,
        service_version_wsh: '2026-08-11T10:00:00',
      },
    })
    getKeepersByMerchant.mockResolvedValue({
      code: 200,
      data: [
        keeper(11, { status: 1, qualificationStatus: 'approved' }),
        keeper(22, { status: 1, qualificationStatus: 'approved' }),
      ],
    })
    const tomorrow = toDateOnly(new Date(Date.now() + 86400000))
    getServiceAvailability.mockResolvedValue({
      code: 200,
      data: {
        days_wsh: [
          { date_wsh: tomorrow, bookable_wsh: true, windows_wsh: [{ slots_wsh: [makeSlot(new Date(Date.now() + 86400000))] }] },
        ],
      },
    })

    const wrapper = mountDialog({ initialServiceId: '7' })
    await flushPromises()
    const keeperSelect = findSelectByText(wrapper, '看护人')
    await keeperSelect.setValue(11)
    await flushPromises()
    const dateInput = wrapper.findAll('input').filter(i => i.attributes('type') === 'date').at(0)
    await dateInput.setValue(tomorrow)
    await flushPromises()
    await findSelectWithOption(wrapper, makeSlot(new Date(Date.now() + 86400000))).setValue(makeSlot(new Date(Date.now() + 86400000)))
    await flushPromises()
    expect(dateInput.element.value).toBe(tomorrow)

    await keeperSelect.setValue(22)
    await flushPromises()

    const lastCall = getServiceAvailability.mock.calls.at(-1)
    expect(lastCall[3]).toBe(22)
    expect(dateInput.element.value).toBe('')
    expect(findSelectByText(wrapper, '请先选择送达日期').element.value).toBe('')
  })

  it('blocks submission when pickup has no available slot on the chosen date', async () => {
    mockMerchants([merchant(1, '服务商家')])
    getServiceDetail.mockResolvedValue({
      code: 200,
      data: {
        id_wsh: 7,
        name_wsh: '上门寄养',
        price_wsh: 199,
        merchant_id_wsh: 1,
        service_version_wsh: '2026-08-11T10:00:00',
      },
    })
    getKeepersByMerchant.mockResolvedValue({
      code: 200,
      data: [keeper(11, { status: 1, qualificationStatus: 'approved' })],
    })
    const tomorrow = toDateOnly(new Date(Date.now() + 86400000))
    const restDay = toDateOnly(new Date(Date.now() + 2 * 86400000))
    getServiceAvailability.mockResolvedValue({
      code: 200,
      data: {
        days_wsh: [
          { date_wsh: tomorrow, bookable_wsh: true, windows_wsh: [{ slots_wsh: [makeSlot(new Date(Date.now() + 86400000))] }] },
          { date_wsh: restDay, bookable_wsh: false, reason_code_wsh: 'MERCHANT_REST_DAY', windows_wsh: [] },
        ],
      },
    })

    const wrapper = mountDialog({ initialServiceId: '7' })
    await flushPromises()
    await findSelectByText(wrapper, '小白').setValue(1)
    await findSelectByText(wrapper, '看护人').setValue(11)
    await flushPromises()
    const dateInputs = wrapper.findAll('input').filter(i => i.attributes('type') === 'date')
    await dateInputs.at(0).setValue(tomorrow)
    await flushPromises()
    await findSelectWithOption(wrapper, makeSlot(new Date(Date.now() + 86400000))).setValue(makeSlot(new Date(Date.now() + 86400000)))
    // 接回日期选在休息日（不可预约）=> 提交被拦截
    await dateInputs.at(1).setValue(restDay)
    await flushPromises()
    await wrapper.find('input[placeholder="联系人姓名"]').setValue('张三')
    await wrapper.find('input[placeholder="联系人手机号"]').setValue('13800000000')

    const submitButton = wrapper.findAll('button[type="button"]').find(b => b.text().includes('确认下单'))
    await submitButton.trigger('click')
    await flushPromises()

    const appStore = useAppStore()
    // 批量行 UI 下接回日无可用槽位 => 该行无法补全，提交被拦截
    expect(appStore.toasts.some(t => t.message === '请为每只宠物补全送达/接回日期与时间')).toBe(true)
    expect(createOrder).not.toHaveBeenCalled()
  })

  // ---- A2 多单位预约：红灯测试 ----

  it('F-BKG-UNIT-001 session 服务使用槽位模式且 payload 包含 billing_unit/quantity/start_time', async () => {
    mockMerchants([merchant(1, '服务商家')])
    getServiceDetail.mockResolvedValue({
      code: 200,
      data: {
        id_wsh: 7,
        name_wsh: '上门遛狗',
        price_wsh: 88,
        merchant_id_wsh: 1,
        service_version_wsh: 'v1',
        unit_wsh: 'session',
        booking_mode_wsh: 'slot',
        duration_minutes_wsh: 60,
      },
    })
    getKeepersByMerchant.mockResolvedValue({
      code: 200,
      data: [keeper(11, { status: 1, qualificationStatus: 'approved' })],
    })
    const tomorrow = toDateOnly(new Date(Date.now() + 86400000))
    getServiceAvailability.mockResolvedValue({
      code: 200,
      data: {
        days_wsh: [
          { date_wsh: tomorrow, bookable_wsh: true, windows_wsh: [{ slots_wsh: [makeSlot(new Date(Date.now() + 86400000))] }] },
        ],
      },
    })

    const wrapper = mountDialog({ initialServiceId: '7' })
    await flushPromises()
    await submitSlotForm(wrapper)
    await flushPromises()

    expect(createOrder).toHaveBeenCalledTimes(1)
    const payload = createOrder.mock.calls[0][0]
    expect(payload.billing_unit_wsh).toBe('session')
    expect(payload.quantity_wsh).toBe(1)
    expect(Number(payload.expected_unit_price_wsh)).toBe(88)
    expect(payload.start_time_wsh).toBeTruthy()
    expect(payload.pickup_time_wsh).toBeFalsy()
  })

  it('F-BKG-UNIT-002 hour 服务按数量计价且预估金额为 数量*单价', async () => {
    mockMerchants([merchant(1, '服务商家')])
    getServiceDetail.mockResolvedValue({
      code: 200,
      data: {
        id_wsh: 7,
        name_wsh: '计时陪护',
        price_wsh: 30,
        merchant_id_wsh: 1,
        service_version_wsh: 'v1',
        unit_wsh: 'hour',
        booking_mode_wsh: 'slot',
        duration_minutes_wsh: 60,
      },
    })
    getKeepersByMerchant.mockResolvedValue({
      code: 200,
      data: [keeper(11, { status: 1, qualificationStatus: 'approved' })],
    })
    const tomorrow = toDateOnly(new Date(Date.now() + 86400000))
    getServiceAvailability.mockResolvedValue({
      code: 200,
      data: {
        days_wsh: [
          { date_wsh: tomorrow, bookable_wsh: true, windows_wsh: [{ slots_wsh: [makeSlot(new Date(Date.now() + 86400000))] }] },
        ],
      },
    })

    const wrapper = mountDialog({ initialServiceId: '7' })
    await flushPromises()
    await submitSlotForm(wrapper, { quantity: 2 })
    await flushPromises()

    expect(wrapper.find('.estimate').text()).toContain('60')
    expect(createOrder).toHaveBeenCalledTimes(1)
    const payload = createOrder.mock.calls[0][0]
    expect(payload.billing_unit_wsh).toBe('hour')
    expect(payload.quantity_wsh).toBe(2)
    expect(Number(payload.expected_unit_price_wsh)).toBe(30)
    expect(payload.start_time_wsh).toBeTruthy()
  })

  it('F-BKG-UNIT-003 day 服务继续使用日期区间模式且 payload 包含 billing_unit=day', async () => {
    mockMerchants([merchant(1, '服务商家')])
    getServiceDetail.mockResolvedValue({
      code: 200,
      data: {
        id_wsh: 7,
        name_wsh: '标准寄养',
        price_wsh: 199,
        merchant_id_wsh: 1,
        service_version_wsh: 'v1',
        unit_wsh: 'day',
        booking_mode_wsh: 'date_range',
        duration_minutes_wsh: 1440,
      },
    })
    getKeepersByMerchant.mockResolvedValue({
      code: 200,
      data: [keeper(11, { status: 1, qualificationStatus: 'approved' })],
    })
    const tomorrow = toDateOnly(new Date(Date.now() + 86400000))
    const dayAfter = toDateOnly(new Date(Date.now() + 2 * 86400000))
    getServiceAvailability.mockResolvedValue({
      code: 200,
      data: {
        days_wsh: [
          { date_wsh: tomorrow, bookable_wsh: true, windows_wsh: [{ slots_wsh: [makeSlot(new Date(Date.now() + 86400000))] }] },
          { date_wsh: dayAfter, bookable_wsh: true, windows_wsh: [{ slots_wsh: [makeSlot(new Date(Date.now() + 2 * 86400000))] }] },
        ],
      },
    })

    const wrapper = mountDialog({ initialServiceId: '7' })
    await flushPromises()
    await submitValidForm(wrapper)
    await flushPromises()

    expect(createOrder).toHaveBeenCalledTimes(1)
    const payload = createOrder.mock.calls[0][0]
    expect(payload.billing_unit_wsh).toBe('day')
    expect(payload.quantity_wsh).toBeGreaterThanOrEqual(1)
    expect(Number(payload.expected_unit_price_wsh)).toBe(199)
  })

  it('F-BKG-UNIT-004 UNSUPPORTED_SERVICE_UNIT 错误提示不再误报为按天预约', async () => {
    mockMerchants([merchant(1, '服务商家')])
    getServiceDetail.mockResolvedValue({
      code: 200,
      data: {
        id_wsh: 7,
        name_wsh: '上门遛狗',
        price_wsh: 88,
        merchant_id_wsh: 1,
        service_version_wsh: 'v1',
        unit_wsh: 'session',
        booking_mode_wsh: 'slot',
        duration_minutes_wsh: 60,
      },
    })
    getKeepersByMerchant.mockResolvedValue({
      code: 200,
      data: [keeper(11, { status: 1, qualificationStatus: 'approved' })],
    })
    const tomorrow = toDateOnly(new Date(Date.now() + 86400000))
    getServiceAvailability.mockResolvedValue({
      code: 200,
      data: {
        days_wsh: [
          { date_wsh: tomorrow, bookable_wsh: true, windows_wsh: [{ slots_wsh: [makeSlot(new Date(Date.now() + 86400000))] }] },
        ],
      },
    })
    createOrder.mockResolvedValue({
      code: 400,
      errorCode: 'UNSUPPORTED_SERVICE_UNIT',
      message: '不支持',
      data: null,
    })

    const wrapper = mountDialog({ initialServiceId: '7' })
    await flushPromises()
    await submitSlotForm(wrapper)
    await flushPromises()

    const appStore = useAppStore()
    const toast = appStore.toasts.find(t => t.message.includes('不支持') || t.message.includes('预约'))
    expect(toast).toBeTruthy()
    expect(toast.message).not.toContain('按天')
  })

  it('pickup slot options exclude slots not later than the same-day delivery slot', async () => {
    mockMerchants([merchant(1, '服务商家')])
    getServiceDetail.mockResolvedValue({
      code: 200,
      data: {
        id_wsh: 7,
        name_wsh: '标准寄养',
        price_wsh: 199,
        merchant_id_wsh: 1,
        service_version_wsh: 'v1',
        unit_wsh: 'day',
        booking_mode_wsh: 'date_range',
        duration_minutes_wsh: 1440,
      },
    })
    getKeepersByMerchant.mockResolvedValue({
      code: 200,
      data: [keeper(11, { status: 1, qualificationStatus: 'approved' })],
    })
    const tomorrow = toDateOnly(new Date(Date.now() + 86400000))
    const earlySlot = tomorrow + 'T09:00:00'
    const midSlot = tomorrow + 'T12:00:00'
    const lateSlot = tomorrow + 'T17:00:00'
    getServiceAvailability.mockResolvedValue({
      code: 200,
      data: {
        days_wsh: [
          { date_wsh: tomorrow, bookable_wsh: true, windows_wsh: [{ slots_wsh: [earlySlot, midSlot, lateSlot] }] },
        ],
      },
    })

    const wrapper = mountDialog({ initialServiceId: '7' })
    await flushPromises()

    // 批量行 UI：先选宠物与看护人，再选送达日期，送达时间选「中间槽位」
    const petSelect = findSelectByText(wrapper, '小白')
    await petSelect.setValue(1)
    await flushPromises()
    await findSelectByText(wrapper, '看护人').setValue(11)
    await flushPromises()

    const dates = wrapper.findAll('input').filter(i => i.attributes('type') === 'date')
    await dates.at(0).setValue(tomorrow)
    await flushPromises()
    await findSelectWithOption(wrapper, midSlot).setValue(midSlot)
    await flushPromises()

    // 接回日期选在同一天，接回时间下拉应只剩晚于送达的槽位
    await dates.at(1).setValue(tomorrow)
    await flushPromises()

    // 批量行 UI select 顺序：宠物、看护人、送达时间、接回时间
    // 送达下拉含 [early, mid, late]，接回下拉过滤后仅剩 [late]
    const allSelects = wrapper.findAll('select')
    const withLate = allSelects.filter(s => Array.from(s.element.options).some(o => o.value === lateSlot))
    const deliverySelect = withLate[0]
    const pickupSelect = withLate[1]
    expect(deliverySelect).toBeTruthy()
    expect(pickupSelect).toBeTruthy()
    const pickupSlotOptions = Array.from(pickupSelect.element.options).map(o => o.value).filter(Boolean)
    expect(pickupSlotOptions).toContain(lateSlot)
    expect(pickupSlotOptions).not.toContain(earlySlot)
    expect(pickupSlotOptions).not.toContain(midSlot)
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
  const isServiceMode = wrapper.find('.readonly-field').exists()
  const selects = wrapper.findAll('select')
  // 批量（day 服务）模式下看护人 select 位于宠物行之前，按选项文本定位宠物下拉
  const petSelect = findSelectByText(wrapper, '小白')
  await petSelect.setValue(1)
  await flushPromises()

  let keeperSelect
  if (isServiceMode) {
    keeperSelect = findSelectByText(wrapper, '看护人')
  } else {
    const merchantSelect = selects.at(1)
    await merchantSelect.setValue(1)
    await flushPromises()
    keeperSelect = selects.at(2)
  }
  await keeperSelect.setValue(11)
  await flushPromises()

  const inputs = wrapper.findAll('input')
  const datetime = inputs.filter(i => i.attributes('type') === 'datetime-local')
  if (datetime.length > 0) {
    const tomorrow = toLocalDateTime(new Date(Date.now() + 86400000))
    const dayAfter = toLocalDateTime(new Date(Date.now() + 2 * 86400000))
    await datetime.at(0).setValue(tomorrow)
    await datetime.at(1).setValue(dayAfter)
  } else {
    // 按天服务模式（批量行 UI）：宠物/看护人/槽位按选项定位，不依赖 DOM 顺序
    const dates = inputs.filter(i => i.attributes('type') === 'date')
    const deliverySlotValue = makeSlot(new Date(Date.now() + 86400000))
    const pickupSlotValue = makeSlot(new Date(Date.now() + 2 * 86400000))
    await dates.at(0).setValue(toDateOnly(new Date(Date.now() + 86400000)))
    await flushPromises()
    await findSelectWithOption(wrapper, deliverySlotValue).setValue(deliverySlotValue)
    await dates.at(1).setValue(toDateOnly(new Date(Date.now() + 2 * 86400000)))
    await flushPromises()
    await findSelectWithOption(wrapper, pickupSlotValue).setValue(pickupSlotValue)
  }

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

async function submitSlotForm(wrapper, options = {}) {
  const isServiceMode = wrapper.find('.readonly-field').exists()
  const selects = wrapper.findAll('select')
  await selects.at(0).setValue(1)
  await flushPromises()

  let keeperIdx
  if (isServiceMode) {
    keeperIdx = 1
  } else {
    await selects.at(1).setValue(1)
    await flushPromises()
    keeperIdx = 2
  }
  await selects.at(keeperIdx).setValue(11)
  await flushPromises()

  const slotIdx = keeperIdx + 1
  const tomorrow = toDateOnly(new Date(Date.now() + 86400000))
  const dateInput = wrapper.findAll('input').filter(i => i.attributes('type') === 'date').at(0)
  await dateInput.setValue(tomorrow)
  await flushPromises()
  await selects.at(slotIdx).setValue(makeSlot(new Date(Date.now() + 86400000)))
  await flushPromises()

  const qtyInput = wrapper.find('[data-testid="quantity-input"]')
  if (options.quantity && qtyInput.exists()) {
    await qtyInput.setValue(options.quantity)
    await flushPromises()
  }

  const addresses = wrapper.findAll('input.address-stub')
  if (addresses.length > 0) await addresses.at(0).setValue('测试地址')

  const emergencyName = wrapper.findAll('input').filter(i => i.attributes('placeholder') === '联系人姓名').at(0)
  if (emergencyName.exists()) await emergencyName.setValue('张三')
  const emergencyPhone = wrapper.findAll('input').filter(i => i.attributes('placeholder') === '联系人手机号').at(0)
  if (emergencyPhone.exists()) await emergencyPhone.setValue('13800000000')

  await flushPromises()
  const submitButton = wrapper.findAll('button[type="button"]').find(b => b.text().includes('确认下单'))
  if (!submitButton) return
  await submitButton.trigger('click')
  await flushPromises()
}

function findSelectByText(wrapper, text) {
  const select = wrapper.findAll('select').find(s =>
    s.findAll('option').some(o => o.text().includes(text)))
  if (!select) throw new Error(`select with option text "${text}" not found`)
  return select
}

function findSelectWithOption(wrapper, value) {
  const select = wrapper.findAll('select').find(s =>
    Array.from(s.element.options).some(o => o.value === value))
  if (!select) throw new Error(`select with option value "${value}" not found`)
  return select
}

function toLocalDateTime(date) {
  const pad = value => String(value).padStart(2, '0')
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate()),
  ].join('-') + `T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function toDateOnly(date) {
  return toLocalDateTime(date).slice(0, 10)
}

function makeSlot(date) {
  return toLocalDateTime(date).slice(0, 16) + ':00'
}

function displayTime(date) {
  return makeSlot(date).slice(11, 16)
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
