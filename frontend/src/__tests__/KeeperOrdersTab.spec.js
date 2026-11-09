import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import KeeperOrdersTab from '@/components/keeper/KeeperOrdersTab.vue'

vi.mock('@/composables/useAmapLocation', () => ({
  getCurrentAddress: vi.fn(() => Promise.resolve({ latitude_wsh: 31.2, longitude_wsh: 121.4, address_wsh: '当前位置' })),
}))
vi.mock('@/components/common/AppDialog.vue', () => ({
  // 真实 AppDialog 只在 visible 时渲染插槽；桩必须一致，否则会去读 null 的弹窗数据
  default: {
    name: 'AppDialog',
    props: ['visible'],
    template: '<div v-if="visible"><slot /><slot name="footer" /></div>',
  },
}))

const poolOrder = {
  id_wsh: 1,
  order_no_wsh: 'ORD-POOL-1',
  status_wsh: 'paid',
  keeper_id_wsh: null,
  service_name_wsh: '标准寄养',
  pet_name_wsh: '豆豆',
  start_date_wsh: '2026-11-04',
  end_date_wsh: '2026-11-05',
}

const assignedOrder = { ...poolOrder, id_wsh: 2, order_no_wsh: 'ORD-MINE-2', keeper_id_wsh: 9 }

function mountTab(props) {
  return mount(KeeperOrdersTab, {
    props: { nowMs: Date.now(), ...props },
  })
}

describe('KeeperOrdersTab 待抢单', () => {
  it('pool 页签只提供「抢单」，不出现接单/拒单', () => {
    const wrapper = mountTab({ subTab: 'pool', poolOrders: [poolOrder] })
    const texts = wrapper.findAll('button').map(button => button.text())
    expect(texts).toContain('抢单')
    // pool 里的订单没有负责人，接单/拒单都会 403，必须在 UI 上就不出现
    expect(texts).not.toContain('拒单')
    expect(texts).not.toContain('接单')
  })

  it('点击抢单发出 claim 事件并带上订单号', async () => {
    const wrapper = mountTab({ subTab: 'pool', poolOrders: [poolOrder] })
    const claimButton = wrapper.findAll('button').find(button => button.text() === '抢单')
    await claimButton.trigger('click')
    expect(wrapper.emitted('claim')).toBeTruthy()
    expect(wrapper.emitted('claim')[0][0].order_no_wsh).toBe('ORD-POOL-1')
  })

  it('不再提供「待处理」页签（下单不再指定负责人，订单只有抢单与我的订单）', () => {
    const wrapper = mountTab({ subTab: 'pool', poolOrders: [] })
    const texts = wrapper.findAll('button').map(button => button.text())
    expect(texts.some(t => t.includes('待处理'))).toBe(false)
    expect(texts.some(t => t.includes('待抢单'))).toBe(true)
    expect(texts.some(t => t.includes('我的订单'))).toBe(true)
  })

  it('池子为空时给出可抢订单的说明', () => {
    const wrapper = mountTab({ subTab: 'pool', poolOrders: [] })
    expect(wrapper.text()).toContain('暂无可抢订单')
    expect(wrapper.text()).toContain('30 分钟')
  })
})

const inProgressOrder = {
  ...assignedOrder,
  id_wsh: 3,
  order_no_wsh: 'ORD-STAY-3',
  status_wsh: 'in_progress',
  checkout_confirmed_at_wsh: null,
}

describe('KeeperOrdersTab 确认接走', () => {
  it('服务中订单的按钮是「确认接走」，不再是「完成订单」', () => {
    const wrapper = mountTab({ subTab: 'active', activeOrders: [inProgressOrder] })
    const texts = wrapper.findAll('button').map(button => button.text())
    expect(texts).toContain('确认接走')
    expect(texts).not.toContain('完成订单')
  })

  it('格式不合法（非 4 位数字）不发请求', async () => {
    const wrapper = mountTab({ subTab: 'active', activeOrders: [inProgressOrder] })
    await wrapper.findAll('button').find(b => b.text() === '确认接走').trigger('click')

    const input = wrapper.find('[data-testid="pickup-code-input"]')
    await input.setValue('42')
    const submit = wrapper.findAll('button').filter(b => b.text() === '确认接走').at(-1)
    await submit.trigger('click')

    expect(wrapper.emitted('checkout')).toBeFalsy()
  })

  it('4 位码（含前导 0）原样提交，并区分送达码', async () => {
    const wrapper = mountTab({ subTab: 'active', activeOrders: [inProgressOrder] })
    await wrapper.findAll('button').find(b => b.text() === '确认接走').trigger('click')

    await wrapper.find('[data-testid="pickup-code-input"]').setValue('0007')
    const submit = wrapper.findAll('button').filter(b => b.text() === '确认接走').at(-1)
    await submit.trigger('click')

    const emitted = wrapper.emitted('checkout')
    expect(emitted).toBeTruthy()
    expect(emitted[0][0]).toEqual({ order_no_wsh: 'ORD-STAY-3', pickup_code_wsh: '0007' })
  })

  it('接走已确认但待补费时不再弹码，直接提示待主人补费', () => {
    const waiting = {
      ...inProgressOrder,
      checkout_confirmed_at_wsh: '2026-11-09T15:30:00',
      overtime_hours_wsh: 2,
      overtime_amount_wsh: 12.5,
      overtime_pay_no_wsh: 'PAY-OT-1',
      overtime_payment_status_wsh: 'pending',
    }
    const wrapper = mountTab({ subTab: 'active', activeOrders: [waiting] })
    expect(wrapper.text()).toContain('待主人补费')
    expect(wrapper.text()).toContain('12.50')
    expect(wrapper.findAll('button').map(b => b.text())).not.toContain('确认接走')
  })
})
