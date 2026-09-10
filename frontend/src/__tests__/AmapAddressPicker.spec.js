import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import AmapAddressPicker from '@/components/common/AmapAddressPicker.vue'
import { loadAmap, reverseGeocodeDetail, searchAmapAddress } from '@/composables/useAmapLocation'

vi.mock('@/composables/useAmapLocation', () => ({
  getCurrentAddress: vi.fn(),
  loadAmap: vi.fn(),
  reverseGeocodeDetail: vi.fn(),
  searchAmapAddress: vi.fn(),
}))

function createFakeAmap() {
  const handlers = {}
  const map = {
    on: vi.fn((event, handler) => {
      handlers[event] = handler
    }),
    add: vi.fn(),
    destroy: vi.fn(),
    setCenter: vi.fn(),
    getCenter: vi.fn(() => ({ lng: 113.2644, lat: 23.1291 })),
  }
  class FakeMap {
    constructor() {
      return map
    }
  }
  class FakeMarker {
    constructor(options = {}) {
      this.position = options.position
      this.handlers = {}
    }

    on(event, handler) {
      this.handlers[event] = handler
    }

    setPosition(position) {
      this.position = position
    }

    getPosition() {
      return { lng: this.position[0], lat: this.position[1] }
    }
  }
  return {
    AMap: { Map: FakeMap, Marker: FakeMarker },
    handlers,
  }
}

describe('AmapAddressPicker.vue', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    setActivePinia(createPinia())
    document.body.innerHTML = ''
    searchAmapAddress.mockReset()
    searchAmapAddress.mockResolvedValue([])
    reverseGeocodeDetail.mockReset()
    reverseGeocodeDetail.mockResolvedValue({
      address_wsh: '广东省广州市白云区测试路1号',
    })
  })

  afterEach(() => {
    delete window.AMap
    vi.useRealTimers()
  })

  it('syncs the map search input and queues one search after manual picking', async () => {
    const { AMap, handlers } = createFakeAmap()
    window.AMap = AMap
    loadAmap.mockResolvedValue(AMap)

    const wrapper = mount(AmapAddressPicker, {
      props: {
        modelValue: '',
        showLocateButton: false,
      },
      global: {
        plugins: [createPinia()],
        stubs: {
          ElIcon: true,
          teleport: true,
        },
      },
    })

    await wrapper.find('.picker-actions button').trigger('click')
    await flushPromises()

    handlers.click({ lnglat: { lng: 113.2644, lat: 23.1291 } })
    await flushPromises()

    expect(wrapper.find('.map-search-row input').element.value).toBe('广东省广州市白云区测试路1号')
    expect(searchAmapAddress).not.toHaveBeenCalled()

    await vi.advanceTimersByTimeAsync(499)
    expect(searchAmapAddress).not.toHaveBeenCalled()

    await vi.advanceTimersByTimeAsync(1)
    await flushPromises()

    expect(searchAmapAddress).toHaveBeenCalledTimes(1)
    expect(searchAmapAddress).toHaveBeenCalledWith('广东省广州市白云区测试路1号', expect.objectContaining({
      withNearby: true,
      limit: 12,
    }))
  })

  it('keeps only the latest manual pick result when reverse lookups finish out of order', async () => {
    const { AMap, handlers } = createFakeAmap()
    window.AMap = AMap
    loadAmap.mockResolvedValue(AMap)
    let resolveFirst
    reverseGeocodeDetail
      .mockReturnValueOnce(new Promise(resolve => { resolveFirst = resolve }))
      .mockResolvedValueOnce({ address_wsh: '广东省广州市白云区第二个点' })

    const wrapper = mount(AmapAddressPicker, {
      props: { modelValue: '', showLocateButton: false },
      global: {
        plugins: [createPinia()],
        stubs: { ElIcon: true, teleport: true },
      },
    })
    await wrapper.find('.picker-actions button').trigger('click')
    await flushPromises()

    handlers.click({ lnglat: { lng: 113.26, lat: 23.12 } })
    handlers.click({ lnglat: { lng: 113.27, lat: 23.13 } })
    await flushPromises()
    resolveFirst({ address_wsh: '广东省广州市白云区第一个点' })
    await flushPromises()

    expect(wrapper.find('.map-search-row input').element.value).toBe('广东省广州市白云区第二个点')
    await vi.advanceTimersByTimeAsync(500)
    await flushPromises()
    expect(searchAmapAddress).toHaveBeenCalledTimes(1)
    expect(searchAmapAddress).toHaveBeenCalledWith('广东省广州市白云区第二个点', expect.any(Object))
  })

  it('does not let a closed map dialog receive a late reverse lookup result', async () => {
    const { AMap, handlers } = createFakeAmap()
    window.AMap = AMap
    loadAmap.mockResolvedValue(AMap)
    let resolveReverse
    reverseGeocodeDetail.mockReturnValue(new Promise(resolve => { resolveReverse = resolve }))

    const wrapper = mount(AmapAddressPicker, {
      props: { modelValue: '', showLocateButton: false },
      global: {
        plugins: [createPinia()],
        stubs: { ElIcon: true, teleport: true },
      },
    })
    await wrapper.find('.picker-actions button').trigger('click')
    await flushPromises()
    handlers.click({ lnglat: { lng: 113.2644, lat: 23.1291 } })
    wrapper.find('.map-dialog__footer .btn-secondary').trigger('click')
    resolveReverse({ address_wsh: '广东省广州市白云区迟到结果' })
    await flushPromises()
    await vi.advanceTimersByTimeAsync(500)

    expect(searchAmapAddress).not.toHaveBeenCalled()
    expect(wrapper.find('.map-dialog-overlay').exists()).toBe(false)
  })

  it('closes the map dialog when amap loading fails', async () => {
    loadAmap.mockRejectedValue(new Error('未配置高德地图 key'))
    const wrapper = mount(AmapAddressPicker, {
      props: { modelValue: '', showLocateButton: false },
      global: {
        plugins: [createPinia()],
        stubs: { ElIcon: true, teleport: true },
      },
    })
    await wrapper.find('.picker-actions button').trigger('click')
    await flushPromises()
    expect(wrapper.find('.map-dialog-overlay').exists()).toBe(false)
  })
})
