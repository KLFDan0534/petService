import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Keepers from '@/views/user/Keepers.vue'

const state = vi.hoisted(() => ({
  router: { push: vi.fn() },
  keepers: { code: 200, data: [{ id_wsh: 1, merchant_id_wsh: 2, name_wsh: '小王', price_per_day_wsh: 50 }] },
}))

vi.mock('vue-router', () => ({
  useRouter: () => state.router,
}))

vi.mock('@/api/keeper', () => ({
  getKeepers: vi.fn(() => Promise.resolve(state.keepers)),
}))

function mountKeepers() {
  return mount(Keepers, {
    global: {
      stubs: { PageHero: true },
      mocks: { $router: state.router },
    },
  })
}

describe('Keepers page (F-NAV-004)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('routes 选择服务 to the keeper merchant page, never to /orders', async () => {
    const wrapper = mountKeepers()
    await flushPromises()

    const button = wrapper.findAll('button').find(b => b.text().includes('选择服务'))
    await button.trigger('click')

    expect(state.router.push).toHaveBeenCalledWith('/merchants/2')
    expect(state.router.push.mock.calls.join('|')).not.toContain('/orders')
  })

  it('keeps 查看主页 navigation to the keeper detail page', async () => {
    const wrapper = mountKeepers()
    await flushPromises()

    const button = wrapper.findAll('button').find(b => b.text().includes('查看主页'))
    await button.trigger('click')

    expect(state.router.push).toHaveBeenCalledWith('/keepers/1')
  })
})
