# Page Flows（关键页面流程）

## F1 认证流（EXISTING AuthController）

```
Register: 输入资料 →(可选)获取验证码 /auth/register/captcha → POST /auth/register
Login: POST /auth/login → setAuth(access/refresh/roles) → 注入动态路由 → Dashboard
Forgot: POST /auth/forgot-password → 重置 → Login
Refresh: 401 → POST /auth/refresh(refresh_token_wsh) → 换新对 → 重放原请求 [E request.js]
Logout: POST /auth/logout → 清存储 → 清动态路由 → Login [E]
```

## F2 下单支付流（核心，EXISTING Order/Payment/Coupon）

```
ServiceDetail
 → 选日期(availability 校验) → CreateOrder（选宠物/地址/备注）
 → POST /coupons/quote(券抵扣报价) → POST /orders/batch 创建（status=pending）
 → Payment: 钱包余额 POST /payments/create + /payments/pay
 → 成功 → Snackbar + replace 订单详情（confirmed 链路）
 失败/超时 → 订单 pending → 可继续支付或取消（POST /orders/cancel）
```

## F3 履约流（KEEPER/MERCHANT，EXISTING OrderFulfillmentController）

```
新订单(pending) → 商家/寄养师 accept(→confirmed) → delivered 配送
 → keeper 接收 received → start（可传图 upload）→ in_progress
 → 每日：check-in/check-out（考勤）+ 日报 CareRecord upload + 会话沟通
 → complete → completed → 用户评价 Rating → (可选) Tip
旁路：reject 拒单 / cancel 取消 / refund 退款仲裁
实时：order-events SSE 推送状态变化 → 订单列表/详情刷新
```

## F4 售后流（EXISTING Refund/Complaint/Ticket）

```
Refund: 用户申请(pending) → ADMIN approve(→approved 待退款) → complete(→completed) / reject
Complaint: 用户提交(pending) → 受理 processing → resolve/rejected（可传证据 evidence）
Ticket: 用户提交(pending) → CS assign(→processing) → 消息往来 → resolve → close
```

## F5 入驻/资质流（EXISTING Qualification/Keeper/Merchant）

```
用户提交 KeeperApply（资质材料 upload）
 → ADMIN /keepers/:id/approve|reject → 审核状态通知
 商家侧：MERCHANT /keepers/merchant/pending → merchant-approve|reject（雇佣）
 商家入驻：MerchantStatus 0待审→1通过→2拒绝（ADMIN 审批）
CS 申请：CsApplication pending→approved(上岗)/rejected/resigned/terminated
```

## F6 消息流（EXISTING ChatController + SSE）

```
会话列表(unread-count 徽标) → 会话详情(历史+发送 send)
SSE chat-events/stream 实时收消息 → 未读已读 read/read-conversation
AI：/ai/chat → sessions 历史 → POST /api/ai/chat 问答（RAG 知识库 /rag/ask）
```

## F7 财务流（EXISTING Wallet/Withdrawal/Transaction）

```
充值：/recharge → POST /wallet/recharge → 余额+
收入：订单完成/小费 → 余额+（Transaction 流水 /transactions/me）
提现：POST /withdrawals(pending) → ADMIN approve→complete / reject
会员：购买 MembershipOrder(pending→pay) → 权益次数 usages 下单抵扣
```
