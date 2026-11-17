import { login } from '@/api/auth'

/**
 * HTTP 契约：登录必须绕开统一 request 实例。
 *
 * 背景：`utils/request.js` 的 401 拦截器会尝试刷新 token。
 * 「密码错误」本身就是 401，若登录走 request，用户会看到「登录已过期」而不是后端原文。
 * 批次 06 把裸 axios 从 Login.vue 搬进 api 层，这里锁住这个约束，防止以后被"统一"回去。
 */
const mocks = vi.hoisted(() => ({
  axiosPost: vi.fn(),
  requestPost: vi.fn(),
}))

vi.mock('axios', () => ({
  default: { post: mocks.axiosPost },
}))

vi.mock('@/utils/request', () => ({
  default: { post: mocks.requestPost },
}))

describe('api/auth.login 的传输层选择', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('走裸 axios，不走 utils/request', async () => {
    mocks.axiosPost.mockResolvedValue({ data: { code: 200, data: { access_token_wsh: 't' } } })

    const result = await login({ username_wsh: 'owner', password_wsh: 'secret123' })

    expect(mocks.axiosPost).toHaveBeenCalledTimes(1)
    expect(mocks.requestPost).not.toHaveBeenCalled()
    // 返回 Result 信封，与其它 api 方法一致
    expect(result).toEqual({ code: 200, data: { access_token_wsh: 't' } })
  })

  it('请求地址与方法不变：POST /api/auth/login', async () => {
    mocks.axiosPost.mockResolvedValue({ data: { code: 200 } })

    await login({ username_wsh: 'owner', password_wsh: 'secret123' })

    expect(mocks.axiosPost.mock.calls[0][0]).toBe('/api/auth/login')
    expect(mocks.axiosPost.mock.calls[0][1]).toEqual({
      username_wsh: 'owner',
      password_wsh: 'secret123',
    })
  })

  it('后端返回 401 时原样透出，不被拦截器改写', async () => {
    mocks.axiosPost.mockResolvedValue({ data: { code: 401, message: '用户名或密码不正确' } })

    const result = await login({ username_wsh: 'owner', password_wsh: 'wrong' })

    expect(result.code).toBe(401)
    expect(result.message).toBe('用户名或密码不正确')
  })
})
