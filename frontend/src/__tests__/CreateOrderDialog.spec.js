import { describe, it, expect, vi, beforeEach } from 'vitest'
import { shallowMount, flushPromises } from '@vue/test-utils'
import CreateOrderDialog from '@/components/order/CreateOrderDialog.vue'

const state = vi.hoisted(() => ({
  app: { addToast: vi.fn(), showLoginPrompt: false, loginRedirectPath: '' },
  serviceVersion: 'v1',
  servicePrice: 88,
  serviceUnit: 'night',
  serviceName: '真实服务',
  merchantId: 10,
  keeperCalls: [],
  detailCalls: [],
  coupons: [],
  couponQuotes: [],
}))

const merchant = { id_wsh: 10, name_wsh: 'M1', address_wsh: '测试路1号', latitude_wsh: 31.24, longitude_wsh: 121.48 }
function detailData() {
  return {
    id_wsh: 5,
    name_wsh: state.serviceName,
    price_wsh: state.servicePrice,
    unit_wsh: state.serviceUnit,
    service_version_wsh: state.serviceVersion,
    merchant_id_wsh: state.merchantId,
  }
}

vi.mock('@/api/pet', () => ({
  getPets: vi.fn(() => Promise.resolve({ code: 200, data: [{ id_wsh: 1, name_wsh: '豆豆' }] })),
}))
vi.mock('@/api/merchant', () => ({
  getMerchants: vi.fn(() => Promise.resolve({ code: 200, data: [merchant] })),
  getMerchant: vi.fn(() => Promise.resolve({ code: 200, data: merchant })),
}))
vi.mock('@/api/service', () => ({
  getServiceDetail: vi.fn(() => {
    const holder = {}
    state.detailCalls.push(holder)
    return new Promise(resolve => { holder.resolve = resolve })
  }),
  getServiceAvailability: vi.fn((serviceId, from, to, keeperId) => {
    const holder = {}
    state.keeperCalls.push({ serviceId, from, to, keeperId, holder })
    return new Promise((resolve, reject) => { holder.resolve = resolve; holder.reject = reject })
  }),
}))
vi.mock('@/api/order', () => ({
  createOrder: vi.fn(),
  // 金额一律由服务端报价返回；mock 只给出固定报价，不在前端按单价/折扣计算。
  previewOrder: vi.fn((payload) => {
    const quantity = Number(payload?.quantity_wsh || 1)
    return Promise.resolve({
      code: 200,
      data: {
        billing_unit_wsh: state.serviceUnit,
        quantity_wsh: quantity,
        unit_price_wsh: Number(state.servicePrice),
        total_amount_wsh: '88.00',
        long_stay_discount_wsh: '0.00',
        coupon_discount_wsh: '0.00',
        membership_discount_wsh: '0.00',
        platform_subsidy_wsh: '0.00',
        discount_wsh: '0.00',
        final_amount_wsh: '88.00',
        start_date_wsh: payload?.start_date_wsh || null,
        end_date_wsh: payload?.end_date_wsh || null,
        delivery_time_wsh: payload?.delivery_time_wsh || payload?.start_time_wsh || null,
        pickup_time_wsh: payload?.pickup_time_wsh || null,
      },
    })
  }),
}))
vi.mock('@/api/coupon', () => ({
  getAvailableCoupons: vi.fn(() => Promise.resolve({ code: 200, data: state.coupons })),
  quoteCoupon: vi.fn((data) => {
    state.couponQuotes.push({ userCouponId: data.user_coupon_id_wsh })
    return Promise.resolve({ code: 200, data: null })
  }),
}))
vi.mock('@/api/membership', () => ({
  quoteMembershipOrderDiscount: vi.fn(() => Promise.resolve({ code: 200, data: null })),
}))
vi.mock('@/utils/profileRequirements', () => ({
  ensureProfileRequirement: vi.fn(() => Promise.resolve(true)),
  PROFILE_ACTIONS: { CREATE_ORDER: 'create_order' },
}))
vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({}),
}))
vi.mock('@/stores/app', () => ({
  useAppStore: () => state.app,
}))
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
}))
vi.mock('@/components/common/AmapAddressPicker.vue', () => ({
  default: { name: 'AmapAddressPicker', template: '<input class="amap-picker" />' },
}))
vi.mock('@/composables/useAmapLocation', () => ({
  getCurrentAddress: vi.fn(() => Promise.resolve({ latitude_wsh: 31.23, longitude_wsh: 121.47, address_wsh: '当前位置' })),
}))

async function settle() {
  await flushPromises()
  await flushPromises()
}

function dateStr(offsetDays) {
  const d = new Date()
  d.setDate(d.getDate() + offsetDays)
  const pad = value => String(value).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function makeDay(offsetDays, slots) {
  const date = dateStr(offsetDays)
  return { date_wsh: date, bookable_wsh: true, windows_wsh: [{ slots_wsh: slots.map(t => `${date}T${t}:00`) }] }
}

async function pickSelect(wrapper, optionText, value) {
  for (const select of wrapper.findAll('select')) {
    const options = select.findAll('option')
    if (options.some(option => option.text().includes(optionText))) {
      await select.setValue(value)
      return
    }
  }
  throw new Error(`select with option "${optionText}" not found`)
}

async function pickSelectByValue(wrapper, value) {
  for (const select of wrapper.findAll('select')) {
    if (select.findAll('option').some(option => option.element.value === value)) {
      await select.setValue(value)
      return
    }
  }
  const debug = wrapper.findAll('select').map((select, index) => `${index}: [${select.findAll('option').map(o => o.element.value).join(' | ')}]`)
  throw new Error(`select with option value "${value}" not found\n${debug.join('\n')}`)
}

function couponSelectOf(wrapper) {
  return wrapper.findAll('select').find(s =>
    s.findAll('option').some(o => ['不使用优惠券', '暂无优惠券', '正在加载优惠券...'].includes(o.text())))
}

async function mountDialog(props = {}) {
  const wrapper = shallowMount(CreateOrderDialog, {
    props,
    global: {
      stubs: {
        // AppDialog 为共享弹窗外壳，shallowMount 需渲染其插槽以暴露表单内容
        AppDialog: {
          name: 'AppDialog',
          template: '<div class="app-dialog-stub"><slot /><slot name="footer" /></div>',
        },
      },
    },
  })
  await settle()
  if (state.detailCalls.length > 0) {
    const latest = state.detailCalls[state.detailCalls.length - 1]
    latest.resolve({ code: 200, data: detailData() })
  }
  await settle()
  return wrapper
}

function submitButton(wrapper) {
  return wrapper.findAll('button').find(button => button.text() === '确认下单')
}

describe('CreateOrderDialog booking', () => {
  beforeEach(() => {
    state.serviceVersion = 'v1'
    state.servicePrice = 88
    state.serviceUnit = 'night'
    state.serviceName = '真实服务'
    state.merchantId = 10
    state.keeperCalls.length = 0
    state.detailCalls.length = 0
    state.coupons = []
    state.couponQuotes.length = 0
    vi.clearAllMocks()
  })

  it('F-BKG-006: reloads service by ID and does not trust prop name/price/merchant', async () => {
    const wrapper = await mountDialog({
      visible: true,
      initialServiceId: '5',
      initialServiceName: '伪造名称',
      initialPrice: '999',
      initialMerchantId: '777',
      initialKeeperId: '42',
    })

    const { getServiceDetail } = await import('@/api/service')
    expect(getServiceDetail).toHaveBeenCalledWith('5')

    // 服务模式下商家是只读展示，不是 select
    expect(wrapper.find('.readonly-field').exists()).toBe(true)
    expect(wrapper.text()).toContain('M1')
    expect(wrapper.text()).toContain('真实服务')
    expect(wrapper.text()).not.toContain('伪造名称')

    // 负责人由商家/系统分配，弹窗不再提供选人入口
    const hasKeeperSelect = wrapper.findAll('select').some(s =>
      s.findAll('option').some(o => o.text().includes('K1') || o.text().includes('K2')))
    expect(hasKeeperSelect).toBe(false)
  })

  it('F-BKG-014: coupon select is disabled and shows 暂无优惠券 when user has no coupons', async () => {
    state.coupons = []
    const wrapper = await mountDialog({ visible: true, initialServiceId: '5' })

    await pickSelect(wrapper, '豆豆', '1')
    await settle()

    const couponSelect = couponSelectOf(wrapper)
    expect(couponSelect).toBeTruthy()
    expect(couponSelect.attributes('disabled')).toBeDefined()
    expect(couponSelect.text()).toContain('暂无优惠券')
    expect(couponSelect.text()).not.toContain('不使用优惠券')
  })

  it('F-BKG-015: auto-selects the first available coupon returned by backend and quotes it', async () => {
    state.servicePrice = 88
    state.serviceUnit = 'night'
    // 自动勾选只按后端可用券列表顺序，前端不估算优惠金额；列表首张为券1（八折券）
    state.coupons = [
      { id_wsh: 201, name_wsh: '八折券', type_wsh: 'percent', discount_rate_wsh: 0.8, max_discount_amount_wsh: 50, threshold_amount_wsh: 0 },
      { id_wsh: 202, name_wsh: '满减券', type_wsh: 'amount', discount_amount_template_wsh: 10, threshold_amount_wsh: 50 },
    ]
    const wrapper = await mountDialog({ visible: true, initialServiceId: '5' })

    await pickSelect(wrapper, '豆豆', '1')
    await settle()
    expect(state.keeperCalls.length).toBeGreaterThanOrEqual(1)
    state.keeperCalls[state.keeperCalls.length - 1].holder.resolve({ code: 200, data: { days_wsh: [makeDay(1, ['10:00']), makeDay(2, ['10:00'])] } })
    await settle()

    await wrapper.findAll('input[type="date"]')[0].setValue(dateStr(1))
    await settle()
    await pickSelectByValue(wrapper, `${dateStr(1)}T10:00:00`)
    // 单宠物按夜寄养：离店日期由夜数推导（默认 1 晚 = 送达日 +1），是只读框，不再手动填接回日期
    expect(wrapper.find('[data-testid="checkout-date"]').attributes('disabled')).toBeDefined()
    await pickSelectByValue(wrapper, `${dateStr(2)}T10:00:00`)
    await settle()

    const couponSelect = couponSelectOf(wrapper)
    expect(couponSelect.element.value).toBe('201')
    expect(couponSelect.text()).toContain('八折券')
    expect(couponSelect.text()).toContain('满减券')
    // 券的抵扣由后端预览计算：断言最后一次询价确实带上了自动选中的券
    const { previewOrder } = await import('@/api/order')
    const quotedWithCoupon = previewOrder.mock.calls.filter(([payload]) => Number(payload?.user_coupon_id_wsh) === 201)
    expect(quotedWithCoupon.length).toBeGreaterThan(0)
  })

  it('F-BKG-008: double submit sends one POST', async () => {
    const { createOrder } = await import('@/api/order')
    createOrder.mockReturnValue(new Promise(() => {}))

    const wrapper = await mountDialog({ visible: true, initialServiceId: '5' })

    await pickSelect(wrapper, '豆豆', '1')
    await settle()
    state.keeperCalls[state.keeperCalls.length - 1].holder.resolve({ code: 200, data: { days_wsh: [makeDay(1, ['10:00']), makeDay(2, ['10:00'])] } })
    await settle()

    await wrapper.findAll('input[type="date"]')[0].setValue(dateStr(1))
    await settle()
    await pickSelectByValue(wrapper, `${dateStr(1)}T10:00:00`)
    await pickSelectByValue(wrapper, `${dateStr(2)}T10:00:00`)

    const nameInput = wrapper.find('input[placeholder="联系人姓名"]')
    const phoneInput = wrapper.find('input[placeholder="联系人手机号"]')
    await nameInput.setValue('张三')
    await phoneInput.setValue('13800000000')

    const button = submitButton(wrapper)
    await button.trigger('click')
    await button.trigger('click')

    expect(createOrder).toHaveBeenCalledTimes(1)
  })

  it('F-BKG-009: PRICE_CHANGED reloads product/quote and requires reconfirmation', async () => {
    const { createOrder } = await import('@/api/order')
    const { getServiceDetail } = await import('@/api/service')
    createOrder.mockRejectedValue({ response: { data: { errorCode: 'PRICE_CHANGED' } } })

    const wrapper = await mountDialog({ visible: true, initialServiceId: '5' })

    await pickSelect(wrapper, '豆豆', '1')
    await settle()
    expect(state.keeperCalls.length).toBeGreaterThanOrEqual(1)
    state.keeperCalls[state.keeperCalls.length - 1].holder.resolve({ code: 200, data: { days_wsh: [makeDay(1, ['10:00', '14:00']), makeDay(2, ['10:00'])] } })
    await settle()

    await wrapper.findAll('input[type="date"]')[0].setValue(dateStr(1))
    await settle()
    await pickSelectByValue(wrapper, `${dateStr(1)}T10:00:00`)
    // 单宠物按夜寄养：离店日期由夜数推导（只读），不再手动填接回日期
    await pickSelectByValue(wrapper, `${dateStr(2)}T10:00:00`)
    await settle()
    await wrapper.find('input[placeholder="联系人姓名"]').setValue('张三')
    await wrapper.find('input[placeholder="联系人手机号"]').setValue('13800000000')

    await submitButton(wrapper).trigger('click')
    await settle()

    expect(state.app.addToast).toHaveBeenCalledWith(expect.stringContaining('价格'), 'error')
    expect(getServiceDetail.mock.calls.length).toBeGreaterThanOrEqual(2)

    const firstPayload = createOrder.mock.calls[0][0]
    expect(firstPayload.service_version_wsh).toBe('v1')

    state.serviceVersion = 'v2'
    state.servicePrice = 99
    expect(state.detailCalls.length).toBe(2)
    state.detailCalls[1].resolve({ code: 200, data: detailData() })
    await settle()

    expect(wrapper.findAll('input[type="date"]')[0].element.value).toBe('')
    expect(submitButton(wrapper).exists()).toBe(true)

    expect(state.keeperCalls.length).toBeGreaterThanOrEqual(2)
    state.keeperCalls[state.keeperCalls.length - 1].holder.resolve({ code: 200, data: { days_wsh: [makeDay(1, ['10:00', '14:00']), makeDay(2, ['10:00'])] } })
    await settle()

    await wrapper.findAll('input[type="date"]')[0].setValue(dateStr(1))
    await settle()
    await pickSelectByValue(wrapper, `${dateStr(1)}T10:00:00`)
    // 单宠物按夜寄养：离店日期由夜数推导（只读），不再手动填接回日期
    await pickSelectByValue(wrapper, `${dateStr(2)}T10:00:00`)
    await settle()

    createOrder.mockResolvedValue({ code: 200, data: { id_wsh: 9 } })
    await submitButton(wrapper).trigger('click')
    await settle()

    const secondPayload = createOrder.mock.calls[1][0]
    expect(secondPayload.service_version_wsh).toBe('v2')
    expect(wrapper.emitted('created')[0][0].id_wsh).toBe(9)
  })

  it('F-BKG-019: 送达地址固定为门店地址且不可编辑', async () => {
    const wrapper = await mountDialog({ visible: true, initialServiceId: '5' })

    // 地址自动取门店地址，只读展示，客户不能再选送达地点
    const address = wrapper.find('[data-testid="delivery-address"]')
    expect(address.exists()).toBe(true)
    expect(address.attributes('readonly')).toBeDefined()
    expect(address.element.value).toBe('测试路1号')
    // 地图选点与距离提示均已下线
    expect(wrapper.find('.amap-picker').exists()).toBe(false)
    expect(wrapper.find('.delivery-distance').exists()).toBe(false)
  })

  it('F-BKG-013: success response without an order id closes defensively without emitting created', async () => {
    const { createOrder } = await import('@/api/order')
    createOrder.mockResolvedValue({ code: 200, data: {} })

    const wrapper = await mountDialog({ visible: true, initialServiceId: '5' })

    await pickSelect(wrapper, '豆豆', '1')
    await settle()
    state.keeperCalls[state.keeperCalls.length - 1].holder.resolve({ code: 200, data: { days_wsh: [makeDay(1, ['10:00']), makeDay(2, ['10:00'])] } })
    await settle()

    await wrapper.findAll('input[type="date"]')[0].setValue(dateStr(1))
    await settle()
    await pickSelectByValue(wrapper, `${dateStr(1)}T10:00:00`)
    await pickSelectByValue(wrapper, `${dateStr(2)}T10:00:00`)
    await wrapper.find('input[placeholder="联系人姓名"]').setValue('张三')
    await wrapper.find('input[placeholder="联系人手机号"]').setValue('13800000000')

    await submitButton(wrapper).trigger('click')
    await settle()

    expect(wrapper.emitted('created')).toBeUndefined()
    expect(wrapper.emitted('close')).toBeTruthy()
    expect(state.app.addToast).toHaveBeenCalledWith(expect.stringContaining('订单已提交'), 'warning')
  })
})

describe('CreateOrderDialog 预约窗口', () => {
  beforeEach(() => {
    state.serviceVersion = 'v1'
    state.servicePrice = 88
    state.serviceUnit = 'night'
    state.serviceName = '真实服务'
    state.merchantId = 10
    state.keeperCalls.length = 0
    state.detailCalls.length = 0
    state.coupons = []
    state.couponQuotes.length = 0
    vi.clearAllMocks()
  })

  it('F-BKG-016: 窗口由服务端 booking_window_days_wsh 决定（默认 91），输入框 max = 今天+90', async () => {
    const wrapper = await mountDialog({ visible: true, initialServiceId: '5' })
    await pickSelect(wrapper, '豆豆', '1')
    await settle()
    expect(state.keeperCalls.length).toBeGreaterThanOrEqual(1)
    // 请求范围先用请求上界（今天 + 365，服务端会收敛到实际窗口）
    const firstCall = state.keeperCalls[state.keeperCalls.length - 1]
    expect(firstCall.from).toBe(dateStr(0))
    expect(firstCall.to).toBe(dateStr(365))
    firstCall.holder.resolve({ code: 200, data: { booking_window_days_wsh: 91, days_wsh: [makeDay(1, ['10:00'])] } })
    await settle()

    const dateInputs = wrapper.findAll('input[type="date"]')
    // 单宠物按夜寄养：只有入住日期可编辑；离店日期由夜数推导，是只读框（data-testid="checkout-date"）
    expect(dateInputs.length).toBe(1)
    expect(dateInputs[0].attributes('max')).toBe(dateStr(90))
    expect(wrapper.find('[data-testid="checkout-date"]').attributes('disabled')).toBeDefined()
  })
})
