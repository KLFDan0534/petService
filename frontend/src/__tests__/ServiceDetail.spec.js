import { flushPromises, mount, enableAutoUnmount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'
import ServiceDetail from '@/views/user/ServiceDetail.vue'

enableAutoUnmount(afterEach)

const routeState = vi.hoisted(() => ({ id: 1 }))
const routeHolder = vi.hoisted(() => ({ route: null }))
const mocks = vi.hoisted(() => ({
  routerPush: vi.fn(),
  getServiceDetail: vi.fn(),
  getRatings: vi.fn(),
  merchantGetById: vi.fn(),
  keeperGetByMerchant: vi.fn(),
}))

// 路由参数放在外面这个 reactive 对象里，测试中途改一下就能假装"用户从 S1 跳到了 S2"
vi.mock('vue-router', async () => {
  const { reactive } = await import('vue')
  const route = reactive({ params: { id: routeState.id } })
  routeHolder.route = route
  return {
    useRoute: () => routeHolder.route,
    useRouter: () => ({ push: mocks.routerPush }),
  }
})

vi.mock('@/api/service', () => ({
  getServiceDetail: mocks.getServiceDetail,
}))

vi.mock('@/api/rating', () => ({
  getRatings: mocks.getRatings,
}))

vi.mock('@/services/merchantService', () => ({
  getById: mocks.merchantGetById,
}))

vi.mock('@/services/keeperService', () => ({
  getByMerchant: mocks.keeperGetByMerchant,
}))

function detailData(overrides = {}) {
  return {
    code: 200,
    data: {
      id_wsh: 1,
      merchant_id_wsh: 10,
      merchant_name_wsh: '萌宠之家',
      name_wsh: '标准寄养',
      type_wsh: 'boarding',
      category_id_wsh: 2,
      category_name_wsh: '寄养·标准',
      description_wsh: '专业寄养服务，含每日遛弯与健康观察。',
      price_wsh: 199,
      unit_wsh: '天',
      service_version_wsh: 'v1',
      service_rating_wsh: 4.5,
      service_rating_count_wsh: 12,
      bookable_wsh: true,
      bookable_reason_wsh: null,
      media_wsh: [
        { file_id_wsh: 1, sort_order_wsh: 0, is_cover_wsh: 1, url_wsh: '/images/cover.jpg' },
        { file_id_wsh: 2, sort_order_wsh: 1, is_cover_wsh: 0, url_wsh: '/images/second.jpg' },
      ],
      ...overrides,
    },
  }
}

function createWrapper() {
  const pinia = createPinia()
  setActivePinia(pinia)
  return mount(ServiceDetail, {
    global: {
      plugins: [pinia],
      stubs: {
        'el-icon': { template: '<span class="icon-stub"><slot /></span>' },
        Picture: true,
        FavoriteToggleButton: true,
      },
    },
  })
}

function mainImageSrc(wrapper) {
  const img = wrapper.find('.gallery-main img')
  return img.exists() ? img.attributes('src') : null
}

// 这一组都在测公开的产品详情页：内容能不能显示出来、图册怎么切、接口挂了怎么提示
describe('ServiceDetail.vue 公开产品详情页', () => {
  beforeEach(() => {
    routeState.id = 1
    localStorage.clear()
    vi.clearAllMocks()
    mocks.getServiceDetail.mockResolvedValue(detailData())
    mocks.getRatings.mockResolvedValue({
      code: 200,
      data: [{ id_wsh: 1, user_id_wsh: 3, score_wsh: 5, content_wsh: '很满意', created_at_wsh: '2026-01-01' }],
    })
    mocks.merchantGetById.mockResolvedValue({ id_wsh: 10, name_wsh: '萌宠之家', address_wsh: '上海市' })
    mocks.keeperGetByMerchant.mockResolvedValue([
      { id_wsh: 5, name_wsh: '张阿姨', avatar_wsh: '', experience_years_wsh: 3 },
    ])
  })

  // 让接口一直不返回，页面就会永远停在加载中，这样才能检查加载提示本身
  it('F-DET-001 加载状态具有可访问标签', () => {
    mocks.getServiceDetail.mockImplementation(() => new Promise(() => {}))
    const wrapper = createWrapper()
    const loading = wrapper.find('.loading')
    expect(loading.attributes('role')).toBe('status')
    expect(loading.attributes('aria-label')).toBe('加载服务信息...')
    wrapper.unmount()
  })

  it('F-DET-002 完整渲染产品内容与首张封面图', async () => {
    const wrapper = createWrapper()
    await flushPromises()
    expect(wrapper.find('h1').text()).toBe('标准寄养')
    expect(wrapper.find('.badge').text()).toBe('寄养·标准')
    expect(wrapper.find('.service-price').text()).toContain('199.00')
    expect(wrapper.find('.service-price').text()).toContain('/ 天')
    expect(wrapper.find('.service-desc').text()).toContain('专业寄养服务')
    expect(wrapper.find('.rating-summary').text()).toContain('4.5')
    expect(mainImageSrc(wrapper)).toBe('/images/cover.jpg')
  })

  // 鼠标点缩略图、按回车、按空格，三种方式都应该换主图，而且主图那块容器一直存在不会跳
  it('F-DET-003 缩略图点击与键盘操作切换主图且容器不位移', async () => {
    const wrapper = createWrapper()
    await flushPromises()
    const thumbs = wrapper.findAll('.gallery-thumb')
    expect(thumbs).toHaveLength(2)
    await thumbs[1].trigger('click')
    expect(mainImageSrc(wrapper)).toBe('/images/second.jpg')
    expect(wrapper.find('.gallery-main').exists()).toBe(true)
    await thumbs[0].trigger('keydown', { key: 'Enter' })
    expect(mainImageSrc(wrapper)).toBe('/images/cover.jpg')
    await thumbs[1].trigger('keydown', { key: ' ' })
    expect(mainImageSrc(wrapper)).toBe('/images/second.jpg')
  })

  // 没有图册、或者图片地址本身是坏的，都拿占位块顶上，页面其他部分照常显示
  it('F-DET-004 无图册或畸形URL使用占位且不破坏页面', async () => {
    mocks.getServiceDetail.mockResolvedValue(detailData({ media_wsh: [] }))
    const wrapper = createWrapper()
    await flushPromises()
    expect(wrapper.find('.gallery-main img').exists()).toBe(false)
    expect(wrapper.find('.gallery-main .media-placeholder').exists()).toBe(true)
    expect(wrapper.find('h1').text()).toBe('标准寄养')
    wrapper.unmount()

    mocks.getServiceDetail.mockResolvedValue(detailData({
      media_wsh: [{ file_id_wsh: 1, sort_order_wsh: 0, is_cover_wsh: 1, url_wsh: '::bad url::' }],
    }))
    const broken = createWrapper()
    await flushPromises()
    expect(broken.find('h1').text()).toBe('标准寄养')
    await broken.find('.gallery-main img').trigger('error')
    expect(broken.find('.gallery-main .media-placeholder').exists()).toBe(true)
  })

  // 图多到十张也不能撑坏，缩略图数量对得上，默认展示的还是第一张
  it('F-DET-005 十张图册保持可用', async () => {
    const media = Array.from({ length: 10 }, (_, i) => ({
      file_id_wsh: i + 1,
      sort_order_wsh: i,
      is_cover_wsh: i === 0 ? 1 : 0,
      url_wsh: `/images/m${i}.jpg`,
    }))
    mocks.getServiceDetail.mockResolvedValue(detailData({ media_wsh: media }))
    const wrapper = createWrapper()
    await flushPromises()
    expect(wrapper.findAll('.gallery-thumb')).toHaveLength(10)
    expect(wrapper.find('.gallery-thumbs').exists()).toBe(true)
    expect(mainImageSrc(wrapper)).toBe('/images/m0.jpg')
  })

  // 接口说找不到或已下架时，要给一句人话提示，并留一个能点回列表的按钮
  it('F-DET-006 404/不可用显示明确操作回到列表', async () => {
    mocks.getServiceDetail.mockResolvedValue({ code: 404, message: '服务产品不存在' })
    const wrapper = createWrapper()
    await flushPromises()
    expect(wrapper.find('.empty-state h3').text()).toBe('服务不可用')
    expect(wrapper.find('.empty-state p').text()).toContain('服务产品不存在')
    await wrapper.find('.empty-state .btn').trigger('click')
    expect(mocks.routerPush).toHaveBeenCalledWith('/services')
    wrapper.unmount()

    mocks.getServiceDetail.mockResolvedValue({ code: 400, message: '服务已下架' })
    const unavailable = createWrapper()
    await flushPromises()
    expect(unavailable.find('.empty-state p').text()).toContain('服务已下架')
    expect(unavailable.find('.empty-state .btn').text()).toBe('返回服务列表')
  })

  // 评价、商家、看护员三个接口一起报错，产品本身的内容也不能跟着消失
  it('F-DET-007 商家/评价加载失败不抹掉核心产品内容', async () => {
    mocks.getRatings.mockRejectedValue(new Error('评价接口异常'))
    mocks.merchantGetById.mockRejectedValue(new Error('商家接口异常'))
    mocks.keeperGetByMerchant.mockRejectedValue(new Error('看护员接口异常'))
    const wrapper = createWrapper()
    await flushPromises()
    expect(wrapper.find('h1').text()).toBe('标准寄养')
    expect(mainImageSrc(wrapper)).toBe('/images/cover.jpg')
    expect(wrapper.find('.provider-info strong').text()).toBe('萌宠之家')
    expect(wrapper.find('.detail-main').text()).toContain('评价加载失败')
    expect(wrapper.find('.keepers-sub').exists()).toBe(false)
  })

  // 从 S1 切到 S2 时，S1 的请求可能比 S2 还晚回来。这里故意让慢的那个后回来，看会不会把新内容盖掉
  it('F-DET-008 路由参数 S1→S2 切换丢弃过期异步响应', async () => {
    const pending = []
    mocks.getServiceDetail.mockImplementation((id) => new Promise(resolve => {
      pending.push({ id, resolve })
    }))
    const wrapper = createWrapper()
    await nextTick()
    expect(pending).toHaveLength(1)
    expect(pending[0].id).toBe(1)

    routeHolder.route.params.id = 2
    await nextTick()
    expect(pending).toHaveLength(2)
    expect(pending[1].id).toBe(2)

    pending[1].resolve(detailData({ id_wsh: 2, name_wsh: 'S2 寄养', price_wsh: 299 }))
    await flushPromises()
    expect(wrapper.find('h1').text()).toBe('S2 寄养')

    // 现在才把老请求 S1 的结果放出来，它已经过期了，页面上必须还是 S2 的名字和价格
    pending[0].resolve(detailData({ id_wsh: 1, name_wsh: 'S1 寄养' }))
    await flushPromises()
    expect(wrapper.find('h1').text()).toBe('S2 寄养')
    expect(wrapper.find('.service-price').text()).toContain('299.00')
  })

  // 名字、介绍、价格都往夸张了写，靠 text-break 保证不会把布局撑破
  it('F-DET-009 长名称/长介绍/大价格不溢出', async () => {
    const longName = '超长寄养服务名称'.repeat(10)
    const longDesc = '超长介绍'.repeat(200)
    mocks.getServiceDetail.mockResolvedValue(detailData({
      name_wsh: longName,
      description_wsh: longDesc,
      price_wsh: 999999.99,
    }))
    const wrapper = createWrapper()
    await flushPromises()
    const h1 = wrapper.find('h1')
    expect(h1.text()).toBe(longName)
    expect(h1.classes()).toContain('text-break')
    const desc = wrapper.find('.service-desc')
    expect(desc.text()).toBe(longDesc)
    expect(desc.classes()).toContain('text-break')
    expect(wrapper.find('.service-price').text()).toContain('999999.99')
  })

  // 读屏和键盘用户也要能用：缩略图、商家卡片、看护员卡片都得有能念出来的名字，还得能聚焦
  it('F-DET-010 按钮/缩略图/商家/看护员有可访问名称与可聚焦性', async () => {
    const wrapper = createWrapper()
    await flushPromises()
    const thumbs = wrapper.findAll('.gallery-thumb')
    expect(thumbs[0].attributes('aria-label')).toBe('查看第 1 张图片')
    expect(thumbs[0].attributes('aria-current')).toBe('true')
    expect(thumbs[1].attributes('aria-current')).toBeUndefined()
    expect(wrapper.find('.provider-card').attributes('role')).toBe('link')
    expect(wrapper.find('.provider-card').attributes('tabindex')).toBe('0')
    expect(wrapper.find('.provider-card').attributes('aria-label')).toContain('萌宠之家')
    const keeper = wrapper.find('.keeper-mini-item')
    expect(keeper.attributes('role')).toBe('link')
    expect(keeper.attributes('tabindex')).toBe('0')
    expect(keeper.attributes('aria-label')).toContain('张阿姨')
    expect(wrapper.find('.btn-primary').text()).toBe('立即预约')
  })

  // 门店不能在线预约时，按钮不能变成点不动的死按钮，点下去要说明为什么不能约
  it('不可预约时按钮可点击并提示原因（详情仍完整）', async () => {
    mocks.getServiceDetail.mockResolvedValue(detailData({
      bookable_wsh: false,
      bookable_reason_wsh: '该服务以次计费，暂不支持在线预约',
    }))
    const wrapper = createWrapper()
    await flushPromises()
    const bookButton = wrapper.find('.service-actions .btn-primary')
    expect(bookButton.attributes('disabled')).toBeUndefined()
    expect(wrapper.find('.bookable-reason').text()).toBe('该服务以次计费，暂不支持在线预约')
    expect(wrapper.find('h1').text()).toBe('标准寄养')
    await bookButton.trigger('click')
  })
})
