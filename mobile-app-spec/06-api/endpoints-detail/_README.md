# Endpoints Detail（端点详单索引）

> 逐域端点规格。**EXISTING**：路径来自 Controller 注解扫描（全量）。
> **DERIVED**：请求/响应字段按 data-models.md 与前端消费代码推导，联调时以真实响应修正（记 conflicts.md）。
> 通用契约（Result 包裹 / 分页 / `_wsh` 命名 / 401 刷新）见 `../common.md`、`../authentication.md`，各条目不再重复。

## 域索引

| 文件 | 覆盖 Controller |
|---|---|
| `01-auth-users.md` | AuthController · UserController · RoleController · RealNameReviewController |
| `02-pets-care.md` | PetController · CategoryController · CareRecordController |
| `03-keepers.md` | KeeperController · KeeperAttendanceController · KeeperLeaveController · QualificationController |
| `04-merchants-services.md` | MerchantController · BusinessHoursController · ServiceItemController · ServiceCategoryController · AddressController · GeoController |
| `05-orders-fulfillment.md` | OrderController · OrderFulfillmentController · OrderEventController |
| `06-payments-finance.md` | PaymentController · RefundController · TipController · WalletController · TransactionController · WithdrawalController |
| `07-chat-support.md` | ChatController · ChatEventController · ComplaintController · CsWorkbenchController · MerchantCustomerServiceController · RatingController · TicketController |
| `08-marketing.md` | CouponController · MemberPlanController · MembershipController · MembershipOrderController |
| `09-ops.md` | NoticeController · NotificationController · NotificationEventController · FavoriteController · FileController · ContentReviewController · OperationLogController · RecycleBinController · StatisticsController |
| `10-ai.md` | AiChatController · AiChatHistoryController · RagController · AiReportController · AgentController · AiConfigController |

## 条目格式

```
METHOD /path —— 用途
权限：角色/登录要求    页面：page-xxx
请求：参数/Body 要点
响应：data 要点
错误：主要业务错误
```

## 动词约定

扫描未逐条确认动词（unresolved #2）。默认约定：读取=GET · 创建/动作=POST · 更新=PUT · 局部更新/状态=PATCH · 删除=DELETE。带 `approve/reject/cancel/complete/pay/accept` 等动作词一律 POST。
