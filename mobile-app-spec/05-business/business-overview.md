# Business Overview（业务总览）

## 领域划分（EXISTING，pet-business 7 子域 + system/ai）

| 子域 | Controller（EXISTING） | 说明 |
|---|---|---|
| 宠物 | Pet · Category · CareRecord | 宠物档案/两级分类/照护日报 |
| 寄养 | Keeper · KeeperAttendance · KeeperLeave · Merchant · Address · ServiceItem · BusinessHours · ServiceCategory · Qualification | 供给侧全部 |
| 订单 | Order · OrderFulfillment · OrderEvent(SSE) · Payment · Refund · Tip | 交易核心 |
| 客服 | Chat · ChatEvent(SSE) · Complaint · Rating · Ticket · CsWorkbench · MerchantCustomerService | 沟通与售后 |
| 财务 | Wallet · Transaction · Withdrawal | 资金 |
| 运营 | Notice · Favorite · File · Notification(+SSE) · ContentReview · OperationLog · RecycleBin · Coupon · Membership(3) · Geo | 平台运营 |
| AI | AiChat(+History) · Rag · AiReport · Agent · AiConfig | 智能能力 |
| 系统 | Auth · User · Role · RealNameReview · Statistics | 账号与统计 |

## 交易闭环（核心）

```
上架服务 → 浏览/收藏 → 下单(券/会员权益抵扣) → 支付(钱包)
→ 履约(接单→配送→接收→开始→日报/考勤→完成) → 评价/打赏
→ 资金结算(余额/提现) ；旁路：取消/退款/投诉/工单
```

## 事件与实时（EXISTING）

RabbitMQ 异步：订单状态事件、消息、AI 报告生成、投诉处理等（6 队列）。
SSE 三通道：`order-events`（订单状态）、`notification-events`（通知红点）、`chat-events`（IM）——统一 `?token=<jwt>` 鉴权、断线指数退避、失败降级 30s 轮询 [E SocketManager]。

## 平台治理闭环

入驻/资质/内容/实名四类审核（pending→approve/reject）+ 软删除回收站 + 操作日志审计 + RBAC。
