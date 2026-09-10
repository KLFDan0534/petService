# Navigation Map（导航地图）

> 路由全部 EXISTING（router/index.js 783 行）。括号内为守卫规则。

## 启动与认证流

```
App Launch
├─ 有有效 token → Home（否则 token 过期自动清登录态 [E 守卫第1步]）
└─ 无 token
    ├─ 公开页可浏览：Dashboard / Services / ServiceDetail [E requiresAuth:false]
    ├─ 触达受限功能 → LoginPromptDialog（留在当前页，记录 redirect [E]）
    └─ 直接进受保护页 → /login?redirect=... [E]
Login 成功 → 按角色注入动态路由 [E addDynamicRoutes] → Dashboard
```

## 常驻 Tab（PLATFORM：5 Tab）

```
首页 ── Banner→ServiceDetail · 公告→PopupNotice · 宫格→各列表
服务 ── Chip 分类 Rail → ServiceGrid → ServiceDetail
宠物 ── PetCard → PetDetail → PetFormDialog(编辑)
订单 ── 状态 Segmented → OrderCard → OrderDetail
我的 ── 头像/资料/钱包/优惠券/会员/收藏/地址/评价/售后/通知/工作台入口/设置(主题/退出)
```

## 角色动态区（EXISTING addDynamicRoutes）

```
MERCHANT|ADMIN            → 商家区：/merchant/keepers /merchant/orders /merchant/services
                           /merchant/pets /merchant/profile /merchant/statistics /merchant/business-hours
MERCHANT|ADMIN|CS         → 支持区：/merchant/support/*（客服工作台/工单）
ADMIN                     → 管理区：/admin/dashboard /admin/orders /admin/users …（18 页）
KEEPER|ADMIN              → /keeper-workflow（考勤/接单/日报）
```

## 核心跳转边（EXISTING）

```
ServiceDetail →(立即预约)→ CreateOrder →(提交)→ Payment →(成功)→ OrderDetail
OrderDetail → 取消(→CancelOrderDialog) / 评价(→ReviewDialog) / 退款(→Refunds) / 打赏(→TipDialog)
Orders ← OrderDetail 返回（保持滚动位置 [E keepAlive]）
Chat：用户↔商家会话；AI 助手：/ai 入口 → /ai/chat（历史 sessions [E]）
KeeperApply/CustomerServiceApply → 提交资质 → 等审核（Qualification/CsApplication）
通知：SSE 推送 → 红点 → Notifications → NoticeDetail/OrderDetail
收藏：列表卡心形按钮 → Favorites 聚合页（merchant/keeper/service 三类型 [E]）
```

## 守卫规则（EXISTING）

1. token 过期 → 清 token/refreshToken/user
2. 未匹配动态路由 → 尝试按本地角色注入后重试，否则 → /login?redirect
3. requiresAuth 且无登录态 → 用户区弹 LoginPrompt；其他 → /login
4. meta.roles 校验失败 → /403
5. 已登录访问 login/register/forget-password → /dashboard
