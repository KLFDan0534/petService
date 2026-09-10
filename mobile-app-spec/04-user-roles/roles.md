# Roles（角色定义）

> 全部 EXISTING：`permission.js ROLES`、`stores/auth.js`、路由 meta、后端 `ROLE_` 前缀。

| Role | 代码 | 职责 | 核心页面（移动） |
|---|---|---|---|
| 宠物主人 | `OWNER` | 下单/支付/追踪/评价/售后 | 首页·服务·宠物·订单·钱包·IM |
| 寄养师 | `KEEPER` | 接单执行/考勤/日报/在线状态 | 寄养工作台·订单任务 |
| 商家 | `MERCHANT` | 服务上架/接单派单/寄养师与客服管理/提现 | 商家工作台 |
| 客服 | `CUSTOMER_SERVICE` | 会话/工单/投诉处理 | 客服中心 |
| 管理员 | `ADMIN` | 全平台审核/运营/财务/权限 | 管理后台 |

## 角色语义（EXISTING 代码证据）

- 一个账号可持多角色（`roles_wsh[]` 数组；auth 提供 `isAdmin/isMerchant/isOwner/isCs` 计算 [E]）
- 角色来自 JWT（`roles_wsh`），refresh 时会随响应更新 [E request.js tryRefresh]
- 前端比较前统一去掉 `ROLE_` 前缀 [E]
- `USER_AREA_ROLES = OWNER/KEEPER/MERCHANT/ADMIN`；`ACCOUNT_AREA_ROLES = +CUSTOMER_SERVICE`；`SUPPORT_AREA_ROLES = MERCHANT/ADMIN/CUSTOMER_SERVICE` [E router]

## 各角色可用动作摘要

| 动作 | OWNER | KEEPER | MERCHANT | CS | ADMIN |
|---|---|---|---|---|---|
| 浏览服务/商家/寄养师 | ✓ | ✓ | ✓ | ✓ | ✓ |
| 宠物建档 | ✓ | — | — | — | — |
| 下单/支付 | ✓ | — | — | — | — |
| 订单 accept/reject/delivered/complete | — | ✓ | ✓ | — | — |
| 订单 received/评价/退款申请 | ✓ | — | — | — | — |
| 日报/考勤/在线状态 | — | ✓ | — | — | — |
| 服务 CRUD | — | — | ✓ | — | — |
| 寄养师审核 | — | — | ✓(商家侧) | — | ✓(平台侧) |
| 工单/投诉处理 | — | — | ✓ | ✓ | ✓ |
| 各类平台审核 | — | — | — | — | ✓ |
| 钱包充值/提现申请 | ✓ | ✓ | ✓ | — | — |
| 提现审批/钱包调整 | — | — | — | — | ✓ |
