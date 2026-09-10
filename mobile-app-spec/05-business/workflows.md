# Workflows（业务工作流）

> 每条流程的 API 与页面映射：页面见 03-pages/page-flows.md，API 见 06-api/endpoints.md。

## WF-1 注册→建档→首单（OWNER 冷启动）

```
Register(/auth/register) → Login
→ 建宠物(/pets POST) + 地址(/addresses POST，地图选点)
→ Services 选服务 → availability 查时段
→ CreateOrder(/coupons/quote + /orders/batch)
→ Payment(/payments/create + /pay)
→ OrderDetail（confirmed 链路，等待送达）
```

## WF-2 订单履约（供给侧）

```
商家/寄养师：pending 列表(/orders/pending)
→ accept(→confirmed) → 配送 delivered(→delivered)
→ 寄养师接收 received → start(→in_progress，可传图)
→ 每日循环：check-in → 日报 CareRecord → 与用户会话 → check-out
→ complete(→completed) → 触发结算事件（MQ）
→ 用户评价/打赏（可选）
旁路：reject / cancel（原因）/ refund 申请
实时：order-events SSE 驱动双方 UI 刷新
```

## WF-3 售后（退款）

```
用户：OrderDetail → 申请退款（原因+凭证）→ RefundStatus=pending
ADMIN：待办列表 → approve(→approved，待退款) → complete(→completed，资金退回)
      或 reject(→rejected)
Order 同步 → refunding / refunded；MQ 通知
```

## WF-4 入驻与资质

```
KEEPER: 用户提交申请(资质材料 upload) → ADMIN /keepers/{id}/approve|reject
        → 通过后可被商家雇佣（/keepers/merchant/pending → merchant-approve|reject）
        → 获得工作台（/keeper-workflow）
MERCHANT: 提交入驻 → ADMIN /merchants/{id}/approve|reject（0→1/2）
CS: 用户申请 → 商家 approve(上岗) / ADMIN admin-approve；离职 resign / 终止 terminate
```

## WF-5 客服处理

```
Ticket: 用户提交(pending) → CS assign(→processing)
        → 消息往来(/tickets/{id}/messages) → resolve → close
Complaint: 用户提交(pending，可传证据) → 受理 accept(→processing)
        → resolve / reject；双方可发消息
```

## WF-6 资金（提现）

```
用户：Wallet → 申请提现(金额校验≤余额) → pending
ADMIN：approve → complete（余额扣减）/ reject（退回）
流水全程可查（/transactions/me）
```

## WF-7 内容治理

```
UGC 提交 → ContentReview pending → approve（公开可见）/ reject（驳回+通知）
误删 → 回收站 restore；敏感操作 → OperationLog
```
