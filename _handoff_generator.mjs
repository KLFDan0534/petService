import { readFileSync, writeFileSync, existsSync, statSync, mkdirSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const SRC = join(__dirname, 'frontend', 'src')

// MODE: 'all' | 'admin'  （admin 仅输出管理后台缺失页面）
const MODE = process.argv[2] === 'admin' ? 'admin' : 'all'

const read = (p) => {
  const fp = join(__dirname, 'frontend', 'src', p)
  return existsSync(fp) ? readFileSync(fp, 'utf8') : null
}

// ---------- 缺失页面清单（category -> pages）----------
const CATEGORIES = [
  {
    id: 'part1', num: '第一部分', title: '用户端（C 端）缺失页面', note: '均为用户角色皆可访问（OWNER/KEEPER/MERCHANT/ADMIN），布局用 user 布局',
    pages: [
      { route: '/keeper/profile', name: '寄养师个人主页', desc: '寄养师查看/编辑自己的主页、资质、排班、日常照护记录与评价', roles: 'KEEPER / MERCHANT / ADMIN', file: 'views/user/KeeperProfile.vue' },
      { route: '/orders/:id', name: '订单详情', desc: '订单全流程详情：概览、时间线、聊天、报告、侧边操作（原 OrderDetailView）', roles: 'OWNER / KEEPER / MERCHANT / ADMIN', file: 'views/order/OrderDetailView.vue' },
      { route: '/customer-service/apply', name: '客服入驻申请', desc: '用户申请成为平台客服，填写资料提交审核', roles: 'OWNER / KEEPER / MERCHANT / ADMIN', file: 'views/user/CustomerServiceApply.vue' },
      { route: '/files', name: '我的文件 / 素材', desc: '用户上传、管理自己的图片/文件素材，支持按目录上传、下载、删除', roles: 'OWNER / KEEPER / MERCHANT / ADMIN', file: 'views/user/Files.vue' },
      { route: '/credit-reputation', name: '信用 / 信誉中心', desc: '展示用户信用分与信誉评价明细', roles: 'OWNER / KEEPER / MERCHANT / ADMIN', file: 'views/user/CreditReputation.vue' },
      { route: '/anomaly', name: '异常 / 安全页', desc: '异常数据/安全提示页面（如账号异常、数据异常提示）', roles: 'OWNER / KEEPER / MERCHANT / ADMIN', file: 'views/user/Anomaly.vue' },
      { route: '/profile/phone | /profile/email | /profile/real-name | /profile/payment-password', name: '账户资料编辑', desc: '4 个入口共用 1 个页面，通过 accountType 区分（phone/email/realName/paymentPassword）', roles: 'ACCOUNT_AREA（含 CUSTOMER_SERVICE）', file: 'views/user/ProfileAccountEdit.vue' },
    ],
  },
  {
    id: 'part2', num: '第二部分', title: '错误页', note: '无需登录，全局可见',
    pages: [
      { route: '/403', name: 'Forbidden 无权限', desc: '无权限访问提示页', roles: '公共', file: 'views/error/Forbidden.vue' },
      { route: '/404', name: 'NotFound 页面不存在', desc: '路由未匹配的兜底页', roles: '公共', file: 'views/error/NotFound.vue' },
      { route: '/500', name: 'ServerError 服务器错误', desc: '服务器异常提示页', roles: '公共', file: 'views/error/ServerError.vue' },
      { route: '/503', name: 'ServiceUnavailable 服务不可用', desc: '服务暂不可用提示页', roles: '公共', file: 'views/error/ServiceUnavailable.vue' },
    ],
  },
  {
    id: 'part3', num: '第三部分', title: '商家端缺失页面', note: '角色 MERCHANT/ADMIN，布局用 merchant 布局；当前仅有 /merchant/dashboard',
    pages: [
      { route: '/merchant/profile', name: '店铺资料', desc: '商家编辑店铺资料/资质/营业信息', roles: 'MERCHANT / ADMIN', file: 'views/merchant/MerchantProfile.vue' },
      { route: '/merchant/services', name: '服务管理', desc: '商家管理自己的寄养/托管服务条目（增删改查、上下架）', roles: 'MERCHANT / ADMIN', file: 'views/merchant/MerchantServices.vue' },
      { route: '/merchant/business-hours', name: '营业时间', desc: '商家设置日常营业/照护时间', roles: 'MERCHANT / ADMIN', file: 'views/merchant/MerchantBusinessHours.vue' },
      { route: '/merchant/pets', name: '托管宠物管理', desc: '商家查看/管理在托/预约的宠物', roles: 'MERCHANT / ADMIN', file: 'views/merchant/MerchantPets.vue' },
      { route: '/merchant/orders', name: '订单管理', desc: '商家订单列表与状态处理', roles: 'MERCHANT / ADMIN', file: 'views/merchant/MerchantOrders.vue' },
      { route: '/merchant/keepers', name: '寄养师管理', desc: '商家管理名下寄养师', roles: 'MERCHANT / ADMIN', file: 'views/merchant/MerchantKeepers.vue' },
      { route: '/merchant/customer-service', name: '客服配置', desc: '商家配置客服接待', roles: 'MERCHANT / ADMIN', file: 'views/merchant/MerchantCustomerService.vue' },
      { route: '/merchant/statistics', name: '经营统计', desc: '商家经营数据统计', roles: 'MERCHANT / ADMIN', file: 'views/merchant/MerchantStatistics.vue' },
    ],
  },
  {
    id: 'part4', num: '第四部分', title: '客服支持缺失页面', note: '角色 MERCHANT/ADMIN/CUSTOMER_SERVICE，布局用 merchant 布局，路径前缀 /merchant/support',
    pages: [
      { route: '/merchant/support/dashboard', name: '客服工作台', desc: '客服工作台（原 CsWorkbench）', roles: 'MERCHANT / ADMIN / CUSTOMER_SERVICE', file: 'views/cs/CsWorkbench.vue' },
      { route: '/merchant/support/tickets', name: '工单审核', desc: '工单列表/审核（复用 AdminTickets）', roles: 'MERCHANT / ADMIN / CUSTOMER_SERVICE', file: 'views/admin/AdminTickets.vue' },
      { route: '/merchant/support/complaints', name: '投诉处理', desc: '投诉处理列表（复用 AdminComplaints）', roles: 'MERCHANT / ADMIN / CUSTOMER_SERVICE', file: 'views/admin/AdminComplaints.vue' },
      { route: '/merchant/support/chat', name: '在线客服聊天', desc: '客服在线聊天（原 CsChat）', roles: 'MERCHANT / ADMIN / CUSTOMER_SERVICE', file: 'views/cs/CsChat.vue' },
    ],
  },
  {
    id: 'part5', num: '第五部分', title: '管理后台缺失页面', note: '角色 ADMIN，布局用 admin 布局，路径前缀 /admin；共 35 个页面（工单/投诉已在第四部分，此处不重复）',
    pages: [
      { route: '/admin/dashboard', name: '运营概览', desc: '平台运营数据总览', roles: 'ADMIN', file: 'views/admin/AdminDashboard.vue' },
      { route: '/admin/statistics', name: '统计报表', desc: '平台多维统计', roles: 'ADMIN', file: 'views/admin/AdminStatistics.vue' },
      { route: '/admin/users', name: '用户管理', desc: '平台用户查询与管理', roles: 'ADMIN', file: 'views/admin/AdminUsers.vue' },
      { route: '/admin/merchants', name: '商家管理', desc: '商家审核与管理', roles: 'ADMIN', file: 'views/admin/AdminMerchants.vue' },
      { route: '/admin/real-name-reviews', name: '实名认证审核', desc: '用户/商家实名认证审核', roles: 'ADMIN', file: 'views/admin/AdminRealNameReviews.vue' },
      { route: '/admin/orders', name: '订单管理', desc: '全平台订单查询与干预', roles: 'ADMIN', file: 'views/admin/AdminOrders.vue' },
      { route: '/admin/keepers', name: '寄养师审核', desc: '寄养师入驻审核与管理', roles: 'ADMIN', file: 'views/admin/AdminKeepers.vue' },
      { route: '/admin/pets', name: '宠物档案', desc: '全平台宠物档案管理', roles: 'ADMIN', file: 'views/admin/AdminPets.vue' },
      { route: '/admin/refunds', name: '退款管理', desc: '退款申请审核处理', roles: 'ADMIN', file: 'views/admin/AdminRefunds.vue' },
      { route: '/admin/coupons', name: '优惠券运营', desc: '优惠券发放与管理', roles: 'ADMIN', file: 'views/admin/AdminCoupons.vue' },
      { route: '/admin/member-plans', name: '会员方案', desc: '会员套餐配置', roles: 'ADMIN', file: 'views/admin/AdminMemberPlans.vue' },
      { route: '/admin/membership-users', name: '会员用户', desc: '平台会员用户管理', roles: 'ADMIN', file: 'views/admin/AdminMembershipUsers.vue' },
      { route: '/admin/membership-orders', name: '会员订单', desc: '会员开通/续费订单', roles: 'ADMIN', file: 'views/admin/AdminMembershipOrders.vue' },
      { route: '/admin/notices', name: '公告管理', desc: '公告发布与维护', roles: 'ADMIN', file: 'views/admin/AdminNotices.vue' },
      { route: '/admin/withdrawals', name: '提现管理', desc: '商家/寄养师提现审核', roles: 'ADMIN', file: 'views/admin/AdminWithdrawals.vue' },
      { route: '/admin/roles', name: '角色权限', desc: '角色与权限配置', roles: 'ADMIN', file: 'views/admin/AdminRoles.vue' },
      { route: '/admin/reviews', name: '评价管理', desc: '平台评价审核', roles: 'ADMIN', file: 'views/admin/AdminReviews.vue' },
      { route: '/admin/categories', name: '宠物分类', desc: '宠物类型分类管理', roles: 'ADMIN', file: 'views/admin/AdminCategory.vue' },
      { route: '/admin/service-categories', name: '服务分类', desc: '服务类目管理', roles: 'ADMIN', file: 'views/admin/AdminServiceCategory.vue' },
      { route: '/admin/banners', name: 'Banner 管理', desc: '首页轮播图管理', roles: 'ADMIN', file: 'views/admin/AdminBanner.vue' },
      { route: '/admin/operation-logs', name: '操作日志', desc: '后台操作审计日志', roles: 'ADMIN', file: 'views/admin/AdminOperationLogs.vue' },
      { route: '/admin/recycle-bin', name: '回收站', desc: '软删除数据回收站', roles: 'ADMIN', file: 'views/admin/AdminRecycleBin.vue' },
      { route: '/admin/transactions', name: '交易流水', desc: '钱包交易流水', roles: 'ADMIN', file: 'views/admin/AdminTransactions.vue' },
      { route: '/admin/wallets', name: '钱包管理', desc: '用户/商家钱包与余额调整', roles: 'ADMIN', file: 'views/admin/AdminWallets.vue' },
      { route: '/admin/rag', name: '知识库管理', desc: 'AI 知识库（RAG）文档管理', roles: 'ADMIN', file: 'views/admin/AdminRag.vue' },
      { route: '/admin/ai-config', name: 'AI 配置', desc: 'AI 模型/参数配置', roles: 'ADMIN', file: 'views/admin/AdminAiConfig.vue' },
      { route: '/admin/qualifications', name: '资质审核', desc: '寄养师/商家资质审核', roles: 'ADMIN', file: 'views/admin/AdminQualifications.vue' },
      { route: '/admin/payments', name: '支付管理', desc: '支付单/支付渠道管理', roles: 'ADMIN', file: 'views/admin/AdminPayments.vue' },
      { route: '/admin/tips', name: '打赏管理', desc: '打赏记录管理', roles: 'ADMIN', file: 'views/admin/AdminTips.vue' },
      { route: '/admin/files', name: '文件管理', desc: '全平台文件管理', roles: 'ADMIN', file: 'views/admin/AdminFiles.vue' },
      { route: '/admin/notifications', name: '站内信', desc: '全平台通知推送', roles: 'ADMIN', file: 'views/admin/AdminNotifications.vue' },
      { route: '/admin/favorites', name: '收藏数据', desc: '收藏数据统计与管理', roles: 'ADMIN', file: 'views/admin/AdminFavorites.vue' },
      { route: '/admin/attendance', name: '考勤管理', desc: '寄养师考勤管理', roles: 'ADMIN', file: 'views/admin/AdminAttendance.vue' },
      { route: '/admin/leaves', name: '请假管理', desc: '寄养师请假审批', roles: 'ADMIN', file: 'views/admin/AdminLeaves.vue' },
      { route: '/admin/cs-applications', name: '客服申请审核', desc: '客服入驻申请审核', roles: 'ADMIN', file: 'views/admin/AdminCsApplications.vue' },
    ],
  },
]

// ---------- 工具：解析 import ----------
function collectImports(src) {
  const out = []
  const re = /import\s+(.+?)\s+from\s+['"](@\/[^'"]+)['"]/gs
  let m
  while ((m = re.exec(src))) {
    const raw = m[1].trim()
    const spec = m[2]
    if (spec.startsWith('@/api')) out.push({ type: 'api', spec, raw })
    else if (spec.startsWith('@/services')) out.push({ type: 'service', spec, raw })
    else if (spec.startsWith('@/components')) out.push({ type: 'component', spec, raw })
  }
  return out
}

function apiFilePath(spec) {
  // spec like '@/api/admin' or '@/services/favoriteService'
  const rel = spec.replace('@/', '') // 'api/admin'
  const abs = rel + '.js'
  return { rel, abs }
}

// ---------- 解析 api 文件中的函数 -> 方法/路径 ----------
function parseApiFunctions(apiSrc) {
  const fns = []
  const re = /async\s+function\s+([A-Za-z0-9_]+)\s*\(([^)]*)\)\s*\{[^]*?\}/gs
  let sm
  // 简单逐函数匹配（seek 方式避免跨函数误配）
  const idRe = /async\s+function\s+([A-Za-z0-9_]+)/g
  const callRe = /await\s+request\.([a-zA-Z]+)\(([^)]*?)\)/gs
  const fnRe = /async\s+function\s+([A-Za-z0-9_]+)\s*\(/g
  // 收集所有函数体起点
  const starts = []
  let mm
  while ((mm = fnRe.exec(apiSrc))) starts.push({ name: mm[1], idx: mm.index })
  for (let i = 0; i < starts.length; i++) {
    const s = starts[i]
    const end = i + 1 < starts.length ? starts[i + 1].idx : apiSrc.length
    const body = apiSrc.slice(s.idx, end)
    const cm = callRe.exec(body)
    let method = '', path = ''
    if (cm) { method = cm[1]; path = (cm[2] || '').split(',')[0].trim() }
    fns.push({ name: s.name, method: method || '-', path: path || '-' })
  }
  return fns
}

const KIND_LABEL = { api: 'api', service: 'service' }

// ---------- 读取页面 + 收集全局引用 ----------
// admin 模式下剔除已具备的后台页（dashboard/statistics/ai-config），仅保留真正缺失的 32 页
const ADMIN_EXCLUDE = new Set(['dashboard', 'statistics', 'ai-config'])
const BEEN_DONE = (c, p) => {
  if (MODE !== 'admin') return false
  if (c.id !== 'part5') return true // 非后台类别在 admin 模式下一律排除
  const lower = (p.route || '').replace('/admin/', '')
  return ADMIN_EXCLUDE.has(lower)
}
const ACTIVE_CATEGORIES = MODE === 'admin'
  ? CATEGORIES.filter(c => c.id === 'part5')
      .map(c => ({ ...c, pages: c.pages.filter(p => !BEEN_DONE(c, p)) }))
  : CATEGORIES

const pages = []
for (const cat of ACTIVE_CATEGORIES) {
  for (const p of cat.pages) {
    const src = read(p.file)
    if (src == null) { pages.push({ ...p, cat, src: null, imports: [] }); continue }
    pages.push({ ...p, cat, src, imports: collectImports(src) })
  }
}

// 去重的 api/service 目标
const apiModuleMap = new Map() // rel ('api/admin') -> { abs, content, functions }
const allImports = pages.flatMap(p => p.imports)
for (const imp of allImports) {
  if (imp.type !== 'api' && imp.type !== 'service') continue
  const { rel, abs } = apiFilePath(imp.spec)
  if (!apiModuleMap.has(rel)) {
    apiModuleMap.set(rel, { rel, abs, type: imp.type, content: null, functions: [] })
  }
}
for (const key of apiModuleMap.keys()) {
  const info = apiModuleMap.get(key)
  const full = join(__dirname, 'frontend', 'src', info.abs)
  try { info.content = readFileSync(full, 'utf8') } catch (e) { info.content = null }
  info.functions = info.content ? parseApiFunctions(info.content) : []
}

// 去重的共享组件
const normCompRel = (spec) => spec.replace('@/components/', '').replace(/\.vue$/i, '')
const componentMap = new Map() // abs rel path
const compImports = allImports.filter(i => i.type === 'component')
for (const imp of compImports) {
  const rel = normCompRel(imp.spec)
  const abs = 'components/' + rel + '.vue'
  if (!componentMap.has(abs)) {
    const c = existsSync(join(__dirname, 'frontend', 'src', abs)) ? readFileSync(join(__dirname, 'frontend', 'src', abs), 'utf8') : null
    componentMap.set(abs, { abs, content: c })
  }
}
// 显式必带组件（后台壳等不一定会被页面 import，需强制纳入）
const FORCED_COMPONENTS = MODE === 'admin' ? ['layout/AdminLayout.vue'] : []
for (const rel of FORCED_COMPONENTS) {
  const abs = 'components/' + rel
  if (!componentMap.has(abs)) {
    const c = existsSync(join(__dirname, 'frontend', 'src', abs)) ? readFileSync(join(__dirname, 'frontend', 'src', abs), 'utf8') : null
    componentMap.set(abs, { abs, content: c })
  }
}

// 共享样式
const cssFiles = ['assets/css/design-tokens.css', 'assets/css/app.css']
const cssContent = cssFiles.map(f => ({ f, c: read(f) }))
const requestWrapper = read('utils/request.js')

// ---------- 组装 markdown ----------
const L = []
const push = (s) => L.push(s)
const code = (s) => '```\n' + (s || '').replace(/\n+$/,'') + '\n```\n'

const TITLE = MODE === 'admin'
  ? '# 宠物寄养平台 · 管理后台缺失页面开发交接清单（32 页）'
  : '# 宠物寄养平台 · 缺失前端页面开发交接清单'

push(TITLE)
push('')
push('> 生成时间：' + new Date().toLocaleString('zh-CN') + '　｜　共 ' + pages.filter(p=>p.src!=null).length + ' 个页面')
push('')
push('<details open><summary><b>如何为本清单开发（给接手的 AI 阅读）</b></summary>')
push('')
push('- **技术栈**：本清单给出的是**原始 Vue 完整源码作参考**。你要为项目的 React 新前端（React + react-router）逐个实现下方页面，把 Vue 模板/逻辑改写成 React 组件。')
push('- **角色与权限**：所有页面仅 **ADMIN** 角色可访问，统一挂在 `admin` 布局下（见下方[共享组件库](#5-共享组件库)中的 `AdminLayout`）。')
push('- **每个页面固定输出**：`路由`、`页面说明`、`本页用到的 API 接口（函数/HTTP/路径）`、`前端样式源码`、`用到的共享组件`。')
push('- **API 统一走 `/api` 前缀**，由全局 axios 封装（见[全局请求封装](#2-全局请求封装-utilsrequestjs)）发起。')
push('- **样式**：优先使用全局设计令牌 `--ref-*`（见[共享设计样式](#4-共享设计样式)），保持明暗双主题一致；不要把页面写死成单一色块。')
push('</details>')
push('')
push('---')

// 目录
push('## 目录')
push('')
let toc = ['1. [全局请求封装 (utils/request.js)](#2-全局请求封装-utilsrequestjs)', '2. [API 接口层（按域拆分，完整源码）](#3-api-接口层按域拆分完整源码)', '3. [共享设计样式](#4-共享设计样式)', '4. [共享组件库](#5-共享组件库)']
ACTIVE_CATEGORIES.forEach((c, i) => toc.push(`5.${i+1}. ${c.num} ${c.title}（${c.pages.length} 页）`))
toc.forEach(t => push('- ' + t))
push('')

push('---')
push('## 2. 全局请求封装 (utils/request.js)')
push('')
push('所有 API 均通过该 axios 实例发起；统一处理 token、错误码、响应结构 `{ code, message, data }`。')
push('')
push(code(requestWrapper))
push('---')

// API 层
const apiEntries = [...apiModuleMap.keys()].sort()
let apiAnchorCount = 0
push('## 3. API 接口层（按域拆分，完整源码）')
push('')
push('按业务域拆分的完整接口定义如下。每个页面在其小节给出“本页用到的接口”，在此查看对应文件全文，便于接手 AI 了解全部可用接口。')
push('')
const apiIds = {}
for (const key of apiEntries) {
  const info = apiModuleMap.get(key)
  apiAnchorCount++
  const fid = `api-${String(apiAnchorCount).padStart(2,'0')}`
  apiIds[key] = fid
  push(`### ${fid}｜${info.abs}${info.type === 'service' ? '（页面级服务）' : ''}`)
  push('')
  push('```js')
  push(info.content ? info.content.trim() : '（文件缺失）')
  push('```')
  push('')
}
push('---')

// 共享样式
push('## 4. 共享设计样式')
push('')
push('### design-tokens.css（全局设计令牌，页面样式优先引用 `--ref-*`）')
push('')
for (const { f, c } of cssContent) {
  push(code(c))
  push('---')
}
if (requestWrapper) {
  // 已在上文
}

// 共享组件
push('## 5. 共享组件库')
push('')
push('被缺失页面引用的通用组件完整源码如下，页面可直接复用。')
push('')
const compKeys = [...componentMap.keys()].sort()
let compCount = 0
const compIds = {}
for (const abs of compKeys) {
  compCount++
  const cid = `comp-${String(compCount).padStart(2,'0')}`
  compIds[abs] = cid
  push(`### ${cid}｜components/${abs.replace('components/','')}`)
  push('')
  push(code(componentMap.get(abs).content))
  push('')
}
if (compKeys.length === 0) push('（本清单页面未引用额外共享组件）')
push('---')

// 页面部分
for (const c of ACTIVE_CATEGORIES) {
  push(`## 6. ${c.num}：${c.title}`)
  push('')
  push(c.note || '')
  push('')
  for (const p of pages.filter(p => p.cat === c)) {
    push(`### ${p.route}　·　${p.name}`)
    push('')
    push('- **说明**：' + (p.desc || '-'))
    push('- **可访问角色**：' + (p.roles || '-'))
    push('- **源文件**：`frontend/src/' + p.file + '`')
    if (!p.src) { push('\n> ⚠️ 源文件缺失，请参考后端接口或临近页面实现\n'); push('---'); continue }
    // API 用到的
    const usedApi = p.imports.filter(i => i.type === 'api' || i.type === 'service')
    push('')
    push('**本页用到的 API 接口**')
    push('')
    if (usedApi.length === 0) {
      push('- 无（纯展示/静态）')
    } else {
      for (const imp of usedApi) {
        const { rel } = apiFilePath(imp.spec)
        const info = apiModuleMap.get(rel)
        const fid = apiIds[rel]
        push(`- 引用 \`${imp.spec}\`（完整源码见 ${fid || '上文'}）`)
        // 该页面 import 了哪些函数
        const names = (imp.raw.match(/\{([^}]+)\}/) || [])[1]?.split(',').map(s => s.trim().replace(/ as .*/, '')) || []
        if (names.length && info) {
          push('')
          push('| 函数 | HTTP | 路径 |')
          push('| --- | --- | --- |')
          for (const n of names) {
            const fn = (info.functions || []).find(f => f.name === n)
            push(`| \`${n}\` | ${fn ? fn.method : '-'} | \`${fn ? fn.path : '-'}\` |`)
          }
          push('')
        }
      }
    }
    // 用到的共享组件
    const usedComp = p.imports.filter(i => i.type === 'component')
    if (usedComp.length) {
      push('**用到的共享组件**')
      push('')
      for (const imp of usedComp) {
        const rel = normCompRel(imp.spec)
        const abs2 = 'components/' + rel + '.vue'
        const cid = compIds[abs2]
        push(`- \`${imp.spec}\`（完整源码见 ${cid || '共享组件库'}）`)
      }
      push('')
    }
    push('**前端源码（Vue 完整参考）**')
    push('')
    push(`<details><summary>${p.file}（点击展开）</summary>`)
    push('')
    push(code(p.src))
    push('</details>')
    push('')
    push('---')
  }
}

// 最后清单表
push('## 7. 附件：全部页面一行速查')
push('')
push('| 部分 | 路由 | 页面 | 源文件 |')
push('| --- | --- | --- | --- |')
for (const c of ACTIVE_CATEGORIES) for (const p of c.pages) push(`| ${c.num} | \`${p.route}\` | ${p.name} | ${p.file} |`)
push('')

const out = L.join('\n')
const outPath = join(__dirname, MODE === 'admin' ? '缺失页面开发交接清单-管理后台.md' : '缺失页面开发交接清单.md')
writeFileSync(outPath, out, 'utf8')
const kb = (statSync(outPath).size / 1024).toFixed(0)
console.log('pages(total):', pages.length, 'missing:', pages.filter(p=>p.src==null).length)
console.log('api modules:', apiModuleMap.size, ' components:', componentMap.size)
console.log('output:', outPath, ' size: ' + kb + ' KB')