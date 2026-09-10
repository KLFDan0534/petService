# Business Rules（业务规则清单）

> EXISTING（来自 Controller 语义 / 前端表单校验 / 状态枚举）+ DERIVED 标注。

## 订单域

| # | 规则 | 等级 |
|---|---|---|
| O1 | 订单创建后为 `pending`，支付超时未付可被取消（前端有支付倒计时 `orderPaymentTimeout` [E]） | E/D |
| O2 | 一个订单可批量创建（多宠物，`/orders/batch`） | E |
| O3 | 预约日期必须落在服务可用时段（`/services/{id}/availability`） | E |
| O4 | 支付走钱包余额（`/payments/create`→`/payments/pay`）；金额=报价-券-会员权益抵扣 | E |
| O5 | `completed` 后才能评价；评价可被商家回复（reply） | E/D |
| O6 | 小费挂在订单（`/tips/order/{orderId}`） | E |
| O7 | 状态变更全部由后端裁决，前端按钮按状态机渲染（见 state-machines.md） | E/D |

## 优惠券 / 会员

| # | 规则 | 等级 |
|---|---|---|
| C1 | 券有模板（发放 grant/grant-all/grant-condition）、领取（claim）、可用（available）三层 | E |
| C2 | 下单前用 `/coupons/quote` 实时报价，禁止前端自算抵扣 | E |
| C3 | 会员权益下单可抵扣（`/membership/benefits/order/quote` + usages 记次） | E |

## 履约域

| # | 规则 | 等级 |
|---|---|---|
| F1 | 寄养师需资质审核通过（Qualification）且被商家雇佣（merchant-approve）才可执行 | E |
| F2 | 考勤 check-in/check-out 按日（`/me/current`、`/me/today`） | E |
| F3 | 日报（CareRecord）按订单+日期，支持图片上传 | E |
| F4 | 在线状态 1 在线 / 3 离线 / 4 忙碌可自切换 | E |

## 资金域

| # | 规则 | 等级 |
|---|---|---|
| W1 | 充值 → 余额+；收入（订单/小费）→ 余额+；全部生成流水（Transaction） | E |
| W2 | 提现 pending → ADMIN approve → complete（打款）→ 余额-；reject 退回 | E |
| W3 | ADMIN 可人工调整钱包（`/wallet/admin/adjust`，记审计） | E |

## 治理域

| # | 规则 | 等级 |
|---|---|---|
| G1 | 所有删除为软删除（`deleted_wsh`）→ 回收站可恢复/彻底删 | E |
| G2 | 写操作带 `@LogOperation` 审计 | E |
| G3 | UGC（评价/头像/服务图等）过内容审核（pending→approved/rejected） | E |
| G4 | 实名认证 0→1→2/3；未实名可限制某些操作 [D] | E/D |

## 通用

- 金额一律后端计算与校验；前端只展示（PRODUCT.md 安全原则）[E]
- API 内容按纯文本渲染 [E]
- 分页默认约定见 06-api/pagination.md
