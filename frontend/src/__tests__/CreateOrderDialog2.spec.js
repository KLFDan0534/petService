import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import CreateOrderDialog from '@/components/order/CreateOrderDialog.vue'
import { getPets } from '@/api/pet'
import { createOrder, previewOrder } from '@/api/order'
import { getMerchants, getMerchant } from '@/api/merchant'
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
vi.mock('@/api/merchant', () => ({ getMerchants: vi.fn(), getMerchant: vi.fn() }))
vi.mock('@/api/service', () => ({ getServiceDetail: vi.fn(), getServiceAvailability: vi.fn() }))
vi.mock('@/api/order', () => ({
  createOrder: vi.fn(),
  previewOrder: vi.fn(),
}))
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
    getMerchant.mockResolvedValue({ code: 200, data: merchant(1, '默认商家') })
    getServiceDetail.mockResolvedValue({ code: 200, data: null })
    getServiceAvailability.mockResolvedValue({ code: 200, data: { days_wsh: [] } })
    getAvailableCoupons.mockResolvedValue({ code: 200, data: [] })
    // 金额一律由服务端报价返回；mock 按数量给出固定报价，不在前端按单价计算。
    previewOrder.mockImplementation(async (payload) => {
      const quantity = Number(payload?.quantity_wsh || 1)
      const quoteByQuantity = { 1: '30.00', 2: '60.00' }
      const total = quoteByQuantity[quantity] || '30.00'
      return {
        code: 200,
        data: {
          billing_unit_wsh: payload?.billing_unit_wsh || 'night',
          quantity_wsh: quantity,
          unit_price_wsh: 30,
          total_amount_wsh: total,
          long_stay_discount_wsh: '0.00',
          coupon_discount_wsh: '0.00',
          membership_discount_wsh: '0.00',
          platform_subsidy_wsh: '0.00',
          discount_wsh: '0.00',
          final_amount_wsh: total,
          start_date_wsh: payload?.start_date_wsh || null,
          end_date_wsh: payload?.end_date_wsh || null,
          delivery_time_wsh: payload?.delivery_time_wsh || payload?.start_time_wsh || null,
          pickup_time_wsh: payload?.pickup_time_wsh || null,
        },
      }
    })
    quoteCoupon.mockResolvedValue({ code: 200, data: null })
    quoteMembershipOrderDiscount.mockResolvedValue({ code: 200, data: null })
  })

  it('maps a stable FUTURE_BOOKING_DISABLED error code to an actionable toast', async () => {
    mockMerchants([merchant(1, '可接单商家')])
    mockServiceForBooking()
    createOrder.mockResolvedValue({
      code: 400,
      errorCode: 'FUTURE_BOOKING_DISABLED',
      message: '商家未开放未来预约',
      data: null,
    })

    const wrapper = mountDialog({ initialServiceId: '7' })
    await flushPromises()
    await submitValidForm(wrapper)
    await flushPromises()

    const appStore = useAppStore()
    expect(appStore.toasts.some(t => t.message === '该商家当前未开放未来预约')).toBe(true)
    expect(appStore.toasts.some(t => t.message === '商家未开放未来预约')).toBe(false)
  })

  it('maps a stable CAPACITY_EXCEEDED error code to an actionable toast', async () => {
    mockMerchants([merchant(1, '可接单商家')])
    mockServiceForBooking()
    createOrder.mockResolvedValue({
      code: 400,
      errorCode: 'CAPACITY_EXCEEDED',
      message: '看护者排班与已有订单冲突',
      data: null,
    })

    const wrapper = mountDialog({ initialServiceId: '7' })
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
        unit_wsh: 'night',
        booking_mode_wsh: 'interval',
        duration_minutes_wsh: 1440,
      },
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
        unit_wsh: 'night',
        booking_mode_wsh: 'interval',
        duration_minutes_wsh: 1440,
      },
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
    await flushPromises()
    const dateInputs = wrapper.findAll('input').filter(i => i.attributes('type') === 'date')
    await dateInputs.at(0).setValue(tomorrow)
    await flushPromises()
    await findSelectWithOption(wrapper, makeSlot(new Date(Date.now() + 86400000))).setValue(makeSlot(new Date(Date.now() + 86400000)))
    // 按夜寄养：离店日 = 入住日 + 1 夜，正好落在休息日（不可预约）=> 接回槽位为空，提交被拦截
    expect(wrapper.find('[data-testid="checkout-date"]').element.value).toBe(restDay)
    await flushPromises()
    await wrapper.find('input[placeholder="联系人姓名"]').setValue('张三')
    await wrapper.find('input[placeholder="联系人手机号"]').setValue('13800000000')

    const submitButton = wrapper.findAll('button[type="button"]').find(b => b.text().includes('确认下单'))
    await submitButton.trigger('click')
    await flushPromises()

    const appStore = useAppStore()
    // 单宠物模式下接回日无可用槽位 => 接回时间无法补全，提交被拦截
    expect(appStore.toasts.some(t => t.message === '请补全宠物、商家、送达时间和接回时间')).toBe(true)
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

  it('F-BKG-UNIT-002 hour 服务按数量提交且金额取自服务端报价', async () => {
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

  it('F-BKG-UNIT-003 night 服务使用连续区间模式且 payload 包含 billing_unit=night', async () => {
    mockMerchants([merchant(1, '服务商家')])
    getServiceDetail.mockResolvedValue({
      code: 200,
      data: {
        id_wsh: 7,
        name_wsh: '标准寄养',
        price_wsh: 199,
        merchant_id_wsh: 1,
        service_version_wsh: 'v1',
        unit_wsh: 'night',
        booking_mode_wsh: 'interval',
        duration_minutes_wsh: 1440,
      },
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
    expect(payload.billing_unit_wsh).toBe('night')
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

  it('night 订单的接回日期由入住日 + 夜数推导，接回槽位取该日窗口', async () => {
    mockMerchants([merchant(1, '服务商家')])
    getServiceDetail.mockResolvedValue({
      code: 200,
      data: {
        id_wsh: 7,
        name_wsh: '标准寄养',
        price_wsh: 199,
        merchant_id_wsh: 1,
        service_version_wsh: 'v1',
        unit_wsh: 'night',
        booking_mode_wsh: 'interval',
        duration_minutes_wsh: 1440,
      },
    })
    const tomorrow = toDateOnly(new Date(Date.now() + 86400000))
    const dayAfter = toDateOnly(new Date(Date.now() + 2 * 86400000))
    const deliverySlot = tomorrow + 'T14:00:00'
    const pickupSlot = dayAfter + 'T14:00:00'
    getServiceAvailability.mockResolvedValue({
      code: 200,
      data: {
        days_wsh: [
          { date_wsh: tomorrow, bookable_wsh: true, windows_wsh: [{ slots_wsh: [deliverySlot] }] },
          { date_wsh: dayAfter, bookable_wsh: true, windows_wsh: [{ slots_wsh: [pickupSlot] }] },
        ],
      },
    })

    const wrapper = mountDialog({ initialServiceId: '7' })
    await flushPromises()

    const petSelect = findSelectByText(wrapper, '小白')
    await petSelect.setValue(1)
    await flushPromises()

    const dates = wrapper.findAll('input').filter(i => i.attributes('type') === 'date')
    await dates.at(0).setValue(tomorrow)
    await flushPromises()
    await findSelectWithOption(wrapper, deliverySlot).setValue(deliverySlot)
    await flushPromises()

    // 离店日期是只读推导值：入住日 + 1 夜 = 后天（单宠物模式为只读输入框）
    const checkoutDate = wrapper.find('[data-testid="checkout-date"]')
    expect(checkoutDate.element.value).toBe(dayAfter)
    expect(checkoutDate.attributes('disabled')).toBeDefined()
    // 接回槽位来自离店日当天窗口
    const pickupSelects = wrapper.findAll('select')
      .filter(s => Array.from(s.element.options).some(o => o.value === pickupSlot))
    expect(pickupSelects.length).toBeGreaterThan(0)
    const pickupOptions = Array.from(pickupSelects.at(-1).element.options).map(o => o.value).filter(Boolean)
    expect(pickupOptions).toContain(pickupSlot)
    expect(pickupOptions).not.toContain(deliverySlot)
  })

  it('reopening the dialog resets the night count to 1', async () => {
    mockMerchants([merchant(1, '服务商家')])
    mockServiceForBooking()
    const wrapper = mountDialog({ initialServiceId: '7' })
    await flushPromises()

    const nightsInput = wrapper.find('[data-testid="nights-input"]')
    await nightsInput.setValue(5)
    expect(nightsInput.element.value).toBe('5')

    await wrapper.setProps({ visible: false })
    await flushPromises()
    await wrapper.setProps({ visible: true })
    await flushPromises()

    expect(wrapper.find('[data-testid="nights-input"]').element.value).toBe('1')
  })

  it('falls back to fetching the merchant when the list misses it', async () => {
    mockMerchants([])
    mockServiceForBooking()
    getMerchant.mockResolvedValue({ code: 200, data: merchant(1, '补拉商家') })

    const wrapper = mountDialog({ initialServiceId: '7' })
    await flushPromises()

    expect(getMerchant).toHaveBeenCalledWith(1)
    expect(wrapper.find('[data-testid="delivery-address"]').element.value).toBe('补拉商家地址')

    await submitValidForm(wrapper)
    expect(createOrder).toHaveBeenCalledTimes(1)
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

// 服务模式：详情 + 可约日期/槽位（默认 night，离店日由夜数推导）
function mockServiceForBooking({ unit = 'night' } = {}) {
  getServiceDetail.mockResolvedValue({
    code: 200,
    data: {
      id_wsh: 7,
      name_wsh: '上门寄养',
      price_wsh: 199,
      merchant_id_wsh: 1,
      merchant_name_wsh: '服务商家',
      merchant_address_wsh: '服务商家地址',
      service_version_wsh: '2026-08-11T10:00:00',
      unit_wsh: unit,
      booking_mode_wsh: unit === 'night' ? 'interval' : 'slot',
      duration_minutes_wsh: unit === 'night' ? 1440 : 60,
    },
  })
  const tomorrow = toDateOnly(new Date(Date.now() + 86400000))
  const dayAfter = toDateOnly(new Date(Date.now() + 2 * 86400000))
  getServiceAvailability.mockResolvedValue({
    code: 200,
    data: {
      booking_window_days_wsh: 91,
      days_wsh: [
        { date_wsh: tomorrow, bookable_wsh: true, windows_wsh: [{ slots_wsh: [makeSlot(new Date(Date.now() + 86400000))] }] },
        { date_wsh: dayAfter, bookable_wsh: true, windows_wsh: [{ slots_wsh: [makeSlot(new Date(Date.now() + 2 * 86400000))] }] },
      ],
    },
  })
}

async function submitValidForm(wrapper) {
  const petSelect = findSelectByText(wrapper, '小白')
  await petSelect.setValue(1)
  await flushPromises()

  const inputs = wrapper.findAll('input')
  // 送达日期可编辑；按夜服务下离店日期由夜数推导（只读），时间槽位一律按选项值定位。
  const dates = inputs.filter(i => i.attributes('type') === 'date')
  const deliverySlotValue = makeSlot(new Date(Date.now() + 86400000))
  const pickupSlotValue = makeSlot(new Date(Date.now() + 2 * 86400000))
  await dates.at(0).setValue(toDateOnly(new Date(Date.now() + 86400000)))
  await flushPromises()
  await findSelectWithOption(wrapper, deliverySlotValue).setValue(deliverySlotValue)
  await findSelectWithOption(wrapper, pickupSlotValue).setValue(pickupSlotValue)

  const addresses = wrapper.findAll('input.address-stub')
  if (addresses.length > 0) await addresses.at(0).setValue('测试地址')

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

  // 负责人由系统分配，不再有看护人下拉
  let slotIdx = 1
  if (!isServiceMode) {
    await selects.at(1).setValue(1)
    await flushPromises()
    slotIdx = 2
  }

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
  // 到店交接要求门店有坐标，缺坐标下单会被拦
  return { id_wsh: id, name_wsh: name, address_wsh: `${name}地址`, latitude_wsh: 31.24, longitude_wsh: 121.48 }
}

