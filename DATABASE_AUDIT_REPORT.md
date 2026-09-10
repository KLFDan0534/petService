# 数据库完整审查报告（DATABASE AUDIT REPORT）

> 生成日期：2026-09-10
> 审查依据：当前完整后端代码（Entity/Mapper/Service/Controller/DTO/VO）+ 前端 API + 数据库 INFORMATION_SCHEMA（SSOT）+ pet-admin/src/main/resources/db/schema.sql + seed.sql + docs/database 历史快照
> 审查原则：以代码真实调用为准，禁止仅凭数据库猜测

---

## 1. 环境与基础设施状态

| 组件 | 状态 | 说明 |
|---|---|---|
| MySQL (pet-mysql / Docker) | ✅ 运行中 | 端口 3308，库 `pet_service`，lib/插件正常 |
| Redis (pet-redis) | ✅ 运行中 | 端口 6379，healthcheck healthy |
| RabbitMQ (pet-rabbitmq) | ✅ 运行中 | 端口 5672/15672 |
| MinIO (pet-minio) | ✅ 运行中 | 端口 9000/9001 |
| Chroma (pet-chroma) | ✅ 运行中 | 端口 8000 |
| Nginx (pet-nginx) | ✅ 运行中 | 端口 80 |
| 后端 (8080) | ❌ 启动失败 | 详见 §6 启动错误清单 |

---

## 2. 数据库当前状态（SSOT，INFORMATION_SCHEMA 直接导出）

### 2.1 总体情况

| 指标 | 值 |
|---|---|
| 表总数 | 52 |
| 字符集 | utf8mb4 / utf8mb4_0900_ai_ci |
| 有数据表 | **仅 2 张**：`knowledge_document_wsh`(5行)、`migration_ledger_wsh`(1行) |
| 空表 | **50 张业务表全部为空** ⚠️ |
| 外键 | 1（document_embedding_wsh → knowledge_document_wsh） |

### 2.2 结论（重要）

**数据库被重建后，所有业务数据（用户/角色/商家/宠物/服务/订单/支付/钱包等）全部丢失。**
表结构以 schema.sql + 增量 DDL 重建，但存在如下问题（见 §3/§4/§5）。

---

## 3. 表完整性对照（Entity ↔ 数据库）

### 3.1 代码引用且数据库存在的表（46 个不同 @TableName 全部存在）✅

| # | @TableName | Entity 类 | 模块 | 状态 |
|---|---|---|---|---|
| 1 | user_wsh | User | pet-system | ✅ 表存在，缺列见 §4 |
| 2 | role_wsh | Role | pet-system | ✅ |
| 3 | user_role_wsh | UserRole | pet-system | ✅ |
| 4 | pet_wsh | Pet | pet-business/pet | ✅ |
| 5 | category_wsh | Category | pet-business/pet | ✅ |
| 6 | care_record_wsh | CareRecord | pet-business/pet | ✅ |
| 7 | merchant_wsh | Merchant | pet-business/boarding | ✅ |
| 8 | keeper_wsh | Keeper | pet-business/boarding | ✅ |
| 9 | keeper_attendance_wsh | KeeperAttendance | pet-business/boarding | ✅ |
| 10 | keeper_leave_wsh | KeeperLeave | pet-business/boarding | ⚠️ 缺列见 §4 |
| 11 | service_category_wsh | ServiceCategory | pet-business/boarding | ✅（sort_order_wsh→sort_wsh 有 @TableField 映射） |
| 12 | pet_service_wsh | ServiceItem | pet-business/boarding | ✅ |
| 13 | pet_service_media_wsh | ServiceMedia | pet-business/boarding | ✅ |
| 14 | business_hours_wsh | BusinessHours | pet-business/boarding | ✅ |
| 15 | address_wsh | Address | pet-business/boarding | ✅ |
| 16 | pet_order_wsh | PetOrder | pet-business/order | ✅（10 个展示字段 exist=false） |
| 17 | order_snapshot_wsh | OrderSnapshot | pet-business/order | ✅ |
| 18 | payment_wsh | Payment | pet-business/order | ✅ |
| 19 | refund_wsh | Refund | pet-business/order | ✅ |
| 20 | tip_wsh | Tip | pet-business/order | ✅ |
| 21 | chat_message_wsh | ChatMessage | pet-business/customer | ✅ |
| 22 | complaint_wsh | Complaint | pet-business/customer | ✅（owner_name_wsh exist=false） |
| 23 | complaint_message_wsh | ComplaintMessage | pet-business/customer | ✅ |
| 24 | rating_wsh | Rating | pet-business/customer | ✅ |
| 25 | ticket_wsh | Ticket | pet-business/customer | ✅ |
| 26 | ticket_message_wsh | TicketMessage | pet-business/customer | ⚠️ 缺列见 §4 |
| 27 | merchant_customer_service_wsh | MerchantCustomerService | pet-business/customer | ✅ |
| 28 | wallet_wsh | Wallet | pet-business/finance | ✅（version_wsh 已存在） |
| 29 | wallet_transaction_wsh | Transaction | pet-business/finance | ⚠️ 缺列/缺索引见 §4/§5 |
| 30 | withdrawal_wsh | Withdrawal | pet-business/finance | ✅ |
| 31 | notice_wsh | Notice | pet-business/operation | ⚠️ 缺列见 §4 |
| 32 | notice_read_wsh | NoticeRead | pet-business/operation | ✅ |
| 33 | notification_wsh | Notification | pet-business/operation | ✅ |
| 34 | favorite_wsh | Favorite | pet-business/operation | ✅ |
| 35 | content_review_wsh | ContentReview | pet-business/operation | ✅ |
| 36 | operation_log_wsh | OperationLog | pet-business/operation | ✅ |
| 37 | file_record_wsh | FileRecord | pet-business/operation | ⚠️ 缺列见 §4 |
| 38 | coupon_template_wsh | CouponTemplate | pet-business/marketing | ✅ |
| 39 | user_coupon_wsh | UserCoupon | pet-business/marketing | ✅ |
| 40 | coupon_usage_wsh | CouponUsage | pet-business/marketing | ✅ |
| 41 | member_plan_wsh | MemberPlan | pet-business/membership | ✅ |
| 42 | user_membership_wsh | UserMembership | pet-business/membership | ✅ |
| 43 | membership_order_wsh | MembershipOrder | pet-business/membership | ✅ |
| 44 | membership_benefit_usage_wsh | MembershipBenefitUsage | pet-business/membership | ✅ |
| 45 | membership_event_wsh | MembershipEvent | pet-business/membership | ✅ |
| 46 | qualification_wsh | Qualification | pet-business/qualification | ✅ |
| 47 | ai_report_wsh | AiReport | pet-ai | ✅ |
| 48 | knowledge_document_wsh | KnowledgeDocument (pet-ai 与 pet-business/com.pet.ai 各一份，同表) | pet-ai / pet-business | ✅ |
| 49 | document_embedding_wsh | (pet-ai 模块 Mapper) | pet-ai | ✅ |
| 50 | ai_chat_history_wsh | AiChatHistory | pet-business/ai | ✅ |
| 51 | ai_config_wsh | AiConfigWsh | pet-business/ai | ✅ |

> 说明：`recycle_bin` 无独立表 —— RecycleBin 使用动态 SQL 直接对 `SOFT_DELETABLE_TABLES` 中的现表做删除/恢复/查询，不依赖 `recycle_bin_wsh`，无需建表。

### 3.2 代码存在但数据库不存在的表

**无。** 46 个活跃 Entity 的表全部存在。

### 3.3 已从代码移除、无需恢复的表

| 表 | 结论 |
|---|---|
| adoption_pet_wsh / adoption_application_wsh | 前端与后端代码已无任何 adoption 引用，模块整体移除，**不需要重建** |

---

## 4. 数据库表存在但字段缺失（Entity 真实映射列 ↔ 数据库列）

以下字段在 Entity 中使用 `@TableField(value=...)`（或默认映射）指向真实列，**数据库缺少该列**，会导致 MyBatis-Plus 生成 SQL 包含不存在的列 → 运行时报 `Unknown column`。

| 表 | 缺失字段 | Entity 位置 | 是否必须 | 影响 |
|---|---|---|---|---|
| user_wsh | `reject_reason_wsh` | pet-system/.../User.java:64 | **必须** | 实名认证驳回原因，插入/查询报错 |
| notice_wsh | `delivery_type_wsh` | pet-business/.../Notice.java:41 | **必须** | 公告投递方式(popup/notification/broadcast) |
| keeper_leave_wsh | `status_wsh` | pet-business/.../KeeperLeave.java:51 | **必须** | 请假审批状态 pending/approved/rejected |
| ticket_message_wsh | `file_url_wsh` | pet-business/.../TicketMessage.java:35 | **必须** | 工单消息图片附件 |
| ticket_message_wsh | `is_read_wsh` | pet-business/.../TicketMessage.java:39 | **必须** | 已读标记（Mapper 已有 `UPDATE ... SET is_read_wsh=1`） |
| wallet_transaction_wsh | `operator_id_wsh` | pet-business/.../Transaction.java:82 | **必须** | 管理员调账操作人追踪 |
| file_record_wsh | `purpose_wsh` | pet-business/.../FileRecord.java:42 | **必须** | 文件用途（product/avatar/evidence） |
| file_record_wsh | `merchant_id_wsh` | pet-business/.../FileRecord.java:46 | **必须** | 受管文件归属商家 |

### 4.1 非持久化字段（exist=false，无需建列，审查确认无问题）

| Entity | 字段 | 用途 |
|---|---|---|
| Pet | owner_name_wsh | 展示冗余 |
| PetOrder | service_name_wsh / owner_name_wsh / pet_name_wsh / keeper_name_wsh / keeper_phone_wsh / keeper_avatar_wsh / merchant_name_wsh / merchant_phone_wsh / merchant_address_wsh / service_description_wsh | 订单列表/详情联表投影 |
| Merchant | distance_wsh | 附近商家距离 |
| Complaint | owner_name_wsh | 展示冗余 |
| FileRecord | url_wsh | 计算 URL，仅 getter |

---

## 5. 索引 / 约束缺失

| 表 | 缺失索引 | 依据 | 影响 |
|---|---|---|---|
| wallet_transaction_wsh | `uk_wallet_tx_request` (唯一, request_id_wsh) | schema.sql ALTER 段 | 幂等请求防重依赖该唯一约束 |
| wallet_transaction_wsh | `idx_wallet_tx_user` (user_id_wsh) | schema.sql ALTER 段 | 用户流水查询性能 |
| wallet_transaction_wsh | `idx_wallet_tx_business` (business_type_wsh, business_id_wsh) | schema.sql ALTER 段 | 业务关联查询性能 |

> 其余唯一键/索引（username 唯一、role code 唯一、uk_user_role、uk_user_target、uk_merchant_day、uk_wallet_user、order_no 唯一等）均已在库中存在 ✅

---

## 6. 字段类型审查

| 字段 | 数据库类型 | 代码类型 | 结论 |
|---|---|---|---|
| 全部金额字段 (price/amount/balance/fee) | DECIMAL(10,2)/(12,2) | BigDecimal | ✅ 规范 |
| 经纬度 (merchant/address/user) | DECIMAL(10,6)/(10,7) | BigDecimal | ✅ 规范 |
| gender/status 等枚举 | TINYINT | Integer | ✅ |
| deferred 逻辑删除 | TINYINT | Integer + @TableLogic | ✅ |
| order_snapshot 各快照 | TEXT | String(JSON) | ✅ |
| rating score | TINYINT | Integer | ✅ |

未发现类型错误的字段。

---

## 7. 命名规范一致性（_wsh）

- MyBatis 配置：`map-underscore-to-camel-case: false` → 列名即实体字段名（`_wsh` 后缀）。
- 后端 Entity/DTO/VO/请求体/响应体：统一 `_wsh` 后缀（如 `access_token_wsh`、`pickup_address_wsh`）。
- 前端 request.js / stores / 页面：统一 `_wsh` 后缀（`access_token_wsh`、`refresh_token_wsh`、`roles_wsh`、`name_wsh` 等，经 CreateOrderDialog.vue L1121-L1141、Addresses.vue L134-L170 验证）。
- `service_category_wsh.sort_wsh` ↔ Entity `sort_order_wsh`：通过 `@TableField(value="sort_wsh")` 显式映射，✅ 无冲突。
- **无重复业务字段**（未同时存在 `category_id` 与 `category_id_wsh`）。

---

## 8. Mapper SQL 一致性

- 无 Mapper XML 文件（全部注解 SQL / MyBatis-Plus Wrapper）。
- 检查所有 `@Select/@Insert/@Update/@Delete`：
  - ✅ `WalletMapper` 增减余额 SQL 字段均存在
  - ✅ `KeeperMapper/MerchantMapper/PetMapper/OrderMapper` 联表投影列均存在
  - ⚠️ `TicketMessageMapper` 使用 `is_read_wsh`（缺失列，列入 §4 修复）
  - ✅ `RecycleBinMapper` 动态表名白名单内字段结构一致
  - ✅ `FavoriteMapper`、`UserRoleMapper`、`NoticeMapper`、`ChatMessageMapper`、`ComplaintMessageMapper` 引用的列均存在

---

## 9. 数据缺失清单（测试数据需全部重建）

| 数据域 | 现状 | 目标 |
|---|---|---|
| 角色 role_wsh | 空（seed.sql 有 INSERT IGNORE 但未执行；schema.sql 有 5 角色定义含 CUSTOMER_SERVICE） | 5 角色：ADMIN/OWNER/KEEPER/MERCHANT/CUSTOMER_SERVICE |
| 用户 user_wsh | 空 | 20+（多角色） |
| 钱包 wallet_wsh | 空 | 每人一个（管理员调账可验证） |
| 分类 category_wsh | 空 | 两级分类（狗/猫/鸟/爬宠…） |
| 服务分类 service_category_wsh | 空 | 5 个一级 + 14 个二级（seed.sql 已定义） |
| 商家 merchant_wsh | 空 | 5+（approved 状态） |
| 看护人 keeper_wsh | 空 | 10+ |
| 宠物 pet_wsh | 空 | 20+ |
| 服务 pet_service_wsh | 空 | 30+ |
| 营业时间 business_hours_wsh | 空 | 每个商家 7 天 |
| 订单 pet_order_wsh | 空 | 30+，覆盖 pending/paid/confirmed/delivered/in_progress/completed/cancelled/refunding/refunded |
| 支付 payment_wsh | 空 | 关联订单 |
| 退款 refund_wsh | 空 | 覆盖 pending/approved/rejected/completed |
| 评价 rating_wsh | 空 | 20+（必须关联订单） |
| 收藏 favorite_wsh | 空 | 关联真实用户+服务/商家 |
| 通知 notification_wsh | 空 | 20+（order/payment/ticket/system 类型） |
| 公告 notice_wsh / notice_read_wsh | 空 | 公告+Banner |
| 投诉 complaint_wsh / complaint_message_wsh | 空 | 覆盖 pending/processing/resolved/rejected |
| 工单 ticket_wsh / ticket_message_wsh | 空 | 覆盖多状态 |
| 提现 withdrawal_wsh | 空 | 覆盖 pending/approved/completed |
| 打赏 tip_wsh | 空 | 关联完成的订单 |
| 会员/优惠券（member_plan/user_membership/coupon_template/user_coupon 等） | 空 | 套餐/券模板/用户券 |
| 照护记录 care_record_wsh | 空 | 关联进行中/完成订单 |
| 订单快照 order_snapshot_wsh | 空 | 为已支付订单生成 |
| 资质 qualification_wsh | 空 | 商家/看护人资质 |
| AI 配置 ai_config_wsh | 空 | agent/cs 两条启用配置 |
| 对话历史 ai_chat_history_wsh | 空 | 可选少量 |

> 注：seed.sql 内嵌的演示数据密码哈希为 `password`（BCrypt），不符合项目"测试密码统一 123456"约定，且仅覆盖 4 角色 2 用户，不满足规模要求；将按全部业务表重建合法测试数据（见后续 DATABASE_REPAIR / TEST_DATA DOCUMENT）。

---

## 10. 启动错误清单（当前后端无法启动）

### Bug #1（启动阻断）：JWT 密钥为空导致 WeakKeyException

- **错误信息**：`io.jsonwebtoken.security.WeakKeyException: The specified key byte array is 0 bits...`，`Caused by: com.pet.security.JwtUtil.<init>(JwtUtil.java:45)`
- **发生位置**：`pet-security/.../JwtUtil.java:44-45`（`Base64.getDecoder().decode(secret)` → `Keys.hmacShaKeyFor`）
- **调用链**：Spring 启动 → JwtAuthenticationFilter 构造 → JwtUtil 构造（`@Value("${jwt.secret}")`）
- **根因**：`application.yml` 中 `jwt.secret: ${JWT_SECRET:}`；`.env` 文件位于项目根目录，通过 `spring.config.import: optional:file:.env[.properties]` 加载。**从 pet-admin 目录直接启动（或 IDE 运行）时 cwd 不是项目根目录 → .env 找不到 → JWT_SECRET 为空 → 密钥 0 bit**。
- **修复方案**：从项目根目录启动（与 start-backend.ps1 一致）；不修改代码。
- **验证**：启动后 `jwt.secret` 使用 .env 中 64 字符 Base64 密钥（≥256 bit）。

### 未到达的阶段（启动被阻后）

- MyBatis 运行期 SQL 错误（含 §4 缺失列）需在启动成功后 API 测试阶段收集。

---

## 11. 前后端契约结论

| 项 | 结论 |
|---|---|
| API 前缀 | 前后端统一 `/api` ✅ |
| 字段命名 | 前后端统一 `_wsh` ✅ |
| 登录响应字段 | `access_token_wsh / refresh_token_wsh / user_id_wsh / username_wsh / nickname_wsh / avatar_wsh / roles_wsh` ✅ |
| 注册必填 | username_wsh/password_wsh/phone_wsh/captcha_wsh/nickname_wsh/email_wsh（验证码存 Redis `register_captcha:{phone}`）→ **测试账号不能走注册接口，必须直插 SQL** |
| 角色常量 | 前端 `ROLES = {OWNER, KEEPER, MERCHANT, ADMIN, CUSTOMER_SERVICE}`，与 role_wsh 5 角色一致 ✅ |

---

## 12. 修复优先级汇总（供 DATABASE_REPAIR 与后续阶段）

| 优先级 | 内容 |
|---|---|
| P0 | 修复 8 个缺失列（§4） |
| P0 | 补齐 wallet_transaction 3 个索引（§5） |
| P0 | 恢复完整合法测试数据（§9 清单） |
| P1 | 确保从项目根目录启动后端（§10 Bug#1） |
| P1 | 启动后 API 全量回归（阶段 7） |
| P2 | Redis 缓存结构核查（service-item-page 等 Key 的序列化兼容） |