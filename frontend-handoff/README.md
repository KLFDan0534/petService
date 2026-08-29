# 前端交接包：用户端页面暖色编辑风适配

本目录是「宠物寄养平台」Vue3 前端用户端页面的完整交接材料，供接手 AI 将尚未适配的页面统一改造为**暖色编辑风（Editorial Warm）**设计风格。

## 目录结构

```
frontend-handoff/
├── README.md                 ← 本文件：设计系统 + 适配规范 + 清单
├── design-tokens.css         ← 全局设计令牌（暖色 + 明暗双主题）唯一权威来源
├── examples/                 ← 5 个已完成适配的页面【风格参考，必须先读】
│   ├── Dashboard.vue         （首页）
│   ├── Services.vue          （服务列表）
│   ├── ServiceDetail.vue     （服务详情）
│   ├── Orders.vue            （订单中心）
│   └── Profile.vue           （个人中心）
├── pages/                    ← 36 个待适配的用户端页面（原样源码）
└── api/                      ← 34 个 API 模块（接口层，一般不需要改）
```

## 一、设计系统（务必严格遵守）

### 1. 统一使用设计令牌，禁止硬编码颜色

全局令牌定义在 `design-tokens.css`（明暗双主题由 `html[data-theme="dark"]` 自动切换）。

暖色编辑风核心令牌：

| 令牌 | 亮色值 | 用途 |
|---|---|---|
| `--ref-canvas` | `#FBF7F0` | 页面底色（米白） |
| `--ref-surface` | `#FFFFFF` | 卡片/面板表面 |
| `--ref-sand` | `#F1E9DB` | 次级底/占位 |
| `--ref-cream` | `#F6EFE3` | 强调底 |
| `--ref-line` | `#E9DFCD` | 描边/分隔线 |
| `--ref-ink` | `#241C14` | 主文字（暖黑） |
| `--ref-ink-soft` | `#55463A` | 次级文字 |
| `--ref-muted` | `#8A7A6A` | 弱化文字/说明 |
| `--ref-brand` | `#BF5B2B` | 品牌橙（陶土色） |
| `--ref-brand-deep` | `#9A411A` | 品牌深橙（hover） |
| `--ref-font-display` | Noto Serif SC 等衬线 | 标题/数字字体 |

其他布局/交互令牌（已含在 design-tokens.css 中，按需引用）：`--color-*`（全局 UI）、`--radius-*`、`--shadow-*`、`--space-*`、`--transition-*`。

### 2. 视觉语言要点

- 底色用 `--ref-canvas`，卡片用 `--ref-surface` + `--ref-line` 描边，圆角用卡片级 `--radius-*`。
- 大标题用衬线字体 `--ref-font-display`，正文用默认无衬线（全局 `--font-body`）。
- 分区小节带编号眉题（如 `01`/`02`）+ 英文小标（如 `Pet Profiles`）+ 分隔线，参考 `Profile.vue` 的 `ps-eyebrow` 结构。
- 状态徽章/标签使用 `badge badge-{tone}`（tone 含 `action/active/done/closed/queued`）命名，见各示例页。
- 主按钮 `cta cta-primary`、次按钮 `cta cta-outline`、深色按钮 `cta cta-dark`、文字链接 `text-link`。
- 图片缺省用 `MediaWithFallback`（示例页有完整用法）。
- 滚动显示动画用 `Reveal` 风格（IntersectionObserver + `transitionDelay` 错峰），移动端尊重 `prefers-reduced-motion`。

### 3. 响应式

- 用 CSS Grid/Flex + `clamp()`/媒体断点（参考示例页断点：约 `1100px / 900px / 760px / 520px`）。
- 移动端网格自动降列，卡片间距收窄，保证最小宽度不溢出。

### 4. 主题

- 站点全局暗色由 `html[data-theme="dark"]` 控制，令牌自动切换；**不要**在页面内写死暗色覆盖，除非像 Trust/预约卡片这类「明暗都保持深色」的特殊深色块（参考 Dashboard 的 `.db-dark` 写法，仍用 `--ref-*`/语义色表达）。

## 二、适配要求

1. **不改后端、不改接口语义**：数据字段（`*_wsh` 后缀）、路由、API 调用逻辑一律保持不变。
2. **完整适配、不要硬编码**：所有颜色/圆角/阴影/间距优先引用设计令牌；确需品牌强调色时使用语义色组合（如 `color-mix(in srgb, ...)`），避免散落十六进制色值。
3. **保留全部功能**：表单校验、弹窗、分页、状态流转、权限判断、加载/错误/空态骨架屏等逻辑与交互一个都不能少，只改视觉呈现。
4. **逐页完成后自检**：构建通过（`npm run build`）、无 `_ctx.xxx is not a function` 之类运行时错误、明暗两主题均正常。

## 三、待适配页面清单（pages/，共 36 个）

| 文件 | 说明 | 依赖 API 模块（均位于 api/） |
|---|---|---|
| Pets.vue | 我的宠物列表/建档 | pet.js, file.js |
| PetDetail.vue | 宠物详情 | pet.js |
| Favorites.vue | 收藏列表 | favorite.js |
| Membership.vue | 会员中心/套餐 | membership.js, payment.js, order.js |
| Wallet.vue | 钱包（余额/流水/提现） | wallet.js |
| Recharge.vue | 充值 | wallet.js, payment.js |
| Refunds.vue | 退款/售后 | refund.js, order.js |
| Tickets.vue | 工单中心 | ticket.js |
| Complaints.vue | 投诉 | complaint.js, order.js |
| Coupons.vue | 优惠券 | coupon.js |
| Merchants.vue | 附近商户列表 | merchant.js, geo.js |
| MerchantDetail.vue | 商户详情 | merchant.js, service.js, review.js |
| Keepers.vue | 照护师列表 | keeper.js, merchant.js |
| KeeperDetail.vue | 照护师详情 | keeper.js |
| KeeperProfile.vue | 照护师主页 | keeper.js |
| KeeperApply.vue | 申请成为照护师 | keeper.js, merchant.js |
| KeeperWorkflow.vue | 照护师工作台 | keeper.js, order.js, attendance.js |
| Login.vue | 登录 | auth.js |
| Register.vue | 注册 | auth.js |
| ForgetPassword.vue | 忘记密码 | auth.js |
| ProfileAccountEdit.vue | 账户信息编辑（手机/邮箱/实名/支付密码） | auth.js, user 相关 |
| Addresses.vue | 收货/寄送地址 | address.js |
| Payments.vue | 支付记录 | payment.js |
| Notifications.vue | 消息通知 | notification.js |
| NoticeDetail.vue | 公告详情 | notice.js |
| Chat.vue | 订单沟通/消息 | chat.js, order.js, file.js |
| AiChat.vue | AI 助手对话 | ai.js |
| AI.vue | AI 助手入口 | ai.js |
| Agent.vue | 智能体 | ai.js |
| RAG.vue | 知识库问答 | ai.js |
| Files.vue | 我的文件 | file.js |
| Ratings.vue | 评价 | rating.js, order.js |
| RevenueCenter.vue | 收益中心 | statistics.js, wallet.js |
| CreditReputation.vue | 信用与信誉 | user 相关 |
| CustomerServiceApply.vue | 申请商家客服 | merchantCustomerService.js |
| Anomaly.vue | 异常/告警 | 按需 |

> 注：`examples/` 中已适配页面对应关系——Home→Dashboard、ServiceList→Services、ServiceDetail、Orders、Profile。接手 AI 请先通读 1~2 个示例页（推荐 `Profile.vue` 与 `Services.vue`）掌握风格，再逐页改造。

## 四、API 接口层（api/，共 34 个）

接口文件为请求封装层，一般**不需要改动**。若接手 AI 需要接口签名/字段，直接阅读对应文件即可。请求实例见 `src/utils/request.js`（不在本包内，路径：`frontend/src/utils/request.js`），统一 `GET/POST/PUT/DELETE` + `/api` 前缀。

## 五、交接给接手 AI 的建议提示词片段

> 这是一个 Vue3 + Element Plus + Pinia 的宠物寄养平台前端。请将 `frontend-handoff/pages/` 下的页面统一改造为暖色编辑风（Editorial Warm）。设计令牌见 `frontend-handoff/design-tokens.css`，风格参考 `frontend-handoff/examples/`（尤其 Profile.vue、Services.vue）。要求：全部颜色/圆角/阴影用 `--ref-*` 令牌；保留所有功能与交互逻辑；不改后端与接口；响应式适配；构建通过。请逐页完成。

## 六、项目上下文

- 框架：Vue 3（`<script setup>`）+ Vite 5 + Element Plus + Pinia + vue-router
- 用户端路由挂在 `src/components/layout/UserLayout.vue`（站点头/页脚，全局 `--color-*` 令牌，无需改造）
- 全局样式入口：`src/assets/css/app.css`（引 design-tokens.css）
- 后端服务已运行于 8080，前端 dev 服务器 5173
