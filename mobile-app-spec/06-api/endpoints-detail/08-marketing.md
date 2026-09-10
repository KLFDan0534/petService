# 08 · 优惠券 / 会员（Coupon · Membership）

## Coupon（页面：page-016 / 005）
- `GET /api/coupons/templates` 模板列表（ADMIN 管理）
- `GET /api/coupons/templates/active` 可领模板（用户）
- `POST /api/coupons/templates` 创建 · `PUT /api/coupons/templates/{id}` · `PATCH .../{id}/status` 上/下架（ADMIN）
- `POST /api/coupons/templates/{id}/grant` 定向发放 · `/grant-all` 全员 · `/grant-condition` 条件发放（ADMIN）
- `POST /api/coupons/templates/{id}/claim` 领取（错误：已领取/已领完/过期）
- `GET /api/coupons/my` 我的券 · `GET /api/coupons/available` 下单可用券
- `POST /api/coupons/quote` —— 下单抵扣报价
  请求：`coupon_id_wsh, service_id_wsh, 金额/日期参数 [D]`
  响应：抵扣金额 + 应付（**前端不自算 [E C2]**）
  错误：券不可用/过期/门槛不足

## 会员计划 MemberPlan（ADMIN）
- `GET /api/membership/plans/active` 在售计划（用户）
- `GET /api/membership/admin/plans` · `POST/PUT /api/membership/admin/plans` · `PATCH .../{id}/status` · `DELETE .../{id}`

## 会员身份与权益 Membership（页面：page-016 / 005）
- `GET /api/membership/me` 我的会员（到期 meta [D]）
- `POST /api/membership/benefits/order/quote` 下单权益抵扣报价（同券 quote 模式 [E]）
- `GET /api/membership/usages` 权益使用记录
- `GET /api/membership/admin/users` · `GET /api/membership/admin/usages`（ADMIN）
- `POST /api/membership/admin/users/expire` 手动过期（ADMIN）

## 会员订单 MembershipOrder（页面：page-016 / 008）
- `POST /api/membership/orders`（MemberPlanController 下单，语义 [D]）→ orderNo（pending）
- `GET /api/membership/orders/{orderNo}` · `POST .../{orderNo}/cancel` 取消 · `POST .../{orderNo}/pay` 支付（钱包，同 page-008 模式）
- `GET /api/membership/orders/admin/list` · `POST .../admin/{orderNo}/pay`（ADMIN 代支付/核销 [D]）

## 规则
券/权益在 CreateOrder 页为只读引用 + quote 实时计算；支付链路复用订单支付流（page-008）。
