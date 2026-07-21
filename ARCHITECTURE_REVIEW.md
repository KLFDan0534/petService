# Pet Service Platform — 体系架构审查与补全设计报告

> 审查日期：2026-07-01  |  审查范围：全栈（38 DB表 / 70+ API / 67 Vue路由 / 9 Maven模块）

---

## 一、项目整体问题总结

### 1.1 总体评分

| 维度 | 评分 | 说明 |
|------|------|------|
| 功能广度 | ★★★★☆ | 基础模块齐全，远超一般创业项目 |
| 业务深度 | ★★★☆☆ | 核心流程有骨架但无血肉（支付/财务/订单均未真正闭环） |
| 架构质量 | ★★★☆☆ | 模块依赖清晰但模块间严重脱节 |
| 数据完整性 | ★★☆☆☆ | 38张表无任何外键，大量缺失索引 |
| 安全/合规 | ★★☆☆☆ | 密码重置完全损坏，资金流转毫无约束 |
| 商业成熟度 | ★★☆☆☆ | 缺少商业化平台必备的营销/会员/风控/对账体系 |

### 1.2 必须立即修复的 Bug（P0）

| # | 问题 | 位置 | 严重程度 |
|---|------|------|----------|
| 1 | **找回密码完全损坏**：提示"邮件已发送"但无邮件基础设施，临时密码明文返回API，前端忽略返回值 | `UserServiceImpl.forgotPassword()` + `ForgetPassword.vue` | **安全/UX崩溃** |
| 2 | **支付不涉及资金变动**：`pay()`仅将状态设为 `success`，不调用 `walletService.deductBalance()` | `PaymentServiceImpl.pay()` | **业务逻辑失效** |
| 3 | **退款不涉及资金退还**：`RefundServiceImpl` 完全不操作钱包，仅是状态变更 | `RefundServiceImpl` 全文件 | **业务逻辑失效** |
| 4 | **打赏不走财务**：`TipServiceImpl.create()` 不调用钱包的任何加/扣款方法 | `TipServiceImpl` | **资金流失** |
| 5 | **订单状态可任意跳转**：`PUT /api/orders/{id}/status` 绕过所有状态机约束 | `OrderController.updateStatus()` | **数据完整性崩溃** |
| 6 | **钱包模块与订单/支付/退款完全解耦**：order 包从无 import finance 包 | 跨模块架构 | **核心断裂** |

---

## 二、缺失模块清单

### 2.1 商业化必备（P0-P1）

| 模块 | 优先级 | 状态 | 说明 |
|------|--------|------|------|
| **真实支付接入** | P0 | ❌ 缺失 | 微信/支付宝支付集成，含回调、签名、对账 |
| **钱包-支付联动** | P0 | ❌ 缺失 | 支付扣款、退款回充、打赏入账、交易流水自动生成 |
| **商品/用品商城** | P1 | ❌ 缺失 | 目前仅有服务（寄养/洗护），无宠物商品购买 |
| **优惠券/促销系统** | P1 | ❌ 缺失 | 满减券、折扣码、新客优惠、节日活动 |
| **会员/订阅体系** | P1 | ❌ 缺失 | 会员等级、储值卡、次卡、月卡（如10次洗护） |
| **服务预约日历** | P1 | ❌ 缺失 | 可视化日历排期，实时看护者/商家忙闲状态 |

### 2.2 增强型功能（P2）

| 模块 | 优先级 | 说明 |
|------|--------|------|
| **商家后台增强** | P2 | 经营报表、服务定价管理、员工排班 |
| **商家下线/暂停** | P2 | 目前无 SUSPENDED/DISABLED 状态 |
| **信用/声誉体系** | P2 | UI有8个字段，后台仅返回4个 |
| **自动好评催评** | P2 | 订单完成后定时提醒评价 |
| **IM增强** | P2 | 聊天消息推送、已读回执、离线消息 |
| **退款自动化** | P2 | 当前仅管理员手动完成，无自动退款 |
| **对账/结算系统** | P2 | 商家/TK（看护者）按周期结算账单 |

### 2.3 运营后台增强（P2-P3）

| 模块 | 优先级 | 说明 |
|------|--------|------|
| **数据看板 ECharts** | P2 | 当前仅数字卡片，无趋势图/饼图/柱状图 |
| **内容管理 CMS** | P2 | 首页Banner管理、帮助中心、FAQ |
| **短信/邮件通知** | P2 | 目前无任何外部通知通道 |
| **风控系统** | P3 | 防刷单、异常交易检测、登录风控 |
| **价格策略引擎** | P3 | 动态定价、淡旺季调价 |

---

## 三、完整业务架构设计

### 3.1 目标架构图（含当前缺失部分）

```
┌──────────────────────────────────────────────────────────────┐
│                       用户层 (H5/Web/小程序)                  │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────┐  ┌───────────┐  ┌───────────────────┐    │
│  │  用户前台       │  │ 商家后台   │  │ 运营管理后台         │    │
│  │  ──────────    │  │ ─────────  │  │ ─────────────      │    │
│  │  · 首页/搜索   │  │ · 服务管理  │  │ · 用户管理          │    │
│  │  · 服务列表    │  │ · 订单管理  │  │ · 商家审核          │    │
│  │  · 下单预约    │  │ · 看护者管理 │  │ · 订单监控          │    │
│  │  · 订单中心    │  │ · 收益统计  │  │ · 退款审核          │    │
│  │  · 宠物管理    │  │ · 经营报表  │  │ · 内容审核          │    │
│  │  · 钱包/提现   │  │ · 排班管理  │  │ · 数据看板          │    │
│  │  · 评价投诉    │  │            │  │ · 系统配置          │    │
│  │  · 消息通知    │  │            │  │ · 权限管理          │    │
│  │  · 会员中心    │  │            │  │ · 风控管理          │    │
│  │  · 积分/优惠券  │  │            │  │ · 结算管理          │    │
│  └──────────────┘  └───────────┘  └───────────────────┘    │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│                       API 网关 / 统一认证                     │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌────────┐  │
│  │用户   │ │商家   │ │订单   │ │财务   │ │商品   │ │ 营销    │  │
│  │服务   │ │服务   │ │服务   │ │服务   │ │服务   │ │ 服务    │  │
│  ├──────┤ ├──────┤ ├──────┤ ├──────┤ ├──────┤ ├────────┤  │
│  │注册   │ │入驻   │ │下单   │ │钱包   │ │商品   │ │优惠券   │  │
│  │登录   │ │资质   │ │支付   │ │交易   │ │分类   │ │积分     │  │
│  │鉴权   │ │审核   │ │退款   │ │提现   │ │库存   │ │会员     │  │
│  │档案   │ │服务   │ │评价   │ │结算   │ │购物车 │ │营销     │  │
│  │地址   │ │排班   │ │投诉   │ │对账   │ │订单   │ │活动     │  │
│  └──────┘ └──────┘ └──────┘ └──────┘ └──────┘ └────────┘  │
│                                                              │
│  ┌────────┐ ┌──────────┐ ┌─────────┐ ┌──────────────┐      │
│  │消息     │ │  AI/智能   │ │  CMS     │ │  基础服务      │      │
│  │服务     │ │  服务      │ │  内容    │ │              │      │
│  ├────────┤ ├──────────┤ ├─────────┤ ├──────────────┤      │
│  │通知     │ │ AI报告    │ │ Banner  │ │  MinIO       │      │
│  │聊天    │ │ RAG知识库  │ │ 公告    │ │  Redis       │      │
│  │站内信   │ │ 智能推荐   │ │ 帮助中心  ││  RabbitMQ     │      │
│  └────────┘ └──────────┘ └─────────┘ └──────────────┘      │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│                    数据层 (MySQL / Redis / MinIO / Chroma)   │
└──────────────────────────────────────────────────────────────┘
```

### 3.2 关键业务流程 —— 目标状态（当前：虚线以内，缺失：全部）

#### 3.2.1 订单生命周期（目标完整状态）

```
                      ┌─────────────┐
                      │  待支付       │ ← 创建订单
                      │  (PENDING)   │
                      └──────┬──────┘
                             │ 支付成功（真实扣款）
                             v
                      ┌─────────────┐
                      │  已支付       │
                      │  (PAID)      │ ← 资金进入平台中间账户
                      └──────┬──────┘
                             │ 商家/看护者接单
                             v
                      ┌─────────────┐
                      │  已确认       │
                      │  (CONFIRMED) │
                      └──────┬──────┘
                             │ 用户送达宠物
                             v
                      ┌─────────────┐
                      │  已送达       │
                      │  (DELIVERED) │
                      └──────┬──────┘
                             │ 商家确认接收（交接码验证）
                             v
                      ┌─────────────┐
                      │  已接收       │
                      │  (RECEIVED)  │
                      └──────┬──────┘
                             │ 开始服务（拍照）
                             v
                      ┌─────────────┐
                      │  服务中       │ ← 每日护理记录/AI
                      │  (IN_PROGRESS)│   报告生成
                      └──────┬──────┘
                             │ 完成服务
                             v
                      ┌─────────────┐
                      │  已完成       │
                      │  (COMPLETED) │ ← 资金解冻→商家入账
                      └──────┬──────┘     ← 自动催评
                             │
                     ┌───────┴───────┐
                     v               v
              ┌──────────┐  ┌──────────────┐
              │  待评价    │  │  申请退款      │
              │          │  │  (REFUNDING)  │
              └──────────┘  └──────┬───────┘
                                   │ 审核通过→原路退回
                                   v
                            ┌──────────────┐
                            │  已退款        │
                            │  (REFUNDED)   │
                            └──────────────┘

        ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ←
        任一步骤（退款除外）均可由用户取消（待平台审核）
        任一步骤均支持发起售后/投诉工单
```

#### 3.2.2 资金流向（目标）

```
用户下单 ──→ 平台中间账户（待支付冻结）
                 │
                 ├── 服务完成 ──→ 平台抽取佣金
                 │                   │
                 │                   ├── 商家结算（按周期）
                 │                   └── 平台毛利
                 │
                 ├── 退款 ──→ 原路返回用户
                 │
                 └── 取消 ──→ 解冻返回用户
```

---

## 四、后端优化建议

### 4.1 架构层面

| 问题 | 建议 | 优先级 |
|------|------|--------|
| 订单/支付/退款/钱包 彻底解耦 | 引入 `PaymentService` 接口，订单层仅依赖接口 -> `WalletPaymentServiceImpl` 实现真实扣款逻辑 | P0 |
| 订单状态无集中管控 | 引入 `StateMachine<OrderStatus, OrderEvent>` 模式或自定义状态机，禁止 `updateStatus()` 后门 | P0 |
| ServiceImpl 直接注入 Mapper | `KeeperController`、`RoleController` 等直接操作 Mapper，应改为全走 Service 层 | P2 |
| 后端无支付网关抽象层 | 设计 `PaymentGateway` 接口（WechatPayGateway / AlipayGateway / MockGateway），支持策略切换 | P0 |
| 地理位置算距在内存中执行 | `GeoDistanceUtils` 是全表加载后内存计算，应改为 MySQL 空间索引 或 引入 ElasticSearch | P2 |
| 无统一分页/排序规范 | 部分 `GET /api/xxx` 返回全量 List，大表时应加 page/size 约束，默认分页 | P2 |
| 查询无缓存策略 | 高频查询（服务分类树、商家列表等）应加 Redis 缓存 + 缓存更新策略 | P2 |

### 4.2 接口/REST 规范

| 问题 | 建议 | 优先级 |
|------|------|--------|
| API URL 命名不一致 | `/api/chat` vs `/api/order-fulfillments` vs `/api/real-name-reviews`，统一 kebab-case | P2 |
| `_wsh` 后缀暴露到前端 | 所有 JSON 返回值含 `_wsh` 后缀，前端被迫使用 `user_wsh.roles_wsh` 访问。考虑在后端序列化时去除或加 `@JsonProperty` 别名 | P3 |
| 部分 Controller 无 @PreAuthorize | `QualificationController`、`RecycleBinController` 缺少权限注解 | P1 |
| `updateStatus()` 后门接口 | 删除或限制为仅内部调用 | P0 |
| 参数校验缺失 | `MerchantCreateRequestDTO` 无任何 `@NotNull`/`@NotBlank`，`@Valid` 无效 | P1 |

### 4.3 安全

| 问题 | 建议 | 优先级 |
|------|------|--------|
| 找回密码返回明文临时密码 | 废弃当前实现，改为真实邮箱验证码 或 短信验证码。移除 `forgotPassword` 返回密码的行为 | P0 |
| Geo API Key 直接暴露给前端 | `GET /api/geo/config` 返回 `apiKey` 和 `securityCode` → 使用代理层，前端不直接调用 AMap | P1 |
| 无登录锁定/频率限制 | 引入登录失败次数限制 + 图形验证码（集成极验或自建） | P2 |
| 无 XSS/SQL注入过滤 | 全局过滤器 + MyBatis-Plus 预编译已部分防护，但建议加统一输入清洗 | P3 |

---

## 五、前端优化建议

### 5.1 架构层面

| 问题 | 建议 | 优先级 |
|------|------|--------|
| 订单详情 Dialog → 独立页面 | `OrderDetailView.vue` 已是独立页面，但 CreateOrder 是 Dialog，应提取为独立路由页面 `/orders/create` | P1 |
| 无 TypeScript | 全部使用 JS，无法获得类型提示，API 响应中的 `_wsh` 字段尤其需要类型约束。建议渐进式迁移 | P2 |
| API 层无请求/响应类型定义 | `api/order.js` 中的 `getOrders()` 返回裸 `res.data`，调用方猜测字段。建议加 JSDoc 类型或 TS | P2 |
| 全局 Toast 在 App.vue 中 | 耦合度高，应提取为独立组件 + Teleport | P2 |
| Vite 配置无打包优化 | 缺少分包策略、gzip、CDN 外部化 | P3 |

### 5.2 页面改进

| 页面 | 问题 | 建议 | 优先级 |
|------|------|------|--------|
| `Dashboard.vue` | 统计区域被注释；无运营 banner 之外的营销内容 | 恢复统计、增加热门服务推荐、附近商家推荐 | P1 |
| `AdminDashboard.vue` | 仅10个数字卡片无图表 | 集成 ECharts，增加营收趋势、订单量趋势、新用户增长 | P2 |
| `Orders.vue` | 列表需手动刷新，无 WebSocket 实时更新 | socket 推送订单状态变更 → 自动刷新卡片状态 | P1 |
| `CreditReputation.vue` | 8个信用字段只填充了4个，其余显示为0/暂无 | 后端补齐信用统计数据，或前端隐藏不存在字段 | P2 |
| `ForgetPassword.vue` | 提示"邮件已发送"但从无发送 | 在修复后端前，改为显示"请联系管理员重置密码" | P0 |
| `Profile.vue` (460行) | 单体文件过长 | 拆分为宠物/订单/钱包/设置等独立子组件 | P2 |
| `MerchantDashboard.vue` | 内容单薄 | 增加服务管理入口、订单趋势、今日数据 | P1 |

### 5.3 路由优化

```
建议的路由结构（新增/调整项标 ★）：

/dashboard                    # 首页
/services/:id                 # 服务详情
/merchants                    # 商家列表
/merchants/:id                # 商家主页 ★（当前缺失）
/keepers                      # 看护者列表
/keepers/:id                  # 看护者主页 ★（当前缺失）
/pets                         # 我的宠物
/pets/:id                     # 宠物详情 ★（当前缺失）
/orders                       # 订单列表
/orders/create                # 创建订单 ★（从Dialog迁移）
/orders/:id                   # 订单详情
/orders/:id/timeline          # 服务时间线 ★（当前在Dialog）
/payments                     # 支付记录
/refunds                      # 退款记录
/wallet                       # 钱包
/withdrawals                  # 提现
/vouchers                     # 优惠券 ★（新增）
/membership                   # 会员中心 ★（新增）
/cart                         # 购物车 ★（新增：用于商品购买）
/products                     # 商品列表 ★（新增）
/products/:id                 # 商品详情 ★（新增）
/addresses                    # 地址管理
/chat                         # 消息
/notifications                # 通知
/tickets                      # 客服工单
/complaints                   # 投诉
/ratings                      # 我的评价
/credit                       # 信用中心
/anomaly                      # 异常报告
/revenue                      # 收益
/favorites                    # 收藏
/files                        # 文件
/profile                      # 个人中心
/profile/phone                # 修改手机
/profile/email                # 修改邮箱
/profile/real-name            # 实名认证
/profile/settings             # 设置 ★（新增）
/merchant                     # 商家后台入口
/merchant/dashboard           # 商家仪表盘
/merchant/services            # 服务管理 ★（当前缺失独立页）
/merchant/products            # 商品管理 ★（新增）
/merchant/orders              # 商家订单
/merchant/keepers             # 看护者管理
/merchant/staff               # 员工管理 ★（新增）
/merchant/schedule            # 排班管理 ★（新增）
/merchant/settlement          # 结算中心 ★（新增）
/merchant/statistics          # 经营报表
/admin                        # 管理后台入口
/admin/dashboard              # 总看板
/admin/users                  # 用户管理
/admin/merchants              # 商家审核
/admin/keepers                # 看护者管理
/admin/orders                 # 订单管理
/admin/refunds                # 退款审核
/admin/complaints             # 投诉处理
/admin/tickets                # 工单管理
/admin/reviews                # 内容审核
/admin/pets                   # 宠物管理
/admin/notices                # 公告管理
/admin/banners                # Banner管理 ★（新增）
/admin/categories             # 品类管理
/admin/service-categories     # 服务分类
/admin/products               # 商品管理 ★（新增）
/admin/vouchers               # 优惠券管理 ★（新增）
/admin/membership             # 会员等级配置 ★（新增）
/admin/settlement             # 结算管理 ★（新增）
/admin/withdrawals            # 提现审核
/admin/wallets                # 钱包管理
/admin/transactions           # 交易记录
/admin/roles                  # 角色管理
/admin/qualifications         # 资质审核
/admin/operation-logs         # 操作日志
/admin/recycle-bin            # 回收站
/admin/rag                    # 知识库管理
/admin/system-config          # 系统配置 ★（新增：全局参数设置）
```

---

## 六、数据库设计建议

### 6.1 新增表

```sql
-- ============================================================
-- 1. 商品/产品表 ★ P1
-- ============================================================
CREATE TABLE product_wsh (
    id_wsh              BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id_wsh     BIGINT NOT NULL COMMENT '商家ID',
    category_id_wsh     BIGINT COMMENT '分类ID',
    name_wsh            VARCHAR(200) NOT NULL COMMENT '商品名称',
    description_wsh     TEXT COMMENT '商品描述',
    price_wsh           DECIMAL(10,2) NOT NULL COMMENT '原价',
    discount_price_wsh  DECIMAL(10,2) COMMENT '折扣价',
    stock_wsh           INT DEFAULT 0 COMMENT '库存',
    images_wsh          TEXT COMMENT '商品图片(JSON数组)',
    specs_wsh           TEXT COMMENT '规格(JSON: 如颜色/尺寸)',
    status_wsh          TINYINT DEFAULT 1 COMMENT '1=上架 0=下架',
    sort_order_wsh      INT DEFAULT 0,
    deleted_wsh         TINYINT DEFAULT 0,
    created_at_wsh      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at_wsh      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_merchant (merchant_id_wsh),
    INDEX idx_category (category_id_wsh),
    INDEX idx_status (status_wsh)
) COMMENT '商品表';

-- ============================================================
-- 2. 购物车表 ★ P1
-- ============================================================
CREATE TABLE cart_wsh (
    id_wsh              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id_wsh         BIGINT NOT NULL,
    product_id_wsh      BIGINT NOT NULL COMMENT '商品ID',
    service_item_id_wsh BIGINT COMMENT '服务ID（商品和服务共用购物车）',
    quantity_wsh        INT DEFAULT 1,
    selected_wsh        TINYINT DEFAULT 1 COMMENT '是否选中',
    deleted_wsh         TINYINT DEFAULT 0,
    created_at_wsh      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at_wsh      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_product (user_id_wsh, product_id_wsh),
    INDEX idx_user (user_id_wsh)
) COMMENT '购物车表';

-- ============================================================
-- 3. 优惠券表 ★ P1
-- ============================================================
CREATE TABLE coupon_wsh (
    id_wsh              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name_wsh            VARCHAR(100) NOT NULL COMMENT '优惠券名称',
    type_wsh            VARCHAR(20) NOT NULL COMMENT '类型: full_reduce/discount/cash',
    condition_amount_wsh DECIMAL(10,2) COMMENT '满减条件(0=无门槛)',
    discount_amount_wsh  DECIMAL(10,2) COMMENT '减免金额/折扣金额',
    discount_rate_wsh    DECIMAL(3,2) COMMENT '折扣率(如0.80=8折)',
    total_count_wsh     INT DEFAULT 0 COMMENT '发行总量(0=不限)',
    remain_count_wsh    INT DEFAULT 0 COMMENT '剩余数量',
    per_user_limit_wsh  INT DEFAULT 1 COMMENT '每人限领',
    start_time_wsh      DATETIME COMMENT '有效期开始',
    end_time_wsh        DATETIME COMMENT '有效期结束',
    scope_type_wsh      VARCHAR(20) DEFAULT 'ALL' COMMENT 'ALL/MERCHANT/CATEGORY/PRODUCT',
    scope_value_wsh     VARCHAR(500) COMMENT '适用范围的ID(JSON数组)',
    status_wsh          TINYINT DEFAULT 1 COMMENT '1=启用 0=停用',
    deleted_wsh         TINYINT DEFAULT 0,
    created_at_wsh      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at_wsh      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '优惠券表';

-- ============================================================
-- 4. 用户优惠券 ★ P1
-- ============================================================
CREATE TABLE user_coupon_wsh (
    id_wsh              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id_wsh         BIGINT NOT NULL,
    coupon_id_wsh       BIGINT NOT NULL,
    order_id_wsh        BIGINT COMMENT '使用的订单ID',
    status_wsh          VARCHAR(20) DEFAULT 'unused' COMMENT 'unused/used/expired',
    used_at_wsh         DATETIME COMMENT '使用时间',
    expired_at_wsh      DATETIME,
    deleted_wsh         TINYINT DEFAULT 0,
    created_at_wsh      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id_wsh),
    INDEX idx_status (status_wsh)
) COMMENT '用户优惠券表';

-- ============================================================
-- 5. 会员等级表 ★ P1
-- ============================================================
CREATE TABLE member_level_wsh (
    id_wsh              BIGINT AUTO_INCREMENT PRIMARY KEY,
    level_wsh           INT NOT NULL UNIQUE COMMENT '等级数字(1,2,3...)',
    name_wsh            VARCHAR(50) NOT NULL COMMENT '等级名称(普通/银卡/金卡/钻石)',
    min_exp_wsh         INT DEFAULT 0 COMMENT '所需经验值',
    discount_rate_wsh   DECIMAL(3,2) DEFAULT 1.00 COMMENT '服务折扣率',
    benefits_wsh        TEXT COMMENT '权益描述(JSON)',
    deleted_wsh         TINYINT DEFAULT 0,
    created_at_wsh      DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '会员等级表';

-- ============================================================
-- 6. 用户会员表 ★ P1
-- ============================================================
CREATE TABLE user_member_wsh (
    id_wsh              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id_wsh         BIGINT NOT NULL UNIQUE,
    level_id_wsh        BIGINT NOT NULL,
    exp_wsh             INT DEFAULT 0 COMMENT '当前经验值',
    total_spent_wsh     DECIMAL(12,2) DEFAULT 0 COMMENT '累计消费',
    join_at_wsh         DATETIME COMMENT '成为会员时间',
    expire_at_wsh       DATETIME COMMENT '会员到期时间',
    deleted_wsh         TINYINT DEFAULT 0,
    created_at_wsh      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at_wsh      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id_wsh)
) COMMENT '用户会员表';

-- ============================================================
-- 7. 商家结算表 ★ P2
-- ============================================================
CREATE TABLE settlement_wsh (
    id_wsh              BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id_wsh     BIGINT NOT NULL,
    period_start_wsh    DATE NOT NULL COMMENT '结算周期开始',
    period_end_wsh      DATE NOT NULL COMMENT '结算周期结束',
    total_amount_wsh    DECIMAL(12,2) DEFAULT 0 COMMENT '订单总额',
    commission_wsh      DECIMAL(12,2) DEFAULT 0 COMMENT '平台佣金',
    actual_amount_wsh   DECIMAL(12,2) DEFAULT 0 COMMENT '实际结算',
    status_wsh          VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/confirmed/paid',
    paid_at_wsh         DATETIME COMMENT '结算时间',
    deleted_wsh         TINYINT DEFAULT 0,
    created_at_wsh      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at_wsh      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_merchant (merchant_id_wsh),
    INDEX idx_status (status_wsh)
) COMMENT '商家结算表';

-- ============================================================
-- 8. 预约表 ★ P1
-- ============================================================
CREATE TABLE appointment_wsh (
    id_wsh              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id_wsh         BIGINT NOT NULL COMMENT '用户ID',
    merchant_id_wsh     BIGINT NOT NULL COMMENT '商家ID',
    keeper_id_wsh       BIGINT COMMENT '看护者ID',
    service_id_wsh      BIGINT COMMENT '服务ID',
    order_id_wsh        BIGINT COMMENT '关联订单ID(下单后填充)',
    appointment_date_wsh DATE NOT NULL COMMENT '预约日期',
    appointment_time_wsh TIME COMMENT '预约时间段',
    status_wsh          VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/confirmed/cancelled/completed',
    remark_wsh          TEXT,
    deleted_wsh         TINYINT DEFAULT 0,
    created_at_wsh      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at_wsh      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id_wsh),
    INDEX idx_keeper_date (keeper_id_wsh, appointment_date_wsh),
    INDEX idx_merchant_date (merchant_id_wsh, appointment_date_wsh)
) COMMENT '预约表';

-- ============================================================
-- 9. 系统配置表 ★ P2
-- ============================================================
CREATE TABLE system_config_wsh (
    id_wsh              BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key_wsh      VARCHAR(100) NOT NULL UNIQUE,
    config_value_wsh    TEXT NOT NULL,
    description_wsh     VARCHAR(500),
    deleted_wsh         TINYINT DEFAULT 0,
    created_at_wsh      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at_wsh      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '系统配置表';
```

### 6.2 现有表关键修改

| 表 | 修改 | 优先级 |
|----|------|--------|
| `wallet_wsh` | 加 `UNIQUE KEY uk_user (user_id_wsh)` | **P0** |
| `payment_wsh` | 加 `UNIQUE KEY uk_pay_no (pay_no_wsh)` | P1 |
| `pet_order_wsh` | 加 `coupon_id_wsh` / `discount_detail_wsh` 字段 | P1 |
| `merchant_wsh` | 加 `commission_rate_wsh`(平台佣金比例) / `status_wsh` 加 `3=SUSPENDED` | P1 |
| `merchant_wsh` | 加 `auto_approve_wsh` 开启自动通过模式 | P2 |

### 6.3 必须补充的索引（现有表）

| 表 | 索引 | 优先级 |
|----|------|--------|
| `pet_wsh` | `INDEX idx_owner (owner_id_wsh)` | **P0** |
| `address_wsh` | `INDEX idx_user (user_id_wsh)` | **P0** |
| `pet_order_wsh` | `INDEX idx_owner (owner_id_wsh)`, `idx_keeper (keeper_id_wsh)`, `idx_merchant (merchant_id_wsh)`, `idx_pet (pet_id_wsh)`, `idx_status (status_wsh)`, `idx_merchant_status (merchant_id_wsh, status_wsh)` | **P0** |
| `chat_message_wsh` | `INDEX idx_from_to (from_user_id_wsh, to_user_id_wsh)`, `idx_order (order_id_wsh)` | **P0** |
| `rating_wsh` | `INDEX idx_target (target_id_wsh, target_type_wsh)` | **P1** |
| `ticket_message_wsh` | `INDEX idx_ticket (ticket_id_wsh)` | **P1** |
| `payment_wsh` | `INDEX idx_order (order_id_wsh)` | **P1** |
| `refund_wsh` | `INDEX idx_order (order_id_wsh)` | P1 |
| `wallet_transaction_wsh` | `INDEX idx_wallet (wallet_id_wsh)`, `idx_user (user_id_wsh)` | P1 |
| `withdrawal_wsh` | `INDEX idx_user (user_id_wsh)` | P1 |
| `ai_report_wsh` | `INDEX idx_order (order_id_wsh)` | P2 |

---

## 七、API 接口设计清单

### 7.1 新增接口

```
### 商品（P1）
GET    /api/products                    # 商品列表（分页+分类+关键词）
GET    /api/products/{id}               # 商品详情
POST   /api/products                    # 创建商品（MERCHANT）
PUT    /api/products/{id}               # 更新商品（MERCHANT）
DELETE /api/products/{id}               # 删除商品（MERCHANT）
POST   /api/products/{id}/toggle-status # 上下架（MERCHANT）

### 购物车（P1）
GET    /api/cart                        # 购物车列表
POST   /api/cart                        # 加入购物车
PUT    /api/cart/{id}                   # 修改数量
DELETE /api/cart/{id}                   # 删除购物车项
POST   /api/cart/clear                  # 清空购物车

### 优惠券（P1）
GET    /api/coupons                     # 可领取优惠券列表（PUBLIC）
GET    /api/coupons/available           # 我的可用优惠券
GET    /api/coupons/mine                # 我的优惠券（全部）
POST   /api/coupons/{id}/claim          # 领取优惠券
POST   /api/coupons                     # 创建优惠券（ADMIN）
PUT    /api/coupons/{id}                # 更新优惠券（ADMIN）
DELETE /api/coupons/{id}                # 删除优惠券（ADMIN）

### 会员（P1）
GET    /api/membership/my               # 我的会员信息
GET    /api/membership/levels           # 会员等级列表
POST   /api/membership/renew            # 续费/开通会员
GET    /api/membership/benefits         # 我的权益
POST   /api/membership/exchange         # 积分兑换
GET    /api/membership/levels           # 等级列表（ADMIN）
POST   /api/membership/levels           # 创建等级（ADMIN）

### 预约（P1）
GET    /api/appointments                # 我的预约
GET    /api/appointments/merchant       # 商家预约列表（MERCHANT）
POST   /api/appointments                # 创建预约
POST   /api/appointments/{id}/confirm   # 确认预约（MERCHANT）
POST   /api/appointments/{id}/cancel    # 取消预约
GET    /api/appointments/slots          # 可预约时间段

### 结算（P2）
GET    /api/settlements/merchant        # 商家结算记录
GET    /api/settlements/{id}            # 结算详情
GET    /api/settlements/admin           # 结算管理（ADMIN）
POST   /api/settlements/{id}/pay        # 标记已结算（ADMIN）

### 系统配置（P2）
GET    /api/admin/configs               # 配置列表
GET    /api/admin/configs/{key}         # 单个配置
PUT    /api/admin/configs/{key}         # 更新配置

### 支付优化（P0）
POST   /api/payments/create             # 创建支付（含钱包扣款）
POST   /api/payments/gateway            # 调用支付网关
POST   /api/payments/gateway/callback   # 支付网关回调
GET    /api/payments/gateway/query      # 支付结果查询（对账用）

### 通知优化（P1）
POST   /api/admin/sms/send              # 发送短信（ADMIN）
POST   /api/admin/email/send            # 发送邮件（ADMIN）
```

### 7.2 需要重构的现有接口

| 接口 | 问题 | 改造方案 |
|------|------|----------|
| `PUT /api/orders/{id}/status` | 绕过状态机 | 废弃，改为各业务语义接口 |
| `POST /api/payments/pay` | 不扣款 | 改为真实扣款流程 + 支付网关 |
| `POST /api/refunds/{id}/complete` | 不退钱 | 改为原路退回+钱包回充 |
| `POST /api/tips` | 不走钱包 | 增加钱包扣款逻辑 |
| `GET /api/notifications/read-all` | stub | 实现批量已读逻辑 |
| `POST /api/auth/forgot-password` | 返回明文密码 | 改为验证码流程 |

---

## 八、开发优先级路线图

### Phase 1: 核心修复（P0）→ 2-3周

```
┌─────────────────────────────────────────────────────────────────┐
│ Phase 1 - 止血（P0 🚨）                                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│ [1] 钱包-支付联动                                                  │
│     ├─ PaymentServiceImpl.pay() → walletService.deductBalance()   │
│     ├─ RefundServiceImpl.complete() → walletService.addBalance()  │
│     ├─ TipServiceImpl.create() → walletService.addBalance()       │
│     ├─ Transaction 自动打日志                                      │
│     └─ wallet_wsh 加 UNIQUE(user_id_wsh)                          │
│                                                                  │
│ [2] 订单状态机重构                                                  │
│     ├─ 废弃 updateStatus() 后门                                    │
│     ├─ 引入 OrderStateMachine 模式                                 │
│     └─ 所有状态转换走统一校验入口                                    │
│                                                                  │
│ [3] 找回密码修复                                                   │
│     ├─ 删除明文返回逻辑                                            │
│     ├─ 引入短信验证码 或 邮件验证码 基础设施                            │
│     └─ 前端提示匹配实际行为                                         │
│                                                                  │
│ [4] 关键索引补充                                                   │
│     ├─ pet_wsh.owner_id_wsh                                       │
│     ├─ address_wsh.user_id_wsh                                    │
│     ├─ pet_order_wsh 6个复合索引                                    │
│     └─ chat_message_wsh 2个索引                                    │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Phase 2: 商业化补齐（P1）→ 4-6周

```
┌─────────────────────────────────────────────────────────────────┐
│ Phase 2 - 商业化核心（P1 💰）                                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│ [5] 商品/用品模块                                                  │
│     ├─ product_wsh / cart_wsh 表创建                              │
│     ├─ 商品 CRUD + 分类 + 图片                                    │
│     ├─ 购物车 API                                                │
│     ├─ 商品下单（复用订单系统或新建商品订单）                             │
│     └─ 前端商品/购物车页面                                          │
│                                                                  │
│ [6] 优惠券/促销系统                                                 │
│     ├─ coupon_wsh / user_coupon_wsh 表创建                         │
│     ├─ 优惠券模板管理（ADMIN）                                      │
│     ├─ 用户领取/使用优惠券                                          │
│     ├─ 下单时应用优惠券（Discount计算）                                │
│     └─ 前端优惠券中心/我的优惠券                                      │
│                                                                  │
│ [7] 会员/等级体系                                                   │
│     ├─ member_level_wsh / user_member_wsh 表创建                    │
│     ├─ 等级自动升降级（基于消费/经验值）                                 │
│     ├─ 会员折扣应用到服务价格                                        │
│     └─ 前端会员中心                                                   │
│                                                                  │
│ [8] 预约日历系统                                                    │
│     ├─ appointment_wsh 表创建                                       │
│     ├─ 可视化时间段选择                                              │
│     ├─ 看护者/商家忙闲状态实时同步                                    │
│     └─ 前端日历组件集成                                              │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Phase 3: 体验增强（P1-P2）→ 3-4周

```
┌─────────────────────────────────────────────────────────────────┐
│ Phase 3 - 体验增强（P1-P2 🚀）                                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│ [9] 商家后台增强                                                  │
│     ├─ 经营报表（ECharts折线图/饼图）                               │
│     ├─ 服务定价管理                                                │
│     ├─ 员工排班                                                    │
│     ├─ 商家 SUSPENDED 状态 + 自动下线机制                             │
│     └─ MERCHANT 角色自动分配                                        │
│                                                                  │
│ [10] 真实支付网关接入                                              │
│     ├─ PaymentGateway 接口抽象                                      │
│     ├─ 微信支付 Native/JSAPI 集成                                  │
│     ├─ 支付宝当面付/网站支付集成                                     │
│     ├─ 异步回调处理 + 幂等                                          │
│     └─ 支付结果前端实时展示                                          │
│                                                                  │
│ [11] 通知基础设施                                                  │
│     ├─ 短信服务（阿里云/腾讯云短信）                                  │
│     ├─ 邮件服务（JavaMailSender）                                   │
│     ├─ WebSocket 强推（当前 polling 改为 real push）                │
│     └─ 通知模板 + 频率控制                                          │
│                                                                  │
│ [12] 前端体验升级                                                  │
│     ├─ CreateOrder Dialog → 独立页面                                │
│     ├─ 搜索页 + 筛选器                                              │
│     ├─ WebSocket 通知自动更新                                       │
│     └─ 移动端适配（H5 / 小程序）                                     │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Phase 4: 运营/风控（P2-P3）→ 3-4周

```
┌─────────────────────────────────────────────────────────────────┐
│ Phase 4 - 运营后台（P2-P3 ⚙️）                                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│ [13] 结算/对账系统                                                │
│     ├─ settlement_wsh 表创建                                       │
│     ├─ 按周/双周/月自动生成结算单                                    │
│     ├─ 平台佣金计算                                                │
│     └─ 商家结算记录查询                                             │
│                                                                  │
│ [14] 数据看板 + ECharts                                           │
│     ├─ 营收趋势（日/周/月）                                        │
│     ├─ 用户增长曲线                                                │
│     ├─ 订单量趋势 + 服务分布                                       │
│     └─ 商家排名/Top N 看护者                                       │
│                                                                  │
│ [15] 风控/安全                                                    │
│     ├─ 登录失败锁定                                                │
│     ├─ 图形验证码                                                  │
│     ├─ 操作频率限制                                                │
│     ├─ Geo API 前端代理层                                           │
│     └─ 敏感操作日志增强                                             │
│                                                                  │
│ [16] 内容管理 CMS                                                 │
│     ├─ Banner管理                                                  │
│     ├─ 帮助中心/FAQ                                                │
│     ├─ 隐私政策/用户协议管理                                        │
│     └─ 首页推荐位管理                                              │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Phase 5: 架构优化（P3）→ 2-3周

```
┌─────────────────────────────────────────────────────────────────┐
│ Phase 5 - 架构优化（P3 🏗️）                                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│ [17] 搜索优化                                                    │
│     ├─ ElasticSearch 集成                                         │
│     ├─ 全文搜索（商家/服务/商品）                                   │
│     ├─ 地理空间搜索（ES Geo Distance）                              │
│     └─ 搜索建议/热词                                              │
│                                                                  │
│ [18] 缓存策略                                                    │
│     ├─ Redis 缓存服务分类树                                         │
│     ├─ Redis 缓存热门商家/服务                                     │
│     ├─ 缓存穿透/击穿/雪崩防护                                      │
│     └─ 缓存更新事件通知                                            │
│                                                                  │
│ [19] 代码质量                                                    │
│     ├─ Service 层全面覆盖（移除 Controller 直接注入 Mapper）          │
│     ├─ _wsh 后缀 API 迁移（@JsonProperty 别名）                     │
│     ├─ 统一分页规范                                                │
│     └─ 集成测试覆盖核心流程                                          │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 附录 A：当前系统已具备的优势

本报告指出了大量问题，但必须承认该项目已有相当的完成度：

| 优势 | 说明 |
|------|------|
| 模块结构清晰 | 7 个 Maven 模块，依赖链明确 |
| 角色权限完善 | 5 种角色 + Spring Security + @PreAuthorize + v-permission |
| 前端组件化良好 | 67 个 Vue 页面 + 30+ 复用组件 + Pinia 6 个 store |
| 系统功能齐全 | 评价/投诉/工单/通知/聊天/文件/回收站/日志等辅助系统完整 |
| AI 集成深度 | DeepSeek LLM + Chroma RAG + 规则引擎 Agent |
| 数据库设计合理 | 38 张表覆盖核心业务领域，命名统一 |
| 订单状态丰富 | 10 种状态，覆盖了商业场景的绝大部分 |
| 钱包功能已就绪 | Wallet 模块功能完整（加/扣/冻结/解冻），仅未接入支付 |
| 地理位置支持 | AMap API + 距离计算 + 附近搜索 |
| 消息队列引入 | RabbitMQ 6 队列用于异步解耦 |
