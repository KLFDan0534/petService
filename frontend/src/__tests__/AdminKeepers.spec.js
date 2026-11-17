import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import AdminKeepers from '@/views/admin/AdminKeepers.vue'
import { useAppStore } from '@/stores/app'

/**
 * AdminKeepers 状态切换的回归测试。
 *
 * 批次 05 修过一个真实 Bug：同一函数里混用两种响应约定 ——
 * approve/reject 走 api（返回 Result 信封，`r.code`），
 * 而"其它状态"分支走裸 `request.put`（返回 axios response，应 `r.data.code`），
 * 但判断统一写的是 `r.code` → 第三个分支恒为 undefined，成功提示和列表刷新永远不触发。
 *
 * 这里的用例必须真正**触发那条分支并断言副作用**（toast + 重新拉列表），
 * 只在静态代码层面看有没有 `.data` 是不够的。
 */
const mocks = vi.hoisted(() => ({
  getKeepers: vi.fn(),
  approveKeeper: vi.fn(),
  rejectKeeper: vi.fn(),
  updateKeeper: vi.fn(),
}))

vi.mock('@/api/keeper', () => ({
  getKeepers: mocks.getKeepers,
  approveKeeper: mocks.approveKeeper,
  rejectKeeper: mocks.rejectKeeper,
  updateKeeper: mocks.updateKeeper,
}))

const ROW = {
  id_wsh: 7,
  name_wsh: '小王',
  user_id_wsh: 21,
  status_wsh: 1,
  merchant_name_wsh: 'PetCare',
  phone_wsh: '13800000000',
}

function mountPage() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const appStore = useAppStore(pinia)
  const addToast = vi.spyOn(appStore, 'addToast').mockImplementation(() => {})
  const wrapper = mount(AdminKeepers, { global: { plugins: [pinia] } })
  return { wrapper, addToast }
}

/** 拿到第一行的状态下拉框 */
async function statusSelect(wrapper) {
  await flushPromises()
  const selects = wrapper.findAll('select')
  expect(selects.length).toBeGreaterThan(0)
  return selects[0]
}

describe('AdminKeepers 状态切换', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.getKeepers.mockResolvedValue({ code: 200, data: [ROW] })
  })

  it('加载列表：getKeepers 返回信封，data 为数组时正常渲染一行', async () => {
    const { wrapper } = mountPage()
    await flushPromises()

    expect(mocks.getKeepers).toHaveBeenCalled()
    expect(wrapper.findAll('tbody tr').length).toBe(1)
    expect(wrapper.text()).toContain('小王')
  })

  it('兼容 { list: [...] } 返回结构', async () => {
    mocks.getKeepers.mockResolvedValue({ code: 200, data: { list: [ROW] } })
    const { wrapper } = mountPage()
    await flushPromises()

    expect(wrapper.findAll('tbody tr').length).toBe(1)
  })

  // ── 回归重点：status = 0 走 updateKeeper 分支 ──
  it('改为「待审核」(status=0)：调用 updateKeeper、弹成功提示、重新拉列表', async () => {
    mocks.updateKeeper.mockResolvedValue({ code: 200 })
    const { wrapper, addToast } = mountPage()

    const select = await statusSelect(wrapper)
    await select.setValue('0')
    await flushPromises()

    expect(mocks.updateKeeper).toHaveBeenCalledWith(7, { status_wsh: 0 })
    // 这两条断言就是 Bug 的回归点：修复前 r.code 是 undefined，两者都不会发生
    expect(addToast).toHaveBeenCalledWith('状态已改为待审核', 'success')
    expect(mocks.getKeepers).toHaveBeenCalledTimes(2)   // 首次加载 + 切换后刷新
  })

  it('改为「已通过」(status=1)：走 approveKeeper', async () => {
    mocks.approveKeeper.mockResolvedValue({ code: 200 })
    const { wrapper, addToast } = mountPage()

    const select = await statusSelect(wrapper)
    await select.setValue('1')
    await flushPromises()

    expect(mocks.approveKeeper).toHaveBeenCalledWith(7)
    expect(mocks.updateKeeper).not.toHaveBeenCalled()
    expect(addToast).toHaveBeenCalledWith('状态已改为已通过', 'success')
    expect(mocks.getKeepers).toHaveBeenCalledTimes(2)
  })

  it('改为「已拒绝」(status=2)：走 rejectKeeper', async () => {
    mocks.rejectKeeper.mockResolvedValue({ code: 200 })
    const { wrapper, addToast } = mountPage()

    const select = await statusSelect(wrapper)
    await select.setValue('2')
    await flushPromises()

    expect(mocks.rejectKeeper).toHaveBeenCalledWith(7)
    expect(addToast).toHaveBeenCalledWith('状态已改为已拒绝', 'success')
  })

  it('后端返回非 200：不弹成功提示，也不刷新列表', async () => {
    mocks.updateKeeper.mockResolvedValue({ code: 400, message: '状态不可变' })
    const { wrapper, addToast } = mountPage()

    const select = await statusSelect(wrapper)
    await select.setValue('0')
    await flushPromises()

    expect(addToast).not.toHaveBeenCalledWith('状态已改为待审核', 'success')
    expect(mocks.getKeepers).toHaveBeenCalledTimes(1)
  })

  it('接口抛异常：提示操作失败', async () => {
    mocks.updateKeeper.mockRejectedValue(new Error('network'))
    const { wrapper, addToast } = mountPage()

    const select = await statusSelect(wrapper)
    await select.setValue('0')
    await flushPromises()

    expect(addToast).toHaveBeenCalledWith('操作失败', 'error')
  })

  it('选择占位项（空值）不发请求', async () => {
    const { wrapper } = mountPage()

    const select = await statusSelect(wrapper)
    await select.setValue('')
    await flushPromises()

    expect(mocks.updateKeeper).not.toHaveBeenCalled()
    expect(mocks.approveKeeper).not.toHaveBeenCalled()
    expect(mocks.rejectKeeper).not.toHaveBeenCalled()
  })
})
