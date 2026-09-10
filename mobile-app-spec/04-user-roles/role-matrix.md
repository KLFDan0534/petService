# Role × Feature × Permission 矩阵

> ✓=可 · —=不可 · △=受限/协作（DERIVED from 路由 meta + Controller 语义；后端最终裁决以 @PreAuthorize/归属校验为准）

| 功能 | OWNER | KEEPER | MERCHANT | CS | ADMIN | 证据 |
|---|---|---|---|---|---|---|
| 公开浏览（Dashboard/Services/ServiceDetail） | ✓ | ✓ | ✓ | ✓ | ✓ | requiresAuth:false [E] |
| 用户区页面（pets/orders/...） | ✓ | ✓ | ✓ | — | ✓ | USER_AREA_ROLES [E] |
| 账户区页面（profile/notifications/tickets） | ✓ | ✓ | ✓ | ✓ | ✓ | ACCOUNT_AREA [E] |
| 支持区工作台 | — | — | ✓ | ✓ | ✓ | SUPPORT_AREA [E] |
| 管理区 | — | — | — | — | ✓ | adminRoutes [E] |
| 创建/支付订单 | ✓ | — | — | — | — | 下单流程 |
| 取消订单 | ✓(own) | — | △ | — | — | CancelOrderDialog |
| accept/reject/delivered/start/complete | — | ✓ | ✓ | — | — | OrderController |
| received/评价/打赏/退款申请 | ✓ | — | — | — | — | — |
| 日报/考勤 | — | ✓ | — | — | — | keeper-attendance |
| 服务 CRUD/图片 | — | — | ✓ | — | — | ServiceItemController |
| 营业时间/开关店 | — | — | ✓ | — | — | BusinessHours/store-mode |
| 寄养师平台审核 | — | — | — | — | ✓ | /keepers/{id}/approve |
| 寄养师商家审核 | — | — | ✓ | — | — | merchant-approve |
| CS 申请商家审批 | — | — | ✓ | — | — | merchant/applications |
| CS 申请平台审批 | — | — | — | — | ✓ | admin/{id}/approve |
| 工单 assign/resolve/close | — | — | ✓ | ✓ | ✓ | TicketController |
| 投诉 accept/resolve/reject | — | — | ✓ | ✓ | ✓ | ComplaintController |
| 退款 approve/complete/reject | — | — | — | — | ✓ | RefundController |
| 提现审批 | — | — | — | — | ✓ | WithdrawalController |
| 钱包 admin/adjust | — | — | — | — | ✓ | WalletController |
| 公告/Banner/优惠券运营 | — | — | — | — | ✓ | Notice/Coupon |
| 用户封禁/角色管理 | — | — | — | — | ✓ | users/{id}/status, roles |
| 审计/回收站 | — | — | — | — | ✓ | operation-logs, recycle-bin |
| 商家资料/统计 | △ | — | ✓(own) | — | ✓ | merchants/my, statistics |
