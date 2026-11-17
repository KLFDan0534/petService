/**
 * 统一拆解后端返回信封 { code, message, data }。
 *
 * 背景：`src/utils/request.js` 的响应拦截器把整个 axios response 原样返回，
 * 所以 api 层拿到的 `res.data` 才是后端的 { code, message, data }。
 * 各 service 之前各自复制了同一份拆包函数（7 份），这里收敛成一份。
 *
 * 规则：
 * - 无响应 → 抛错（调用方不必再判空）
 * - code !== 200 → 抛错并带上后端 message
 * - 其它 → 返回 data；后端直接返回裸值时退回整个信封
 */
export function unwrapResult(res) {
  if (!res) throw new Error('无响应')
  if (res.code !== 200) throw new Error(res.message || '请求失败')
  return res.data ?? res
}
