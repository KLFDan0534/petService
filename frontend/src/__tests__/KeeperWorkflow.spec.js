import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import KeeperWorkflow from '@/views/user/KeeperWorkflow.vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'

/**
 * 看护者工作流测试。
 *
 * 批次 06 把 KeeperWorkflow 里 19 处 authStore.apiXxx 换成了具名 api，
 * 这里按「用户实际操作」的顺序验证：初始化加载 → 抢单/拒单 → 收宠/开始/接走 → 动态 → 聊天。
 *
 * 子组件用 stub 替身，通过 emit 触发父组件处理器——这样测的是父组件真正接的 API 与参数，
 * 不依赖子组件内部实现。
 */
const mocks = vi.hoisted(() => ({
  routerPush: vi.fn(),
  ensureProfileRequirement: vi.fn(),
  getCurrentUser: vi.fn(),
  updateCurrentUser: vi.fn(),
  getMyKeeperInfo: vi.fn(),
  updateKeeper: vi.fn(),
  createKeeper: vi.fn(),
  getMerchants: vi.fn(),
  getPoolOrders: vi.fn(),
  getMyKeeperOrders: vi.fn(),
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
  uploadFileToDirectory: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mocks.routerPush }),
}))

vi.mock('@/utils/profileRequirements', () => ({
  ensureProfileRequirement: mocks.ensureProfileRequirement,
  PROFILE_ACTIONS: { APPLY_KEEPER: 'applyKeeper' },
}))

vi.mock('@/api/auth', () => ({
  getCurrentUser: mocks.getCurrentUser,
  updateCurrentUser: mocks.updateCurrentUser,
}))

vi.mock('@/api/keeper', () => ({
  getMyKeeperInfo: mocks.getMyKeeperInfo,
  updateKeeper: mocks.updateKeeper,
  createKeeper: mocks.createKeeper,
}))

vi.mock('@/api/merchant', () => ({
  getMerchants: mocks.getMerchants,
}))

vi.mock('@/api/order', () => ({
  getPoolOrders: mocks.getPoolOrders,
  getMyKeeperOrders: mocks.getMyKeeperOrders,
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
}))

vi.mock('@/api/file', () => ({
  uploadFileToDirectory: mocks.uploadFileToDirectory,
}))

const OrdersTabStub = {
  name: 'KeeperOrdersTab',
  emits: ['claim', 'reject', 'receive', 'start', 'checkout', 'select-order', 'update:subTab'],
  template: `
    <div class="orders-stub">
      <button class="t-claim" @click="$emit('claim', { order_no_wsh: 'ORD-1' })">claim</button>
      <button class="t-reject" @click="$emit('reject', { order_no_wsh: 'ORD-1' })">reject</button>
      <button class="t-receive" @click="$emit('receive', { order_no_wsh: 'ORD-1', code: '1234' })">receive</button>
      <button class="t-start" @click="$emit('start', 'ORD-1', { name: 'a.jpg', type: 'image/jpeg' })">start</button>
      <button class="t-checkout" @click="$emit('checkout', { order_no_wsh: 'ORD-1' })">checkout</button>
      <button class="t-select" @click="$emit('select-order', { id_wsh: 55 })">select</button>
    </div>`,
}

const DailyTabStub = {
  name: 'KeeperDailyTab',
  emits: ['upload-timeline', 'refresh', 'update:orderId'],
  template: `<button class="t-upload" @click="$emit('upload-timeline', { orderId: 55, type: 'feeding', content: '已喂食', files: [] })">upload</button>`,
}

function mountPage() {
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
        KeeperCertTab: true,
        KeeperProfileTab: true,
        KeeperAttendancePanel: true,
        'el-icon': { template: '<span><slot /></span>' },
      },
    },
  })
  return { wrapper, addToast, authStore }
}

/** 切到指定 tab（按 chip 文案） */
async function goTab(wrapper, label) {
  const chip = wrapper.findAll('button').find(b => b.text() === label)
  expect(chip, `未找到 tab: ${label}`).toBeTruthy()
  await chip.trigger('click')
  await flushPromises()
}

describe('看护者工作流', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    mocks.getCurrentUser.mockResolvedValue({ code: 200, data: { nickname_wsh: '小王', avatar_wsh: 'a.png' } })
    mocks.getMyKeeperInfo.mockResolvedValue({ code: 200, data: { id_wsh: 9, status_wsh: 1 } })
    mocks.getMerchants.mockResolvedValue({ code: 200, data: [{ id_wsh: 1, name_wsh: 'PetCare' }] })
    mocks.getPoolOrders.mockResolvedValue({ code: 200, data: [{ order_no_wsh: 'ORD-1' }] })
    mocks.getMyKeeperOrders.mockResolvedValue({ code: 200, data: [{ id_wsh: 55, order_no_wsh: 'ORD-1' }] })
    mocks.ensureProfileRequirement.mockResolvedValue(true)
  })

  // ── 1~3：初始化加载 ──
  it('初始化：拉当前用户、看护者信息、商家、待抢单池、我的订单', async () => {
    mountPage()
    await flushPromises()

    expect(mocks.getCurrentUser).toHaveBeenCalledTimes(1)
    expect(mocks.getMyKeeperInfo).toHaveBeenCalledTimes(1)
    expect(mocks.getMerchants).toHaveBeenCalledTimes(1)
    expect(mocks.getPoolOrders).toHaveBeenCalledTimes(1)
    expect(mocks.getMyKeeperOrders).toHaveBeenCalledTimes(1)
  })

  // ── 4：抢单 ──
  it('抢单成功：调用 claimOrder({order_no_wsh})、提示成功、切到「我的订单」并刷新', async () => {
    mocks.claimOrder.mockResolvedValue({ code: 200 })
    const { wrapper, addToast } = mountPage()
    await flushPromises()

    await goTab(wrapper, '订单')
    await wrapper.get('.t-claim').trigger('click')
    await flushPromises()

    expect(mocks.claimOrder).toHaveBeenCalledWith({ order_no_wsh: 'ORD-1' })
    expect(addToast).toHaveBeenCalledWith('抢单成功', 'success')
    // 抢完刷新待抢单池 + 我的订单
    expect(mocks.getPoolOrders).toHaveBeenCalledTimes(2)
    expect(mocks.getMyKeeperOrders).toHaveBeenCalledTimes(2)
  })

  it('抢单失败(400)：把后端原因提示出来，并刷新列表', async () => {
    mocks.claimOrder.mockRejectedValue({
      response: { data: { message: '排班已满' } },
    })
    const { wrapper, addToast } = mountPage()
    await flushPromises()

    await goTab(wrapper, '订单')
    await wrapper.get('.t-claim').trigger('click')
    await flushPromises()

    // 400 的具体原因在 response.data.message 里，不能只显示 "Request failed with status code 400"
    expect(addToast).toHaveBeenCalledWith('排班已满', 'error')
    expect(mocks.getPoolOrders).toHaveBeenCalledTimes(2)
  })

  // ── 5：拒单 ──
  it('拒单：调用 rejectOrder、提示「已拒单」', async () => {
    mocks.rejectOrder.mockResolvedValue({ code: 200 })
    const { wrapper, addToast } = mountPage()
    await flushPromises()

    await goTab(wrapper, '订单')
    await wrapper.get('.t-reject').trigger('click')
    await flushPromises()

    expect(mocks.rejectOrder).toHaveBeenCalledWith({ order_no_wsh: 'ORD-1' })
    expect(addToast).toHaveBeenCalledWith('已拒单', 'success')
  })

  it('拒单失败：显示后端 message', async () => {
    mocks.rejectOrder.mockResolvedValue({ code: 400, message: '订单已被他人接走' })
    const { wrapper, addToast } = mountPage()
    await flushPromises()

    await goTab(wrapper, '订单')
    await wrapper.get('.t-reject').trigger('click')
    await flushPromises()

    expect(addToast).toHaveBeenCalledWith('订单已被他人接走', 'error')
  })

  // ── 6：确认收宠 ──
  it('确认收宠：原样透传 payload 给 markReceived', async () => {
    mocks.markReceived.mockResolvedValue({ code: 200 })
    const { wrapper, addToast } = mountPage()
    await flushPromises()

    await goTab(wrapper, '订单')
    await wrapper.get('.t-receive').trigger('click')
    await flushPromises()

    expect(mocks.markReceived).toHaveBeenCalledWith({ order_no_wsh: 'ORD-1', code: '1234' })
    expect(addToast).toHaveBeenCalledWith('已确认收宠', 'success')
  })

  // ── 7：开始服务（FormData）──
  it('开始服务：把订单号与图片装进 FormData 交给 startService', async () => {
    mocks.startService.mockResolvedValue({ code: 200 })
    const { wrapper, addToast } = mountPage()
    await flushPromises()

    await goTab(wrapper, '订单')
    await wrapper.get('.t-start').trigger('click')
    await flushPromises()

    expect(mocks.startService).toHaveBeenCalledTimes(1)
    const fd = mocks.startService.mock.calls[0][0]
    expect(fd).toBeInstanceOf(FormData)
    expect(fd.get('order_no_wsh')).toBe('ORD-1')
    expect(fd.get('file')).toBeTruthy()
    expect(addToast).toHaveBeenCalledWith('已开始护理', 'success')
  })

  // ── 8：完成服务（两个分支）──
  it('确认接走：0 费用时提示订单已完成', async () => {
    mocks.completeOrder.mockResolvedValue({ code: 200, data: { completed_wsh: true } })
    const { wrapper, addToast } = mountPage()
    await flushPromises()

    await goTab(wrapper, '订单')
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

    await goTab(wrapper, '订单')
    await wrapper.get('.t-checkout').trigger('click')
    await flushPromises()

    expect(addToast).toHaveBeenCalledWith('已确认接走，待主人补费 ¥35.50', 'warning')
  })

  // ── 9~10：服务时间线 + 每日状态 ──
  it('选择订单：并行拉时间线与每日状态', async () => {
    mocks.getTimeline.mockResolvedValue({ code: 200, data: [{ id_wsh: 1 }] })
    mocks.getDailyStatus.mockResolvedValue({ code: 200, data: { day_wsh: 2 } })
    const { wrapper } = mountPage()
    await flushPromises()

    await goTab(wrapper, '订单')
    await wrapper.get('.t-select').trigger('click')
    await flushPromises()

    // selectOrder 会把 id 统一成字符串再传给 api
    expect(mocks.getTimeline).toHaveBeenCalledWith('55')
    expect(mocks.getDailyStatus).toHaveBeenCalledWith('55')
  })

  // ── 8（补充）：上传日常动态 ──
  it('上传动态：把表单字段与文件装进 FormData 交给 uploadTimeline(orderId, fd)', async () => {
    mocks.uploadTimeline.mockResolvedValue({ code: 200 })
    const { wrapper, addToast } = mountPage()
    await flushPromises()

    await goTab(wrapper, '日常动态')
    await wrapper.get('.t-upload').trigger('click')
    await flushPromises()

    expect(mocks.uploadTimeline).toHaveBeenCalledTimes(1)
    const [orderId, fd] = mocks.uploadTimeline.mock.calls[0]
    expect(orderId).toBe(55)
    expect(fd).toBeInstanceOf(FormData)
    expect(fd.get('type_wsh')).toBe('feeding')
    expect(fd.get('content_wsh')).toBe('已喂食')
    expect(addToast).toHaveBeenCalledWith('动态已上传', 'success')
  })

  // ── 11~12：会话 ──
  it('切换会话订单：拉取会话消息', async () => {
    mocks.getConversation.mockResolvedValue({ code: 200, data: [{ id_wsh: 1, content_wsh: '你好' }] })
    const { wrapper } = mountPage()
    await flushPromises()

    await goTab(wrapper, '聊天')
    await wrapper.get('select.kw-select').setValue('55')
    await flushPromises()

    expect(mocks.getConversation).toHaveBeenCalledWith(55)
  })

  it('发送消息：调用 sendMessage(orderId, {content_wsh}) 后重新拉会话', async () => {
    mocks.getConversation.mockResolvedValue({ code: 200, data: [] })
    mocks.sendMessage.mockResolvedValue({ code: 200 })
    const { wrapper } = mountPage()
    await flushPromises()

    await goTab(wrapper, '聊天')
    await wrapper.get('select.kw-select').setValue('55')
    await flushPromises()

    await wrapper.get('input.kw-input').setValue('在吗')
    await wrapper.get('button.cta-primary').trigger('click')
    await flushPromises()

    expect(mocks.sendMessage).toHaveBeenCalledWith(55, { content_wsh: '在吗' })
    // 发送后清空输入并重新拉会话
    expect(mocks.getConversation).toHaveBeenCalledTimes(2)
  })

  it('空消息不发送', async () => {
    const { wrapper } = mountPage()
    await flushPromises()

    await goTab(wrapper, '聊天')
    await wrapper.get('select.kw-select').setValue('55')
    await flushPromises()

    await wrapper.get('button.cta-primary').trigger('click')
    await flushPromises()

    expect(mocks.sendMessage).not.toHaveBeenCalled()
  })
})
