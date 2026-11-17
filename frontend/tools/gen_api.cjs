/**
 * 从 petService 前端 api/*.js 抽取完整接口契约
 * 输出 spec-out/07_API_CONTRACT.md
 *
 * 用法: node tools/gen_api.js
 */
const fs = require('fs');
const path = require('path');

const SRC = path.resolve(__dirname, '..', 'src');
const OUT = path.resolve(__dirname, '..', 'spec-out');

/* 请求封装的基本约定 */
function requestBasics() {
  const f = path.join(SRC, 'utils', 'request.js');
  const s = fs.readFileSync(f, 'utf8');
  const base = (s.match(/baseURL\s*[:=]\s*['"`]([^'"`]*)['"`]/) || [])[1] || '(同源)';
  const tokenHeader = (s.match(/(Authorization|token)[^,\n]{0,40}/) || [])[0] || '';
  const hasInterceptors = /interceptors/.test(s);
  const returnsData = /response\.data|res\.data/.test(s);
  return { base, tokenHeader, hasInterceptors, returnsData, lines: s.split('\n').length };
}

/* 解析单个 api 模块 */
function parseModule(file) {
  const s = fs.readFileSync(path.join(SRC, 'api', file), 'utf8');
  const lines = s.split('\n');
  const out = [];

  for (let i = 0; i < lines.length; i++) {
    const m = lines[i].match(/export\s+(?:async\s+)?function\s+(\w+)\s*\(([^)]*)\)/);
    if (!m) continue;

    // 向上收集紧邻的注释
    const comments = [];
    for (let j = i - 1; j >= 0; j--) {
      const l = lines[j].trim();
      if (l.startsWith('//')) comments.unshift(l.replace(/^\/\/\s?/, ''));
      else if (l === '') { if (comments.length) break; }
      else break;
    }

    // 向下 12 行找 request.xxx
    let method = '', url = '';
    for (let k = i; k < Math.min(i + 14, lines.length); k++) {
      const r = lines[k].match(/request\.(get|post|put|patch|delete)\s*\(\s*[`'"]([^`'"]+)[`'"]/);
      if (r) { method = r[1].toUpperCase(); url = r[2]; break; }
    }
    if (!method) continue;

    const params = m[2].trim();
    out.push({ fn: m[1], method, url, params, desc: comments.join(' ') });
  }
  return { file, endpoints: out };
}

const basics = requestBasics();
const files = fs.readdirSync(path.join(SRC, 'api')).filter((f) => f.endsWith('.js')).sort();

const L = [];
L.push('# 后端接口契约（前端真实调用）');
L.push('');
L.push('> 由 `src/api/*.js` 自动抽取。**复刻版的每一个数据动作都必须命中这里的端点与字段**，');
L.push('> 不允许自造路径或改写字段名——后端不会为了前端再改。');
L.push('');
L.push('## 基础约定');
L.push('');
L.push(`- 请求封装：\`src/utils/request.js\`（${basics.lines} 行）`);
L.push(`- baseURL：\`${basics.base}\``);
L.push('- 统一返回：`res.data` 即业务数据（axios 响应体），业务错误由拦截器抛出');
L.push('- 鉴权：登录后下发 JWT，后续请求带 Authorization 头（见 `utils/jwt.js` / `utils/request.js`）');
L.push('- 分页：列表类接口普遍支持 `page` / `pageSize`，返回 `{ list, total }` 或数组');
L.push('');
L.push('## 端点总表');
L.push('');
L.push('| 模块 | 方法 | 端点 | 说明 |');
L.push('|---|---|---|---|');

let total = 0;
const mods = [];
for (const f of files) {
  const m = parseModule(f);
  if (!m.endpoints.length) continue;
  mods.push(m);
  total += m.endpoints.length;
  for (const e of m.endpoints) {
    const d = (e.desc || '').replace(/\|/g, '/').slice(0, 40);
    L.push(`| ${f.replace('.js', '')} | ${e.method} | \`${e.url}\` | ${d} |`);
  }
}
L.push('');
L.push(`> 共 ${files.length} 个模块、${total} 个端点。`);
L.push('');
L.push('## 分模块明细');
L.push('');
for (const m of mods) {
  L.push(`### \`src/api/${m.file}\``);
  L.push('');
  for (const e of m.endpoints) {
    L.push(`- **${e.fn}**${e.params ? `(${e.params})` : '()'} → \`${e.method} ${e.url}\``);
    if (e.desc) L.push(`  - ${e.desc}`);
  }
  L.push('');
}

fs.writeFileSync(path.join(OUT, '07_API_CONTRACT.md'), L.join('\n'));
console.log('模块:', mods.length, '| 端点:', total, '| 文件大小:', (fs.statSync(path.join(OUT, '07_API_CONTRACT.md')).size / 1024).toFixed(0), 'KB');
console.log('baseURL:', basics.base);
