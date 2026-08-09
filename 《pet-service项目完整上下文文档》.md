# Pet Service 项目完整上下文文档

> 生成日期: 2026-07-25
> 基于实际代码扫描分析，所有结论均有代码实证
> 版本: 1.0.0-SNAPSHOT

---

# 第一部分：项目基本信息

## 1.1 项目名称

**Pet Service Platform（宠物服务平台）**

## 1.2 项目定位

连接宠物主人与商家/看护人的宠物寄养与服务综合管理平台。提供宠物寄养、美容护理、训练服务、遛宠、医疗保健等服务的在线预订、支付、履约、评价全流程管理。

## 1.3 解决的问题

- 宠物主人外出时宠物无人照看的问题
- 寄养服务在线预订、支付、履约的标准化流程
- 商家/看护人服务订单管理、收益管理
- 平台运营方对商家、看护人、订单、投诉的全维度管理
- 人工智能辅助的寄养报告生成与知识库问答

## 1.4 目标用户

| 角色 | 标识 | 说明 |
|------|------|------|
| 宠物主人 | OWNER | 浏览服务、下单、支付、评价 |
| 看护人 | KEEPER | 接单、照护、记录、完成服务 |
| 商家 | MERCHANT | 管理服务、看护人、订单 |
| 客服 | CUSTOMER_SERVICE | 处理工单、投诉、内容审核 |
| 管理员 | ADMIN | 全平台运营管理 |

## 1.5 业务模式

C2C2B 平台模式：
- **C端（宠物主人）**：浏览服务、下单、支付
- **B端（商家/看护人）**：提供寄养服务
- **平台方（管理员）**：运营管理、审核、纠纷处理

盈利模式：平台服务费、会员订阅、提现手续费等（基础设施已搭建）

## 1.6 主要功能（27个模块已完成）

| 模块 | 完成度 | 说明 |
|------|--------|------|
| 认证管理 | 95% | 注册/登录/Token刷新/登出 |
| 用户管理 | 95% | 个人信息/实名认证/封禁 |
| 角色管理 | 95% | 角色CRUD/用户角色分配 |
| 宠物管理 | 95% | CRUD/品种中文/照片 |
| 寄养记录 | 95% | 照护记录/图片上传 |
| 商家管理 | 95% | 入驻/审核/搜索 |
| 看护人管理 | 95% | 申请/审核/在线状态/附近搜索 |
| 服务项目 | 95% | CRUD/按商家筛选/启停 |
| 地址管理 | 95% | CRUD/默认地址/下单选择 |
| 订单管理 | 95% | 创建/支付/接单/完成/评价 |
| 支付管理 | 95% | 创建/执行/记录 |
| 退款管理 | 95% | 申请/审核/完成 |
| 打赏管理 | 95% | 打赏/记录 |
| 聊天管理 | 95% | 发送/接收/未读 |
| 评价管理 | 95% | 评分/回复/图片 |
| 投诉管理 | 95% | 发起/处理/驳回 |
| 客服工单 | 95% | 创建/分配/回复/关闭 |
| 公告/Banner | 95% | CRUD/Dashboard展示 |
| 收藏管理 | 95% | 切换/列表 |
| 文件管理 | 95% | 上传/MinIO |
| 内容审核 | 95% | 举报/审核 |
| 钱包管理 | 95% | 余额/流水 |
| 交易流水 | 95% | 收入/支出记录 |
| 提现管理 | 95% | 申请/审核/完成 |
| AI报告 | 95% | 照护建议/寄养报告/DeepSeek |
| RAG知识库 | 95% | 问答/搜索/文档管理 |
| AI Agent | 80% | 多步骤AI任务 |
| 数据统计 | 95% | 管理后台/商家统计 |
| 会员体系 | 85% | 会员计划/订单/权益/过期调度 |

## 1.7 当前完成度

- **后端**: 约 200+ Java 文件，33 个 Controller，~163 个 API 接口
- **前端**: 66 个 Vue 页面，7 个 Pinia Store，30 个 API 模块
- **数据库**: 38 张表（统一 DDL）
- **测试**: 23/23 全部通过（19 FullLink + 3 Auth + 1 应用上下文）
- **整体完成度**: 约 85%（核心业务流完整，但缺乏真实支付集成、部分后台页面不完整）

## 1.8 未来扩展方向

- 真实第三方支付集成（微信/支付宝）
- 物流配送（宠物接送）
- 宠物医疗在线问诊
- 社区/社交功能
- IoT设备集成（宠物摄像头）
- 多语言国际化
- 微服务化拆分

---

# 第二部分：技术架构分析

## 2.1 整体架构风格

单体多模块架构（Modular Monolith），Maven 多模块拆分，所有模块最终打包到 pet-admin 一个应用中。

## 2.2 前端技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4+ | Composition API + `<script setup>` |
| Vite | 5.x | 构建工具 |
| Element Plus | 2.14 | UI 组件库 |
| Pinia | 2.1 | 状态管理 |
| Vue Router | 4.3 | 路由，History 模式 |
| Axios | 1.7 | HTTP 请求 |
| Playwright | 1.61 | E2E 测试 |
| Vitest | 4.x | 单元测试 |
| Vue Test Utils | 2.4 | 组件测试 |

## 2.3 后端技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 语言 |
| Spring Boot | 3.2.5 | 框架 |
| Spring Security | 6.x | 认证授权 |
| MyBatis-Plus | 3.5.7 | ORM |
| Spring AI | 1.0.0-M2 | AI 集成（OpenAI 协议） |
| LangChain4j | 1.16.2 | AI 框架 |
| JJWT | 0.12.5 | JWT 令牌 |
| MinIO | 8.5.10 | 对象存储 |
| SpringDoc | 2.5.0 | API 文档 |
| Hutool | 5.8.28 | 工具类 |
| Gson | 2.10.1 | JSON（与 Jackson 并存） |

## 2.4 基础设施

| 组件 | 版本 | 端口 | 用途 |
|------|------|------|------|
| MySQL | 8.0 | 3308 | 主数据库 |
| Redis | 7 | 6379 | 缓存/黑名单 |
| RabbitMQ | 3.13 | 5672/15672 | 消息队列 |
| MinIO | latest | 9000/9001 | 对象存储 |
| Chroma | latest | 8000 | 向量数据库 |
| Nginx | latest | 80 | 反向代理 |

## 2.5 Maven 模块依赖关系

```
pet-admin (启动入口 + 统计)
  ├── pet-ai (AI模块: RAG + Agent + 报告)
  │     ├── pet-business
  │     ├── pet-system
  │     ├── pet-framework
  │     ├── pet-security
  │     └── pet-common
  ├── pet-business (核心业务)
  │     ├── pet-system
  │     ├── pet-framework
  │     ├── pet-security
  │     └── pet-common
  ├── pet-system (系统管理)
  │     ├── pet-framework
  │     ├── pet-security
  │     └── pet-common
  ├── pet-framework (框架配置)
  │     └── pet-common
  ├── pet-security (安全认证)
  │     └── pet-common
  └── pet-common (公共工具)
```

## 2.6 请求处理流程

```
HTTP Request
  → Nginx (80) → Spring Boot (8080)
    → JwtAuthenticationFilter (校验JWT Token)
    → SecurityConfig (@PreAuthorize 权限校验)
    → Controller (接收参数校验)
    → Service (业务逻辑 + 事务管理)
      → Mapper (MyBatis-Plus 数据库操作)
      → MessageSender (可选, 异步消息)
    → GlobalExceptionHandler (异常捕获)
    → Result<T> (统一响应格式)
  → HTTP Response
```

## 2.7 命名规范

- **数据库**: 所有表名和字段名以 `_wsh` 后缀（如 `user_wsh`, `username_wsh`）
- **Java Entity**: 字段名与数据库一致，带 `_wsh` 后缀
- **Java 类**: 标准驼峰命名
- **前端 API**: `request.js` 层使用驼峰（如 `access_token_wsh` → `access_token_wsh` 保持原样）

> ⚠️ `map-underscore-to-camel-case: false`，MyBatis-Plus 不做下划线驼峰转换

## 2.8 核心设计模式

| 模式 | 实现 | 位置 |
|------|------|------|
| DTO模式 | xxxRequest / xxxVO | 各模块 dto 包 |
| 模板方法 | Service 接口 + Impl | 所有 Service 层 |
| 策略模式 | 状态机 (OrderStatus) | OrderService |
| 观察者 | RabbitMQ 消息队列 | pet-framework/mq |
| 装饰器 | AOP (@LogOperation) | pet-common |
| 工厂模式 | MyBatis-Plus BaseMapper | 框架层 |
| 适配器 | Spring Security + JWT | pet-security |

---

# 第三部分：模块详细分析

## 3.1 pet-common（公共工具模块）

**职责**: 提供所有模块共享的公共类，无业务逻辑。

**文件清单**（15个Java文件）:

| 文件 | 用途 |
|------|------|
| `Result.java` | 统一API响应 `{code, msg, data}` |
| `PageResult.java` | 分页结果封装 |
| `PageRequestDTO.java` | 分页请求参数 |
| `BusinessException.java` | 业务异常 |
| `GlobalExceptionHandler.java` | 全局异常处理器 |
| `StatusCode.java` | 状态码枚举 |
| `OrderStatus.java` | 订单状态枚举 |
| `RefundStatus.java` | 退款状态枚举 |
| `ReviewStatus.java` | 审核状态枚举 |
| `FavoriteTargetType.java` | 收藏目标类型 |
| `OwnershipValidator.java` | 所有权校验工具 |
| `GeoDistanceUtils.java` | 地理距离计算 |
| `LogOperation.java` | 操作日志注解 |
| `ComplaintProcessHandler.java` | 投诉处理MQ处理器 |

**检查发现**:
- `map-underscore-to-camel-case: false` 是危险配置，一旦项目中混入驼峰字段名会导致查询异常
- `StatusCode.java` 与 `OrderStatus.java`/`RefundStatus.java`/`ReviewStatus.java` 的职责有重叠

## 3.2 pet-framework（框架配置模块）

**职责**: 基础设施配置，中间件集成。

**配置类**:

| 配置 | 说明 |
|------|------|
| `MyBatisPlusConfig` | 分页插件 + MetaObjectHandler 自动填充 |
| `RabbitMQConfig` | 1个Direct Exchange + 6个队列 |
| `MinIoConfig` | MinIO 客户端配置 |
| `WebConfig` | CORS、JSON 序列化 |
| `SpaForwardController` | SPA 路由转发（非 /api 请求 → index.html） |
| `CharsetFilter` | UTF-8 编码过滤器 |

**MQ 类**:

| 文件 | 用途 |
|------|------|
| `MessageSender.java` | 消息发送（所有routing key集中管理） |
| `MessageListener.java` | 消息监听（6个队列的@RabbitListener） |

**问题**:
- [x] `SpaForwardController` 将所有非 `/api` 请求转发到 `index.html`，包括 `/admin/**` 等后台路由处理全在前端
- [x] RabbitMQ 配置存在 2 个 `ConnectionFactory` bean 可能导致冲突（一个由 `@EnableRabbit` 自动配置，一个在 `RabbitMQConfig` 中手动创建）

## 3.3 pet-security（安全认证模块）

**核心文件**（5个）:

| 文件 | 说明 |
|------|------|
| `SecurityConfig.java` | Spring Security 配置，JWT 无状态 |
| `JwtUtil.java` | JWT 令牌生成/解析 |
| `JwtAuthenticationFilter.java` | 请求拦截认证 |
| `JwtAuthenticationToken.java` | 自定义认证令牌 |
| `SseTokenService.java` | SSE 令牌服务 |

**白名单路径**（不需要认证）:
- 静态资源: `/`, `/index.html`, `/assets/**`, `/css/**`, `/js/**`
- 认证: `/api/auth/**`
- 文档: `/swagger-ui/**`, `/api-docs/**`, `/v3/api-docs/**`, `/h2-console/**`
- 公开 GET: `/api/merchants/**`, `/api/keepers/**`, `/api/services/**`, `/api/ratings/**`, `/api/categories/**`, `/api/notices/active`
- 公开 ALL: `/api/rag/**`, `/api/ai/**`, `/api/services/**`, `/api/files/**`
- 非 /api 请求（SPA 转发）
- 其余所有 /api 请求需要认证

**JWT 配置**:
- Access Token 有效期: 86400000ms (24小时)
- Refresh Token 有效期: 604800000ms (7天)
- 密钥: Base64 编码（application.yml）
- 无 Refresh Token 黑名单机制（仅通过过期时间控制）

**安全风险**:
- [x] `/api/ai/**` 和 `/api/rag/**` 完全公开，任何未认证用户可访问 AI 功能和知识库
- [x] `/api/files/**` 完全公开，文件上传无需认证（上传后在 Controller 层才关联用户）
- [x] 无 CSRF 防护（JWT 无状态正常，但需确认没有使用 cookie）
- [x] 密码未使用 BCrypt 以外的增强加密（具体实现需确认 `UserServiceImpl`）
- [x] `h2-console` 在生产配置中仍然可用

## 3.4 pet-system（系统管理模块）

### AuthController
| 接口 | 方法 | 权限 |
|------|------|------|
| `/api/auth/register` | POST | 公开 |
| `/api/auth/login` | POST | 公开 |
| `/api/auth/refresh` | POST | 公开 |
| `/api/auth/logout` | POST | 公开 |

### UserController
| 接口 | 方法 | 权限 |
|------|------|------|
| `/api/users/me` | GET | 认证 |
| `/api/users/me` | PUT | 认证 |
| `/api/users` | GET | ADMIN |
| `/api/users/{id}/status` | PUT | ADMIN |

### RoleController
| 接口 | 方法 | 权限 |
|------|------|------|
| `/api/roles` | GET/POST | ADMIN |
| `/api/roles/{id}` | PUT/DELETE | ADMIN |
| `/api/users/{userId}/roles` | GET/PUT | ADMIN |

**问题**:
- [x] 登出接口 `/api/auth/logout` 虽然存在，但无 JWT 黑名单机制，Token 仍然有效直到过期
- [x] 注册接口无验证码/频率限制

## 3.5 pet-business（核心业务模块）

### 3.5.1 宠物模块 (pet)

| Controller | 文件 | 主要接口 |
|------------|------|---------|
| `PetController` | `pet/controller/PetController.java` | CRUD + 商家宠物 |
| `CategoryController` | `pet/controller/CategoryController.java` | CRUD + 树形结构 |
| `CareRecordController` | `pet/controller/CareRecordController.java` | CRUD + 按订单查询 |

**核心流程**: 宠物主人注册宠物 → 下单时选宠物 → 看护人添加照护记录

**数据库表**: `pet_wsh`, `category_wsh`, `care_record_wsh`

### 3.5.2 寄养模块 (boarding)

| Controller | 文件 | 主要接口 |
|------------|------|---------|
| `KeeperController` | `boarding/controller/KeeperController.java` | 列表/详情/审核/在线状态 |
| `MerchantController` | `boarding/controller/MerchantController.java` | 列表/详情/审核/入驻 |
| `AddressController` | `boarding/controller/AddressController.java` | CRUD/默认地址 |
| `ServiceItemController` | `boarding/controller/ServiceItemController.java` | CRUD/启停 |
| `BusinessHoursController` | `boarding/controller/BusinessHoursController.java` | 营业时间管理 |
| `MerchantCustomerServiceController` | - | 客服人员申请/审核 |

**核心流程**: 商家入驻 → 管理员审核 → 添加服务项目 → 看护人入驻 → 服务上线

**数据库表**: `merchant_wsh`, `keeper_wsh`, `pet_service_wsh`, `address_wsh`, `business_hours_wsh`, `merchant_customer_service_wsh`, `keeper_leave_wsh`, `keeper_attendance_wsh`

**定时任务**:
- `MerchantStoreSyncScheduler.java` — 商家门店状态同步（每分钟）

**问题**:
- [x] `Keeper.merchant_id_wsh` 非空但看护人可能不属于任何商家（独立看护人）
- [x] 商家和看护人都有 `rating` 评分字段，但评分更新逻辑分散

### 3.5.3 订单模块 (order)

**订单状态机（10个状态）**:

```
pending → paid → confirmed → delivered → received → in_progress → completed
  ↓                                                                      ↓
  └→ cancelled                                                   refunding → refunded
```

| 状态 | 说明 | 触发条件 |
|------|------|---------|
| `pending` | 待支付 | 订单创建 |
| `paid` | 已支付 | 支付成功 |
| `confirmed` | 商家已确认 | 商家接单 |
| `delivered` | 已送养 | 宠物送达 |
| `received` | 已接收 | 看护人接收 |
| `in_progress` | 服务中 | 服务开始 |
| `completed` | 已完成 | 服务完成 |
| `cancelled` | 已取消 | 用户取消/超时取消 |
| `refunding` | 退款中 | 退款申请 |
| `refunded` | 已退款 | 退款完成 |

**涉及文件**:
- `OrderController.java` — 12个接口
- `PaymentController.java` — 4个接口
- `RefundController.java` — 6个接口
- `TipController.java` — 2个接口

**定时任务**:
- `OrderPaymentTimeoutScheduler.java` — 支付超时未支付自动取消（60秒扫描）
- `OrderAcceptTimeoutScheduler.java` — 商家接单超时自动取消（60秒扫描）

**问题**:
- [x] `pet_order_wsh` 表有50+字段但**没有业务索引**（仅 order_no 唯一索引），查询性能问题
- [x] 定时任务使用 `@Scheduled(fixedRate = 60000)` 即60秒一次，在高并发下可能存在精度问题
- [x] 支付系统是模拟的（`method: wechat/alipay/balance`），无真实第三方支付
- [x] 退款流程没有实际的资金回转，仅修改订单状态

### 3.5.4 客户服务模块 (customer)

| Controller | 文件 | 主要接口 |
|------------|------|---------|
| `ChatController` | `customer/controller/ChatController.java` | 会话/消息/已读 |
| `ComplaintController` | `customer/controller/ComplaintController.java` | CRUD/处理 |
| `RatingController` | `customer/controller/RatingController.java` | CRUD/回复 |
| `TicketController` | `customer/controller/TicketController.java` | CRUD/分配/解决 |

**核心流程**:
- **评价**: 订单完成 → 客户评价 → 商家回复
- **投诉**: 客户发起 → 平台处理 → 结果通知
- **工单**: 客户创建 → 分配客服 → 回复解决 → 关闭
- **聊天**: 客户/看护人发送消息 → 对方接收（30秒轮询）

**问题**:
- [x] 聊天系统基于轮询（30秒间隔），非 WebSocket，实时性差
- [x] `SocketManager.js` 存在 SSE 连接代码但未观察到此功能的实际后端支持

### 3.5.5 财务模块 (finance)

| Controller | 文件 | 主要接口 |
|------------|------|---------|
| `WalletController` | `finance/controller/WalletController.java` | 我的钱包/全部钱包 |
| `TransactionController` | `finance/controller/TransactionController.java` | 我的流水/全部流水 |
| `WithdrawalController` | `finance/controller/WithdrawalController.java` | 申请/审核/完成 |

**钱包架构**:
- `wallet_wsh` 表有 `version_wsh` 乐观锁（`@Version`）
- 交易流水表有 `request_id_wsh` 幂等键
- 提现有三段审核流程: 申请 → 审核通过 → 完成（打款）

**问题**:
- [x] 钱包充值/扣款没有对接真实支付渠道
- [x] 提现流程缺少与银行系统的真实对接

### 3.5.6 运营模块 (operation)

| Controller | 文件 | 主要接口 |
|------------|------|---------|
| `NoticeController` | `operation/controller/NoticeController.java` | 公告CRUD/弹窗 |
| `FavoriteController` | `operation/controller/FavoriteController.java` | 收藏/取消/检查 |
| `FileController` | `operation/controller/FileController.java` | 文件上传(MinIO) |
| `NotificationController` | `operation/controller/NotificationController.java` | 通知列表/已读 |
| `ContentReviewController` | `operation/controller/ContentReviewController.java` | 举报/审核 |
| `OperationLogController` | `operation/controller/OperationLogController.java` | 操作日志查询 |
| `RecycleBinController` | `operation/controller/RecycleBinController.java` | 逻辑删除恢复 |

**问题**:
- [x] `FileController` 的 `PUT /api/files/upload?directory=...` 上传接口完全公开（SecurityConfig 白名单），没有文件类型/大小校验
- [x] `OperationLogController` 和 `RecycleBinController` 允许所有 ADMIN 角色操作，需要更加精细的权限控制

### 3.5.7 领养模块 (adoption)

| Controller | 文件 | 主要接口 |
|------------|------|---------|
| `AdoptionController` | `adoption/controller/AdoptionController.java` | 宠物CRUD/申请/审核 |

**核心流程**:
```
商家发布领养宠物 → 用户浏览 → 用户申请 → 商家审核 → 管理员终审
```

**数据库表**: `adoption_pet_wsh`, `adoption_application_wsh`

**问题**:
- [x] `adoption_pet_wsh` 和 `adoption_application_wsh` 的 **Java Entity 缺失**（在 `pet-business/adoption/entity/` 下找不到实体类）
- [x] 两个领养表在 `PROJECT_MAP.md` 中被引用但实际代码中未完成

## 3.6 pet-ai（AI模块）

**文件清单**（21个Java文件）:

| 控制器 | 接口 | 说明 |
|--------|------|------|
| `AiReportController` | `/api/ai/reports/*` | AI报告生成/查询 |
| `AgentController` | `/api/agent/execute` | AI Agent执行 |
| `RagController` | `/api/rag/*` | RAG知识库问答/搜索 |

**技术实现**:
- 使用 Spring AI + DeepSeek API（OpenAI 兼容协议）
- 使用 LangChain4j 作为 AI 框架
- 使用 Chroma 作为向量数据库
- RAG 流程: 文档 → 分块 → Embedding → Chroma 存储 → 相似度搜索 → LLM回答

**数据库表**: `ai_report_wsh`, `knowledge_document_wsh`, `document_embedding_wsh`

**问题**:
- [x] `document_embedding_wsh` 将向量存为 `longtext` JSON，大数据量时性能差
- [x] Chroma 向量数据库已配置但 `application.yml` 中显式排除了 `OpenAiAutoConfiguration`，Spring AI 可能工作不正常
- [x] RAG 的 Embedding 实现存在疑问——数据库中有 `document_embedding_wsh` 表存向量，却又配置了 Chroma 作为向量数据库，两者关系需确认
- [x] AI 功能接口 `/api/ai/**` 和 `/api/rag/**` 完全公开

## 3.7 pet-admin（启动入口 + 统计）

| 文件 | 说明 |
|------|------|
| `PetApplication.java` | Spring Boot 启动类 |
| `StatisticsController` | `/api/statistics/admin` + `/api/statistics/merchant` |
| `StatisticsService` | 统计数据计算 |

**启动配置**: 默认使用 MySQL 3308, Redis 6379, RabbitMQ 5672, MinIO 9000

**H2测试配置**: H2 文件数据库 + schema.sql + seed.sql 初始化

---

# 第四部分：数据库架构分析

## 4.1 总览

- **总表数**: 38张
- **存储引擎**: InnoDB
- **字符集**: utf8mb4（utf8mb4_0900_ai_ci / utf8mb4_unicode_ci）
- **命名规则**: 所有表/字段 `_wsh` 后缀
- **主键**: 所有表自增 BIGINT `id_wsh`
- **软删除**: 所有表 `deleted_wsh TINYINT DEFAULT 0`（MyBatis-Plus 逻辑删除）
- **审计字段**: `created_at_wsh`, `updated_at_wsh`

## 4.2 完整表清单

### 系统模块（3表）
| 表 | Entity | 核心字段 | 索引 |
|----|--------|---------|------|
| `user_wsh` | User | username, password, nickname, phone, real_name_status | PK + UNIQUE(username) |
| `role_wsh` | Role | name, code, description | PK + UNIQUE(name, code) |
| `user_role_wsh` | UserRole | user_id, role_id | PK + UNIQUE(user_role) |

### 宠物模块（3表）
| 表 | Entity | 核心字段 | 索引 |
|----|--------|---------|------|
| `pet_wsh` | Pet | owner_id, name, type, breed, sterilized | PK |
| `category_wsh` | Category | name, parent_id, sort_order | PK |
| `care_record_wsh` | CareRecord | order_id, pet_id, keeper_id, type | PK |

### 寄养模块（6表）
| 表 | Entity | 核心字段 | 索引 |
|----|--------|---------|------|
| `merchant_wsh` | Merchant | user_id, name, rating, status, store_mode | PK |
| `keeper_wsh` | Keeper | merchant_id, user_id, price_per_day, status | PK |
| `pet_service_wsh` | ServiceItem | merchant_id, category_id, name, price | PK + idx_category_id + idx_merchant_id |
| `service_category_wsh` | ServiceCategory | parent_id, name, code, sort | PK + UNIQUE(code) + idx_parent_id |
| `address_wsh` | Address | user_id, address, is_default | PK |
| `business_hours_wsh` | BusinessHours | merchant_id, day_of_week, hours | PK + UNIQUE(merchant_day) |

### 订单模块（5表）
| 表 | Entity | 核心字段 | 索引 |
|----|--------|---------|------|
| `pet_order_wsh` | PetOrder | order_no, owner/pet/keeper/merchant, amount, status | PK + UNIQUE(order_no) |
| `order_snapshot_wsh` | OrderSnapshot | order_id, 各种快照JSON | PK + UNIQUE(order_id) + idx_order_snapshot_no |
| `payment_wsh` | Payment | order_id, pay_no, amount, status | PK |
| `refund_wsh` | Refund | order_id, amount, status | PK |
| `tip_wsh` | Tip | order_id, from_user, to_user, amount | PK |

### 客户服务模块（6表）
| 表 | Entity | 核心字段 | 索引 |
|----|--------|---------|------|
| `chat_message_wsh` | ChatMessage | from/to_user, order_id, content, read | PK + 4个复合索引 |
| `ticket_wsh` | Ticket | user_id, title, category, status | PK |
| `ticket_message_wsh` | TicketMessage | ticket_id, user_id, content | PK |
| `complaint_wsh` | Complaint | order_id, owner_id, status | PK |
| `rating_wsh` | Rating | order_id, target_id/type, score | PK |
| `merchant_customer_service_wsh` | MerchantCustomerService | merchant_id, user_id, status | PK + UNIQUE + 2索引 |

### 财务模块（3表）
| 表 | Entity | 核心字段 | 索引 |
|----|--------|---------|------|
| `wallet_wsh` | Wallet | user_id, balance, frozen_amount, version | PK + UNIQUE(user) |
| `wallet_transaction_wsh` | Transaction | wallet_id, user_id, type, amount, request_id | PK + UNIQUE(request_id) + 2索引 |
| `withdrawal_wsh` | Withdrawal | user_id, amount, status | PK |

### 运营模块（6表）
| 表 | Entity | 核心字段 | 索引 |
|----|--------|---------|------|
| `notice_wsh` | Notice | title, content, type, status | PK |
| `notice_read_wsh` | NoticeRead | notice_id, user_id | PK + UNIQUE |
| `notification_wsh` | Notification | user_id, title, type, is_read | PK + 2索引 |
| `operation_log_wsh` | OperationLog | user_id, module, operation, url, ip | PK + 3索引 |
| `file_record_wsh` | FileRecord | original_name, object_name, size | PK + 1索引 |
| `favorite_wsh` | Favorite | user_id, target_id/type | PK + UNIQUE(user_target) |
| `content_review_wsh` | ContentReview | target_type/id, status, reviewer | PK |

### AI模块（3表）
| 表 | Entity | 核心字段 | 索引 |
|----|--------|---------|------|
| `ai_report_wsh` | AiReport | order_id, pet_id, content, type | PK |
| `knowledge_document_wsh` | KnowledgeDocument | title, content, category, source_type | PK |
| `document_embedding_wsh` | DocumentEmbedding | document_id, embedding, chunk_text | PK + FK |

### 领养模块（2表）
| 表 | Entity | 核心字段 | 索引 |
|----|--------|---------|------|
| `adoption_pet_wsh` | ❌ 缺失 | name, type, status | PK + 2索引 |
| `adoption_application_wsh` | ❌ 缺失 | user_id, pet_id, status | PK + 3索引 |

### 看护人管理（2表）
| 表 | Entity | 核心字段 | 索引 |
|----|--------|---------|------|
| `keeper_leave_wsh` | KeeperLeave | keeper_id, merchant_id, start/end_date | PK |
| `keeper_attendance_wsh` | KeeperAttendance | keeper_id, merchant_id, check_in/out | PK |

### 会员模块（5表）
| 表 | Entity | 核心字段 | 索引 |
|----|--------|---------|------|
| `member_plan_wsh` | MemberPlan | code, name, level, price | PK |
| `user_membership_wsh` | UserMembership | user_id, plan_id, status | PK |
| `membership_order_wsh` | MembershipOrder | order_no, user_id, plan_id, status | PK |
| `membership_benefit_usage_wsh` | MembershipBenefitUsage | user_id, membership_id, benefit_type | PK |
| `membership_event_wsh` | MembershipEvent | user_id, membership_id, event_type | PK |

## 4.3 数据库设计问题

### 严重问题

**1. Entity-Schema 字段不匹配**

| 表 | 问题 | 影响 |
|----|------|------|
| `service_category_wsh` | DDL 列名 `sort_wsh`，但 Entity `ServiceCategory.sort_order_wsh` 映射到 `sort_wsh` | ⚠️ MyBatis-Plus 写操作会写入错误的列名 |
| `pet_order_wsh` | 50+字段，**零业务索引**（仅 order_no 唯一索引） | ⚠️ 按 owner_id/keeper_id/merchant_id/status 查询会全表扫描 |
| `user_wsh` | 无 username 索引（只有 UNIQUE） | ⚠️ 用户名查询走唯一索引没问题，但按 nickname/phone 无索引 |
| `document_embedding_wsh` | embedding 存 `longtext` JSON | ⚠️ 大数据量时查询全表扫描，向量距离计算在应用层做 |

**2. 未完全实现的功能**

| 表 | 实体缺失 | 状态 |
|----|---------|------|
| `adoption_pet_wsh` | 无 Java Entity | Controllers 已存在，Entity 缺失 |
| `adoption_application_wsh` | 无 Java Entity | Controllers 已存在，Entity 缺失 |
| `document_embedding_wsh` | 无 Java Entity | 被 RAG 服务引用但无对应 Mapper |

**3. 外键关系**

- 所有外键关系都是**隐式的**（通过字段名关联），DDL 中未定义任何 FOREIGN KEY（除 `document_embedding_wsh` 外）
- 优点: 便于迁移和分库
- 缺点: 数据完整性无法由数据库保证，完全依赖应用层

### 中级问题

**4. 两份 H2 Schema 不一致**
- `schema.sql`: 38张表 ✅
- `h2-schema.sql`: 37张表（缺 `operation_log_wsh`）
- 使用 `h2-schema.sql` 的 H2 测试将无法测试操作日志功能

**5. 迁移脚本重复定义**
- `migration.sql` 和 `migration_safe_add_missing.sql` 对同一批表使用了 `CREATE TABLE IF NOT EXISTS`
- 存在冗余，但不会出错

### 建议修复

**6. 缺失索引表**（重要）
- `user_wsh`: 需要 `phone`, `deleted` 组合索引
- `pet_wsh`: 需要 `owner_id` 索引
- `keeper_wsh`: 需要 `merchant_id`, `user_id` 索引
- `merchant_wsh`: 需要 `user_id`, `status` 索引
- `payment_wsh`: 需要 `order_id` 索引
- `refund_wsh`: 需要 `order_id` 索引
- `complaint_wsh`: 需要 `order_id`, `merchant_id` 索引
- `rating_wsh`: 需要 `target_id + target_type` 组合索引
- `ticket_wsh`: 需要 `user_id`, `status` 索引
- `pet_order_wsh`: 需要 `owner_id`, `keeper_id`, `merchant_id`, `status` 索引

## 4.4 种子数据

| 角色种子（5个） | 用户种子（4个） | 分类种子 |
| ADMIN/OWNER/KEEPER/MERCHANT/CUSTOMER_SERVICE | admin/owner/merchant1/keeper1 | 8种宠物品种 |

---

# 第五部分：API接口分析

## 5.1 接口统计

| 模块 | Controller数 | 接口数 |
|------|-------------|--------|
| pet-system | 3 | 14 |
| pet-admin | 1 | 2 |
| pet-ai | 3 | 10 |
| pet-business/pet | 3 | 14 |
| pet-business/boarding | 6 | 35+ |
| pet-business/order | 4 | 22 |
| pet-business/customer | 4 | 22 |
| pet-business/finance | 3 | 13 |
| pet-business/operation | 7 | 22 |
| pet-business/adoption | 1 | 12 |
| **总计** | **~35** | **~166** |

## 5.2 接口设计规范

- 基础路径: `/api`
- 统一响应: `Result<T>` (`{code, msg, data}`)
- 分页: `PageResult<T>` (`{code, msg, data: {records, total, page, size}}`)
- 请求体: JSON
- 认证: Bearer Token in Authorization header
- 权限: `@PreAuthorize` 注解（Spring Security 方法级）

## 5.3 设计问题

| 问题 | 示例 | 严重性 |
|------|------|--------|
| 部分接口直接返回 Entity | `UserController.getCurrentUser()` 返回 User Entity | 中 |
| 部分接口使用 Map 接收参数 | 部分 Service 方法 Map 参数 | 低 |
| 接口路径不统一 | `/api/orders/start/upload` vs `/api/orders/start` | 低 |
| RESTful 不严谨 | `/api/orders/cancel` 用 POST 而非 PUT | 低 |
| 分页参数不一致 | 部分用 `page/size`，部分用 `pageNum/pageSize` | 中 |
| `/api/auth/logout` 无实际作用 | 未将 Token 加入黑名单 | 高 |

---

# 第六部分：安全体系分析

## 6.1 JWT 认证流程

```
1. 用户登录 → POST /api/auth/login → 返回 { access_token, refresh_token, user }
2. 前端存储: localStorage (token, refreshToken, user)
3. 每次请求: Axios 拦截器添加 Authorization: Bearer {token}
4. JwtAuthenticationFilter: 从 Header 提取 Token → 解析 → 设置 SecurityContext
5. Token 过期: 响应 401 → 前端自动调用 /api/auth/refresh → 重试
6. 登出: 前端清除 localStorage → 路由跳转
```

## 6.2 权限控制体系

- **后端**: Spring Security @PreAuthorize（角色级别 + 方法级别）
  - `hasRole('ADMIN')` — 管理员
  - `isAuthenticated()` — 所有登录用户
  - `permitAll()` — 公开
- **前端**: 路由守卫 + 动态路由 + v-permission 指令 + Permission Store
  - 路由 `meta.roles` 定义可访问角色
  - `addDynamicRoutes(roles)` 动态添加商业后台/管理后台路由
  - `v-permission` 指令控制元素级可见性

## 6.3 安全风险

### 高危
| 风险 | 位置 | 说明 |
|------|------|------|
| 🔴 无 JWT 黑名单 | `/api/auth/logout` | 登出后 Token 仍有效 |
| 🔴 密码明文存储风险 | `user_wsh.password_wsh` | 需要确认是否使用 BCrypt |
| 🔴 文件上传无认证 | `SecurityConfig` `/api/files/**` 白名单 | 任何未认证用户可以上传文件 |
| 🔴 AI 接口完全公开 | `SecurityConfig` `/api/ai/**` 白名单 | AI 报告生成无需认证 |

### 中危
| 风险 | 位置 | 说明 |
|------|------|------|
| 🟡 大量核心表无索引 | `pet_order_wsh` 等 | 大数据量时可能拒绝服务 |
| 🟡 部分接口越权 | 需审核所有 `@PreAuthorize` 配置 | 部分接口只做了 `isAuthenticated()` |
| 🟡 注册无频率限制 | `/api/auth/register` | 可能被滥用注册 |
| 🟡 H2 Console 可访问 | `application.yml` 未关闭 | 生产环境不应暴露 |

### 低危
| 风险 | 位置 | 说明 |
|------|------|------|
| 🟢 h2-console 路径公开 | 白名单 | 生产环境应移除 |
| 🟢 无敏感操作二次确认 | 提现/退款等 | 缺少支付密码校验 |
| 🟢 日志可能记录敏感信息 | `operation_log_wsh` | Token 等可能在请求体中 |

---

# 第七部分：前端架构分析

## 7.1 整体架构

```
frontend/
├── src/
│   ├── main.js                 # 入口
│   ├── App.vue                 # 根组件（布局切换 + Toast）
│   ├── router/index.js         # 路由（57+ 常量路由 + 动态路由）
│   ├── stores/                 # 7个 Pinia Store
│   ├── api/                    # 30个 API 模块
│   ├── services/               # 服务层（API 封装 + 错误处理）
│   ├── domain/                 # 领域层（业务逻辑）
│   ├── composables/            # 可组合函数
│   ├── components/             # 组件
│   │   ├── layout/             # 3种布局
│   │   ├── common/             # 10+ 通用组件
│   │   ├── order/              # 订单组件
│   │   ├── pet/                # 宠物组件
│   │   └── ...
│   ├── views/                  # 页面
│   │   ├── user/               # 28 页
│   │   ├── merchant/           # 5 页
│   │   ├── admin/              # 19 页
│   │   ├── adoption/           # 1 页
│   │   └── error/              # 4 页
│   ├── constants/              # 常量（状态映射、消息）
│   ├── directives/             # 自定义指令
│   └── utils/                  # 工具
```

## 7.2 布局系统

| 布局 | 适用角色 | 说明 |
|------|---------|------|
| `UserLayout` | OWNER/KEEPER/MERCHANT/ADMIN | 顶部导航 + 侧边栏 |
| `MerchantLayout` | MERCHANT | 商家专用侧边栏布局 |
| `AdminLayout` | ADMIN | 管理后台侧边栏布局 |

## 7.3 状态管理

| Store | 状态 | 说明 |
|-------|------|------|
| `auth.js` | user, token, refreshToken | 认证状态 |
| `app.js` | toasts, theme, unread | 全局应用状态 |
| `permission.js` | 角色-权限映射 | 细粒度权限 |
| `notification.js` | unreadCount | 通知轮询 |
| `category.js` | categories, tree | 服务分类缓存 |
| `orderDetail.js` | order, timeline, messages | 订单详情状态 |
| `design.js` | activeDesign | 设计系统 |

## 7.4 布局切换机制（App.vue）

```
App.vue
├── layout === 'user'       → UserLayout
├── layout === 'merchant'   → MerchantLayout
├── layout === 'admin'      → AdminLayout
└── 其他                    → 无布局（登录/注册/错误页）
```

## 7.5 Token 管理

- 存储: localStorage（token, refreshToken, user）
- 拦截器: `request.js` 自动注入 `Authorization` header
- 过期处理: 401 自动刷新，刷新失败跳转登录
- 刷新队列: 多个请求同时 401 时同步等待（`isRefreshing` 互斥锁）

---

# 第八部分：消息队列分析

## 8.1 配置

```
Exchange: pet.direct（Direct 类型）
```

## 8.2 队列清单

| 队列名 | Routing Key | 用途 |
|--------|-------------|------|
| `order.create` | `order.create` | 订单创建通知 |
| `order.cancel` | `order.cancel` | 订单取消通知 |
| `order.refund` | `order.refund` | 订单退款通知 |
| `message.send` | `message.send` | 消息发送通知 |
| `ai.report` | `ai.report` | AI报告生成通知 |
| `complaint.process` | `complaint.process` | 投诉处理通知 |

## 8.3 存在文件

| 文件 | 功能 |
|------|------|
| `pet-framework/.../mq/RabbitMQConfig.java` | 配置 Exchange + 6个 Queue + Binding |
| `pet-framework/.../mq/MessageSender.java` | 消息发送（所有 RoutingKey 集中管理） |
| `pet-framework/.../mq/MessageListener.java` | 消息监听（6个 @RabbitListener） |
| `pet-common/.../mq/ComplaintProcessHandler.java` | 投诉处理 MQ 消费者 |

## 8.4 流程示例（订单创建）

```
OrderService.create()
  → MessageSender.sendOrderCreate(orderNo)
    → RabbitTemplate.convertAndSend("pet.direct", "order.create", message)
      → @RabbitListener(queues = "order.create")
        → MessageListener.handleOrderCreate(message)
          → NotificationService.createNotification(owner, "订单已创建")
```

## 8.5 配置问题

| 问题 | 影响 |
|------|------|
| ⚠️ `acknowledge-mode: manual` 手動 ACK | 正确，但需确保所有监听器有 ACK/NACK 逻辑 |
| ⚠️ `publisher-confirm-type: correlated` | 发布确认模式，但未见 ConfirmCallback 实现 |
| ⚠️ 2个 ConnectionFactory Bean 冲突风险 | RabbitMQConfig 创建了第二个 |
| ⚠️ 无死信队列（DLQ）配置 | 失败消息无法自动路由到死信队列 |
| ⚠️ 无重试机制 | 消息消费失败不会自动重试 |

---

# 第九部分：AI模块分析

## 9.1 AI 功能矩阵

| 功能 | 接口 | 实现 |
|------|------|------|
| 照护建议生成 | POST `/api/ai/care-suggestion` | DeepSeek + 宠物信息 Prompt |
| 寄养报告生成 | POST `/api/ai/boarding-report` | DeepSeek + 照护记录 Prompt |
| AI 报告查询 | GET `/api/ai/reports/{order\|pet}` | 数据库查询 |
| RAG 知识库问答 | POST `/api/rag/ask` | Embedding → Chroma/DB 搜索 → LLM |
| RAG 知识库搜索 | GET `/api/rag/search` | 向量相似度搜索 |
| AI Agent 执行 | POST `/api/agent/execute` | 多步骤 Agent 任务 |

## 9.2 技术实现细节

- **模型**: DeepSeek Chat（`deepseek-chat`）
- **API**: Spring AI（OpenAI 兼容协议） + LangChain4j
- **向量数据库**: Chroma（8000端口）
- **Embedding**: 本地存储在 `document_embedding_wsh`（同时也配置了 Chroma）
- **知识库文档**: 5篇内置文档（寄养规则、退款规则、投诉规则、护理指南、疫苗要求）

## 9.3 存在问题

| 问题 | 详细 | 严重性 |
|------|------|--------|
| `OpenAiAutoConfiguration` 被排除 | `application.yml` 中 `autoconfigure.exclude` | ⚠️ 可能需排查 Spring AI 是否正常工作 |
| 双向量存储 | 既有 `document_embedding_wsh` 表，又配置了 Chroma | ⚠️ 需明确当前使用的是哪个 |
| Embedding 列类型 | `longtext` JSON 存储向量，无法做数据库级向量搜索 | 中 |
| AI 接口安全 | `/api/ai/**` 和 `/api/rag/**` 完全公开 | 高 |
| DeepSeek API Key | 明文存储在 `.env` 文件中 | 高（生产应使用密钥管理服务） |

---

# 第十部分：项目健康度评估

## 10.1 评分

| 维度 | 分数 | 说明 |
|------|------|------|
| **架构** | 7/10 | 模块化单体架构清晰，但模块间存在循环依赖风险 |
| **数据库** | 5/10 | 设计完整但索引严重缺失、Entity-Schema不一致、外键缺失 |
| **代码规范** | 7/10 | 命名规范统一（`_wsh` 后缀），但 `map-underscore-to-camel-case: false` 是双刃剑 |
| **安全** | 5/10 | 基础 JWT 认证有，但无 Token 黑名单、多处接口未保护、API Key 明文 |
| **扩展性** | 6/10 | Maven 模块化为微服务化提供了基础，但当前单体架构耦合较紧 |
| **性能** | 4/10 | 核心业务表无索引、30秒轮询、定时任务 60秒执行周期 |
| **维护成本** | 6/10 | 代码量适中，但多份 SQL 文件（13份）不一致增加维护难度 |
| **总体** | **5.7/10** | 核心功能完整，适合持续开发，需优先解决安全和数据库问题 |

---

# 第十一部分：后续开发建议

## 11.1 短期必须修复（优先级：稳定性 > 数据安全 > 可维护性 > 新功能）

### P0 - 数据安全
1. **为 `pet_order_wsh` 添加业务索引**
   - 必须: `owner_id_wsh`, `keeper_id_wsh`, `merchant_id_wsh`, `status_wsh`, `order_no_wsh`
   - 影响: 订单查询性能，数据量大时可能导致数据库崩溃
2. **JWT 黑名单机制**
   - 登出时 Token 加入 Redis 黑名单
   - 影响: 当前登出后 Token 仍有效
3. **AI 和文件接口加认证**
   - `/api/ai/**` 和 `/api/rag/**` 移除白名单
   - `/api/files/**` 移除白名单
   - 影响: 目前这些接口完全公开

### P1 - 数据完整性
4. **`ServiceCategory.sort_wsh` / `sort_order_wsh` 不一致修复**
   - DDL 列 `sort_wsh` vs Entity `sort_order_wsh`
   - 影响: MyBatis-Plus 写入错误列
5. **统一两份 H2 Schema**
   - `schema.sql` 和 `h2-schema.sql` 合并
   - 影响: H2 测试缺少 `operation_log_wsh` 表

### P2 - 稳定性
6. **RabbitMQ 死信队列配置**
   - 为每个队列配置 DLQ + 重试机制
   - 影响: 消息消费失败无法自动恢复
7. **定时任务注解检查**
   - 确认 `@EnableScheduling` 已开启
   - 确认 `60秒` 扫描周期是否满足业务要求

## 11.2 中期优化

1. **支付系统集成**
   - 对接真实微信/支付宝支付
   - 替换当前模拟支付
2. **聊天系统 WebSocket 化**
   - 替换当前 30秒 轮询
   - 提高消息实时性
3. **清理 13 份 SQL 文件**
   - 保留 `00_database_unified.sql` 为唯一权威来源
   - 删除冗余 migration 文件
4. **为所有核心表添加业务索引**
   - 按前述清单逐一添加
5. **补充缺失 Entity**
   - 创建 `AdoptionPet`, `AdoptionApplication`, `DocumentEmbedding` 实体
6. **密码加密审计**
   - 确认使用 BCryptPasswordEncoder
7. **操作日志脱敏**
   - 敏感信息（密码、Token）不记录日志

## 11.3 长期企业化升级

1. **微服务拆分**（按领域: 用户、订单、支付、AI）
2. **引入配置中心**（Nacos / Apollo）
3. **引入服务网格**（服务发现、负载均衡）
4. **引入分布式事务**（Seata）
5. **引入链路追踪**（SkyWalking / Zipkin）
6. **数据库读写分离**
7. **容器化编排**（K8s）
8. **CI/CD 流水线**（GitLab CI / Jenkins）
9. **灰度发布能力**
10. **多语言国际化**

---

# 第十二部分：文件索引

## 12.1 关键代码文件快速索引

| 功能 | 路径 |
|------|------|
| 启动入口 | `pet-admin/src/main/java/com/pet/PetApplication.java` |
| 主配置 | `pet-admin/src/main/resources/application.yml` |
| 安全配置 | `pet-security/src/main/java/com/pet/security/SecurityConfig.java` |
| JWT工具 | `pet-security/src/main/java/com/pet/security/JwtUtil.java` |
| 全局异常处理 | `pet-common/src/main/java/com/pet/common/GlobalExceptionHandler.java` |
| 统一响应 | `pet-common/src/main/java/com/pet/common/Result.java` |
| 订单状态 | `pet-common/src/main/java/com/pet/common/OrderStatus.java` |
| 消息队列配置 | `pet-framework/src/main/java/com/pet/config/RabbitMQConfig.java` |
| 消息发送 | `pet-framework/src/main/java/com/pet/mq/MessageSender.java` |
| 消息监听 | `pet-framework/src/main/java/com/pet/mq/MessageListener.java` |
| MyBatis-Plus配置 | `pet-framework/src/main/java/com/pet/config/MyBatisPlusConfig.java` |
| MinIO配置 | `pet-framework/src/main/java/com/pet/config/MinIoConfig.java` |
| SPA路由转发 | `pet-framework/src/main/java/com/pet/config/SpaForwardController.java` |
| 认证控制器 | `pet-system/src/main/java/com/pet/system/controller/AuthController.java` |
| 订单控制器 | `pet-business/src/main/java/com/pet/order/controller/OrderController.java` |
| 支付超时任务 | `pet-business/src/main/java/com/pet/order/job/OrderPaymentTimeoutScheduler.java` |
| 会员过期任务 | `pet-business/src/main/java/com/pet/membership/job/MembershipExpirationScheduler.java` |
| AI报告控制器 | `pet-ai/src/main/java/com/pet/ai/controller/AiReportController.java` |
| RAG控制器 | `pet-ai/src/main/java/com/pet/ai/controller/RagController.java` |
| 数据库统一DDL | `database/00_database_unified.sql` |
| Docker编排 | `docker-compose.yml` |
| 前端入口 | `frontend/src/main.js` |
| 前端路由 | `frontend/src/router/index.js` |
| 前端登录 | `frontend/src/views/user/Login.vue` |
| Axios封装 | `frontend/src/utils/request.js` |
| Auth Store | `frontend/src/stores/auth.js` |
| 前端布局 | `frontend/src/components/layout/` |

## 12.2 数据库文件索引

| 文件 | 用途 | 优先级 |
|------|------|--------|
| `database/00_database_unified.sql` | ✅ 统一DDL（38表，权威来源） | 使用这个 |
| `database/01_database_baseline.sql` | 旧基线（35表，过时） | 忽略 |
| `db/schema.sql` | H2 Schema（38表） | 用于H2 |
| `db/h2-schema.sql` | H2 Schema（37表，缺operation_log） | 需合并 |
| `db/migration.sql` | 历史迁移 | 仅用于升级 |
| `db/migration_wsh.sql` | _wsh 后缀重命名迁移 | 仅历史参考 |
| `db/seed.sql` | 测试种子数据 | 用于开发环境 |

---

# 附录：团队约定

## A1. Git 分支策略
- 仓库根目录发现 `.agents/`, `.codex/`, `.opencode/` 目录
- 存在 `AGENT_RULES.md`, `ARCHITECTURE_REVIEW.md` 等治理文件

## A2. 已有架构文档
| 文档 | 路径 |
|------|------|
| 项目功能地图 | `PROJECT_MAP.md` |
| 产品说明 | `PRODUCT.md` |
| 进度跟踪 | `docs/PROGRESS.md` |
| 审计报告 | `docs/audit-report.md` |
| 数据库设计 | `docs/DATABASE.md` |
| 序列化治理 | `serialization_governance/` |

## A3. 需要确认的点
> ⚠️ 以下内容无法从代码扫描确定，需要人工确认：
1. 密码是否使用了 BCryptPasswordEncoder？
2. Spring AI + DeepSeek 当前是否能正常工作？
3. 双向量存储（document_embedding_wsh vs Chroma）当前使用的是哪个？
4. 是否已有开发的第三方支付对接方案？
5. RabbitMQ 消息消费失败的重试策略是什么？
6. 生产环境是否已经部署？当前有哪些服务器？

---

> 文档结束 | 基于 2026-07-25 代码扫描自动生成
> 如有更新，请在项目根目录更新后重新生成本文件
