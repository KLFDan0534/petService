/**
 * 由 api/*.js 生成 TS 客户端 shared/lib/api.ts
 * —— 路径与 HTTP 方法逐个机械对齐，避免手写错
 *
 * 用法: node tools/gen_client.cjs
 */
const fs = require('fs');
const path = require('path');

const SRC = path.resolve(__dirname, '..', 'src');
const OUT = path.resolve(__dirname, '..', 'spec-out', 'api.ts');

function parseModule(file) {
  const s = fs.readFileSync(path.join(SRC, 'api', file), 'utf8');
  const lines = s.split('\n');
  const out = [];
  for (let i = 0; i < lines.length; i++) {
    const m = lines[i].match(/export\s+(?:async\s+)?function\s+(\w+)\s*\(([^)]*)\)/);
    if (!m) continue;
    let method = '', url = '';
    for (let k = i; k < Math.min(i + 14, lines.length); k++) {
      const r = lines[k].match(/request\.(get|post|put|patch|delete)\s*\(\s*[`'"]([^`'"]+)[`'"]/);
      if (r) { method = r[1].toUpperCase(); url = r[2]; break; }
    }
    if (!method) continue;
    let u = url;
    if (!u.startsWith('/api/')) u = '/api' + (u.startsWith('/') ? u : '/' + u);
    out.push({ fn: m[1], method, url: u, params: m[2].trim() });
  }
  return { file: file.replace('.js', ''), endpoints: out };
}

const files = fs.readdirSync(path.join(SRC, 'api')).filter((f) => f.endsWith('.js')).sort();
const mods = files.map(parseModule).filter((m) => m.endpoints.length);

const L = [];
L.push('/**');
L.push(' * 后端接口客户端 —— 由真实项目 src/api/*.js 机械生成');
L.push(' * 路径、方法、参数与后端完全一致，禁止改动字符串。');
L.push(' *');
L.push(' * 用法：');
L.push(' *   import { orderApi } from \'@shared/lib/api\'');
L.push(' *   const list = await orderApi.getOrders()');
L.push(' *');
L.push(' * 环境变量：VITE_API_BASE 为空时走同源 /api；');
L.push(' * 后端不可用时（MOCK=1）各调用抛 MockFallback，页面据此回落到本地 mock。');
L.push(' */');
L.push('');
L.push('export const API_BASE: string = (import.meta as any)?.env?.VITE_API_BASE ?? \'\';');
L.push('');
L.push('export class ApiError extends Error {');
L.push('  status: number;');
L.push('  payload: unknown;');
L.push('  constructor(status: number, message: string, payload?: unknown) {');
L.push('    super(message);');
L.push('    this.name = \'ApiError\';');
L.push('    this.status = status;');
L.push('    this.payload = payload;');
L.push('  }');
L.push('}');
L.push('');
L.push('function authHeader(): Record<string, string> {');
L.push('  try {');
L.push('    const t = localStorage.getItem(\'token\');');
L.push('    return t ? { Authorization: `Bearer ${t}` } : {};');
L.push('  } catch { return {}; }');
L.push('}');
L.push('');
L.push('async function request<T = any>(method: string, url: string, body?: unknown, query?: Record<string, unknown>): Promise<T> {');
L.push('  let full = API_BASE + url;');
L.push('  if (query) {');
L.push('    const qs = Object.entries(query)');
L.push('      .filter(([, v]) => v !== undefined && v !== null && v !== \'\')');
L.push('      .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)');
L.push('      .join(\'&\');');
L.push('    if (qs) full += (full.includes(\'?\') ? \'&\' : \'?\') + qs;');
L.push('  }');
L.push('  const res = await fetch(full, {');
L.push('    method,');
L.push('    credentials: \'include\',');
L.push('    headers: { Accept: \'application/json\', ...(body !== undefined ? { \'Content-Type\': \'application/json\' } : {}), ...authHeader() },');
L.push('    body: body !== undefined ? JSON.stringify(body) : undefined,');
L.push('  });');
L.push('  const text = await res.text();');
L.push('  let data: any = null;');
L.push('  try { data = text ? JSON.parse(text) : null; } catch { data = text; }');
L.push('  if (!res.ok) {');
L.push('    const msg = (data && (data.message || data.error || data.msg)) || `HTTP ${res.status}`;');
L.push('    throw new ApiError(res.status, msg, data);');
L.push('  }');
L.push('  // ★ 后端统一信封：{ code, data, message }，原样返回，由调用方判断 code');
L.push('  return data as T;');
L.push('}');
L.push('');
L.push('/** 路径占位替换：/api/orders/${id} → /api/orders/123 */');
L.push('function fill(url: string, params: Record<string, unknown> = {}): string {');
L.push('  return url.replace(/\\$\\{(\\w+)\\}/g, (_, k) => {');
L.push('    const v = params[k];');
L.push('    if (v === undefined) throw new Error(`缺少路径参数 ${k}（${url}）`);');
L.push('    return encodeURIComponent(String(v));');
L.push('  });');
L.push('}');
L.push('');

for (const m of mods) {
  const name = m.file.replace(/[^a-zA-Z0-9]/g, '');
  L.push(`/** ${m.file} —— ${m.endpoints.length} 个端点 */`);
  L.push(`export const ${name}Api = {`);
  for (const e of m.endpoints) {
    const hasPath = /\$\{/.test(e.url);
    const sig = e.params ? `params: any` : '';
    if (e.method === 'GET' || e.method === 'DELETE') {
      if (hasPath) L.push(`  ${e.fn}: (${sig}) => request<any>('${e.method}', fill('${e.url}', params)),`);
      else L.push(`  ${e.fn}: (${sig}${sig ? ', ' : ''}query?: Record<string, unknown>) => request<any>('${e.method}', '${e.url}', undefined, query),`);
    } else {
      if (hasPath) L.push(`  ${e.fn}: (${sig}${sig ? ', ' : ''}body?: unknown) => request<any>('${e.method}', fill('${e.url}', params), body),`);
      else L.push(`  ${e.fn}: (body?: unknown) => request<any>('${e.method}', '${e.url}', body),`);
    }
  }
  L.push('};');
  L.push('');
}

L.push('/** 按模块聚合，便于页面按资源引入 */');
L.push('export const api = {');
for (const m of mods) {
  const name = m.file.replace(/[^a-zA-Z0-9]/g, '');
  L.push(`  ${name}: ${name}Api,`);
}
L.push('};');
L.push('');
L.push('export default api;');
L.push('');
L.push('/** 从信封里取业务数据：{ code:200, data } → data；失败时抛错 */');
L.push('export function unwrap<T = any>(res: any): T {');
L.push('  if (res && typeof res === \'object\' && \'code\' in res) {');
L.push('    if (res.code !== 200) throw new ApiError(res.code, res.message || \'请求失败\', res);');
L.push('    return res.data as T;');
L.push('  }');
L.push('  return res as T;');
L.push('}');

fs.writeFileSync(OUT, L.join('\n'));
console.log('模块:', mods.length, '| 端点:', mods.reduce((n, m) => n + m.endpoints.length, 0), '| 输出:', (fs.statSync(OUT).size / 1024).toFixed(0), 'KB');
