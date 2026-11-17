/**
 * 从 petService 前端源码生成「复刻规格文档」
 * 输出到 spec-out/，按卷拆分（便于上传，单文件控制在 2MB 内）
 *
 * 用法: node tools/gen_spec.js
 */
const fs = require('fs');
const path = require('path');

const SRC = path.resolve(__dirname, '..', 'src');
const OUT = path.resolve(__dirname, '..', 'spec-out');
fs.mkdirSync(OUT, { recursive: true });

const read = (p) => fs.readFileSync(p, 'utf8');
const rel = (p) => path.relative(SRC, p).replace(/\\/g, '/');

/* ---------- 路由表 ---------- */
function parseRouter() {
  const f = path.join(SRC, 'router', 'index.js');
  const s = read(f);
  const routes = [];
  // 匹配 { path: '/x', name: 'X', component: () => import('@/views/a/B.vue'), meta: {...} }
  const re = /\{\s*path:\s*'([^']+)'(?:,\s*name:\s*'([^']+)')?(?:,\s*component:\s*\(\)\s*=>\s*import\('@\/([^']+)'\))?/g;
  let m;
  while ((m = re.exec(s))) {
    const after = s.slice(m.index, m.index + 600);
    const metaRole = (after.match(/role:\s*'([^']+)'/) || [])[1] || '';
    const metaTitle = (after.match(/title:\s*'([^']+)'/) || [])[1] || '';
    routes.push({ path: m[1], name: m[2] || '', file: m[3] || '', role: metaRole, title: metaTitle });
  }
  return routes;
}

/* ---------- 单文件抽取 ---------- */
function analyze(file) {
  const full = path.join(SRC, file);
  if (!fs.existsSync(full)) return null;
  const s = read(full);
  const lines = s.split('\n').length;
  const tpl = (s.match(/<template>([\s\S]*?)<\/template>\s*(?:<script|<\/script>|$)/) || [, ''])[1];
  const script = (s.match(/<script[^>]*>([\s\S]*?)<\/script>/) || [, ''])[1];

  const uniq = (arr) => [...new Set(arr)];

  // 组件依赖
  const comps = uniq([...script.matchAll(/import\s+(\w+)\s+from\s+'@\/components\/([^']+)'/g)].map((x) => x[2]));
  // API 模块 + 方法
  const apis = uniq([...script.matchAll(/import\s+\{([^}]+)\}\s+from\s+'@\/api\/([^']+)'/g)]
    .map((x) => `${x[2]}: ${x[1].trim().replace(/\s+/g, ' ')}`));
  // store
  const stores = uniq([...script.matchAll(/import\s+\{\s*(use\w+Store)\s*\}\s+from\s+'@\/stores\/([^']+)'/g)]
    .map((x) => x[2]));
  // props / emits
  const props = (script.match(/defineProps\(\s*\{([\s\S]{0,400})/) || [, ''])[1];
  const propKeys = uniq([...props.matchAll(/^\s*(\w+)\s*:/gm)].map((x) => x[1])).slice(0, 14);
  const emits = (script.match(/defineEmits\(\[([^\]]*)\]\)/) || [, ''])[1]
    .split(',').map((x) => x.trim().replace(/['"]/g, '')).filter(Boolean);

  // 模板里的中文文案（标题/按钮/标签）
  const zh = uniq([...tpl.matchAll(/>([^<>{}]*[\u4e00-\u9fa5][^<>{}]*)</g)]
    .map((x) => x[1].trim())
    .filter((t) => t.length >= 2 && t.length <= 24 && !/^\s*$/.test(t)))
    .slice(0, 40);

  // 表格列 / 表单项 label
  const labels = uniq([...tpl.matchAll(/label="([^"]*[\u4e00-\u9fa5][^"]*)"/g)].map((x) => x[1])).slice(0, 30);
  // 弹窗标题
  const dialogs = uniq([...tpl.matchAll(/<AppDialog[^>]*title="([^"]+)"/g)].map((x) => x[1])).slice(0, 10);
  // 路由跳转
  const links = uniq([...tpl.matchAll(/to="([^"]+)"/g)].map((x) => x[1])).filter((t) => t.startsWith('/')).slice(0, 12);
  // tab 常量
  const groups = (script.match(/const\s+(GROUPS|TABS|TAB_LIST|STATUS_GROUPS)\s*=\s*\[([\s\S]{0,600}?)\]\s*\n/) || [, '', ''])[2];
  const tabLabels = uniq([...groups.matchAll(/label:\s*'([^']+)'/g)].map((x) => x[1]));
  // 主要 class 前缀（判断页面骨架）
  const cls = uniq([...tpl.matchAll(/class="([a-z]{2,4})-[a-z]/g)].map((x) => x[1])).slice(0, 4);

  // 区块结构：抓结构性 class + 其后第一个中文文案
  const blocks = [];
  const bre = /<(\w+)[^>]*class="([^"]+)"/g;
  let bm;
  while ((bm = bre.exec(tpl)) && blocks.length < 14) {
    const c = bm[2];
    // 只保留真正的区块级 class，排除面包屑/分隔符/行内元素
    if (!/[a-z]{2,4}-(page|shell|hero|head|tabs|filters|toolbar|grid|list|panel|table|form|empty|foot|aside|nav|banner|stats?|summary|detail|section|content|wrap|body)\b/.test(c)) continue;
    if (/crumb|sep|link|meta|item|row|dot|icon|text|btn|tag|line/.test(c)) continue;
    const after = tpl.slice(bm.index, bm.index + 600);
    // 文案优先取区块内的标题/按钮，而不是随便一段文字
    const zh = (after.match(/<h[1-4][^>]*>\s*([^<>{}]*[\u4e00-\u9fa5][^<>{}]*)/) || [])[1]
      || (after.match(/<button[^>]*>\s*([^<>{}]*[\u4e00-\u9fa5][^<>{}]*)/) || [])[1]
      || (after.match(/>([^<>{}]*[\u4e00-\u9fa5][^<>{}]*)</) || [])[1];
    const key = c.split(/\s+/)[0];
    blocks.push(key + (zh ? '（' + zh.trim().replace(/\s+/g, ' ').slice(0, 18) + '）' : ''));
  }
  const blockList = uniq(blocks);

  return { file, lines, kb: +(Buffer.byteLength(s) / 1024).toFixed(1), zh, labels, dialogs, links, comps, apis, stores, propKeys, emits, tabLabels, cls, blockList };
}

function pageBlock(r, a) {
  const L = [];
  L.push(`### \`${r.path}\`${r.title ? ' · ' + r.title : ''}`);
  L.push('');
  L.push(`- 源码：\`src/${a.file}\`（${a.lines} 行 / ${a.kb} KB）`);
  if (a.stores.length) L.push(`- 状态：${a.stores.map((s) => '`use' + s.replace(/^./, (c) => c.toUpperCase()) + 'Store`').join('、')}`);
  if (a.comps.length) L.push(`- 子组件：${a.comps.map((c) => '`' + c + '`').join('、')}`);
  if (a.apis.length) L.push(`- 接口：${a.apis.map((x) => '`' + x + '`').join('；')}`);
  if (a.blockList && a.blockList.length) L.push(`- 区块结构：${a.blockList.join(' → ')}`);
  if (a.tabLabels.length) L.push(`- 分组/Tab：${a.tabLabels.join(' / ')}`);
  if (a.dialogs.length) L.push(`- 弹窗：${a.dialogs.join(' / ')}`);
  if (a.labels.length) L.push(`- 字段/列：${a.labels.slice(0, 18).join(' / ')}`);
  if (a.links.length) L.push(`- 跳转：${a.links.join(' ')}`);
  if (a.zh.length) L.push(`- 页面文案（用于还原结构）：${a.zh.slice(0, 24).join(' ｜ ')}`);
  L.push('');
  return L.join('\n');
}

function compBlock(a) {
  const L = [];
  L.push(`### \`${a.file}\`（${a.lines} 行 / ${a.kb} KB）`);
  if (a.propKeys.length) L.push(`- props：${a.propKeys.join(', ')}`);
  if (a.emits.length) L.push(`- emits：${a.emits.join(', ')}`);
  if (a.comps.length) L.push(`- 依赖：${a.comps.join('、')}`);
  if (a.zh.length) L.push(`- 文案：${a.zh.slice(0, 14).join(' ｜ ')}`);
  L.push('');
  return L.join('\n');
}

/* ---------- API 模块 ---------- */
function apiModule(f) {
  const s = read(path.join(SRC, 'api', f));
  const fns = [...s.matchAll(/export\s+(?:async\s+)?function\s+(\w+)\s*\(([^)]*)\)/g)].map((x) => `${x[1]}(${x[2].trim()})`);
  const arrows = [...s.matchAll(/export\s+const\s+(\w+)\s*=\s*(?:async\s*)?\(([^)]*)\)/g)].map((x) => `${x[1]}(${x[2].trim()})`);
  const urls = [...new Set([...s.matchAll(/['"`](\/(?:api|admin|user|merchant|cs)[^'"`\s]*)['"`]/g)].map((x) => x[1]))].slice(0, 40);
  return { file: f, fns: [...fns, ...arrows], urls };
}

/* ---------- 主流程 ---------- */
const routes = parseRouter();
const byFile = new Map(routes.filter((r) => r.file).map((r) => [r.file, r]));

const buckets = { user: [], merchant: [], admin: [], cs: [], order: [], error: [] };
for (const f of fs.readdirSync(path.join(SRC, 'views'), { withFileTypes: true })) {
  if (!f.isDirectory()) continue;
}
function walk(dir) {
  const out = [];
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) out.push(...walk(p));
    else if (e.name.endsWith('.vue')) out.push(p);
  }
  return out;
}
const viewFiles = walk(path.join(SRC, 'views')).map(rel);
for (const vf of viewFiles) {
  const seg = vf.split('/')[1];
  const r = byFile.get(vf) || { path: '(未在路由中)' + vf, title: '', role: '' };
  const a = analyze(vf);
  if (!a) continue;
  (buckets[seg] || buckets.user).push({ r, a });
}

const compFiles = walk(path.join(SRC, 'components')).map(rel);

// 00 索引
const idx = [];
idx.push('# 栖屿宠护（petService）前端 · 复刻规格（完整版）');
idx.push('');
idx.push(`> 由源码自动生成，覆盖 ${viewFiles.length} 个页面、${compFiles.length} 个组件、${fs.readdirSync(path.join(SRC, 'api')).length} 个 API 模块、${routes.length} 条路由。`);
idx.push('> 这是**唯一权威的页面清单**：按本卷的路径与分组逐个实现，不要自行缩减。');
idx.push('');
idx.push('## 技术栈');
idx.push('- Vue 3 + Vite + Pinia + Vue Router + Element Plus（图标用 @element-plus/icons-vue）');
idx.push('- 样式：Tailwind 原子类 + 每页独立 scoped class（前缀见各页 `class 前缀`）');
idx.push('- 真实项目另有 Java 后端；复刻时接口可用本地 mock，但**页面结构与交互必须对齐**');
idx.push('');
idx.push('## 分卷');
idx.push('| 卷 | 内容 | 页面数 |');
idx.push('|---|---|---|');
idx.push(`| 01 | 用户端页面 | ${buckets.user.length} |`);
idx.push(`| 02 | 商家端页面 | ${buckets.merchant.length} |`);
idx.push(`| 03 | 管理端页面 | ${buckets.admin.length} |`);
idx.push(`| 04 | 客服端 + 错误页 | ${buckets.cs.length + buckets.error.length} |`);
idx.push(`| 05 | 公共组件 | ${compFiles.length} |`);
idx.push(`| 06 | API 契约与 Store | — |`);
idx.push('');
// 布局导航
idx.push('## 布局与导航');
for (const lf of ['layout/UserLayout.vue', 'layout/MerchantLayout.vue', 'layout/AdminLayout.vue']) {
  const full = path.join(SRC, 'components', lf);
  if (!fs.existsSync(full)) continue;
  const t = read(full);
  const navs = [...new Set([...t.matchAll(/>\s*([\u4e00-\u9fa5][^<>{}]{1,14})\s*</g)].map((x) => x[1].trim()))].slice(0, 30);
  idx.push(`- **${lf.replace('layout/', '').replace('.vue', '')}**：导航项 ${navs.join(' / ')}`);
}
idx.push('');
idx.push('## 全局约定');
idx.push('- 每个页面：面包屑（首页 › 当前页）+ 页头（标题 + 副标题 + 右侧操作）+ 内容区');
idx.push('- 列表页统一：筛选/Tab → 工具栏（搜索/排序）→ 卡片或表格 → 空态 → 加载态 → 错误态（带重试）');
idx.push('- 弹窗统一用 AppDialog；危险操作二次确认；表单有校验与错误提示');
idx.push('- 窄屏：导航收进汉堡菜单，表格折成卡片，弹窗从底部弹出');
idx.push('- 所有中文文案照抄本卷，不要改写');
idx.push('');
idx.push('## 路由总表');
idx.push('| 路径 | 标题 | 角色 | 文件 |');
idx.push('|---|---|---|---|');
for (const r of routes) idx.push(`| \`${r.path}\` | ${r.title || '-'} | ${r.role || '-'} | ${r.file || '-'} |`);
idx.push('');
fs.writeFileSync(path.join(OUT, '00_INDEX.md'), idx.join('\n'));

// 01-04 页面卷
const vol = (name, title, arr, note) => {
  const L = [`# ${title}`, '', note || '', ''];
  for (const { r, a } of arr) L.push(pageBlock(r, a));
  fs.writeFileSync(path.join(OUT, name), L.join('\n'));
  return Buffer.byteLength(L.join('\n'));
};
const s1 = vol('01_USER_PAGES.md', '用户端页面（' + buckets.user.length + '）', buckets.user,
  '角色：宠物主人 / 看护师。所有页面共用 UserLayout（顶部导航 + 汉堡菜单 + 未读角标）。');
const s2 = vol('02_MERCHANT_PAGES.md', '商家端页面（' + buckets.merchant.length + '）', buckets.merchant,
  '角色：商家。共用 MerchantLayout（侧边栏 + 顶栏）。');
const s3 = vol('03_ADMIN_PAGES.md', '管理端页面（' + buckets.admin.length + '）', buckets.admin,
  '角色：管理员。共用 AdminLayout（侧边栏 + 顶栏 + 面包屑）。多为表格 CRUD，统一模式：筛选区 + 表格 + 分页 + 新增/编辑弹窗 + 删除确认。');
const s4 = vol('04_CS_ERROR_PAGES.md', '客服端 + 错误页（' + (buckets.cs.length + buckets.error.length) + '）',
  [...buckets.cs, ...buckets.error], '客服端共用工作台布局；错误页为全屏居中卡片。');

// 05 组件
const L5 = ['# 公共组件（' + compFiles.length + '）', '', '按目录分组：common / layout / order / pet / keeper / merchant / dashboard / ui。', ''];
for (const cf of compFiles) {
  const a = analyze(cf);
  if (a) L5.push(compBlock(a));
}
fs.writeFileSync(path.join(OUT, '05_COMPONENTS.md'), L5.join('\n'));

// 06 API + store
const L6 = ['# API 契约与状态管理', '', '## API 模块'];
for (const f of fs.readdirSync(path.join(SRC, 'api')).sort()) {
  const m = apiModule(f);
  L6.push(`### \`src/api/${m.file}\``);
  if (m.fns.length) L6.push('- 方法：' + m.fns.slice(0, 40).map((x) => '`' + x + '`').join('、'));
  if (m.urls.length) L6.push('- 端点：' + m.urls.map((x) => '`' + x + '`').join('、'));
  L6.push('');
}
L6.push('## Pinia Store');
for (const f of fs.readdirSync(path.join(SRC, 'stores')).sort()) {
  const s = read(path.join(SRC, 'stores', f));
  const stateKeys = [...s.matchAll(/^\s{2}(\w+):/gm)].map((x) => x[1]).slice(0, 20);
  const actions = [...s.matchAll(/(?:async\s+)?function\s+(\w+)|(\w+)\s*:\s*(?:async\s*)?\(/g)].map((x) => x[1] || x[2]).filter(Boolean).slice(0, 25);
  L6.push(`### \`src/stores/${f}\``);
  if (stateKeys.length) L6.push('- state：' + stateKeys.join(', '));
  if (actions.length) L6.push('- 方法：' + [...new Set(actions)].slice(0, 18).join(', '));
  L6.push('');
}
fs.writeFileSync(path.join(OUT, '06_API_STORES.md'), L6.join('\n'));

// 汇总
const sizes = fs.readdirSync(OUT).map((f) => [f, (fs.statSync(path.join(OUT, f)).size / 1024).toFixed(0) + ' KB']);
console.log('页面总数:', viewFiles.length, '| 组件:', compFiles.length, '| 路由:', routes.length);
console.log('分卷：');
for (const [f, s] of sizes) console.log('  ', f.padEnd(24), s);
console.log('\n用户端', buckets.user.length, '/ 商家', buckets.merchant.length, '/ 管理', buckets.admin.length, '/ 客服', buckets.cs.length, '/ 错误', buckets.error.length);
