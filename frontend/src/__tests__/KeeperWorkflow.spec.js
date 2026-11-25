import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import KeeperWorkflow from '@/views/user/KeeperWorkflow.vue'
import KeeperDailyTab from '@/components/keeper/KeeperDailyTab.vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'

/**
 * 照护师工作台测试。
 *
 * 工作台现在是「左侧三项导航 + 右侧单内容区」：可抢订单 / 我的订单 / 订单工作区。
 * 「我的订单」保留状态分类（全部 / 待接收 / 已接收 / 服务中 / 已完成 / 已退款）；
 * 「订单工作区」按 服务中 → 待接收 → 已接收 → 其他（已完成 + 已退款）推进，默认停在服务中，
 * 有未读消息的订单显示红色数字并排到最前。两处分类与游标互不影响，工作区把订单对话与上传动态
 * 合并到同一订单详情里，考勤收进对话框。
 *
 * 子组件用 stub 替身，通过 emit 触发父组件处理器——测的是父组件真正接的 API 与参数。
 */
const mocks = vi.hoisted(() => ({
  getMyKeeperInfo: vi.fn(),
  scrollPoolOrders: vi.fn(),
  scrollKeeperOrders: vi.fn(),
  claimOrder: vi.fn(),
  rejectOrder: vi.fn(),
  markReceived: vi.fn(),
  startService: vi.fn(),
  completeOrder: vi.fn(),
  getTimeline: vi.fn(),
  getDailyStatus: vi.fn(),
  uploadTimeline: vi.fn(),
  getConversation: vi.fn(),
  sendMessage: vi.fn(),
  markConversationRead: vi.fn(),
  getUnreadMessages: vi.fn(),
  getCurrentAttendance: vi.fn(),
}))

vi.mock('@/api/keeper', () => ({
  getMyKeeperInfo: mocks.getMyKeeperInfo,
}))

vi.mock('@/api/attendance', () => ({
  getCurrentAttendance: mocks.getCurrentAttendance,
}))

vi.mock('@/api/order', () => ({
  scrollPoolOrders: mocks.scrollPoolOrders,
  scrollKeeperOrders: mocks.scrollKeeperOrders,
  claimOrder: mocks.claimOrder,
  rejectOrder: mocks.rejectOrder,
  markReceived: mocks.markReceived,
  startService: mocks.startService,
  completeOrder: mocks.completeOrder,
  getTimeline: mocks.getTimeline,
  getDailyStatus: mocks.getDailyStatus,
  uploadTimeline: mocks.uploadTimeline,
  getConversation: mocks.getConversation,
  sendMessage: mocks.sendMessage,
  markConversationRead: mocks.markConversationRead,
}))

vi.mock('@/api/chat', () => ({
  getUnreadMessages: mocks.getUnreadMessages,
}))

const OrdersTabStub = {
  name: 'KeeperOrdersTab',
  props: ['subTab', 'poolOrders', 'activeOrders', 'loading'],
  emits: ['claim', 'reject', 'receive', 'start', 'checkout'],
  template: `
    <div class="orders-stub" :data-subtab="subTab">
      <button class="t-claim" @click="$emit('claim', { order_no_wsh: 'ORD-1' })">claim</button>
      <button class="t-reject" @click="$emit('reject', { order_no_wsh: 'ORD-1' })">reject</button>
      <button class="t-receive" @click="$emit('receive', { order_no_wsh: 'ORD-1', code: '1234' })">receive</button>
      <button class="t-start" @click="$emit('start', 'ORD-1', { name: 'a.jpg', type: 'image/jpeg' })">start</button>
      <button class="t-checkout" @click="$emit('checkout', { order_no_wsh: 'ORD-1' })">checkout</button>
    </div>`,
}

const DailyTabStub = {
  name: 'KeeperDailyTab',
  props: {
    orderId: { type: String, default: '' },
    orderLocked: { type: Boolean, default: false },
    myOrders: { type: Array, default: () => [] },
    dailyStatus: { type: Object, default: null },
    careRecords: { type: Array, default: () => [] },
    recordUploading: { type: Boolean, default: false },
    submitKey: { type: Number, default: 0 },
  },
  emits: ['upload-timeline', 'refresh'],
  template: `<div class="daily-stub" :data-order-id="orderId" :data-locked="orderLocked">
    <button class="t-upload" @click="$emit('upload-timeline', { orderId: 55, type: 'feeding', content: '已喂食', files: [] })">upload</button>
  </div>`,
}

const AttendancePanelStub = {
  name: 'KeeperAttendancePanel',
  emits: ['status-change'],
  template: `<div class="attendance-stub">考勤面板
    <button class="t-att-in" @click="$emit('status-change', { id_wsh: 1, check_in_at_wsh: '2026-11-20T09:00:00' })">in</button>
    <button class="t-att-out" @click="$emit('status-change', null)">out</button>
  </div>`,
}

const AppDialogStub = {
  name: 'AppDialog',
  props: ['visible', 'title', 'description', 'size'],
  template: `
    <div v-if="visible" class="dialog-stub" :data-title="title">
      <h2>{{ title }}</h2>
      <p class="dialog-desc">{{ description }}</p>
      <slot />
    </div>`,
}

function mountPage(stubOverrides = {}) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const authStore = useAuthStore(pinia)
  // 不设 token：避免 onMounted 里 connectOrderEvents 去建 EventSource（jsdom 无此实现）
  authStore.token = null
  const appStore = useAppStore(pinia)
  const addToast = vi.spyOn(appStore, 'addToast').mockImplementation(() => {})
  const wrapper = mount(KeeperWorkflow, {
    global: {
      plugins: [pinia],
      stubs: {
        KeeperOrdersTab: OrdersTabStub,
        KeeperDailyTab: DailyTabStub,
        KeeperAttendancePanel: AttendancePanelStub,
        AppDialog: AppDialogStub,
        RouterLink: { props: ['to'], template: '<a :href="to"><slot /></a>' },
        ...stubOverrides,
      },
    },
  })
  return { wrapper, addToast }
}

/** 左侧导航按钮（文字里带可选数量，所以按包含匹配） */
function navButton(wrapper, label) {
  const button = wrapper.findAll('.kw-nav-item').find(item => item.text().includes(label))
  if (!button) throw new Error(`导航项「${label}」不存在`)
  return button
}

async function goTo(wrapper, label) {
  await navButton(wrapper, label).trigger('click')
  await flushPromises()
}

function statusTab(wrapper, status) {
  const tab = wrapper.find(`.kw-status-tab[data-status="${status}"]`)
  if (!tab.exists()) throw new Error(`状态分类「${status}」不存在`)
  return tab
}

/** 手动控制 resolve 时机，用来模拟接口响应前的等待窗口 */
function deferred() {
  let resolve
  let reject
  const promise = new Promise((res, rej) => { resolve = res; reject = rej })
  return { promise, resolve, reject }
}

/** 按 status 返回不同订单，便于验证「我的订单」与「工作区」的分类互不串台 */
function mockOrdersByStatus() {
  mocks.scrollKeeperOrders.mockImplementation((params = {}) => {
    const id = params.status === 'confirmed' ? 11 : params.status === 'completed' ? 22 : 33
    return Promise.resolve({
      code: 200,
      data: {
        list_wsh: [{
          id_wsh: id,
          order_no_wsh: `ORD-${id}`,
          pet_name_wsh: '豆豆',
          status_wsh: params.status || 'received',
        }],
        next_cursor_wsh: '',
        has_more_wsh: false,
      },
    })
  })
}

describe('照护师工作台', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    mocks.getMyKeeperInfo.mockResolvedValue({
      code: 200,
      data: {
        id_wsh: 9,
        status_wsh: 1,
        merchant_name_wsh: '门店',
        name_wsh: '张阿姨',
      },
    })
    mocks.scrollPoolOrders.mockResolvedValue({
      code: 200,
      data: { list_wsh: [{ order_no_wsh: 'ORD-1' }], next_cursor_wsh: '', has_more_wsh: false },
    })
    mocks.scrollKeeperOrders.mockResolvedValue({
      code: 200,
      data: {
        list_wsh: [{ id_wsh: 55, order_no_wsh: 'ORD-1', pet_name_wsh: '豆豆', status_wsh: 'received' }],
        next_cursor_wsh: '',
        has_more_wsh: false,
      },
    })
    mocks.getCurrentAttendance.mockResolvedValue({ code: 200, data: null })
    mocks.getConversation.mockResolvedValue({ code: 200, data: [] })
    mocks.markConversationRead.mockResolvedValue({ code: 200 })
    mocks.getUnreadMessages.mockResolvedValue({ code: 200, data: [] })
    mocks.getDailyStatus.mockResolvedValue({
      code: 200,
      data: { uploaded_days_wsh: [], required_days_wsh: [], missing_days_wsh: [] },
    })
  })

  // ── 初始化与信息架构 ──
  it('初始化：只拉看护者信息、抢单池与我的订单', async () => {
    mountPage()
    await flushPromises()

    expect(mocks.getMyKeeperInfo).toHaveBeenCalledTimes(1)
    expect(mocks.scrollPoolOrders).toHaveBeenCalledTimes(1)
    expect(mocks.scrollKeeperOrders).toHaveBeenCalledTimes(1)
  })

  it('紧凑页头：身份行展示「门店 · 姓名」与接单状态，两个旧入口已移除', async () => {
    const { wrapper } = mountPage()
    await flushPromises()

    expect(wrapper.get('.kw-title').text()).toBe('照护师工作台')
    expect(wrapper.get('.kw-identity-text').text()).toBe('门店 · 张阿姨')
    // 接单状态紧邻身份文本，作为胶囊
    expect(wrapper.get('.kw-status-pill').text()).toBe('在线')

    // 资质与资料 / 查看公开主页 两个入口已移除，门店也不再是链接
    expect(wrapper.find('.kw-head-links').exists()).toBe(false)
    expect(wrapper.find('.kw-merchant-link').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('资质与资料')
    expect(wrapper.text()).not.toContain('查看公开主页')
    expect(wrapper.findAll('a').some(a => a.attributes('href') === '/keeper/profile')).toBe(false)

    // 个人中心面包屑保留
    expect(wrapper.get('.kw-crumb').text()).toContain('个人中心')
  })

  it('左侧 3 项导航：默认可抢订单，订单工作区合并且不再有独立对话/动态菜单', async () => {
    const { wrapper } = mountPage()
    await flushPromises()

    const items = wrapper.findAll('.kw-nav-item')
    expect(items).toHaveLength(3)
    expect(items[0].text()).toContain('可抢订单')
    expect(items[1].text()).toContain('我的订单')
    expect(items[2].text()).toContain('订单工作区')
    expect(wrapper.text()).not.toContain('订单对话')
    expect(wrapper.text()).not.toContain('上传动态')
    // 抢单数与进行中数量作为辅助值
    expect(items[0].text()).toContain('1')
    expect(items[1].text()).toContain('1')
    expect(wrapper.get('.kw-nav-item.is-active').text()).toContain('可抢订单')

    // 默认只渲染抢单列表，不再纵向叠着「我的订单」
    expect(wrapper.find('[data-subtab="pool"]').exists()).toBe(true)
    expect(wrapper.find('[data-subtab="active"]').exists()).toBe(false)

    await goTo(wrapper, '我的订单')

    expect(wrapper.get('.kw-nav-item.is-active').text()).toContain('我的订单')
    expect(wrapper.find('[data-subtab="active"]').exists()).toBe(true)
    expect(wrapper.find('[data-subtab="pool"]').exists()).toBe(false)

    await goTo(wrapper, '订单工作区')

    expect(wrapper.get('.kw-nav-item.is-active').text()).toContain('订单工作区')
    expect(wrapper.find('.kw-workspace').exists()).toBe(true)
  })

  it('打卡按钮写明当前状态与动作，考勤面板回传后同步变化', async () => {
    const { wrapper } = mountPage()
    await flushPromises()

    // 没有未下班的班次：按钮同时说明「未打卡」和「点下去做什么」
    expect(mocks.getCurrentAttendance).toHaveBeenCalledTimes(1)
    expect(wrapper.get('.kw-att-btn').text()).toBe('未打卡 · 上班打卡')
    expect(wrapper.find('.attendance-stub').exists()).toBe(false)

    await wrapper.get('.kw-att-btn').trigger('click')
    await flushPromises()

    expect(wrapper.get('.dialog-stub').text()).toContain('今日考勤')
    expect(wrapper.find('.attendance-stub').exists()).toBe(true)

    // 面板加载完成 / 上班打卡成功后回传 currentAttendance，主按钮立即变成下班打卡
    await wrapper.get('.t-att-in').trigger('click')
    expect(wrapper.get('.kw-att-btn').text()).toBe('已打卡上班 · 下班打卡')

    await wrapper.get('.t-att-out').trigger('click')
    expect(wrapper.get('.kw-att-btn').text()).toBe('未打卡 · 上班打卡')
  })

  it('已有当前班次时，首屏按钮直接显示「已打卡上班 · 下班打卡」', async () => {
    mocks.getCurrentAttendance.mockResolvedValue({
      code: 200,
      data: { id_wsh: 3, check_in_at_wsh: '2026-11-20T09:00:00' },
    })
    const { wrapper } = mountPage()
    await flushPromises()

    expect(wrapper.get('.kw-att-btn').text()).toBe('已打卡上班 · 下班打卡')
  })

  it('没有看护者档案时保留入驻申请入口', async () => {
    mocks.getMyKeeperInfo.mockResolvedValue({ code: 200, data: null })
    const { wrapper } = mountPage()
    await flushPromises()

    expect(wrapper.find('.kw-layout').exists()).toBe(false)
    expect(wrapper.text()).toContain('还没有照护师档案')
    expect(wrapper.findAll('a').some(a => a.attributes('href') === '/keeper-apply')).toBe(true)
  })

  // ── 抢单 ──
  it('抢单成功：调用 claimOrder({order_no_wsh})、提示成功并刷新两个列表', async () => {
    mocks.claimOrder.mockResolvedValue({ code: 200 })
    const { wrapper, addToast } = mountPage()
    await flushPromises()

    await wrapper.get('.t-claim').trigger('click')
    await flushPromises()

    expect(mocks.claimOrder).toHaveBeenCalledWith({ order_no_wsh: 'ORD-1' })
    expect(addToast).toHaveBeenCalledWith('抢单成功', 'success')
    expect(mocks.scrollPoolOrders).toHaveBeenCalledTimes(2)
    expect(mocks.scrollKeeperOrders).toHaveBeenCalledTimes(2)
  })

  it('抢单失败(400)：把后端原因提示出来，并刷新列表', async () => {
    mocks.claimOrder.mockRejectedValue({
      response: { data: { message: '排班已满' } },
    })
    const { wrapper, addToast } = mountPage()
    await flushPromises()

    await wrapper.get('.t-claim').trigger('click')
    await flushPromises()

    // 400 的具体原因在 response.data.message 里，不能只显示 "Request failed with status code 400"
    expect(addToast).toHaveBeenCalledWith('排班已满', 'error')
    expect(mocks.scrollPoolOrders).toHaveBeenCalledTimes(2)
  })

  // ── 拒单 ──
  it('拒单：调用 rejectOrder、提示「已拒单」', async () => {
    mocks.rejectOrder.mockResolvedValue({ code: 200 })
    const { wrapper, addToast } = mountPage()
    await flushPromises()
    await goTo(wrapper, '我的订单')

    await wrapper.get('.t-reject').trigger('click')
    await flushPromises()

    expect(mocks.rejectOrder).toHaveBeenCalledWith({ order_no_wsh: 'ORD-1' })
    expect(addToast).toHaveBeenCalledWith('已拒单', 'success')
  })

  it('拒单失败：显示后端 message', async () => {
    mocks.rejectOrder.mockResolvedValue({ code: 400, message: '订单已被他人接走' })
    const { wrapper, addToast } = mountPage()
    await flushPromises()
    await goTo(wrapper, '我的订单')

    await wrapper.get('.t-reject').trigger('click')
    await flushPromises()

    expect(addToast).toHaveBeenCalledWith('订单已被他人接走', 'error')
  })

  // ── 履约动作 ──
  it('确认收宠：原样透传 payload 给 markReceived', async () => {
    mocks.markReceived.mockResolvedValue({ code: 200 })
    const { wrapper, addToast } = mountPage()
    await flushPromises()
    await goTo(wrapper, '我的订单')

    await wrapper.get('.t-receive').trigger('click')
    await flushPromises()

    expect(mocks.markReceived).toHaveBeenCalledWith({ order_no_wsh: 'ORD-1', code: '1234' })
    expect(addToast).toHaveBeenCalledWith('已确认收宠', 'success')
  })

  it('开始服务：把订单号与图片装进 FormData 交给 startService', async () => {
    mocks.startService.mockResolvedValue({ code: 200 })
    const { wrapper, addToast } = mountPage()
    await flushPromises()
    await goTo(wrapper, '我的订单')

    await wrapper.get('.t-start').trigger('click')
    await flushPromises()

    expect(mocks.startService).toHaveBeenCalledTimes(1)
    const fd = mocks.startService.mock.calls[0][0]
    expect(fd).toBeInstanceOf(FormData)
    expect(fd.get('order_no_wsh')).toBe('ORD-1')
    expect(fd.get('file')).toBeTruthy()
    expect(addToast).toHaveBeenCalledWith('已开始护理', 'success')
  })

  it('确认接走：0 费用时提示订单已完成', async () => {
    mocks.completeOrder.mockResolvedValue({ code: 200, data: { completed_wsh: true } })
    const { wrapper, addToast } = mountPage()
    await flushPromises()
    await goTo(wrapper, '我的订单')

    await wrapper.get('.t-checkout').trigger('click')
    await flushPromises()

    expect(mocks.completeOrder).toHaveBeenCalledWith({ order_no_wsh: 'ORD-1' })
    expect(addToast).toHaveBeenCalledWith('已确认接走，订单已完成', 'success')
  })

  it('确认接走：有超时费时提示待主人补费金额', async () => {
    mocks.completeOrder.mockResolvedValue({
      code: 200,
      data: { completed_wsh: false, overtime_amount_wsh: 35.5 },
    })
    const { wrapper, addToast } = mountPage()
    await flushPromises()
    await goTo(wrapper, '我的订单')

    await wrapper.get('.t-checkout').trigger('click')
    await flushPromises()

    expect(addToast).toHaveBeenCalledWith('已确认接走，待主人补费 ¥35.50', 'warning')
  })

  // ── 状态分类 ──
  it('状态分类只包含真实状态 key：待接收 / 已接收 / 服务中 / 已完成 / 已退款', async () => {
    const { wrapper } = mountPage()
    await flushPromises()
    await goTo(wrapper, '我的订单')

    const tabs = wrapper.findAll('.kw-status-tab')
    expect(tabs.map(tab => tab.text())).toEqual(['全部', '待接收', '已接收', '服务中', '已完成', '已退款'])
    // 退款必须用后端真实状态 refunded，不能误用 refunding / cancelled
    expect(statusTab(wrapper, 'refunded').exists()).toBe(true)
    expect(wrapper.find('.kw-status-tab[data-status="refunding"]').exists()).toBe(false)
    expect(wrapper.find('.kw-status-tab[data-status="cancelled"]').exists()).toBe(false)
  })

  it('我的订单：首屏 20 条，切分类带 status 且重置游标，加载更多带 cursor 与当前 status', async () => {
    const firstPage = Array.from({ length: 20 }, (_, index) => ({
      id_wsh: 100 + index,
      order_no_wsh: `ORD-${index}`,
      pet_name_wsh: '豆豆',
      status_wsh: 'confirmed',
    }))
    mocks.scrollKeeperOrders
      .mockResolvedValueOnce({
        code: 200,
        data: { list_wsh: firstPage, next_cursor_wsh: 'c1', has_more_wsh: true },
      })
      .mockResolvedValueOnce({
        code: 200,
        data: { list_wsh: firstPage, next_cursor_wsh: 'c2', has_more_wsh: true },
      })
      .mockResolvedValueOnce({
        code: 200,
        data: {
          list_wsh: [{ id_wsh: 200, order_no_wsh: 'ORD-NEXT', pet_name_wsh: '毛毛', status_wsh: 'confirmed' }],
          next_cursor_wsh: '',
          has_more_wsh: false,
        },
      })
    const { wrapper } = mountPage()
    await flushPromises()
    await goTo(wrapper, '我的订单')

    // 首屏固定 20 条，不再一次渲染全部
    expect(mocks.scrollKeeperOrders).toHaveBeenNthCalledWith(1, { size: 20 })
    expect(wrapper.getComponent(OrdersTabStub).props('activeOrders')).toHaveLength(20)

    await statusTab(wrapper, 'confirmed').trigger('click')
    await flushPromises()

    // 切分类：重置列表与游标，第一请求只带 status
    expect(mocks.scrollKeeperOrders).toHaveBeenNthCalledWith(2, { size: 20, status: 'confirmed' })
    expect(wrapper.getComponent(OrdersTabStub).props('activeOrders')).toHaveLength(20)

    const more = wrapper.findAll('button').find(button => button.text() === '加载更多订单')
    expect(more).toBeTruthy()
    await more.trigger('click')
    await flushPromises()

    // 加载更多必须带当前分类与游标，避免混入别的状态
    expect(mocks.scrollKeeperOrders).toHaveBeenNthCalledWith(3, {
      size: 20,
      cursor: 'c2',
      status: 'confirmed',
    })
    const orders = wrapper.getComponent(OrdersTabStub).props('activeOrders')
    expect(orders.map(order => order.order_no_wsh)).toContain('ORD-NEXT')
    expect(orders).toHaveLength(21)
    expect(wrapper.findAll('button').some(button => button.text() === '加载更多订单')).toBe(false)
  })

  // ── 订单工作区：摘要 + 对话/动态 ──
  it('订单工作区：卡片同时展示最近消息与动态进度，选中后同区切换对话/照护动态', async () => {
    mocks.getConversation.mockResolvedValue({
      code: 200,
      data: [{ id_wsh: 1, content_wsh: '你好', created_at_wsh: '2026-11-20T10:00:00' }],
    })
    mocks.getDailyStatus.mockResolvedValue({
      code: 200,
      data: {
        uploaded_days_wsh: ['2026-11-01'],
        required_days_wsh: ['2026-11-01', '2026-11-02'],
        missing_days_wsh: ['2026-11-02'],
      },
    })
    mocks.getTimeline.mockResolvedValue({ code: 200, data: [{ id_wsh: 1 }] })
    mocks.sendMessage.mockResolvedValue({ code: 200 })
    mocks.uploadTimeline.mockResolvedValue({ code: 200 })
    const { wrapper, addToast } = mountPage()
    await flushPromises()

    await goTo(wrapper, '订单工作区')

    // 进入工作区只预取摘要，不自动选中订单、不弹窗
    expect(mocks.getConversation).toHaveBeenCalledWith('55')
    expect(mocks.getDailyStatus).toHaveBeenCalledWith('55')
    expect(mocks.getTimeline).not.toHaveBeenCalled()
    expect(wrapper.find('.dialog-stub').exists()).toBe(false)

    const card = wrapper.findAll('.kw-ws-card')[0]
    expect(card.text()).toContain('ORD-1')
    expect(card.text()).toContain('豆豆')
    expect(card.text()).toContain('最近消息')
    expect(card.text()).toContain('你好')
    expect(card.text()).toContain('动态进度')
    expect(card.text()).toContain('已上传 1/2 天')
    expect(card.text()).toContain('2026-11-02')

    await card.trigger('click')
    await flushPromises()

    // 选中后加载完整会话；动态进度复用预取缓存，只补时间线
    expect(mocks.getConversation).toHaveBeenCalledTimes(2)
    expect(mocks.getTimeline).toHaveBeenCalledWith('55')
    expect(wrapper.get('[data-testid="chat-input"]').exists()).toBe(true)

    await wrapper.get('[data-testid="chat-input"]').setValue('在吗')
    await wrapper.get('[data-testid="chat-send"]').trigger('click')
    await flushPromises()

    expect(mocks.sendMessage).toHaveBeenCalledWith('55', { content_wsh: '在吗' })

    // 同一个订单区内切到照护动态，订单锁定给 KeeperDailyTab
    await wrapper.get('[data-detail-tab="daily"]').trigger('click')
    await flushPromises()

    const daily = wrapper.getComponent(DailyTabStub)
    expect(daily.props('orderLocked')).toBe(true)
    expect(daily.props('orderId')).toBe('55')

    await wrapper.get('.t-upload').trigger('click')
    await flushPromises()

    const [orderId, fd] = mocks.uploadTimeline.mock.calls[0]
    expect(orderId).toBe(55)
    expect(fd).toBeInstanceOf(FormData)
    expect(fd.get('type_wsh')).toBe('feeding')
    expect(fd.get('content_wsh')).toBe('已喂食')
    expect(addToast).toHaveBeenCalledWith('动态已上传', 'success')
  })

  it('工作区只预取最多 8 笔摘要，其余标示「摘要未加载」', async () => {
    mocks.scrollKeeperOrders.mockResolvedValue({
      code: 200,
      data: {
        list_wsh: Array.from({ length: 12 }, (_, index) => ({
          id_wsh: 100 + index,
          order_no_wsh: `ORD-${index}`,
          pet_name_wsh: '豆豆',
          status_wsh: 'received',
        })),
        next_cursor_wsh: '',
        has_more_wsh: false,
      },
    })
    const { wrapper } = mountPage()
    await flushPromises()
    await goTo(wrapper, '订单工作区')

    expect(mocks.getConversation).toHaveBeenCalledTimes(8)
    expect(mocks.getDailyStatus).toHaveBeenCalledTimes(8)
    const cards = wrapper.findAll('.kw-ws-card')
    expect(cards).toHaveLength(12)
    expect(cards[11].text()).toContain('摘要未加载')
  })

  it('工作区加载更多：新分页追加摘要并保留当前分类', async () => {
    mocks.scrollKeeperOrders
      // 首屏「我的订单」先占一次
      .mockResolvedValueOnce({
        code: 200,
        data: {
          list_wsh: [{ id_wsh: 55, order_no_wsh: 'ORD-1', pet_name_wsh: '豆豆', status_wsh: 'received' }],
          next_cursor_wsh: '',
          has_more_wsh: false,
        },
      })
      // 进入工作区：工作区自己的第一页
      .mockResolvedValueOnce({
        code: 200,
        data: {
          list_wsh: [{ id_wsh: 55, order_no_wsh: 'ORD-1', pet_name_wsh: '豆豆', status_wsh: 'received' }],
          next_cursor_wsh: 'ws-cursor',
          has_more_wsh: true,
        },
      })
      .mockResolvedValue({
        code: 200,
        data: {
          list_wsh: [{ id_wsh: 66, order_no_wsh: 'ORD-2', pet_name_wsh: '毛毛', status_wsh: 'received' }],
          next_cursor_wsh: '',
          has_more_wsh: false,
        },
      })
    const { wrapper } = mountPage()
    await flushPromises()
    await goTo(wrapper, '订单工作区')

    const more = wrapper.findAll('button').find(button => button.text() === '加载更多订单')
    expect(more).toBeTruthy()
    await more.trigger('click')
    await flushPromises()

    // 加载更多用工作区自己的游标，且仍处于「服务中」分类
    expect(mocks.scrollKeeperOrders).toHaveBeenLastCalledWith({ size: 20, cursor: 'ws-cursor', status: 'in_progress' })
    expect(mocks.getConversation).toHaveBeenCalledWith('66')
    expect(wrapper.findAll('.kw-ws-card')).toHaveLength(2)
  })

  it('工作区切分类后加载对应第一页，并清掉已不在结果里的选中订单', async () => {
    mocks.scrollKeeperOrders
      .mockResolvedValueOnce({
        code: 200,
        data: {
          list_wsh: [{ id_wsh: 55, order_no_wsh: 'ORD-1', pet_name_wsh: '豆豆', status_wsh: 'received' }],
          next_cursor_wsh: '',
          has_more_wsh: false,
        },
      })
      .mockResolvedValueOnce({
        code: 200,
        data: {
          list_wsh: [{ id_wsh: 55, order_no_wsh: 'ORD-1', pet_name_wsh: '豆豆', status_wsh: 'received' }],
          next_cursor_wsh: '',
          has_more_wsh: false,
        },
      })
      .mockResolvedValueOnce({
        code: 200,
        data: {
          list_wsh: [{ id_wsh: 77, order_no_wsh: 'ORD-3', pet_name_wsh: '球球', status_wsh: 'completed' }],
          next_cursor_wsh: '',
          has_more_wsh: false,
        },
      })
    const { wrapper } = mountPage()
    await flushPromises()
    await goTo(wrapper, '订单工作区')

    await wrapper.findAll('.kw-ws-card')[0].trigger('click')
    await flushPromises()
    expect(wrapper.find('.kw-ws-detail').text()).toContain('ORD-1')

    await statusTab(wrapper, 'completed,refunded').trigger('click')
    await flushPromises()

    expect(mocks.scrollKeeperOrders).toHaveBeenLastCalledWith({ size: 20, status: 'completed,refunded' })
    expect(wrapper.find('.kw-ws-detail').text()).toContain('从左侧选择一笔订单')
    expect(wrapper.findAll('.kw-ws-card')).toHaveLength(1)
  })

  // ── 两处分类 / 分页互不影响 ──
  it('分类互不影响：我的订单选 confirmed 后进入工作区仍按「服务中」取第一页', async () => {
    mockOrdersByStatus()
    const { wrapper } = mountPage()
    await flushPromises()
    await goTo(wrapper, '我的订单')

    await statusTab(wrapper, 'confirmed').trigger('click')
    await flushPromises()
    expect(mocks.scrollKeeperOrders).toHaveBeenLastCalledWith({ size: 20, status: 'confirmed' })

    await goTo(wrapper, '订单工作区')

    // 工作区默认「服务中」：请求带 status=in_progress，列表是服务中那一份
    expect(mocks.scrollKeeperOrders).toHaveBeenLastCalledWith({ size: 20, status: 'in_progress' })
    expect(statusTab(wrapper, 'in_progress').attributes('aria-selected')).toBe('true')
    expect(wrapper.findAll('.kw-ws-card')).toHaveLength(1)
    expect(wrapper.get('.kw-ws-card').text()).toContain('ORD-33')
  })

  it('分类互不影响：工作区切「其他」后回到我的订单仍是 confirmed', async () => {
    mockOrdersByStatus()
    const { wrapper } = mountPage()
    await flushPromises()
    await goTo(wrapper, '我的订单')
    await statusTab(wrapper, 'confirmed').trigger('click')
    await flushPromises()

    await goTo(wrapper, '订单工作区')
    await statusTab(wrapper, 'completed,refunded').trigger('click')
    await flushPromises()
    expect(mocks.scrollKeeperOrders).toHaveBeenLastCalledWith({ size: 20, status: 'completed,refunded' })

    const callsBefore = mocks.scrollKeeperOrders.mock.calls.length
    await goTo(wrapper, '我的订单')

    // 回到我的订单不重新取数，分类与列表都还是 confirmed 的那一份
    expect(mocks.scrollKeeperOrders.mock.calls.length).toBe(callsBefore)
    expect(statusTab(wrapper, 'confirmed').attributes('aria-selected')).toBe('true')
    expect(wrapper.getComponent(OrdersTabStub).props('activeOrders').map(order => order.id_wsh)).toEqual([11])
  })

  it('分页游标互不影响：两边各自按自己的 cursor 加载更多', async () => {
    mocks.scrollKeeperOrders
      // 我的订单首屏
      .mockResolvedValueOnce({
        code: 200,
        data: {
          list_wsh: [{ id_wsh: 1, order_no_wsh: 'ORD-1', pet_name_wsh: '豆豆', status_wsh: 'received' }],
          next_cursor_wsh: 'my-c1',
          has_more_wsh: true,
        },
      })
      // 工作区首屏
      .mockResolvedValueOnce({
        code: 200,
        data: {
          list_wsh: [{ id_wsh: 2, order_no_wsh: 'ORD-2', pet_name_wsh: '毛毛', status_wsh: 'received' }],
          next_cursor_wsh: 'ws-c1',
          has_more_wsh: true,
        },
      })
      // 工作区加载更多
      .mockResolvedValueOnce({
        code: 200,
        data: {
          list_wsh: [{ id_wsh: 3, order_no_wsh: 'ORD-3', pet_name_wsh: '球球', status_wsh: 'received' }],
          next_cursor_wsh: '',
          has_more_wsh: false,
        },
      })
      // 我的订单加载更多
      .mockResolvedValueOnce({
        code: 200,
        data: {
          list_wsh: [{ id_wsh: 4, order_no_wsh: 'ORD-4', pet_name_wsh: '花花', status_wsh: 'received' }],
          next_cursor_wsh: '',
          has_more_wsh: false,
        },
      })
    const { wrapper } = mountPage()
    await flushPromises()

    await goTo(wrapper, '订单工作区')
    expect(mocks.scrollKeeperOrders).toHaveBeenNthCalledWith(2, { size: 20, status: 'in_progress' })

    const wsMore = wrapper.findAll('button').find(button => button.text() === '加载更多订单')
    expect(wsMore).toBeTruthy()
    await wsMore.trigger('click')
    await flushPromises()
    expect(mocks.scrollKeeperOrders).toHaveBeenNthCalledWith(3, { size: 20, cursor: 'ws-c1', status: 'in_progress' })

    await goTo(wrapper, '我的订单')
    const myMore = wrapper.findAll('button').find(button => button.text() === '加载更多订单')
    expect(myMore).toBeTruthy()
    await myMore.trigger('click')
    await flushPromises()

    // 我的订单仍用自己首屏拿到的游标，没被工作区的 ws-c1 覆盖
    expect(mocks.scrollKeeperOrders).toHaveBeenNthCalledWith(4, { size: 20, cursor: 'my-c1' })
    expect(wrapper.getComponent(OrdersTabStub).props('activeOrders').map(order => order.id_wsh)).toEqual([1, 4])
  })

  it('空消息不发送', async () => {
    mocks.getConversation.mockResolvedValue({ code: 200, data: [] })
    const { wrapper } = mountPage()
    await flushPromises()

    await goTo(wrapper, '订单工作区')
    await wrapper.findAll('.kw-ws-card')[0].trigger('click')
    await flushPromises()

    await wrapper.get('[data-testid="chat-send"]').trigger('click')
    await flushPromises()

    expect(mocks.sendMessage).not.toHaveBeenCalled()
  })

  // ── 游标分页（抢单池） ──
  it('加载更多：按游标继续拉取抢单池', async () => {
    mocks.scrollPoolOrders
      .mockResolvedValueOnce({
        code: 200,
        data: { list_wsh: [{ order_no_wsh: 'ORD-1' }], next_cursor_wsh: 'c1', has_more_wsh: true },
      })
      .mockResolvedValue({
        code: 200,
        data: { list_wsh: [{ order_no_wsh: 'ORD-2' }], next_cursor_wsh: '', has_more_wsh: false },
      })
    const { wrapper } = mountPage()
    await flushPromises()

    const more = wrapper.findAll('button').find(button => button.text() === '加载更多可抢订单')
    expect(more).toBeTruthy()
    await more.trigger('click')
    await flushPromises()

    expect(mocks.scrollPoolOrders).toHaveBeenLastCalledWith({ size: 50, cursor: 'c1' })
    expect(wrapper.findAll('button').some(button => button.text() === '加载更多可抢订单')).toBe(false)
  })

  it('接口 list_wsh 非数组时按空列表处理，不炸页面', async () => {
    mocks.scrollKeeperOrders.mockResolvedValue({
      code: 200,
      data: { list_wsh: { unexpected: true }, next_cursor_wsh: 'x', has_more_wsh: true },
    })
    const { wrapper } = mountPage()
    await flushPromises()
    await goTo(wrapper, '我的订单')

    expect(wrapper.getComponent(OrdersTabStub).props('activeOrders')).toEqual([])
  })

  // ── 分类切换：等待期间不闪空态 ──
  it('我的订单切分类：响应前保留上一份列表并显示加载提示，不闪空态', async () => {
    const pending = deferred()
    const { wrapper } = mountPage()
    await flushPromises()
    await goTo(wrapper, '我的订单')

    expect(wrapper.getComponent(OrdersTabStub).props('activeOrders')).toHaveLength(1)

    mocks.scrollKeeperOrders.mockReturnValueOnce(pending.promise)
    await statusTab(wrapper, 'confirmed').trigger('click')
    await flushPromises()

    // 等待期间列表仍是上一份结果，不是空数组；加载反馈可见且带 aria-busy
    expect(wrapper.getComponent(OrdersTabStub).props('activeOrders')).toHaveLength(1)
    expect(wrapper.get('.kw-block--mine').attributes('aria-busy')).toBe('true')
    expect(wrapper.get('.kw-status-hint').text()).toBe('正在筛选订单…')

    pending.resolve({
      code: 200,
      data: {
        list_wsh: [{ id_wsh: 77, order_no_wsh: 'ORD-3', pet_name_wsh: '球球', status_wsh: 'confirmed' }],
        next_cursor_wsh: 'c9',
        has_more_wsh: true,
      },
    })
    await flushPromises()

    // 数据与游标同批替换，提示行清空但高度仍在
    expect(wrapper.get('.kw-block--mine').attributes('aria-busy')).toBe('false')
    expect(wrapper.get('.kw-status-hint').text()).toBe('')
    expect(wrapper.getComponent(OrdersTabStub).props('activeOrders').map(order => order.id_wsh)).toEqual([77])

    const more = wrapper.findAll('button').find(button => button.text() === '加载更多订单')
    expect(more).toBeTruthy()
    await more.trigger('click')
    await flushPromises()
    // 游标也换成了新分类那一页的，不会沿用旧分类的游标
    expect(mocks.scrollKeeperOrders).toHaveBeenLastCalledWith({ size: 20, cursor: 'c9', status: 'confirmed' })
  })

  it('工作区切分类：等待期间保留列表与已选详情，成功后才清理选中订单', async () => {
    const pending = deferred()
    const { wrapper } = mountPage()
    await flushPromises()
    await goTo(wrapper, '订单工作区')

    await wrapper.findAll('.kw-ws-card')[0].trigger('click')
    await flushPromises()
    expect(wrapper.find('.kw-ws-detail').text()).toContain('ORD-1')

    mocks.scrollKeeperOrders.mockReturnValueOnce(pending.promise)
    await statusTab(wrapper, 'completed,refunded').trigger('click')
    await flushPromises()

    // 等待期间列表与右侧详情都不动，工作区不会先闪一下空态
    expect(wrapper.findAll('.kw-ws-card')).toHaveLength(1)
    expect(wrapper.find('.kw-ws-detail').text()).toContain('ORD-1')
    expect(wrapper.get('.kw-ws-list').attributes('aria-busy')).toBe('true')

    pending.resolve({
      code: 200,
      data: {
        list_wsh: [{ id_wsh: 77, order_no_wsh: 'ORD-3', pet_name_wsh: '球球', status_wsh: 'completed' }],
        next_cursor_wsh: '',
        has_more_wsh: false,
      },
    })
    await flushPromises()

    // 更新成功之后才清掉不在新结果里的旧选中订单
    expect(wrapper.get('.kw-ws-list').attributes('aria-busy')).toBe('false')
    expect(wrapper.findAll('.kw-ws-card')).toHaveLength(1)
    expect(wrapper.find('.kw-ws-detail').text()).toContain('从左侧选择一笔订单')
  })

  it('工作区切分类失败：保持旧分类与旧列表并提示', async () => {
    const { wrapper, addToast } = mountPage()
    await flushPromises()
    await goTo(wrapper, '订单工作区')

    // 挂载时的首屏请求已用完默认实现，这里只让「切分类」这一次失败
    mocks.scrollKeeperOrders.mockRejectedValueOnce({ response: { data: { message: '服务繁忙' } } })
    await statusTab(wrapper, 'completed,refunded').trigger('click')
    await flushPromises()

    // 分类与列表都不动（active 只在成功时提交，所以不需要回滚），并明确提示
    expect(statusTab(wrapper, 'in_progress').attributes('aria-selected')).toBe('true')
    expect(statusTab(wrapper, 'completed,refunded').attributes('aria-selected')).toBe('false')
    expect(wrapper.findAll('.kw-ws-card')).toHaveLength(1)
    expect(wrapper.get('.kw-ws-list').attributes('aria-busy')).toBe('false')
    expect(addToast).toHaveBeenCalledWith('订单筛选失败，已保留上一次结果', 'error')
  })

  it('快速连点分类：先发出的旧响应返回更晚，也不会覆盖后选的分类', async () => {
    const slowFirst = deferred()
    const fastSecond = deferred()
    const { wrapper } = mountPage()
    await flushPromises()
    await goTo(wrapper, '我的订单')

    // 挂载时的首屏请求已用完默认实现，这两次排队分别对应两次点击
    mocks.scrollKeeperOrders
      .mockReturnValueOnce(slowFirst.promise)
      .mockReturnValueOnce(fastSecond.promise)
    await statusTab(wrapper, 'confirmed').trigger('click')
    await statusTab(wrapper, 'completed').trigger('click')

    fastSecond.resolve({
      code: 200,
      data: {
        list_wsh: [{ id_wsh: 22, order_no_wsh: 'ORD-22', pet_name_wsh: '毛毛', status_wsh: 'completed' }],
        next_cursor_wsh: '',
        has_more_wsh: false,
      },
    })
    await flushPromises()
    expect(wrapper.getComponent(OrdersTabStub).props('activeOrders').map(order => order.id_wsh)).toEqual([22])

    slowFirst.resolve({
      code: 200,
      data: {
        list_wsh: [{ id_wsh: 11, order_no_wsh: 'ORD-11', pet_name_wsh: '豆豆', status_wsh: 'confirmed' }],
        next_cursor_wsh: '',
        has_more_wsh: false,
      },
    })
    await flushPromises()

    // 旧分类的慢响应被丢弃：列表与选中分类都还是 completed
    expect(wrapper.getComponent(OrdersTabStub).props('activeOrders').map(order => order.id_wsh)).toEqual([22])
    expect(statusTab(wrapper, 'completed').attributes('aria-selected')).toBe('true')
  })

  it('加载更多在途时切分类：旧分类的下一页不会混进新列表', async () => {
    const pendingMore = deferred()
    mocks.scrollKeeperOrders
      .mockResolvedValueOnce({
        code: 200,
        data: {
          list_wsh: [{ id_wsh: 1, order_no_wsh: 'ORD-1', pet_name_wsh: '豆豆', status_wsh: 'received' }],
          next_cursor_wsh: 'c1',
          has_more_wsh: true,
        },
      })
      .mockReturnValueOnce(pendingMore.promise)
      .mockResolvedValueOnce({
        code: 200,
        data: {
          list_wsh: [{ id_wsh: 9, order_no_wsh: 'ORD-9', pet_name_wsh: '球球', status_wsh: 'completed' }],
          next_cursor_wsh: '',
          has_more_wsh: false,
        },
      })
    const { wrapper } = mountPage()
    await flushPromises()
    await goTo(wrapper, '我的订单')

    const more = wrapper.findAll('button').find(button => button.text() === '加载更多订单')
    await more.trigger('click')
    await statusTab(wrapper, 'completed').trigger('click')
    await flushPromises()

    pendingMore.resolve({
      code: 200,
      data: {
        list_wsh: [{ id_wsh: 2, order_no_wsh: 'ORD-2', pet_name_wsh: '毛毛', status_wsh: 'received' }],
        next_cursor_wsh: '',
        has_more_wsh: false,
      },
    })
    await flushPromises()

    // 新分类的第一页不被旧分类的分页结果污染
    expect(wrapper.getComponent(OrdersTabStub).props('activeOrders').map(order => order.id_wsh)).toEqual([9])
  })

  it('连续两次切换、最后一次失败：分类与列表都停在最初的分类，不会退回到中间那次', async () => {
    const firstSwitch = deferred()
    const lastSwitch = deferred()
    const { wrapper, addToast } = mountPage()
    await flushPromises()
    await goTo(wrapper, '我的订单')

    mocks.scrollKeeperOrders
      .mockReturnValueOnce(firstSwitch.promise)
      .mockReturnValueOnce(lastSwitch.promise)
    await statusTab(wrapper, 'confirmed').trigger('click')
    await statusTab(wrapper, 'completed').trigger('click')

    lastSwitch.reject({ response: { data: { message: '服务繁忙' } } })
    await flushPromises()

    // 列表还是最初的「全部」那一份，分类也仍是「全部」，不是中间那次 confirmed
    expect(wrapper.getComponent(OrdersTabStub).props('activeOrders').map(order => order.id_wsh)).toEqual([55])
    expect(statusTab(wrapper, '').attributes('aria-selected')).toBe('true')
    expect(statusTab(wrapper, 'confirmed').attributes('aria-selected')).toBe('false')
    expect(wrapper.get('.kw-block--mine').attributes('aria-busy')).toBe('false')
    expect(addToast).toHaveBeenCalledWith('订单筛选失败，已保留上一次结果', 'error')

    // 更早发出的 confirmed 响应此时才回来，同样不能覆盖
    firstSwitch.resolve({
      code: 200,
      data: {
        list_wsh: [{ id_wsh: 11, order_no_wsh: 'ORD-11', pet_name_wsh: '豆豆', status_wsh: 'confirmed' }],
        next_cursor_wsh: '',
        has_more_wsh: false,
      },
    })
    await flushPromises()

    expect(wrapper.getComponent(OrdersTabStub).props('activeOrders').map(order => order.id_wsh)).toEqual([55])
    expect(statusTab(wrapper, '').attributes('aria-selected')).toBe('true')
  })

  it('我的订单：分类切换在途时禁用加载更多，点击也不会带旧游标发请求', async () => {
    const pending = deferred()
    mocks.scrollKeeperOrders.mockResolvedValueOnce({
      code: 200,
      data: {
        list_wsh: [{ id_wsh: 1, order_no_wsh: 'ORD-1', pet_name_wsh: '豆豆', status_wsh: 'received' }],
        next_cursor_wsh: 'c1',
        has_more_wsh: true,
      },
    })
    const { wrapper } = mountPage()
    await flushPromises()
    await goTo(wrapper, '我的订单')

    const more = wrapper.findAll('button').find(button => button.text() === '加载更多订单')
    expect(more.attributes('disabled')).toBeUndefined()

    mocks.scrollKeeperOrders.mockReturnValueOnce(pending.promise)
    await statusTab(wrapper, 'confirmed').trigger('click')
    await flushPromises()

    const callsDuringSwitch = mocks.scrollKeeperOrders.mock.calls.length
    const switchingMore = wrapper.findAll('button').find(button => button.text() === '加载更多订单')
    expect(switchingMore.attributes('disabled')).toBeDefined()

    await switchingMore.trigger('click')
    await flushPromises()
    // 处理函数也拦了一道：不会用旧游标配新分类去请求
    expect(mocks.scrollKeeperOrders.mock.calls.length).toBe(callsDuringSwitch)

    pending.resolve({
      code: 200,
      data: {
        list_wsh: [{ id_wsh: 77, order_no_wsh: 'ORD-3', pet_name_wsh: '球球', status_wsh: 'confirmed' }],
        next_cursor_wsh: '',
        has_more_wsh: false,
      },
    })
    await flushPromises()
    expect(wrapper.getComponent(OrdersTabStub).props('activeOrders').map(order => order.id_wsh)).toEqual([77])
  })

  it('工作区：分类切换在途时同样禁用加载更多', async () => {
    const pending = deferred()
    mocks.scrollKeeperOrders.mockResolvedValue({
      code: 200,
      data: {
        list_wsh: [{ id_wsh: 1, order_no_wsh: 'ORD-1', pet_name_wsh: '豆豆', status_wsh: 'received' }],
        next_cursor_wsh: 'ws-c1',
        has_more_wsh: true,
      },
    })
    const { wrapper } = mountPage()
    await flushPromises()
    await goTo(wrapper, '订单工作区')

    const more = wrapper.findAll('button').find(button => button.text() === '加载更多订单')
    expect(more.attributes('disabled')).toBeUndefined()

    mocks.scrollKeeperOrders.mockReturnValueOnce(pending.promise)
    await statusTab(wrapper, 'completed,refunded').trigger('click')
    await flushPromises()

    const callsDuringSwitch = mocks.scrollKeeperOrders.mock.calls.length
    const switchingMore = wrapper.findAll('button').find(button => button.text() === '加载更多订单')
    expect(switchingMore.attributes('disabled')).toBeDefined()

    await switchingMore.trigger('click')
    await flushPromises()
    expect(mocks.scrollKeeperOrders.mock.calls.length).toBe(callsDuringSwitch)

    pending.resolve({ code: 200, data: { list_wsh: [], next_cursor_wsh: '', has_more_wsh: false } })
    await flushPromises()
  })

  // ── 工作区：分类口径 / 排序 / 未读红点 ──
  it('工作区分类：服务中 → 待接收 → 已接收 → 其他（已完成与已退款合并），默认停在服务中', async () => {
    const { wrapper } = mountPage()
    await flushPromises()
    await goTo(wrapper, '订单工作区')

    const tabs = wrapper.findAll('.kw-status-tab')
    expect(tabs.map(tab => tab.text())).toEqual(['服务中', '待接收', '已接收', '其他'])
    // 「其他」是多个真实状态的下推，不能写成 completed 单值
    expect(tabs.map(tab => tab.attributes('data-status')))
      .toEqual(['in_progress', 'confirmed', 'received', 'completed,refunded'])
    expect(statusTab(wrapper, 'in_progress').attributes('aria-selected')).toBe('true')
    expect(mocks.scrollKeeperOrders).toHaveBeenLastCalledWith({ size: 20, status: 'in_progress' })
  })

  it('工作区列表按状态推进排序：服务中 → 待接收 → 已接收 → 其他', async () => {
    mocks.scrollKeeperOrders.mockResolvedValue({
      code: 200,
      data: {
        list_wsh: [
          { id_wsh: 4, order_no_wsh: 'ORD-4', pet_name_wsh: '花花', status_wsh: 'completed' },
          { id_wsh: 1, order_no_wsh: 'ORD-1', pet_name_wsh: '豆豆', status_wsh: 'received' },
          { id_wsh: 3, order_no_wsh: 'ORD-3', pet_name_wsh: '球球', status_wsh: 'in_progress' },
          { id_wsh: 2, order_no_wsh: 'ORD-2', pet_name_wsh: '毛毛', status_wsh: 'confirmed' },
        ],
        next_cursor_wsh: '',
        has_more_wsh: false,
      },
    })
    const { wrapper } = mountPage()
    await flushPromises()
    await goTo(wrapper, '订单工作区')

    // 服务端按创建时间倒序给的是 4/1/3/2，界面按状态推进重排，其他（已完成）垫底
    expect(wrapper.findAll('.kw-ws-card').map(card => card.attributes('data-order-id')))
      .toEqual(['3', '2', '1', '4'])
  })

  it('工作区：有未读消息的订单显示红色数字并排到最前，点开后红点清零并通知服务端已读', async () => {
    mocks.scrollKeeperOrders.mockResolvedValue({
      code: 200,
      data: {
        list_wsh: [
          { id_wsh: 1, order_no_wsh: 'ORD-1', pet_name_wsh: '豆豆', status_wsh: 'in_progress' },
          { id_wsh: 2, order_no_wsh: 'ORD-2', pet_name_wsh: '毛毛', status_wsh: 'in_progress' },
          { id_wsh: 3, order_no_wsh: 'ORD-3', pet_name_wsh: '球球', status_wsh: 'in_progress' },
        ],
        next_cursor_wsh: '',
        has_more_wsh: false,
      },
    })
    // 同一笔订单两条未读：红点显示 2
    mocks.getUnreadMessages.mockResolvedValue({
      code: 200,
      data: [
        { id_wsh: 9, order_id_wsh: 3, from_user_id_wsh: 7, to_user_id_wsh: 1, content_wsh: '明天几点接？', created_at_wsh: '2026-11-20T10:00:00' },
        { id_wsh: 8, order_id_wsh: 3, from_user_id_wsh: 7, to_user_id_wsh: 1, content_wsh: '在吗', created_at_wsh: '2026-11-20T09:00:00' },
      ],
    })
    mocks.getConversation.mockImplementation(orderId => Promise.resolve({
      code: 200,
      data: String(orderId) === '3'
        ? [
            { id_wsh: 8, content_wsh: '在吗', created_at_wsh: '2026-11-20T09:00:00' },
            { id_wsh: 9, content_wsh: '明天几点接？', created_at_wsh: '2026-11-20T10:00:00' },
          ]
        : [],
    }))
    const { wrapper } = mountPage()
    await flushPromises()
    await goTo(wrapper, '订单工作区')

    const cards = wrapper.findAll('.kw-ws-card')
    expect(cards.map(card => card.attributes('data-order-id'))).toEqual(['3', '1', '2'])
    expect(cards[0].get('.kw-unread-badge').text()).toBe('2')
    expect(cards[0].text()).toContain('明天几点接？')
    expect(cards[1].find('.kw-unread-badge').exists()).toBe(false)

    await cards[0].trigger('click')
    await flushPromises()

    expect(mocks.markConversationRead).toHaveBeenCalledWith('3')
    expect(wrapper.findAll('.kw-unread-badge')).toHaveLength(0)
  })
})

describe('KeeperDailyTab 订单锁定', () => {
  it('默认（未锁定）仍保留订单选择器与侧栏进度', () => {
    const wrapper = mount(KeeperDailyTab, {
      props: {
        orderId: '',
        myOrders: [{ id_wsh: 55, service_name_wsh: '寄养', status_wsh: 'in_progress' }],
        dailyStatus: { uploaded_days_wsh: [1], required_days_wsh: [1, 2] },
      },
    })

    expect(wrapper.find('aside').exists()).toBe(true)
    expect(wrapper.get('select').exists()).toBe(true)
    expect(wrapper.text()).toContain('选择订单')
    expect(wrapper.text()).toContain('每日上传进度')
  })

  it('锁定单列：去掉订单侧栏，主区独占一行并挂 locked 类（列宽由组件自己持有）', () => {
    const wrapper = mount(KeeperDailyTab, {
      props: {
        orderId: '55',
        orderLocked: true,
        dailyStatus: { uploaded_days_wsh: [1], required_days_wsh: [1, 2] },
        careRecords: [{
          id_wsh: 1,
          type_wsh: 'feed',
          content_wsh: '上午 8 点喂食 100g，食欲正常，主人反馈良好，继续保持观察并记录饮水量',
          record_time_wsh: '2026-11-20T08:00:00',
        }],
      },
    })

    expect(wrapper.find('aside').exists()).toBe(false)
    expect(wrapper.get('section').classes()).toContain('records-grid--locked')
    expect(wrapper.find('main').exists()).toBe(true)
    // 长正文完整留在 DOM 里，靠断行收敛而不是裁切
    expect(wrapper.get('.timeline-item p').text()).toContain('继续保持观察')
  })
})
