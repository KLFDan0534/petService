# State Machines（状态机）

> 状态值与中文标签全部 EXISTING（statusMaps.js / pet-common 枚举）。转移条件 EXISTING（Controller 端点语义）。
> **App 强制**：UI 按状态渲染操作，禁止发明状态或前端流转。

## Order（订单）

```
pending(待付款,warning)
  ├─ 支付 /payments/pay → paid(已支付,info)
  ├─ 取消 /orders/cancel → cancelled(已取消,danger)
paid → 商家/寄养师 accept /orders/accept → confirmed(待送达,info)
confirmed → /orders/delivered → delivered(已送达,primary)
delivered → /orders/received → received(已接收,primary)
received → /orders/start(+图) → in_progress(服务中,info)
in_progress → /orders/complete → completed(已完成,success)
accept 前拒单 /orders/reject → cancelled
任意未完成态 → 退款申请 → refunding(退款中,warning) → refunded(已退款,success)
```
Who/What：pending 操作=OWNER；accept/reject/delivered/complete=MERCHANT/KEEPER；received=OWNER；start=KEEPER。UI 映射见 page-006/007。

## Refund（退款）[E]

```
pending(待审核,info) → approve → approved(已通过·待退款,primary)
                     → reject → rejected(已拒绝,danger)
approved → complete → completed(已完成,success)
```

## Complaint（投诉）[E]

```
pending(待处理,warning) → accept → processing(受理中,info)
processing → resolve → resolved(已处理,success)
          → reject → rejected(已驳回,danger)
```

## Ticket（工单）[E]

```
pending(待处理,warning) → assign → processing(处理中,info)
→ resolve → resolved(已解决,success) → close → closed(已关闭,neutral)
优先级 low/medium/high/urgent [E]
```

## 审核类（0/1/2 数值态 [E]）

```
MerchantStatus: 0 待审(warning) → 1 通过(success) | 2 拒绝(danger)
KeeperReviewStatus / ReviewStatus / ContentReview: pending → approved | rejected
RealNameStatus: 0 未实名 → 1 审核中 → 2 通过 | 3 未通过
CsApplicationStatus: pending → approved | rejected；在职态 resigned / terminated
LeaveApprovalStatus: pending → approved | rejected
WithdrawalStatus: pending → approved → completed | rejected
ServiceStatus / BannerStatus: 0 下架(secondary) ↔ 1 上架(success)
PaymentStatus / TransactionStatus: pending → success | failed
UserStatus: 1 正常 ↔ 0 封禁
```

## 在线状态 [E]

KeeperOnlineStatus：1 在线(success) · 3 离线(neutral) · 4 忙碌(warning)。

## App 渲染规则

- Badge 类别按 statusMaps [E]；未知值 fallback `badge-info` + 原值文本 [E getStatusBadge]
- 操作按钮 = f(状态, 角色) 白名单（见 04-user-roles/role-matrix.md）
- SSE 事件到达 → 重新拉取详情，禁止本地预测流转
