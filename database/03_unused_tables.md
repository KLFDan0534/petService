# 未使用的表和字段分析报告

> 生成日期: 2026-06-28
> 分析范围: 全项目35张数据库表 + 34个Entity + 35个Mapper + 33个Service + 33个Controller

---

## 一、所有表使用情况总览

| 表名 | Entity存在 | Mapper存在 | Service调用 | Controller调用 | 状态 |
|------|-----------|-----------|------------|---------------|------|
| `user_wsh` | ✅ User | ✅ UserMapper | ✅ UserService | ✅ AuthController/UserController | ✅ 已使用 |
| `role_wsh` | ✅ Role | ✅ RoleMapper | ✅ 注入Controller | ✅ RoleController | ✅ 已使用 |
| `user_role_wsh` | ✅ UserRole | ✅ UserRoleMapper | ✅ RoleService | ✅ RoleController | ✅ 已使用 |
| `pet_wsh` | ✅ Pet | ✅ PetMapper | ✅ PetService | ✅ PetController | ✅ 已使用 |
| `category_wsh` | ✅ Category | ✅ CategoryMapper | ✅ CategoryService | ✅ CategoryController | ✅ 已使用 |
| `care_record_wsh` | ✅ CareRecord | ✅ CareRecordMapper | ✅ CareRecordService | ✅ CareRecordController | ✅ 已使用 |
| `merchant_wsh` | ✅ Merchant | ✅ MerchantMapper | ✅ MerchantService | ✅ MerchantController | ✅ 已使用 |
| `keeper_wsh` | ✅ Keeper | ✅ KeeperMapper | ✅ KeeperService | ✅ KeeperController | ✅ 已使用 |
| `pet_service_wsh` | ✅ ServiceItem | ✅ ServiceItemMapper | ✅ ServiceItemService | ✅ ServiceItemController | ✅ 已使用 |
| `address_wsh` | ✅ Address | ✅ AddressMapper | ✅ AddressService | ✅ AddressController | ✅ 已使用 |
| `business_hours_wsh` | ✅ BusinessHours | ✅ BusinessHoursMapper | ✅ BusinessHoursService | ✅ BusinessHoursController | ✅ 已使用 |
| `pet_order_wsh` | ✅ PetOrder | ✅ OrderMapper | ✅ OrderService | ✅ OrderController | ✅ 已使用 |
| `payment_wsh` | ✅ Payment | ✅ PaymentMapper | ✅ PaymentService | ✅ PaymentController | ✅ 已使用 |
| `refund_wsh` | ✅ Refund | ✅ RefundMapper | ✅ RefundService | ✅ RefundController | ✅ 已使用 |
| `tip_wsh` | ✅ Tip | ✅ TipMapper | ✅ TipService | ✅ TipController | ✅ 已使用 |
| `chat_message_wsh` | ✅ ChatMessage | ✅ ChatMessageMapper | ✅ ChatService | ✅ ChatController | ✅ 已使用 |
| `complaint_wsh` | ✅ Complaint | ✅ ComplaintMapper | ✅ ComplaintService | ✅ ComplaintController | ✅ 已使用 |
| `rating_wsh` | ✅ Rating | ✅ RatingMapper | ✅ RatingService | ✅ RatingController | ✅ 已使用 |
| `ticket_wsh` | ✅ Ticket | ✅ TicketMapper | ✅ TicketService | ✅ TicketController | ✅ 已使用 |
| `ticket_message_wsh` | ✅ TicketMessage | ✅ TicketMessageMapper | ✅ TicketService | ✅ TicketController | ✅ 已使用 |
| `wallet_wsh` | ✅ Wallet | ✅ WalletMapper | ✅ WalletService | ✅ WalletController | ✅ 已使用 |
| `wallet_transaction_wsh` | ✅ Transaction | ✅ TransactionMapper | ✅ TransactionService | ✅ TransactionController | ✅ 已使用 |
| `withdrawal_wsh` | ✅ Withdrawal | ✅ WithdrawalMapper | ✅ WithdrawalService | ✅ WithdrawalController | ✅ 已使用 |
| `notice_wsh` | ✅ Notice | ✅ NoticeMapper | ✅ NoticeService | ✅ NoticeController | ✅ 已使用 |
| `notice_read_wsh` | ✅ NoticeRead | ✅ NoticeReadMapper | ✅ NoticeService | ✅ NoticeController | ✅ 已使用 |
| `notification_wsh` | ✅ Notification | ✅ NotificationMapper | ✅ NotificationService | ✅ NotificationController | ✅ 已使用 |
| `operation_log_wsh` | ✅ OperationLog | ✅ OperationLogMapper | ✅ OperationLogService | ✅ OperationLogController | ✅ 已使用 |
| `file_record_wsh` | ✅ FileRecord | ✅ FileRecordMapper | ✅ FileRecordService | ✅ FileController | ✅ 已使用 |
| `favorite_wsh` | ✅ Favorite | ✅ FavoriteMapper | ✅ FavoriteService | ✅ FavoriteController | ✅ 已使用 |
| `content_review_wsh` | ✅ ContentReview | ✅ ContentReviewMapper | ✅ ContentReviewService | ✅ ContentReviewController | ✅ 已使用 |
| `adoption_pet_wsh` | ✅ AdoptionPet | ✅ AdoptionPetMapper | ✅ AdoptionPetService | ✅ AdoptionController | ✅ 已使用 |
| `adoption_application_wsh` | ✅ AdoptionApplication | ✅ AdoptionApplicationMapper | ✅ AdoptionApplicationService | ✅ AdoptionController | ✅ 已使用 |
| `ai_report_wsh` | ✅ AiReport | ✅ AiReportMapper | ✅ AiReportService | ✅ AiReportController | ✅ 已使用 |
| `knowledge_document_wsh` | ✅ KnowledgeDocument | ✅ KnowledgeDocumentMapper | ✅ RagService | ✅ RagController | ✅ 已使用 |
| `document_embedding_wsh` | ❌ 无 | ❌ 无Mapper | ✅ RagService | ✅ RagController | ⚠️ 特殊处理 |

---

## 二、废弃/未使用的字段

| 表名 | 字段 | 使用状态 | 说明 |
|------|------|---------|------|
| `wallet_wsh` | 无 | ✅ 全部使用 | 所有字段都有Entity映射 |
| `pet_order_wsh` | `emergency_contact_relation_wsh` | ⚠️ 部分使用 | 数据库有但Entity无映射 |
| `merchant_wsh` | `avatar_wsh` | ⚠️ 部分使用 | 数据库有但Entity无映射 |

---

## 三、不存在于数据库的表

以下表在代码或SQL脚本中存在引用，但数据库中不存在：

| 表名 | 引用位置 | 说明 |
|------|---------|------|
| `recycle_bin_wsh` | RecycleBinMapper | 该表在代码中为虚拟概念，RecycleBinService直接通过动态SQL操作各表的deleted字段，不需要独立表 |

---

## 四、无Entity的数据库表

| 表名 | 说明 |
|------|------|
| 无 | ✅ 所有35张表均有对应的Entity |

---

## 五、前端使用分析

所有35张表对应的业务功能在前端66个页面中均有对应路由和页面组件，无前端未使用的表。

---

## 六、DTO/VO使用分析

| DTO/VO | 对应表 | 使用状态 |
|--------|--------|---------|
| `UserVO` | `user_wsh` | ✅ |
| `KeeperVO` | `keeper_wsh` | ✅ |
| `PetDTO` | `pet_wsh` | ✅ |
| `OrderDTO` | `pet_order_wsh` | ✅ |
| 等共98个DTO/VO文件 | 35张表 | ✅ 全部使用 |

**注意**: `TicketMessageDTO.java` 和 `TicketDTO.java` 为空文件(0字节)，但未被任何代码引用。

---

## 七、结论

| 类别 | 数量 |
|------|------|
| 未使用的表 | 0张 |
| 未使用的字段 | 2个(Entity未映射) |
| 废弃的表 | 0张 |
| 废弃的Entity | 0个 |
| 废弃的Mapper | 0个 |
| 空DTO文件 | 2个(TicketMessageDTO, TicketDTO) |
| 数据库存在但无Entity的表 | 0张 |
| Entity存在但数据库无表 | 0张 |
