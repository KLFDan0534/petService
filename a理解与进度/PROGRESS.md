# PROGRESS.md — 项目接管进度记录

> **位置：`a理解与进度/PROGRESS.md`**（放在 a 开头目录，方便在资源管理器里置顶）
> 本文件记录「逐条接管项目」的**会话级**进度，用于中断后恢复。
> 与 `docs/project-learning/`（已有的学习资料）互补：本文件只记录**当前进度**与**已确认事实**。
> 👉 **开工前先读根目录 [`README.md`](../README.md)**；要精确到"读到哪个文件第几行"，看同目录的 [`理解路线.md`](./理解路线.md) 顶部「⚡ 恢复卡」。
> 本文件里出现的 `docs/...` 路径都是相对**项目根目录**的。

---

## 0. 当前状态

| 项目 | 内容 |
|------|------|
| 阶段 | 第一阶段（现状盘点）✅ → 第二阶段（通知/消息链路梳理）✅ → **下一步：修第一个 bug（N2）** |
| 本轮日期 | 2026-10-26（同日两轮） |
| 本轮是否改代码 | **否**（只读调查 + 文档整理） |
| 是否新增依赖 | 否 |
| 是否执行破坏性操作 | 否 |
| 是否运行测试 | **未执行** |

---

## 1. 工作区保护状态（重要）

- 当前分支：`codex/pet-grooming-ui-redesign`
- 未提交改动（**本轮未触碰**）：
  - `M pet-business/src/main/java/com/pet/operation/service/impl/NoticeServiceImpl.java` — 仅新增行尾中文注释，无行为变化
  - `M 功能缺失与中间件增强建议清单.md` — 文档改动
- 大量未追踪文件（`??`）：根目录若干审计报告 `.md`、`pet-allinone/`、`start-all*.bat`、`docker-compose.allinone.yml`、`restore-middleware.ps1` 等
- **约定**：动手改代码前先确认上述改动是否需要保留；不要 `git checkout .` / `git reset --hard`。

---

## 2. 已确认的事实（附证据）

### 2.1 模块结构
| 模块 | Java 文件数 | 职责 |
|------|------------|------|
| pet-common | 20 | 通用返回体 `Result`、`BusinessException`、`GlobalExceptionHandler`、`OrderStatus`、`BookingUnit`、MQ 处理器接口 |
| pet-framework | 10 | 基础设施配置：Redis 缓存、RabbitMQ、MinIO、MyBatis-Plus、SpringDoc、字符集、`.env` 加载 |
| pet-security | 5 | JWT 工具、认证过滤器、SecurityConfig、SSE Token |
| pet-system | 41 | 用户/角色/登录注册/实名审核 |
| pet-business | 419 | 12 个业务域（boarding/order/customer/finance/operation/pet/marketing/membership/fulfillment/qualification/geo/ai） |
| pet-ai | 26 | LLM 对话、Agent 计划、AI 报告、Chroma 向量检索 |
| pet-admin | 29 | 启动入口 `PetApplication`、统计、操作日志切面 |

- 启动入口：`pet-admin/src/main/java/com/pet/admin/PetApplication.java`（`scanBasePackages = "com.pet"`，`@MapperScan` 11 个包）
- 后端 54 个 Controller；前端 `frontend/src` 有 94 个 `.vue` 页面、34 个 api 模块
- 数据库：52 张表，统一 `_wsh` 后缀，逻辑删除字段 `deleted_wsh`（`docs/database/full-schema.sql`）

### 2.2 中间件真实使用情况
| 组件 | 结论 | 证据 |
|------|------|------|
| Redis 缓存（Spring Cache） | **已启用且在用** | `pet-framework/.../config/RedisCacheConfig.java`（`@EnableCaching`、`transactionAware()`）；`@Cacheable` 分布在 Notice/Notification/ServiceItem/ServiceCategory |
| Redis 状态类 | 在用 | 验证码、登录失败计数、JWT 黑名单（`UserServiceImpl`、`JwtAuthenticationFilter`） |
| RabbitMQ | 队列声明完整，但**部分队列无生产者** | `RabbitMQConfig` 声明 11 个队列；实际调用点见 2.3 |
| MinIO | 在用 | `MinIoService`、`MinIoConfig` |
| Spring AI | **未使用** | 仅出现在父 `pom.xml` 的 `dependencyManagement`，**没有任何模块声明**，也没有任何 `import org.springframework.ai.*`；`application.yml` 里排除的 `OpenAiAutoConfiguration` 实际不在 classpath |
| langchain4j | 在用 | `pet-ai/.../LlmChatServiceImpl.java` |
| Chroma | 在用 | `pet-ai/.../ChromaService.java` + `application.yml` `chroma.*` |
| 邮件（spring-boot-starter-mail） | **只有依赖，无使用** | 全项目无 `JavaMailSender` / `MimeMessage` 引用 |
| 分布式锁 | **不存在** | 全项目无 redisson、无 `setIfAbsent`、无 `tryLock` |

### 2.3 RabbitMQ 队列 ↔ 生产者对应关系
| 队列 | 生产者 | 消费者 | 状态 |
|------|--------|--------|------|
| `order.payment.timeout(.delay)` | `OrderServiceImpl:2447` | `OrderPaymentTimeoutListener` | 在用 |
| `order.accept.timeout(.delay)` | `PaymentServiceImpl:456` | `OrderAcceptTimeoutListener` | 在用 |
| `complaint.process` | `ComplaintServiceImpl:805` | `MessageListener` → `ComplaintProcessHandler` | 在用 |
| `notice.notification` | `NoticeServiceImpl:199,291` | `MessageListener` → `NoticeNotificationHandler` | 在用 |
| `order.create` | 仅 `AgentServiceImpl:550`（AI 下单路径） | `MessageListener`（只打日志） | 主下单链路**不走 MQ** |
| `order.cancel` / `order.refund` / `message.send` / `ai.report` | **无生产者** | `MessageListener`（只打日志） | 死队列 |

### 2.4 认证与异常
- `SecurityConfig`：`STATELESS`、CSRF/CORS 关闭、`/api/auth/**` 放行、部分 GET 公开、其余 `authenticated()`；方法级 `@PreAuthorize` 广泛使用（如 `OrderController` 的 `hasRole('KEEPER') or hasRole('MERCHANT')`）
- `GlobalExceptionHandler`：`BusinessException` 按 code 映射 HTTP 状态；兜底 500 打完整堆栈
- 测试：44 个测试文件（pet-admin 20 / pet-business 22 / pet-common 2），**pet-ai、pet-security、pet-system 无测试**

---

## 3. 待调查/待验证问题（按优先级）

| # | 问题 | 类型 | 影响 |
|---|------|------|------|
| Q1 | 下单并发保护有**两层**：① `synchronized (CREATE_ORDER_LOCK)`（:120/:549/:581，单 JVM，且在事务提交前释放）；② **看护者容量**另用 `keeperMapper.selectByIdForUpdate()` 做 `SELECT ... FOR UPDATE` 行锁（:1881）。⚠️ **宠物冲突 `ensureNoPetDateConflict`(:1849) 没有行锁**，只靠 synchronized | 已确认 | 多实例下 synchronized 失效，仅容量有 DB 兜底；宠物冲突存在「查完未插」窗口 |
| Q2 | `serviceItem` / `service-item-page` / `service-category` 三处 `@Cacheable` **没有任何 `@CacheEvict`**，也无编程式失效 | 已确认 | 商家改服务后最长 30 分钟脏读 |
| Q3 | `RedisCacheConfig` 里配置的 `"category"` TTL(2h) 与实际使用的 `"service-category"` 名字不一致 | 已确认 | 配置项形同虚设，走默认 30 分钟 |
| Q4 | MQ 消费失败统一 `basicNack(requeue=false)`，无重试、无死信队列 | 已确认 | 公告批量通知等消息一旦失败即静默丢失 |
| Q5 | `OrderServiceImpl` 2565 行，下单/状态机/履约/结算/权限混在一个类；`requireFutureBookableKeeper` 等资格校验与 `ServiceAvailabilityServiceImpl` 重复 | 已确认（旧审计报告亦有记录） | 可维护性、答辩时难以讲清 |
| Q6 | `pet-business` 与 `pet-ai` 两个模块共用 `com.pet.ai` 包名 | 已确认（无同名类冲突） | 包边界模糊，但当前不报错 |
| Q7 | `pet_order_wsh` 只有 `idx_order_no/idx_owner/idx_merchant/idx_status`，无 `(keeper_id, status)` 组合索引；`order_no` 非唯一索引 | 已确认 | 看护者待接单查询可能全表扫 |
| Q8 | `AiChatServiceImpl` 用 `ConcurrentHashMap` 做限流（单机） | 待复核（旧清单记录，本轮未读该文件） | 多实例失效 |

---

## 4. 本轮修改记录

- **代码修改：无**
- **新增文件：** `PROGRESS.md`（本文件）
- **未创建：** `PROJECT_GUIDE.md`、`TECH_NOTES.md`（按约定先给出草案供确认，不擅自落地；且 `docs/project-learning/` 已有 15 篇现成资料，需先确认是否复用）

---

## 5. 第二轮记录（2026-10-26 下午）：通知 / 消息模块

### 本轮目标
用户选定「通知和消息模块」作为第一/第二个学习目标（此前已加过 MQ，业务理解接近完成）。本轮只做：梳理链路、列小修复、整理文档。**不改业务代码**。

### 完成情况
| 事项 | 结果 |
|---|---|
| 通读通知模块全部代码（operation 包 + MQ + SSE + 前端 store） | ✅ |
| 连库核对 3 张表的索引与数据现状 | ✅ |
| 产出缺陷清单 `docs/BUGFIX清单.md`（N1–N10 + X1/X2） | ✅ |
| 在 `docs/通知系统讲解.md` 补 4 处 ⚠️ 缺陷标注 | ✅ |
| 重写 `docs/index.md` 为文档总索引（旧版已备份） | ✅ |
| 根目录一次性产物归档到 `docs/archive/2026-10/` | ✅ |
| 产出 `docs/通知模块速查.md`（一页纸，用户自测后要求） | ✅ |
| 修 bug | ❌ 未开始（等确认） |

### 本轮新确认的事实（含证据）
- **N1 已确认**：`NoticeRead` 有 `@TableLogic`，`update()` 里那行 `delete` 实为软删；实库唯一键 `uk_notice_read_notice_user(notice_id_wsh,user_id_wsh)` 不含 `deleted_wsh` → 编辑公告后用户再关弹窗会撞唯一键、被 `catch (DuplicateKeyException ignored)` 吞掉。当前库中 `deleted_wsh=1` 的已读记录为 **0 条**，说明该路径尚未被走过（潜伏）。
- **N2 已确认**：`SocketManager.connectSSE()` 首行调 `disconnect()`，而 `disconnect()` 清空 `listeners`；重连走的正是 `connectSSE` → 首次断线后 SSE 监听永久丢失，只剩 30s 轮询。
- **N4/N5 已确认**：`NoticeNotificationHandlerImpl.handle` 与 `NotificationServiceImpl.purgeStaleNoticeNotifications` 都只 evict `notification`，漏 `notificationUnreadCount`。
- **N3 待验证**：`NoticeServiceImpl.create` 在 `@Transactional` 内发 MQ，消费者可能读不到未提交数据 → 打「公告不存在」日志后正常 ack → 通知永久丢失。
- **N6 已确认**：`notification_wsh` 实库无任何唯一键；`handle()` 无 `@Transactional`。当前数据无重复（notice4=43/43、notice8=45/45），说明重投尚未发生。
- 实库 `notice_read_wsh` 有两条完全相同的唯一索引（`uk_notice_read_notice_user` / `uk_notice_user`）。
- 实库索引名与 `docs/database/full-schema.sql` 不一致（`idx_notification_user` vs `idx_user`）。
- 活跃用户 42 人，通知总量 211 条（未读 192，无软删）。

### 已发生的代码修改
**无。** 仅新增/修改文档：`docs/BUGFIX清单.md`（新建）、`docs/index.md`（重写）、`docs/通知系统讲解.md`（4 处 ⚠️ 插入）、`PROGRESS.md`（本文件）。

### 文档/文件整理
> ⚠️ 以下 `docs/archive/2026-10/` 相关内容**已于 2026-10-27 整体删除**，此处仅作历史记录。

- 新建 `docs/archive/2026-10/{audit,tmp,legacy-zip}/`
- 8 份未追踪审计报告 → `docs/archive/2026-10/audit/`
- 10 个临时调试文件 → `docs/archive/2026-10/tmp/`
- 2 个旧 zip（共 8.8MB）→ `docs/archive/2026-10/legacy-zip/`
- 旧 `docs/index.md` → `docs/archive/2026-10/index-old-20项架构清单.md`
- **未动任何 git 追踪文件**，`git status` 里没有新增删除。

---

## 6. 下次开始时的第一个动作

**先修 N2**（前端 `SocketManager.js`，1 个文件、10 行内），因为：
- 影响真实可见（SSE 断一次就永久失效）；
- 改动最小、无后端依赖、不需要重启后端；
- 改完立刻能验证（断网重连后发通知看角标）。

具体动作：打开 `frontend/src/utils/SocketManager.js`，把 `connectSSE` 首行的 `this.disconnect()` 换成新的 `closeTransport()`，`disconnect()` 保持原样。

**验收标准**：登录 → 断网 5 秒恢复 → 另一账号发通知 → 角标 1 秒内变化。

---

## 7. 第三轮记录（2026-10-27）：下单链路

### 本轮目标
用户要求：从最开始理解下单流程，到能完整独立讲解，并能举一反三。**不改业务代码。**

### 完成情况
| 事项 | 结果 |
|---|---|
| 通读下单主链路（Controller → ServiceImpl 2565 行 → Mapper） | ✅ |
| 补齐原清单漏掉的 5 个文件（支付/SSE/快照/通知接缝/MQ 配置） | ✅ |
| 回答卡住的未决问题（为什么只有容量加行锁） | ✅ 三层答案 |
| 产出 `docs/下单链路讲解.md`（14 节） | ✅ |
| 产出 `docs/订单模块速查.md`（一页纸） | ✅ |
| 回填 `理解路线.md` 恢复卡 / 链路状态 / 阅读清单 / 未决问题 | ✅ |
| 跑测试 | ❌ 未执行 |

### 本轮新确认的事实（含证据）

- **`synchronized` 释放早于事务提交**：`@Transactional` 在 `createOrder:543`（方法级），`synchronized` 在方法体 `:549`。代理在方法返回后提交事务 → 监视器先释放。这是"加了锁还可能漏"的根因。
- **宠物检查在 keeper 行锁之前**：`:684` 先于 `:685`，所以宠物冲突检查完全不受 `FOR UPDATE` 保护。
- **`OrderMapper` 只有 `BaseMapper` + 1 个 `@Select`**（券统计）。`insert` 是 MyBatis-Plus 自动生成的动态 SQL，没有手写 INSERT。
- **`accept` 的注解与实现不一致**：`OrderController:302` 允许 `KEEPER or MERCHANT`，但 `OrderServiceImpl:867` 的 `requireAssignedKeeperOrderAccess` 要求必须是该单 keeper 本人 → MERCHANT 分支是死代码。
- **`wechat` / `alipay` 走不通**：`PaymentServiceImpl:409-417` 只放行 `balance`；项目里没有支付回调接口。
- **`refreshKeeperCurrentPets` 顺带改 keeper 状态**：`current >= max_pets → BUSY`；商家营业且非手动离线 → `ACTIVE`；否则 `OFFLINE`。手动离线（`offline_source_wsh==1`）不被覆盖。
- **订单 → 通知的唯一自动化接缝**：`completeOrder → publishCompletedEvent → OrderFeedbackNotifier`，用 `@TransactionalEventListener(AFTER_COMMIT)`，写法比通知模块的 `NoticeServiceImpl.create` 规范（后者在事务内发 MQ，即 N3）。
- **两个超时都双保险**：支付 15min / 接单 30min，MQ 延迟队列（队列 TTL + 死信路由）+ 60 秒扫描器（LIMIT 100）。

### 本轮新发现的待验证项
| # | 问题 | 状态 |
|---|---|---|
| O1 | RR 快照导致 `FOR UPDATE` 之后的 `selectCount` 仍读旧数据 → 容量检查可能依然漏检 | **推断，未实测**。验证：`max_pets=1`，两并发同 keeper 同宠物重叠日期下单 |
| O2 | `couponService.lockForOrder` 是否有独立事务（`REQUIRES_NEW`）→ 影响失败回滚范围 | **未核实**，未打开 `CouponServiceImpl` |
| O3 | ~~加 pet 行锁修复宠物冲突，会导致批量下单死锁（批次 items 顺序不同）~~ | **已失效**：批量下单能力已下线，当前为单宠物、单订单模型 |

> 下单模型：当前系统采用**单宠物、单订单**，一次下单只创建一个订单、只关联一只宠物，多只宠物需分别下单；批量下单能力已下线。

### 已发生的代码修改
**无。** 仅新增/修改文档：`docs/下单链路讲解.md`（新建）、`docs/订单模块速查.md`（新建）、`理解路线.md`（恢复卡 + 3 节回填）、`PROGRESS.md`（本文件）。

---

## 8. 下次开始时的第一个动作（更新）

**做第 11 节「AI 摸底提示词」**，用 5 道题从 L1 考到 L4，确认下单链路是否真的到 L3。

考不过就按 `docs/下单链路讲解.md` 的对应节回读，不要往下一条链路（登录/鉴权）走。

修 bug 的优先级仍然高于换链路：**N2（SSE 重连）** 是当前最值得先修的一条。

---

## 7. 暂时不要做的事

1. 不要动 `OrderServiceImpl` 的结构（拆分/重构），先把链路讲清。
2. 不要引入 Redisson / 分布式锁 / 接口限流 —— 项目已明确"不再复杂化"。
3. 不要一次性修 N1–N10，**一次一条**，修完跑一次验证再继续。
4. 不要给 `notification_wsh` 加唯一索引前不查重（虽然当前 0 冲突，但换环境可能不是）。
5. 不要运行全量 `npm run build`（内存压力大）。
6. 不要恢复已删除的管理员客服审核通道、adoption 模块、**安卓端（`android-app/`）**。
7. `docs/archive/` 已于 2026-10-27 删除；需要留档的一次性产物，新建 `docs/archive/<年>-<月>/` 再放，不要丢根目录。
