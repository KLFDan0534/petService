# pet-service 代码业务注释规范文档

## 一、注释规则

### 1.1 JavaDoc 注释格式

所有 `public` 方法必须遵循以下 JavaDoc 格式：

```java
/**
 * 【业务名称】
 *
 * 业务作用：
 * 描述该方法解决什么业务问题。
 *
 * 调用场景：
 * 描述什么时候会调用该方法。
 *
 * 调用链：
 * Controller/调用方
 * ↓
 * 当前方法
 * ↓
 * 下游调用
 *
 * 数据处理：
 * 描述输入数据如何处理。
 *
 * 业务规则：
 * 描述关键判断逻辑。
 *
 * 状态影响：
 * 描述是否修改业务状态。
 *
 * 异常情况：
 * 描述可能出现的业务异常。
 *
 * 注意事项：
 * 描述维护时需要注意的问题。
 */
```

### 1.2 各层级注释重点

| 层级 | 重点说明内容 |
|------|-------------|
| **Controller** | API用途、请求来源、权限要求、返回数据结构 |
| **Service** | 核心业务逻辑、事务范围、调用链、状态变化 |
| **Mapper** | SQL用途、查询条件说明、为什么查询这些数据 |
| **Entity** | 表映射说明、字段业务含义、状态枚举值 |
| **DTO/VO** | 传输的数据结构说明、字段业务含义 |
| **MQ Producer** | 什么事件产生消息、消息目的、消费方 |
| **MQ Consumer** | 消费什么消息、为什么消费、后续影响 |
| **Utils** | 使用场景、输入输出、为什么存在 |

### 1.3 禁止事项

1. 不修改任何业务代码
2. 不改变方法签名
3. 不改变变量名称
4. 不调整代码结构
5. 不删除已有注释（可优化不准确注释）
6. 不添加无意义注释（如 `// 查询用户`）

### 1.4 特殊标记

- `【需要人工确认】` — 方法业务意义无法确认时标记，说明原因
- `【待优化】` — 代码存在潜在问题但暂不修改

---

## 二、方法调用链说明

### 2.1 用户认证调用链

```
AuthController.login()
    ↓
UserServiceImpl.login()
    ↓
UserMapper.selectOne() → DB: user_wsh
    ↓
JwtUtil.generateAccessToken() + generateRefreshToken()
    ↓
Redis: 存储token
    ↓
返回 LoginResponseVO
```

### 2.2 订单创建调用链

```
OrderController.create()
    ↓
OrderServiceImpl.createOrder()
    ↓
├── PetMapper.selectById() → 校验宠物归属
├── KeeperMapper.selectById() → 校验看护员
├── MerchantMapper.selectById() → 校验商家
├── ServiceItemMapper.selectById() → 获取服务价格
├── CouponService.calcDiscount() → 计算优惠
├── MembershipService → 会员折扣
├── OrderSnapshotServiceImpl.createSnapshot() → 快照
├── PetOrderMapper.insert() → 保存订单
└── MessageSender.sendOrderPaymentTimeout() → MQ超时计时
```

### 2.3 订单状态机

```
pending (待支付)
    │ 用户支付成功
    ▼
paid (已支付)
    │ 商家/看护员接单
    ▼
confirmed (已确认)
    │ 用户送达宠物
    ▼
delivered (已送达)
    │ 商家/看护员接收宠物
    ▼
received (已接收)
    │ 开始服务
    ▼
in_progress (服务中)
    │ 服务完成
    ▼
completed (已完成)

取消路径:
pending → cancelled (支付超时/用户取消)
paid → cancelled (商家拒单)

退款路径:
paid/confirmed/delivered/received/in_progress → refunding → refunded
```

### 2.4 支付调用链

```
PaymentController.createPayment()
    ↓
PaymentServiceImpl.createPayment()
    ↓
├── 校验订单状态必须为 PENDING
├── 生成唯一 pay_no
├── PaymentMapper.insert() → 创建支付记录
└── 返回 PaymentDTO

PaymentController.pay()
    ↓
PaymentServiceImpl.pay()
    ↓
├── PaymentMapper.selectByPayNo() → 查询支付记录
├── 校验支付记录状态
├── AccountingService.transfer() → 钱包扣款
├── CouponService.markUsed() → 标记优惠券已用
├── MembershipService.markMemberUsed() → 会员权益
├── PetOrderMapper.update() → 订单状态 PENDING→PAID
├── OrderStatusBroadcaster.broadcast() → SSE推送
└── MessageSender.sendOrderAcceptTimeout() → MQ接单超时
```

### 2.5 退款调用链

```
RefundController.create()
    ↓
RefundServiceImpl.createRefund()
    ↓
├── 校验订单状态可退款 (PAID/CONFIRMED/DELIVERED/RECEIVED/IN_PROGRESS)
├── 计算退款金额
├── RefundMapper.insert() → 创建退款记录
└── PetOrderMapper.update() → 订单状态→REFUNDING

RefundController.approve()
    ↓
RefundServiceImpl.approve()
    ↓
├── 校验退款状态为 PENDING
├── 状态变为 APPROVED

RefundController.complete()
    ↓
RefundServiceImpl.complete()
    ↓
├── 校验退款状态为 APPROVED
├── AccountingService.transfer() → 钱包退款
├── 退还平台补贴
├── PetOrderMapper.update() → 订单状态→REFUNDED
└── RefundMapper.update() → 退款状态→COMPLETED
```

### 2.6 AI报告生成调用链

```
订单完成触发:
OrderServiceImpl.completeOrder()
    ↓
applicationEventPublisher.publishEvent(new OrderCompletedEvent(...))
    ↓
OrderCompletedReportListener.handleOrderCompleted() (事务后监听)
    ↓
AiReportServiceImpl.generateBoardingReportInternal()
    ↓
├── DeepSeek API 调用 → AI生成报告
├── AiReportMapper.insert() → 保存报告
├── PetOrder.final_report_generated_wsh = 1
└── OrderMapper.updateById() → 标记已生成

手动触发:
AiReportController.generateCareSuggestion()
    ↓
AiReportServiceImpl.generateCareSuggestion()
    ↓
├── 收集宠物/订单/看护信息
├── DeepSeek API 调用
└── AiReportMapper.insert()
```

### 2.7 MQ消息流转

```
Producer                          Queue                          Consumer
─────────                         ─────                          ────────
OrderServiceImpl.createOrder() → order.payment.timeout.delay → OrderPaymentTimeoutListener
                                    (TTL 15分钟，死信转发)
                                                                → cancelPendingOrderIfPaymentTimeout()

OrderServiceImpl.acceptOrder()  → order.accept.timeout.delay  → OrderAcceptTimeoutListener
                                    (TTL 30分钟，死信转发)
                                                                → autoAcceptPaidOrderIfTimeout()

OrderServiceImpl.cancelOrder()  → order.cancel                → MessageListener.handleOrderCancel()
PaymentServiceImpl.pay()        → order.create                → MessageListener.handleOrderCreate()
RefundServiceImpl.complete()    → order.refund                → MessageListener.handleOrderRefund()

ComplaintServiceImpl            → complaint.process           → MessageListener.handleComplaintProcess()
                                                                → ComplaintProcessHandlerImpl.handle()

OrderServiceImpl.completeOrder() → (事件监听，非MQ)             → OrderCompletedReportListener
```

### 2.8 SSE实时推送

```
OrderStatusBroadcaster
    ├── subscribe(token, userId) → 注册 SseEmitter
    ├── broadcast(order) → 向订单参与者推送状态更新
    └── 使用 ConcurrentHashMap + CopyOnWriteArrayList 管理连接

ChatEventBroadcaster
    ├── subscribe(token, userId) → 注册 SseEmitter
    ├── broadcast(message, orderId) → 推送聊天消息
    └── 用户退出时自动移除

NotificationBroadcaster
    ├── subscribe(token, userId) → 注册 SseEmitter
    ├── broadcast(notification) → 推送通知
    └── 一对多广播
```

---

## 三、核心业务流程

### 3.1 用户注册流程

1. 前端调用 `POST /api/auth/register/captcha` 获取验证码
2. 验证码存 Redis（5分钟有效）
3. 前端调用 `POST /api/auth/register` 提交注册
4. 校验验证码、手机号唯一性、密码加密
5. 创建用户，默认赋予 `OWNER` 角色
6. 返回 JWT access_token 和 refresh_token

### 3.2 用户登录流程

1. 前端调用 `POST /api/auth/login`
2. JwtAuthenticationFilter 无认证过滤器（白名单）
3. 校验用户名密码，检查账号状态（是否被封禁）
4. 限制登录尝试（20次/15分钟锁定），本地 ConcurrentHashMap 实现
5. 生成 JWT access_token + refresh_token
6. 返回 LoginResponseVO

### 3.3 寄养订单创建流程

1. 用户选择宠物、商家、看护员、服务项目、服务时间
2. 前端调用 `POST /api/orders` (OrderController.create())
3. 校验：宠物归属、商家营业状态、看护员可用性、时间有效性、库存容量
4. 计算：基础价格 × 天数 - 长期折扣 - 优惠券 - 会员折扣
5. 创建：订单记录 + 快照（JSON序列化所有相关数据）
6. 发送 MQ 支付超时延时消息（15分钟 TTL）
7. 订单状态：pending

### 3.4 支付流程

1. 用户调用 `POST /api/payments` 创建支付
2. 前端调用 `POST /api/payments/pay` 执行支付（余额支付）
3. 校验支付密码 → 扣款钱包余额 → 平台补贴处理
4. 更新订单状态 PENDING → PAID
5. 标记优惠券已使用 + 会员权益已使用
6. SSE 推送订单状态更新给订单参与者
7. 发送 MQ 接单超时延时消息（30分钟 TTL）

### 3.5 接单/拒单流程

- **商家接单**: `acceptOrder()` → PAID → CONFIRMED（需验证看护员所属商家）
- **商家拒单**: `rejectOrder()` → PAID → CANCELLED（触发退款、释放优惠券）
- **超时自动接单**: `autoAcceptPaidOrderIfTimeout()` → 30分钟未处理自动确认

### 3.6 服务执行流程

1. **用户送达**: `markDelivered()` → CONFIRMED → DELIVERED（记录送达地址/坐标）
2. **商家接收**: `markReceived()` → DELIVERED → RECEIVED（校验交接码 + 地理距离）
3. **开始服务**: `startService()` → RECEIVED → IN_PROGRESS（上传开始照片）
4. **完成服务**: `completeOrder()` → IN_PROGRESS → COMPLETED（结算商家、发布事件）

### 3.7 AI报告生成流程

- **自动生成**: 订单完成时，`OrderCompletedEvent` 被 `OrderCompletedReportListener` 监听，事务提交后调用 DeepSeek API 生成寄养总结报告
- **手动生成**: 管理员/看护员可通过 `POST /api/ai/reports` 手动创建报告
- **护理建议**: 用户可通过 `POST /api/ai/care-suggestion` 获取 AI 护理建议
- **宠物寄养报告**: 用户可通过 `POST /api/ai/boarding-report` 获取寄养总结

### 3.8 钱包财务流转

- **收入**: 支付时，用户钱包扣款，商家钱包入账（含平台补贴）
- **退款**: 完成退款时，商家钱包扣回，用户钱包入账（平台补贴退还）
- **打赏**: 订单完成后，用户可打赏看护员，直接从钱包转账
- **提现**: 商家/看护员可申请提现，管理员审核后可完成提现
- **事务一致性**: 所有资金操作均使用 `@Transactional` 保证原子性

### 3.9 优惠券流转

- **发放**: 管理员创建优惠券模板 → 条件发放或手动发放给用户
- **使用**: 下单时计算优惠 → 支付成功时标记已使用
- **取消**: 订单取消/拒单时释放优惠券
- **限制**: 有效期、最低消费、指定品类、使用次数等

### 3.10 会员体系

- **会员计划**: 管理员定义不同等级（月卡/季卡/年卡）
- **购买**: 用户购买会员计划 → 生成会员订单 → 支付 → 激活会员
- **权益**: 下单折扣、优先接单、专属客服等
- **过期**: Scheduled 任务每天检查并标记过期会员
- **退款**: 会员退款需计算已使用天数按比例退款

---

## 四、安全与权限模型

### 4.1 角色体系

| 角色编码 | 说明 | 主要权限 |
|---------|------|---------|
| `ADMIN` | 系统管理员 | 全部权限 |
| `OWNER` | 宠物主（默认） | 管理宠物、下单、评价 |
| `MERCHANT` | 商家 | 管理店铺、看护员、接单 |
| `KEEPER` | 看护员 | 服务、打卡、请假 |
| `CUSTOMER_SERVICE` | 客服 | 处理投诉 |

### 4.2 权限控制

- **方法级别**: `@PreAuthorize("hasRole('ADMIN')")` 注解
- **接口级别**: SecurityConfig 配置 URL 白名单
- **资源所有权**: `OwnershipValidator` 校验资源归属
- **实名认证**: `UserProfileRequirementService` 校验实名状态

### 4.3 JWT认证流程

```
请求头 Authorization: Bearer <token>
    ↓
JwtAuthenticationFilter.doFilterInternal()
    ↓
├── Redis 黑名单校验 → 已加入黑名单则返回 401
├── JwtUtil.validateToken() → 校验签名和有效期
├── 解析 userId, roles, username
├── 构建 JwtAuthenticationToken
└── 设置 SecurityContextHolder

Spring Security 后续拦截 → @PreAuthorize 注解校验
```

---

## 五、已完成注释文件列表

| 模块 | 文件列表 | 状态 |
|------|---------|------|
| pet-common | BusinessException, GlobalExceptionHandler, Result, ... | ✅ |
| pet-security | JwtUtil, JwtAuthFilter, SecurityConfig, SseTokenService | ✅ |
| pet-framework | MessageSender, MessageListener, RabbitMQConfig, ... | ✅ |
| pet-system | UserServiceImpl, UserController, AuthController, RoleController, ... | ✅ |
| pet-business/order | OrderServiceImpl, PaymentServiceImpl, RefundServiceImpl, ... | ✅ |
| pet-business/boarding | MerchantServiceImpl, KeeperServiceImpl, ... | ✅ |
| pet-business/pet | PetServiceImpl, CareRecordServiceImpl, ... | ✅ |
| pet-business/finance | WalletServiceImpl, AccountingServiceImpl, ... | ✅ |
| pet-business/customer | ComplaintServiceImpl, TicketServiceImpl, ... | ✅ |
| pet-business/marketing | CouponServiceImpl | ✅ |
| pet-business/membership | MembershipServiceImpl, MembershipOrderServiceImpl, ... | ✅ |
| pet-business/operation | FileRecordServiceImpl, NotificationServiceImpl, ... | ✅ |
| pet-ai | AiReportServiceImpl, AgentServiceImpl, RagServiceImpl, ... | ✅ |
| pet-admin | StatisticsServiceImpl, LogOperationAspect | ✅ |

---

## 六、未理解方法列表

> 以下方法因业务上下文不足或代码逻辑复杂无法完全确认业务意义，标记为【需要人工确认】

| 文件 | 方法 | 原因 |
|------|------|------|
| `OrderServiceImpl.java` | `toDTO()`, `toDTOEnrichedList()` | 属于纯数据转换，业务含义体现在调用方 |
| `OrderServiceImpl.java` | `refreshKeeperCurrentPets()` | 看护员当前宠物数刷新机制，需确认是否定时触发 |
| `CouponServiceImpl.java` | `grantToUsersByCondition()` | 条件发放策略，需确认具体条件规则 |
| `AgentServiceImpl.java` | `execute()` | AI Agent 完整执行链，需确认各步骤边界和容错策略 |
| `RagServiceImpl.java` | `answer()` | 需要确认 RAG 检索策略和 prompt 模板细节 |
| `ChromaService.java` | `query()`, `delete()` | 向量数据库操作，需确认检索参数调优 |
| `MessageListener.java` | `handleOrderCreate()`, `handleOrderCancel()`, `handleOrderRefund()` | 当前仅打印日志，需确认是否有实际的后续处理逻辑 |

---

## 七、模块调用关系图

```
pet-common (基础工具)
    ↑
pet-framework (基础设施: MQ, MinIO, MyBatis-Plus, SpringDoc)
    ↑
pet-security (JWT 认证鉴权)
    ↑
pet-system (用户/角色/认证)
    ↑
pet-business (核心业务)
    ├── boarding (商家/看护员/地址/营业时间/考勤)
    ├── pet (宠物/分类/护理记录)
    ├── order (订单/支付/退款/打赏)
    ├── finance (钱包/交易/提现)
    ├── customer (投诉/评价/工单/聊天)
    ├── marketing (优惠券)
    ├── membership (会员计划)
    ├── operation (文件/通知/公告/收藏/内容审核/回收站)
    ├── fulfillment (服务执行概览)
    ├── qualification (资质认证)
    └── geo (地理/高德地图)
    ↑
pet-ai (AI 报告/Agent/RAG)
    ↑
pet-admin (启动类/统计/AOP日志)
```

---

## 八、数据库表关系概览

### 8.1 核心业务表

```
user_wsh (用户)
    │
    ├── user_role_wsh → role_wsh (角色多对多)
    │
    ├── pet_wsh (宠物)
    │
    ├── merchant_wsh (商家) -- user_id_wsh 所有者
    │
    ├── keeper_wsh (看护员) -- user_id_wsh + merchant_id_wsh
    │
    ├── pet_order_wsh (订单) -- user_id_wsh + merchant_id_wsh + keeper_id_wsh + pet_id_wsh
    │       │
    │       ├── payment_wsh (支付记录)
    │       ├── refund_wsh (退款记录)
    │       ├── tip_wsh (打赏)
    │       ├── order_snapshot_wsh (快照)
    │       ├── ai_report_wsh (AI报告)
    │       └── care_record_wsh (护理记录)
    │
    ├── wallet_wsh (钱包)
    │       └── wallet_transaction_wsh (交易流水)
    │
    └── user_coupon_wsh → coupon_template_wsh (优惠券)
```

### 8.2 辅助业务表

```
merchant_wsh
    ├── business_hours_wsh (营业时间)
    ├── service_category_wsh (服务分类)
    │       └── service_item_wsh (服务项目)
    ├── keeper_attendance_wsh (考勤)
    └── keeper_leave_wsh (请假)

address_wsh (地址) -- user_id_wsh

rating_wsh (评价) -- target_type + target_id 多态
complaint_wsh (投诉) -- target_type + target_id 多态
ticket_wsh (工单) -- order_id_wsh
chat_message_wsh (聊天) -- order_id_wsh

notice_wsh (公告)
notification_wsh (通知)
file_record_wsh (文件)
favorite_wsh (收藏) -- target_type + target_id 多态
content_review_wsh (内容审核)
operation_log_wsh (操作日志)

member_plan_wsh (会员计划)
membership_order_wsh (会员订单)
user_membership_wsh (用户会员)
membership_benefit_usage_wsh (会员权益使用)
```

---

## 九、数据库列名约定

所有表统一使用 `_wsh` 后缀命名（如 `username_wsh`, `status_wsh`），JSON 序列化时通过 `@JsonGetter` 方法去掉后缀返回给前端。

### 9.1 MyBatis-Plus 配置

- 自动填充: `created_at_wsh` + `updated_at_wsh`
- 逻辑删除: `deleted_wsh` 字段
- 分页: `PaginationInnerInterceptor`

---

*文档版本: v1.0*
*生成日期: 2026-07-25*
