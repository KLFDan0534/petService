# page-008 · 支付与充值（Payment / Recharge）

- **Purpose**：钱包支付订单 / 余额充值 [E /payments /recharge]
- **Role**：登录（用户区）
- **Entry**：CreateOrder 提交 / 钱包页 / 订单详情待付款；**Exit**：订单详情 / 钱包

## Layout

```
支付页：
├── 应付金额（stat 大数字）
├── 支付方式：钱包余额（余额不足→去充值 [D]）；(移动支付渠道 [R])
├── 优惠券/会员权益摘要（只读回显）
└── 底部：确认支付 primary lg（含支付密码校验 [E /users/me/payment-password]）

充值页：
├── 金额快捷档 + 自定义金额（数字键盘，两位小数）
└── 充值按钮 → 成功 → Snackbar + 返回钱包
```

## API（EXISTING）

创建支付 `POST /api/payments/create` · 执行 `POST /api/payments/pay` · 订单支付状态 `GET /api/payments/order/{orderNo}` · 充值 `POST /api/wallet/recharge` · 钱包 `GET /api/wallet/me`。

## States / 规则

Submitting → 成功：replace 订单详情（Snackbar「支付成功」）；失败：保留页 + Toast；pending 超时 → 订单可取消（倒计时 [E]）。
金额校验：>0、≤余额（充值无上限）；防重复提交（幂等：提交中禁点）。
