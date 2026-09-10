# page-017 · 钱包与售后聚合（Wallet / Refunds / Ratings / Credit / Revenue / Anomaly / Complaints）

- **Purpose**：资产与售后自助入口群 [E 多路由]
- **Role**：登录（用户区）

## Wallet（/wallet）

```
余额 stat 大数字 + 明细入口 + 充值/提现按钮
流水列表：TransactionListItem（类型 Badge [E TransactionStatus] + 金额(+/−, tabular) + 时间）
提现：金额表单 → POST /withdrawals（pending）→ 状态追踪（approved→completed / rejected）
```
API：`GET /api/wallet/me` · `GET /api/transactions/me` · `POST /api/withdrawals` + `GET /api/withdrawals/me`。

## Refunds（/refunds）

申请列表 + 发起（关联订单，原因+凭证上传）；状态机 pending→approved(待退款)→completed / rejected [E RefundStatus]。
API：`GET /api/refunds/...`（用户维度）· 管理动作 approve/complete/reject [E，ADMIN]。

## Ratings（/ratings）

我的评价列表 + 商家回复展示；订单完成后从 OrderDetail 进入评价（ReviewDialog→BottomSheet：星级+内容+图片）。
API：`GET /api/ratings/my` · 提交/回复接口（OrderController/RatingController）。

## CreditReputation（/credit-reputation）

信用分 stat + 明细（`GET /api/statistics/reputation`）。

## RevenueCenter（/revenue）/ Anomaly（/anomaly）

收入概览（`GET /api/statistics/user`）与异常记录列表——移动端以只读列表+筛选呈现 [D]。

## Complaints（/complaints）

投诉列表/发起（选目标 `GET /api/complaints/targets` + 描述 + 证据上传 `POST /api/complaints/{id}/evidence`）；状态 pending→processing→resolved/rejected [E]。
