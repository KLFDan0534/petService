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

// 测试环境里没有真的高德 SDK，这里造一套假的 Map / Marker，只保留用例要用到的那几个方法
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

// 这个组件干的事是：打开地图选个点，把地址填回搜索框。下面测的都是选完点之后的连锁反应
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

  // 在地图上点一下，地址先回填到搜索框；搜索要等 500 毫秒才发，而且只发一次
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

    // 还差 1 毫秒没到 500，这时候搜索请求不能提前跑出去
    await vi.advanceTimersByTimeAsync(499)
    expect(searchAmapAddress).not.toHaveBeenCalled()

    // 把最后这 1 毫秒补上，才算凑满 500 毫秒，这时候才真正搜一次
    await vi.advanceTimersByTimeAsync(1)
    await flushPromises()

    expect(searchAmapAddress).toHaveBeenCalledTimes(1)
    expect(searchAmapAddress).toHaveBeenCalledWith('广东省广州市白云区测试路1号', expect.objectContaining({
      withNearby: true,
      limit: 12,
    }))
  })

  // 连点两次地图，第一次的反查故意比第二次晚回来，页面上只认最后一次选的地址
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

  // 点了取消把地图关掉，之前那次反查的结果再晚到，也不能往已经关掉的弹窗里写内容
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

  // 高德脚本都加载不出来，就别硬撑了，直接把弹窗关掉，别留个打不开的空壳
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
