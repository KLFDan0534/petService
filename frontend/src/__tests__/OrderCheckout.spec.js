import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

const api = vi.hoisted(() => ({
  getOrderDetail: vi.fn(),
  getOrderTimeline: vi.fn(),
  getDailyStatus: vi.fn(),
  getOrderMessages: vi.fn(),
  markOrderConversationRead: vi.fn(),
  getOrderReport: vi.fn(),
  regenerateReport: vi.fn(),
  createPayment: vi.fn(),
  executePayment: vi.fn(),
  cancelOrder: vi.fn(),
}))

vi.mock('@/api/orderDetail', () => api)

const { useOrderDetailStore } = await import('@/stores/orderDetail')

function order(overrides = {}) {
  return {
    id_wsh: 5,
    order_no_wsh: 'ORD-OT-5',
    status_wsh: 'in_progress',
    billing_unit_wsh: 'night',
    quantity_wsh: 3,
    overtime_hours_wsh: 2,
    overtime_amount_wsh: 12.5,
    overtime_pay_no_wsh: 'PAY-OT-5',
    overtime_payment_status_wsh: 'pending',
    checkout_confirmed_at_wsh: '2026-11-09T15:30:00',
    ...overrides,
  }
}

describe('订单详情 · 晚接补费支付', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    api.getOrderDetail.mockResolvedValue({ code: 200, data: order() })
  })

  it('补费支付直接用后端补费单号，不调用 createPayment', async () => {
    api.executePayment.mockResolvedValue({ code: 200 })
    const store = useOrderDetailStore()
    store.order = order()

    const result = await store.payOvertime(5, 'PAY-OT-5')

    expect(result.success).toBe(true)
    // 补费单是 keeper 确认接走时创建的，前端不能再去 createPayment
    expect(api.createPayment).not.toHaveBeenCalled()
    expect(api.executePayment).toHaveBeenCalledTimes(1)
    expect(api.executePayment).toHaveBeenCalledWith({ pay_no_wsh: 'PAY-OT-5' })
    // 支付成功后重查订单，页面拿到新的状态与金额
    expect(api.getOrderDetail).toHaveBeenCalled()
  })

  it('缺少补费单号时直接失败，不发任何支付请求', async () => {
    const store = useOrderDetailStore()
    store.order = order({ overtime_pay_no_wsh: null })

    const result = await store.payOvertime(5, null)

    expect(result.success).toBe(false)
    expect(api.createPayment).not.toHaveBeenCalled()
    expect(api.executePayment).not.toHaveBeenCalled()
  })

  it('余额不足时返回后端错误信息，便于提示充值后重试同一笔', async () => {
    api.executePayment.mockResolvedValue({ code: 400, message: '余额不足' })
    const store = useOrderDetailStore()
    store.order = order()

    const result = await store.payOvertime(5, 'PAY-OT-5')

    expect(result.success).toBe(false)
    expect(result.message).toBe('余额不足')
  })

  it('原预付支付仍走 createPayment + executePayment（未被补费路径改坏）', async () => {
    api.createPayment.mockResolvedValue({ code: 200, data: { pay_no_wsh: 'PAY-ORDER-5' } })
    api.executePayment.mockResolvedValue({ code: 200 })
    const store = useOrderDetailStore()
    store.order = order({ status_wsh: 'pending' })

    const result = await store.pay(5, 'balance')

    expect(result.success).toBe(true)
    expect(api.createPayment).toHaveBeenCalledTimes(1)
    expect(api.executePayment).toHaveBeenCalledWith({ pay_no_wsh: 'PAY-ORDER-5' })
  })
})
