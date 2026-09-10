# 06 · 支付 / 退款 / 小费 / 钱包 / 流水 / 提现

## Payment（页面：page-008 / 007）
- `POST /api/payments/create` —— 创建支付单。请求：`order_no_wsh, 金额校验`。响应：支付单（pending）
- `POST /api/payments/pay` —— 执行支付（钱包余额扣减；支付密码校验 [D]）。成功 → 订单 pending→paid
  错误：余额不足（引导去充值）· 密码错误 · 订单状态冲突
- `GET /api/payments/order/{orderNo}` —— 支付结果查询（支付后轮询确认 [D]）
- `GET /api/payments/admin-list`（ADMIN）

## Refund（状态机 pending→approved→completed / rejected [E]；页面 page-017 / 020）
- `GET /api/refunds/...` 列表（用户维度 /admin-list）
- `POST /api/refunds`（或订单侧申请端点）申请：原因 + 凭证 → pending
- `POST /api/refunds/{id}/approve`（ADMIN → approved）
- `POST /api/refunds/{id}/complete`（ADMIN 打款 → completed；资金退回钱包）
- `POST /api/refunds/{id}/reject`（ADMIN；原因）

## Tip（页面：page-007）
- `GET /api/tips/order/{orderId}` 订单小费记录
- `POST /api/tips`（TipDialog → BottomSheet；金额选择；余额支付 [D]）
- `GET /api/tips/me` · `GET /api/tips/admin-list`

## Wallet（页面：page-012 / 017）
- `GET /api/wallet/me` —— 余额（stat 展示）
- `POST /api/wallet/recharge` —— 充值。请求：`amount_wsh`（>0 两位小数）。成功 → 余额+，生成流水
- `POST /api/wallet/admin/adjust`（ADMIN 人工调整；记审计 [E]）

## Transaction
- `GET /api/transactions/me` —— 流水分页（+/− 金额、status [E]）

## Withdrawal（状态机 pending→approved→completed/rejected [E]）
- `GET /api/withdrawals/me` 我的提现记录
- `POST /api/withdrawals` 申请（金额≤余额校验 [D]）→ pending
- `POST /api/withdrawals/{id}/approve`（ADMIN → approved）
- `POST /api/withdrawals/{id}/complete`（ADMIN 打款 → completed；余额扣减）
- `POST /api/withdrawals/{id}/reject`（ADMIN；退回）

## 资金安全规则（DERIVED）
支付/提现提交中禁点；结果以查询接口为准；所有资金变动必有 Transaction 流水（WF-6）。
