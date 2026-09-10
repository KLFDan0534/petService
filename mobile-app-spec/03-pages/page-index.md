# Page Index（页面索引）

> 每页规范见 `pages/`。来源：`router/index.js`（EXISTING）+ 视图清单。Web 65 页 → 移动端 22 个规范文件（同质页面合并）。

| Page | 移动页面 | Web 路由（EXISTING） | 角色 | 规范文件 |
|---|---|---|---|---|
| 登录/注册/找回密码 | Auth | /login /register /forget-password | 公开 | page-001 |
| 首页 | Home | /dashboard | 公开 | page-002 |
| 服务市场 | Services | /services | 公开 | page-003 |
| 服务详情 | ServiceDetail | /services/:id | 公开 | page-004 |
| 创建订单 | CreateOrder | CreateOrderDialog（弹窗流程） | OWNER 等 | page-005 |
| 订单列表 | Orders | /orders | 登录（用户区） | page-006 |
| 订单详情 | OrderDetail | /orders/:id | 登录（用户区） | page-007 |
| 支付/充值 | Payment | /payments /recharge | 登录 | page-008 |
| 宠物档案 | Pets | /pets /pets/:id | 登录 | page-009 |
| 寄养师市场 | Keepers | /keepers /keepers/:id | 登录 | page-010 |
| 商家页 | Merchants | /merchants /merchants/:id | 登录 | page-011 |
| 个人中心 | Profile | /profile(+子页) /addresses /files | 登录（含 CS） | page-012 |
| 消息通知 | Notifications | /notifications /notices/:id | 登录 | page-013 |
| IM 聊天+AI | Chat | /chat /ai /ai/chat | 登录 | page-014 |
| 收藏 | Favorites | /favorites | 登录 | page-015 |
| 优惠券/会员 | Coupons·Membership | /coupons /membership | 登录 | page-016 |
| 钱包/售后 | Wallet·Refunds | /wallet /refunds /ratings /credit-reputation /revenue /anomaly /complaints | 登录 | page-017 |
| 寄养师工作台 | KeeperWorkspace | /keeper-workflow /keeper/profile /keeper-apply /customer-service/apply | KEEPER/ADMIN 等 | page-018 |
| 商家工作台 | MerchantWorkspace | /merchant/*（动态路由） | MERCHANT/ADMIN | page-019 |
| 管理后台 | AdminWorkspace | /admin/*（动态路由） | ADMIN | page-020 |
| 客服中心 | SupportWorkspace | /merchant/support/*（动态路由）+ tickets | MERCHANT/ADMIN/CS | page-021 |
| 错误与全局态 | Errors | /403 /404 /500 /503 | 全部 | page-022 |

## 页面规范统一模板

每页包含：Purpose · Role · Entry/Exit · Navigation · Layout · Sections · Components · Data/API · 状态（Loading/Empty/Error/Success）· 权限 · 交互 · 校验 · 动画 · 平台行为。

## 移动端页面状态标准（全部页面强制）

```
UiState = Loading | Success(data) | Empty | Error(cause) | Refreshing | Submitting | Offline
Trigger → UI → Action 映射见 08-android/state-management.md
```
