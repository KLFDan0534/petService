# page-016 · 优惠券与会员（Coupons / Membership）

- **Purpose**：营销券管理与领取；会员计划与权益 [E /coupons /membership]
- **Role**：登录（用户区）

## Coupons

```
Tab：可领（templates/active）· 我的（/my）· 下单可用（/available）
券卡：面额 stat 衬线 + 门槛 meta + 有效期 + 状态 Badge
      领取=claim；管理员发放=grant/grant-all/grant-condition [E]
```
API：`GET /api/coupons/templates(/active)` · `POST /api/coupons/templates/{id}/claim` · `GET /api/coupons/my|available` · 报价 `POST /api/coupons/quote`。

## Membership

```
会员页：
├── 我的会员卡（GET /api/membership/me）+ 到期 meta
├── 计划列表（plans/active）：价格 stat + 权益清单
├── 购买：POST /api/membership/orders（orderNo）→ 支付（同 page-008，/pay）
└── 权益使用记录 /api/membership/usages；下单权益抵扣 quote [E]
```
API：`GET /api/membership/plans/active` · `POST /api/membership/orders/{orderNo}/pay|cancel` · `GET /api/membership/orders/{orderNo}`。

## States

券不可领/已领 → 按钮 disabled + 文案；会员订单 pending → 可支付/取消（同订单支付流）。
