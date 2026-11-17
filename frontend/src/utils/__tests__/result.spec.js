import { unwrapResult } from '@/utils/result'

/**
 * unwrapResult 是 Service 层唯一的拆包实现（批次 02 收敛）。
 * 它的错误语义决定了所有走 Service 的调用方在失败时是 throw 还是静默 —— 这里把它钉住。
 */
describe('unwrapResult 错误语义', () => {
  it('code=200 时返回 data', () => {
    expect(unwrapResult({ code: 200, message: 'ok', data: { id_wsh: 1 } })).toEqual({ id_wsh: 1 })
  })

  it('data 缺失时回退整个信封（后端直接返回裸值的兼容）', () => {
    const bare = { code: 200, list: [] }
    expect(unwrapResult(bare)).toBe(bare)
  })

  it('code 非 200 时抛错，消息取后端 message', () => {
    expect(() => unwrapResult({ code: 400, message: '参数不合法' }))
      .toThrow('参数不合法')
  })

  it('code 非 200 且无 message 时用兜底文案', () => {
    expect(() => unwrapResult({ code: 500 })).toThrow('请求失败')
  })

  it('无响应时抛「无响应」', () => {
    expect(() => unwrapResult(null)).toThrow('无响应')
    expect(() => unwrapResult(undefined)).toThrow('无响应')
  })

  it('401/403 同样抛错（走 Service 的调用方据此进入 catch）', () => {
    expect(() => unwrapResult({ code: 401, message: '未登录' })).toThrow('未登录')
    expect(() => unwrapResult({ code: 403, message: '无权访问' })).toThrow('无权访问')
  })
})
