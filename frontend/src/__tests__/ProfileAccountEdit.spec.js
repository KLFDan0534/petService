import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ProfileAccountEdit from '@/views/user/ProfileAccountEdit.vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'

/**
 * 账号设置四个分支测试。
 *
 * 批次 06 把「页面拼 endpoint」改成了「配置项自带具名提交函数」，
 * 这里逐个验证四个分支调用的是**正确的具名 API**、参数正确、校验保留、成功/失败状态正确。
 */
const mocks = vi.hoisted(() => ({
  accountType: { value: 'phone' },
  routerPush: vi.fn(),
  getCurrentUser: vi.fn(),
  updatePhone: vi.fn(),
  updateEmail: vi.fn(),
  updateRealName: vi.fn(),
  updatePaymentPassword: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mocks.routerPush }),
  useRoute: () => ({ meta: { get accountType() { return mocks.accountType.value } } }),
}))

vi.mock('@/api/auth', () => ({
  getCurrentUser: mocks.getCurrentUser,
  updatePhone: mocks.updatePhone,
  updateEmail: mocks.updateEmail,
  updateRealName: mocks.updateRealName,
  updatePaymentPassword: mocks.updatePaymentPassword,
}))

function mountPage() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const authStore = useAuthStore(pinia)
  authStore.user = { id_wsh: 1, username_wsh: 'owner', nickname_wsh: '主人' }
  const appStore = useAppStore(pinia)
  const addToast = vi.spyOn(appStore, 'addToast').mockImplementation(() => {})
  const wrapper = mount(ProfileAccountEdit, {
    global: { plugins: [pinia], stubs: { RouterLink: { template: '<a><slot /></a>' } } },
  })
  return { wrapper, addToast, authStore }
}

async function submitForm(wrapper) {
  await wrapper.get('form').trigger('submit')
  await flushPromises()
}

describe('账号设置四个分支', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    mocks.accountType.value = 'phone'
    mocks.getCurrentUser.mockResolvedValue({ code: 200, data: { real_name_wsh: '张三', id_card_no_wsh: '110101199001011234' } })
  })

  // ── 分支 1：手机号 ──
  it('手机号：调用 updatePhone，参数为 { phone_wsh }，成功后进入完成态', async () => {
    mocks.updatePhone.mockResolvedValue({ code: 200, data: { phone_wsh: '138****0000' } })
    const { wrapper, addToast, authStore } = mountPage()
    await flushPromises()

    await wrapper.get('input[inputmode="tel"]').setValue('13800001111')
    await submitForm(wrapper)

    expect(mocks.updatePhone).toHaveBeenCalledWith({ phone_wsh: '13800001111' })
    expect(mocks.updateEmail).not.toHaveBeenCalled()
    expect(mocks.updateRealName).not.toHaveBeenCalled()
    expect(mocks.updatePaymentPassword).not.toHaveBeenCalled()
    expect(addToast).toHaveBeenCalledWith('保存成功', 'success')
    expect(wrapper.text()).toContain('修改已生效')
    expect(authStore.user.phone_wsh).toBe('138****0000')
  })

  it('手机号格式非法：本地拦截，不发请求', async () => {
    const { wrapper } = mountPage()
    await flushPromises()

    await wrapper.get('input[inputmode="tel"]').setValue('123')
    await submitForm(wrapper)

    expect(mocks.updatePhone).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('请输入 11 位有效手机号')
  })

  // ── 分支 2：邮箱 ──
  it('邮箱：调用 updateEmail，参数为 { email_wsh }', async () => {
    mocks.accountType.value = 'email'
    mocks.updateEmail.mockResolvedValue({ code: 200, data: {} })
    const { wrapper } = mountPage()
    await flushPromises()

    await wrapper.get('input[inputmode="email"]').setValue('a@b.com')
    await submitForm(wrapper)

    expect(mocks.updateEmail).toHaveBeenCalledWith({ email_wsh: 'a@b.com' })
    expect(mocks.updatePhone).not.toHaveBeenCalled()
  })

  it('邮箱格式非法：本地拦截', async () => {
    mocks.accountType.value = 'email'
    const { wrapper } = mountPage()
    await flushPromises()

    await wrapper.get('input[inputmode="email"]').setValue('not-an-email')
    await submitForm(wrapper)

    expect(mocks.updateEmail).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('请输入有效的邮箱地址')
  })

  // ── 分支 3：实名 ──
  it('实名：调用 updateRealName，参数含姓名与身份证号，完成态文案为「实名信息已提交」', async () => {
    mocks.accountType.value = 'realName'
    mocks.updateRealName.mockResolvedValue({ code: 200, data: {} })
    const { wrapper } = mountPage()
    await flushPromises()

    // 进入页面时会用 getCurrentUser 回填姓名与身份证
    expect(mocks.getCurrentUser).toHaveBeenCalled()
    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('李四')
    await inputs[1].setValue('110101199001011234')
    await submitForm(wrapper)

    expect(mocks.updateRealName).toHaveBeenCalledWith({
      real_name_wsh: '李四',
      id_card_no_wsh: '110101199001011234',
    })
    expect(wrapper.text()).toContain('实名信息已提交')
  })

  it('实名：身份证位数不对时本地拦截', async () => {
    mocks.accountType.value = 'realName'
    const { wrapper } = mountPage()
    await flushPromises()

    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('李四')
    await inputs[1].setValue('123')
    await submitForm(wrapper)

    expect(mocks.updateRealName).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('请输入 18 位身份证号码')
  })

  // ── 分支 4：支付密码 ──
  it('支付密码：调用 updatePaymentPassword，并标记 payment_password_set_wsh', async () => {
    mocks.accountType.value = 'paymentPassword'
    mocks.updatePaymentPassword.mockResolvedValue({ code: 200 })
    const { wrapper, addToast, authStore } = mountPage()
    await flushPromises()

    // 支付密码页不拉用户信息
    expect(mocks.getCurrentUser).not.toHaveBeenCalled()

    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('123456')
    await inputs[1].setValue('123456')
    await submitForm(wrapper)

    expect(mocks.updatePaymentPassword).toHaveBeenCalledWith({ payment_password_wsh: '123456' })
    expect(authStore.user.payment_password_set_wsh).toBe(true)
    expect(addToast).toHaveBeenCalledWith('保存成功', 'success')
  })

  it('支付密码：两次输入不一致时本地拦截', async () => {
    mocks.accountType.value = 'paymentPassword'
    const { wrapper } = mountPage()
    await flushPromises()

    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('123456')
    await inputs[1].setValue('654321')
    await submitForm(wrapper)

    expect(mocks.updatePaymentPassword).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('两次输入的支付密码不一致')
  })

  it('支付密码：非 6 位数字时本地拦截', async () => {
    mocks.accountType.value = 'paymentPassword'
    const { wrapper } = mountPage()
    await flushPromises()

    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('12345')
    await inputs[1].setValue('12345')
    await submitForm(wrapper)

    expect(mocks.updatePaymentPassword).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('支付密码必须是 6 位数字')
  })

  // ── 失败处理（批次 06 迁移时这里曾残留 r.data.message）──
  it('业务失败：显示后端 message，不进入完成态', async () => {
    mocks.updatePhone.mockResolvedValue({ code: 400, message: '该手机号已被占用' })
    const { wrapper, addToast } = mountPage()
    await flushPromises()

    await wrapper.get('input[inputmode="tel"]').setValue('13800001111')
    await submitForm(wrapper)

    expect(addToast).toHaveBeenCalledWith('该手机号已被占用', 'error')
    expect(addToast).not.toHaveBeenCalledWith('保存成功', 'success')
    expect(wrapper.text()).not.toContain('修改已生效')
  })

  it('网络异常：回退到通用失败文案', async () => {
    mocks.updatePhone.mockRejectedValue({ response: { data: { message: '网络错误' } } })
    const { wrapper, addToast } = mountPage()
    await flushPromises()

    await wrapper.get('input[inputmode="tel"]').setValue('13800001111')
    await submitForm(wrapper)

    expect(addToast).toHaveBeenCalledWith('网络错误', 'error')
  })
})
