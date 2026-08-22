import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ServiceGrid from './ServiceGrid.vue'

const state = vi.hoisted(() => ({
  loggedIn: true,
  router: { push: vi.fn() },
}))

vi.mock('vue-router', () => ({
  useRouter: () => state.router,
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({ get isLoggedIn() { return state.loggedIn } }),
}))

vi.mock('@/stores/category', () => ({
  useCategoryStore: () => ({ getCategoryName: () => '' }),
}))

const services = [
  { id_wsh: 5, name_wsh: '寄养服务', price_wsh: 88, unit_wsh: '天', images_wsh: 'http://x/1.jpg' },
]

function mountGrid() {
  return mount(ServiceGrid, {
    props: { services, loading: false },
    global: {
      stubs: {
        MediaWithFallback: true,
        FavoriteToggleButton: true,
        'el-icon': { template: '<span class="el-icon-stub"><slot /></span>' },
        ArrowRight: true,
        Calendar: true,
        Service: true,
      },
    },
  })
}

describe('ServiceGrid navigation (F-NAV-001/F-NAV-006)', () => {
  beforeEach(() => {
    state.loggedIn = true
    vi.clearAllMocks()
  })

  it('routes the booking button to /services/{id}?book=1 without legacy order query', async () => {
    const wrapper = mountGrid()
    await flushPromises()

    const bookButton = wrapper.findAll('button').find(b => b.text().includes('立即预约'))
    await bookButton.trigger('click')

    expect(state.router.push).toHaveBeenCalledWith({ path: '/services/5', query: { book: '1' } })
    expect(JSON.stringify(state.router.push.mock.calls)).not.toContain('/orders')
    expect(JSON.stringify(state.router.push.mock.calls)).not.toContain('name')
    expect(JSON.stringify(state.router.push.mock.calls)).not.toContain('price')
  })

  it('keeps anonymous users on the catalog: booking routes to detail, no login prompt is triggered from the grid', async () => {
    state.loggedIn = false
    const wrapper = mountGrid()
    await flushPromises()

    const bookButton = wrapper.findAll('button').find(b => b.text().includes('立即预约'))
    await bookButton.trigger('click')

    expect(state.router.push).toHaveBeenCalledWith({ path: '/services/5', query: { book: '1' } })
    expect(state.router.push.mock.calls.join('|')).not.toContain('/orders')
  })

  it('routes card media and detail link to /services/{id}', async () => {
    const wrapper = mountGrid()
    await flushPromises()

    await wrapper.find('.service-media-button').trigger('click')
    expect(state.router.push).toHaveBeenCalledWith('/services/5')

    await wrapper.find('.detail-link').trigger('click')
    expect(state.router.push).toHaveBeenCalledWith('/services/5')
  })

  it('renders the distance chip with km/m formatting when distance is present', async () => {
    const withDistance = [
      { id_wsh: 7, name_wsh: '寄养服务', price_wsh: 88, unit_wsh: 'day', images_wsh: 'http://x/2.jpg', merchant_name_wsh: '优品宠物服务中心', distance_m_wsh: 2460 },
      { id_wsh: 8, name_wsh: '遛宠服务', price_wsh: 30, unit_wsh: 'session', images_wsh: 'http://x/3.jpg', distance_m_wsh: 850 },
    ]
    const wrapper = mount(ServiceGrid, {
      props: { services: withDistance, loading: false },
      global: {
        stubs: {
          MediaWithFallback: true,
          FavoriteToggleButton: true,
          'el-icon': { template: '<span />' },
          ArrowRight: true,
          Calendar: true,
          Service: true,
        },
      },
    })
    await flushPromises()

    const chips = wrapper.findAll('.service-distance').map(node => node.text())
    expect(chips).toEqual(['2.5km', '850m'])
    // 单位显示为中文
    expect(wrapper.findAll('.service-price span')[0].text()).toBe('/ 天')
    expect(wrapper.findAll('.service-price span')[1].text()).toBe('/ 次')
  })

  it('ignores booking click when the service has no id', async () => {
    const wrapper = mount(ServiceGrid, {
      props: { services: [{ name_wsh: '无ID' }], loading: false },
      global: {
        stubs: {
          MediaWithFallback: true,
          FavoriteToggleButton: true,
          'el-icon': { template: '<span />' },
          ArrowRight: true,
          Calendar: true,
          Service: true,
        },
      },
    })
    await flushPromises()

    const bookButton = wrapper.findAll('button').find(b => b.text().includes('立即预约'))
    await bookButton.trigger('click')
    expect(state.router.push).not.toHaveBeenCalled()
  })
})
