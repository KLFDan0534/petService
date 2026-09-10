# Entities（数据实体）

> 55 表（EXISTING PROJECT_MAP）。逐表 DDL 未全量读取（unresolved），实体按 Controller/枚举/前端字段归纳；置信度标注：E=确证 · D=推导。

| 实体 | 关键字段（`_wsh`） | 置信 | 关系 |
|---|---|---|---|
| User | id, username, nickname, avatar, phone, email, real_name_status, status | E | 1-N UserRole |
| Role / UserRole | code(ROLE_*), name | E | RBAC |
| Pet | id, owner_id, name, species, breed, gender, birth, weight, avatar, images, note | D | N-1 User；N-N Order |
| Category | id, parent_id（两级树）, name | E | 自关联 |
| Merchant | id, user_id, name, status(0/1/2), store_mode, store_status, media, intro | D | 1-N ServiceItem/Keeper |
| BusinessHours | merchant_id, 时段, 开关店 | D | N-1 Merchant |
| ServiceItem | id, merchant_id, category_id, title, price, unit(BookingUnit), status, images, description | E | N-1 Merchant/Category |
| ServiceVersion | 服务版本快照 | E(类) | N-1 ServiceItem |
| Address | id, user_id, 联系人/电话/经纬度/详址, default | D | N-1 User |
| Order | id, order_no, user_id, merchant_id, keeper_id, service_id, pet_ids, 日期, 金额, coupon, status | E | 核心 |
| OrderFulfillment | order_id, timeline, daily_status | D | 1-1 Order |
| CareRecord | order_id, date, content, images | D | N-1 Order |
| Payment | order_no, amount, status | E | N-1 Order |
| Refund | order_id, 原因/凭证, status | E | N-1 Order |
| Tip | order_id, 金额 | E | N-1 Order |
| Wallet | user_id, balance | E | 1-1 User |
| Transaction | user_id, type, amount, status | E | N-1 Wallet |
| Withdrawal | user_id, amount, status | E | N-1 User |
| CouponTemplate / UserCoupon | 门槛/面额/有效期；领取记录 | E | — |
| MemberPlan / MembershipOrder / MembershipUsage | 计划/订单/权益次数 | E | — |
| Qualification | owner_type, owner_id, 材料, status | E | 多态（keeper/merchant） |
| Keeper | user_id, 商家隶属, 评分, online_status | D | N-1 User/Merchant |
| KeeperAttendance / KeeperLeave | keeper_id, 打卡/请假状态 | E | N-1 Keeper |
| Conversation / Message | 双方, 最后消息, 已读 | D | IM |
| Complaint / ComplaintMessage / Evidence | 状态机, 证据 | E | — |
| Ticket / TicketMessage | 状态/分类/优先级 | E | — |
| Rating | order_id, 星级, 内容, reply | E | N-1 Order |
| Favorite | user_id, target_type(merchant/keeper/service), target_id | E | 多态 |
| Notice / Banner | 内容, 上架, 弹窗标记 | E | 运营 |
| Notification | user_id, type, read | E | N-1 User |
| File | MinIO 对象, 归属 | E | — |
| ContentReview | 目标内容, status | E | 多态 |
| OperationLog | @LogOperation 审计 | E | — |
| RecycleBin | 软删除快照 | E | — |
| AI: RagDocument / AiReport / AiChatSession | RAG/报告/会话 | E | — |

## 关系图（核心链路）

```
User ─1:N─ Pet        User ─1:1─ Wallet ─1:N─ Transaction
User ─1:N─ Order ─1:1─ OrderFulfillment ─1:N─ CareRecord
Order ─N:1─ ServiceItem ─N:1─ Merchant ─1:N─ BusinessHours
Order ─1:0..1─ Payment / Refund / Tip / Rating
User ─1:N─ Favorite（多态 → Merchant/Keeper/ServiceItem）
```
