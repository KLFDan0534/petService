# Enums（枚举与状态映射）

> 全部 EXISTING（statusMaps.js + pet-common 枚举）。App 端建立等价 enum + `Badge` 类别映射；未知值 fallback：label=原值、badge=info [E getStatusBadge]。

## OrderStatus

| 值 | 中文 | Badge |
|---|---|---|
| pending | 待付款 | warning |
| paid | 已支付 | info |
| confirmed | 待送达 | info |
| delivered | 已送达 | primary |
| received | 已接收 | primary |
| in_progress | 服务中 | info |
| completed | 已完成 | success |
| cancelled | 已取消 | danger |
| refunding | 退款中 | warning |
| refunded | 已退款 | success |

## RefundStatus：pending 待审核(info) · approved 已通过·待退款(primary) · rejected 已拒绝(danger) · completed 已完成(success)

## ComplaintStatus：pending 待处理(warning) · processing 受理中(info) · resolved 已处理(success) · rejected 已驳回(danger)

## TicketStatus：pending 待处理(warning) · processing 处理中(info) · resolved 已解决(success) · closed 已关闭(neutral)
## TicketCategory：complaint 投诉(danger) · question 咨询(info) · suggestion 建议(warning) · other 其他(neutral)
## TicketPriority：low 低(neutral) · medium 中(info) · high 高(warning) · urgent 紧急(danger)

## 数值态（0/1/2）

| 枚举 | 0 | 1 | 2 | 3 |
|---|---|---|---|---|
| MerchantStatus | 待审(warning) | 通过(success) | 拒绝(danger) | — |
| MerchantStoreMode | 自动(info) | 开店(success) | 关店(neutral) | — |
| MerchantStoreStatus | 休息中(neutral) | 营业中(success) | — | — |
| KeeperReviewStatus / ReviewStatus / ContentReview | 待审(warning) | 通过(success) | 拒绝(danger) | — |
| BannerStatus / ServiceStatus | 下架(neutral) | 上架(success) | — | — |
| UserStatus | 封禁(danger) | 正常(success) | — | — |
| RealNameStatus | 未实名(neutral) | 审核中(warning) | 已通过(success) | 未通过(danger) |
| ReadStatus | 未读(warning) | 已读(success) | — | — |

## KeeperOnlineStatus：1 在线(success) · 3 离线(neutral) · 4 忙碌(warning)

## 其他

- CsApplicationStatus：pending/approved/rejected/resigned(已辞职)/terminated(已终止)
- TransactionStatus：pending/success/completed(success)/failed(danger)
- PaymentStatus：pending(warning)/success(success)/failed(danger)
- NotificationType：system(info)/order(primary)/notice(warning)
- FavoriteTargetType：merchant(info)/keeper(primary)/service(success)
- LeaveApprovalStatus：pending→approved/rejected
- FavoriteTargetType（后端枚举）与 BookingUnit（计价单位 [E pet-common]）
