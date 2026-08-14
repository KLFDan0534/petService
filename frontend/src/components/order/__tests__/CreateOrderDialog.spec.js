import { describe, it, expect, vi, beforeEach } from 'vitest'
import { shallowMount, flushPromises } from '@vue/test-utils'
import CreateOrderDialog from '@/components/order/CreateOrderDialog.vue'

const state = vi.hoisted(() => ({
  app: { addToast: vi.fn(), showLoginPrompt: false, loginRedirectPath: '' },
  serviceVersion: 'v1',
  servicePrice: 88,
  serviceName: '真实服务',
  merchantId: 10,
  keeperCalls: [],
  detailCalls: [],
}))

const merchant = { id_wsh: 10, name_wsh: 'M1', address_wsh: '测试路1号' }
const keepers = [
  { id_wsh: 100, name_wsh: 'K1', price_per_day_wsh: 100, status_wsh: 1, qualifications_wsh: [{ status_wsh: 'approved' }], current_pets_wsh: 1, max_pets_wsh: 3 },
  { id_wsh: 101, name_wsh: 'K2', price_per_day_wsh: 120, status_wsh: 1, qualifications_wsh: [{ status_wsh: 'approved' }], current_pets_wsh: 0, max_pets_wsh: 2 },
]

function detailData() {
  return {
    id_wsh: 5,
    name_wsh: state.serviceName,
    price_wsh: state.servicePrice,
    service_version_wsh: state.serviceVersion,
    merchant_id_wsh: state.merchantId,
  }
}

vi.mock('@/api/pet', () => ({
  getPets: vi.fn(() => Promise.resolve({ code: 200, data: [{ id_wsh: 1, name_wsh: '豆豆' }] })),
}))
vi.mock('@/api/merchant', () => ({
  getMerchants: vi.fn(() => Promise.resolve({ code: 200, data: [merchant] })),
}))
vi.mock('@/api/keeper', () => ({
  getKeepersByMerchant: vi.fn(() => Promise.resolve({ code: 200, data: keepers })),
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
}))
vi.mock('@/api/coupon', () => ({
  getAvailableCoupons: vi.fn(() => Promise.resolve({ code: 200, data: [] })),
  quoteCoupon: vi.fn(() => Promise.resolve({ code: 200, data: null })),
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

async function mountDialog(props = {}) {
  const wrapper = shallowMount(CreateOrderDialog, { props })
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
    state.serviceName = '真实服务'
    state.merchantId = 10
    state.keeperCalls.length = 0
    state.detailCalls.length = 0
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

    const selects = wrapper.findAll('select')
    expect(selects[1].element.value).toBe('10')
    expect(selects[2].element.value).toBe('')
    const keeperOptions = selects[2].findAll('option')
    expect(keeperOptions.some(option => option.text().includes('K1'))).toBe(true)
    expect(keeperOptions.some(option => option.text().includes('K2'))).toBe(true)
  })

  it('F-BKG-007: stale availability response cannot overwrite a newer keeper selection', async () => {
    const wrapper = await mountDialog({ visible: true, initialServiceId: '5' })

    await pickSelect(wrapper, 'K1', '100')
    await settle()
    expect(state.keeperCalls.length).toBe(1)

    await pickSelect(wrapper, 'K2', '101')
    await settle()
    expect(state.keeperCalls.length).toBe(2)

    const newer = state.keeperCalls[1].holder
    newer.resolve({ code: 200, data: { days_wsh: [makeDay(1, ['10:00']), makeDay(2, ['10:00'])] } })
    await settle()
    expect(wrapper.text()).toContain('仅展示可预约日期（2 天可约）')

    const stale = state.keeperCalls[0].holder
    stale.resolve({ code: 200, data: { days_wsh: [makeDay(1, ['10:00'])] } })
    await settle()

    expect(wrapper.text()).toContain('仅展示可预约日期（2 天可约）')
  })

  it('F-BKG-008: double submit sends one POST', async () => {
    const { createOrder } = await import('@/api/order')
    createOrder.mockReturnValue(new Promise(() => {}))

    const wrapper = await mountDialog({ visible: true, initialMerchantId: '10' })

    await pickSelect(wrapper, '豆豆', '1')
    await pickSelect(wrapper, 'M1', '10')
    await settle()
    await pickSelect(wrapper, 'K1', '100')

    const delivery = wrapper.findAll('input[type="datetime-local"]')[0]
    const pickup = wrapper.findAll('input[type="datetime-local"]')[1]
    const future = (hours) => {
      const d = new Date(Date.now() + hours * 3600000)
      const pad = value => String(value).padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
    }
    await delivery.setValue(future(2))
    await pickup.setValue(future(4))

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
    await pickSelect(wrapper, 'K1', '100')
    await settle()
    expect(state.keeperCalls.length).toBe(1)
    state.keeperCalls[0].holder.resolve({ code: 200, data: { days_wsh: [makeDay(1, ['10:00', '14:00']), makeDay(2, ['10:00'])] } })
    await settle()

    await wrapper.findAll('input[type="date"]')[0].setValue(dateStr(1))
    await settle()
    await pickSelectByValue(wrapper, `${dateStr(1)}T10:00:00`)
    await wrapper.findAll('input[type="date"]')[1].setValue(dateStr(2))
    await settle()
    await pickSelectByValue(wrapper, `${dateStr(2)}T10:00:00`)
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

    expect(state.keeperCalls.length).toBe(2)
    state.keeperCalls[1].holder.resolve({ code: 200, data: { days_wsh: [makeDay(1, ['10:00', '14:00']), makeDay(2, ['10:00'])] } })
    await settle()

    await wrapper.findAll('input[type="date"]')[0].setValue(dateStr(1))
    await settle()
    await pickSelectByValue(wrapper, `${dateStr(1)}T10:00:00`)
    await wrapper.findAll('input[type="date"]')[1].setValue(dateStr(2))
    await settle()
    await pickSelectByValue(wrapper, `${dateStr(2)}T10:00:00`)

    createOrder.mockResolvedValue({ code: 200, data: { id_wsh: 9 } })
    await submitButton(wrapper).trigger('click')
    await settle()

    const secondPayload = createOrder.mock.calls[1][0]
    expect(secondPayload.service_version_wsh).toBe('v2')
    expect(wrapper.emitted('created')[0][0].id_wsh).toBe(9)
  })

  it('F-BKG-013: success response without an order id closes defensively without emitting created', async () => {
    const { createOrder } = await import('@/api/order')
    createOrder.mockResolvedValue({ code: 200, data: {} })

    const wrapper = await mountDialog({ visible: true, initialMerchantId: '10' })

    await pickSelect(wrapper, '豆豆', '1')
    await pickSelect(wrapper, 'M1', '10')
    await settle()
    await pickSelect(wrapper, 'K1', '100')

    const delivery = wrapper.findAll('input[type="datetime-local"]')[0]
    const pickup = wrapper.findAll('input[type="datetime-local"]')[1]
    const future = (hours) => {
      const d = new Date(Date.now() + hours * 3600000)
      const pad = value => String(value).padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
    }
    await delivery.setValue(future(2))
    await pickup.setValue(future(4))
    await wrapper.find('input[placeholder="联系人姓名"]').setValue('张三')
    await wrapper.find('input[placeholder="联系人手机号"]').setValue('13800000000')

    await submitButton(wrapper).trigger('click')
    await settle()

    expect(wrapper.emitted('created')).toBeUndefined()
    expect(wrapper.emitted('close')).toBeTruthy()
    expect(state.app.addToast).toHaveBeenCalledWith(expect.stringContaining('订单已提交'), 'warning')
  })
})
