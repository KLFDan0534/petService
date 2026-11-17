import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import Login from '@/views/user/Login.vue'
import { useAuthStore } from '@/stores/auth'

/**
 * 登录链路测试（批次 06 把裸 axios 从页面搬进了 api 层，这里锁住行为）。
 *
 * 重点验证「密码错误」不会被显示成「登录已过期」——
 * 这是批次 06 改动登录实现时最容易破坏的地方。
 */
const mocks = vi.hoisted(() => ({
  login: vi.fn(),
  routerPush: vi.fn(),
  addDynamicRoutes: vi.fn(),
  resetTurnstile: vi.fn(),
  turnstile: { value: '' },
  unavailable: { value: false },
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mocks.routerPush }),
  useRoute: () => ({ query: {} }),
}))

vi.mock('@/router', () => ({
  addDynamicRoutes: mocks.addDynamicRoutes,
}))

vi.mock('@/composables/useTurnstile', () => ({
  useTurnstile: () => ({
    token: mocks.turnstile,
    unavailable: mocks.unavailable,
    reset: mocks.resetTurnstile,
  }),
}))

vi.mock('@/api/auth', () => ({
  login: mocks.login,
}))

function mountLogin() {
  const pinia = createPinia()
  setActivePinia(pinia)
  return mount(Login, {
    global: {
      plugins: [pinia],
      stubs: { RouterLink: { template: '<a><slot /></a>' } },
    },
  })
}

async function fillAndSubmit(wrapper, { username = 'owner', password = 'secret123' } = {}) {
  await wrapper.get('#login-username').setValue(username)
  await wrapper.get('#login-password').setValue(password)
  await wrapper.get('form').trigger('submit')
  await flushPromises()
}

describe('登录链路', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    mocks.turnstile.value = 'turnstile-token-abc'
    mocks.unavailable.value = false
  })

  // ── 1. 正常登录 ──
  it('登录成功：保存 token、注册动态路由、跳转 dashboard', async () => {
    mocks.login.mockResolvedValue({
      code: 200,
      data: { access_token_wsh: 'jwt-token', refresh_token_wsh: 'rt', roles_wsh: ['OWNER'] },
    })
    const wrapper = mountLogin()

    await fillAndSubmit(wrapper)

    expect(mocks.login).toHaveBeenCalledTimes(1)
    expect(mocks.login).toHaveBeenCalledWith({
      username_wsh: 'owner',
      password_wsh: 'secret123',
      turnstileToken: 'turnstile-token-abc',
    })
    // Token 落到 localStorage（store 的 setAuth 负责）
    expect(localStorage.getItem('token')).toBe('jwt-token')
    // 动态路由注册 + 跳转
    expect(mocks.addDynamicRoutes).toHaveBeenCalledWith(['OWNER'])
    expect(mocks.routerPush).toHaveBeenCalledWith('/dashboard')
    // 成功时不该重置 Turnstile
    expect(mocks.resetTurnstile).not.toHaveBeenCalled()
  })

  // ── 2. 密码错误（业务码非 200）──
  it('密码错误：显示后端 message，不跳转，重置 Turnstile', async () => {
    mocks.login.mockResolvedValue({ code: 400, message: '用户名或密码不正确' })
    const wrapper = mountLogin()

    await fillAndSubmit(wrapper, { password: 'wrongpass' })

    expect(wrapper.text()).toContain('用户名或密码不正确')
    expect(mocks.routerPush).not.toHaveBeenCalled()
    expect(mocks.addDynamicRoutes).not.toHaveBeenCalled()
    expect(mocks.resetTurnstile).toHaveBeenCalledTimes(1)
    expect(localStorage.getItem('token')).toBeNull()
  })

  // ── 3. 后端返回 401 ──
  // 这是本批次最关键的一条：401 不能被改写成「登录已过期」
  it('后端 401：显示后端原始文案，不出现「登录已过期」', async () => {
    mocks.login.mockResolvedValue({ code: 401, message: '用户名或密码不正确' })
    const wrapper = mountLogin()

    await fillAndSubmit(wrapper, { password: 'wrongpass' })

    const text = wrapper.text()
    expect(text).toContain('用户名或密码不正确')
    // 若登录走了 request 实例，401 拦截器会把它改写成这句
    expect(text).not.toContain('登录已过期')
    expect(text).not.toContain('未登录')
    expect(mocks.routerPush).not.toHaveBeenCalled()
  })

  it('后端只给 msg 字段时也能正确显示', async () => {
    mocks.login.mockResolvedValue({ code: 400, msg: '账号已锁定' })
    const wrapper = mountLogin()

    await fillAndSubmit(wrapper)

    expect(wrapper.text()).toContain('账号已锁定')
  })

  // ── 4. 网络异常 ──
  it('网络异常：显示服务不可用提示并重置 Turnstile', async () => {
    mocks.login.mockRejectedValue({ response: { data: { message: '服务维护中' } } })
    const wrapper = mountLogin()

    await fillAndSubmit(wrapper)

    expect(wrapper.get('[role="alert"]').text()).toBe('服务维护中')
    expect(mocks.resetTurnstile).toHaveBeenCalledTimes(1)
    expect(mocks.routerPush).not.toHaveBeenCalled()
  })

  it('网络异常且无响应体：回退到通用文案', async () => {
    mocks.login.mockRejectedValue(new Error('Network Error'))
    const wrapper = mountLogin()

    await fillAndSubmit(wrapper)

    expect(wrapper.get('[role="alert"]').text()).toContain('登录服务暂时无法连接')
  })

  // ── 5. 前端校验（不发请求）──
  it('用户名为空 / 密码过短：本地拦截，不调用登录接口', async () => {
    const wrapper = mountLogin()

    await fillAndSubmit(wrapper, { username: '', password: '123' })

    expect(mocks.login).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('请输入用户名')
    expect(wrapper.text()).toContain('密码至少 6 位')
  })

  // ── 6. 用户名去空格 ──
  it('用户名提交前会 trim', async () => {
    mocks.login.mockResolvedValue({ code: 200, data: { access_token_wsh: 't', roles_wsh: [] } })
    const wrapper = mountLogin()

    await fillAndSubmit(wrapper, { username: '  owner  ' })

    expect(mocks.login.mock.calls[0][0].username_wsh).toBe('owner')
  })

  // ── 7. 无 Turnstile token 时字段不传 ──
  it('Turnstile 无 token 时该字段为 undefined（JSON 序列化后不会出现在请求体）', async () => {
    mocks.turnstile.value = ''
    mocks.login.mockResolvedValue({ code: 200, data: { access_token_wsh: 't', roles_wsh: [] } })
    const wrapper = mountLogin()

    await fillAndSubmit(wrapper)

    const body = mocks.login.mock.calls[0][0]
    // 键存在但值为 undefined，axios 序列化时会被丢弃，后端收不到该字段
    expect(body.turnstileToken).toBeUndefined()
    expect(JSON.parse(JSON.stringify(body))).not.toHaveProperty('turnstileToken')
  })

  // ── 8. loading 状态 ──
  it('提交过程中按钮进入 loading，结束后恢复', async () => {
    let resolveLogin
    mocks.login.mockImplementation(() => new Promise((resolve) => { resolveLogin = resolve }))
    const wrapper = mountLogin()

    await wrapper.get('#login-username').setValue('owner')
    await wrapper.get('#login-password').setValue('secret123')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    const submitButton = wrapper.get('form button[type="submit"]')
    expect(submitButton.attributes('disabled')).toBeDefined()

    resolveLogin({ code: 200, data: { access_token_wsh: 't', roles_wsh: [] } })
    await flushPromises()

    expect(wrapper.get('form button[type="submit"]').attributes('disabled')).toBeUndefined()
  })
})
