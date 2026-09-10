# Mobile App Specification · 栖屿宠护（petService）

> 本文件夹是把 petService 项目（Web 前端 + Java 后端）逆向提取成的**移动端产品开发规格包**。
> 目标读者：AI Coding Agent / Android 开发者。读完即可开发完整的 Android App，无需重新扫描原项目。

## 1. 这是什么项目

**栖屿宠护（Pet Service Platform）**：宠物寄养/照护服务平台，连接宠物主人（OWNER）与商家（MERCHANT）、寄养师（KEEPER），支持服务浏览→下单→支付→寄养履约（签到/日报/聊天）→完成→评价→退款的全流程，并配有客服（CUSTOMER_SERVICE）与管理员（ADMIN）后台。

## 2. 移动端 App 是什么

将现有 Web 端（Vue 3 SPA，65 个页面）转换为 Android 原生 App。视觉与交互语言来自项目内已固化的 **Warm Editorial 设计语言**（详见 `01-design-system/` 与 `../mobile-design-language/`）。App 覆盖 5 个角色的核心使用场景（管理后台功能以平板/移动适配为主，见 `03-pages/page-020`）。

## 3. 角色（EXISTING，来自 router/index.js + permission.js）

| 角色 | 代码值 | 职责 |
|---|---|---|
| 宠物主人 | `OWNER` | 浏览/下单/支付/评价/售后 |
| 寄养师 | `KEEPER` | 接单、配送、服务执行、日报、考勤 |
| 商家 | `MERCHANT` | 服务管理、订单履约、寄养师审核、客服 |
| 客服 | `CUSTOMER_SERVICE` | 工单/投诉/会话工作台 |
| 管理员 | `ADMIN` | 全平台审核、运营、财务 |

## 4. 核心功能

服务市场（浏览/筛选/收藏）· 宠物档案 · 下单+优惠券 · 支付（钱包/充值）· 订单全生命周期 · 订单履约时间线+日报+图片 · IM 聊天+AI 助手 · 评价/回复 · 退款/投诉/工单 · 钱包/提现 · 会员/优惠券模板 · 通知（SSE 实时+轮询兜底）· 各角色工作台。

## 5. 技术架构

**后端（EXISTING）**：Spring Boot 3.2.5 + Spring Security（无状态 JWT）+ MyBatis-Plus 3.5.7 + MySQL（55 表，字段后缀 `_wsh`）+ RabbitMQ（6 队列）+ Redis + MinIO（文件）+ Spring AI（RAG/Agent/报告）。
**后端响应约定（EXISTING）**：`Result<T>{code,message,data}`、`PageResult<T>`、`BusinessException`+全局异常处理、软删除 `deleted_wsh`、自动填充 `created_at_wsh/updated_at_wsh`。
**Android（RECOMMENDATION）**：Kotlin + Jetpack Compose + MVVM/Clean + Coroutines/Flow + Retrofit/OkHttp + Room/DataStore + Hilt，详见 `08-android/`。

## 6. API 与设计语言来源

- **API**：从 32 个 Controller 提取，~151 个端点，清单见 `06-api/endpoints.md`；认证见 `06-api/authentication.md`（JWT 双 Token：`access_token_wsh`/`refresh_token_wsh`/`roles_wsh`）。
- **设计语言**：项目 `设计语言/` 文件夹（React 镜像库+审计账本）+ `frontend/src/assets/css/design-tokens.css` + `frontend/src/views/user/Services.vue`（视觉权威页）。Primary Design Source 提取结果已固化在 `../mobile-design-language/`，本包 `01-design-system/` 为其自包含摘要。

## 7. 如何理解这个规格包

```
00-overview   → 项目/产品/架构/术语（先读）
01-design-system → 视觉与动效规则（编码依据）
02-components → 组件规范（复用，禁止重复实现）
03-pages      → 每个页面的布局/状态/API/权限
04-user-roles → 角色×页面×API 权限矩阵
05-business   → 业务规则与状态机（Order 等）
06-api        → 端点/请求响应模型/错误/分页/上传/实时
07-data       → 实体/枚举/本地存储
08-android    → Android 架构与实现规范
09-development→ 开发计划/顺序/测试/安全
10-ai-development → AI 编码总入口（system-instructions.md 必读）
11-cross-platform → Flutter / React Native 平台映射（可选技术线）
99-reference  → 来源映射/未解决项/假设/冲突
```

> 端点逐域详单见 `06-api/endpoints-detail/`（01-auth-users … 10-ai，逐端点含权限/请求/响应/错误/页面映射）。
> Android 骨架工程（Phase 1-2 对应代码）见仓库根 `android-app/`，其实现以本规格包为唯一依据。


## 8. 证据等级（全文适用）

**EXISTING** = 项目明确存在 · **DERIVED** = 多处规律归纳 · **PLATFORM_ADAPTATION** = 移动端平台适配 · **RECOMMENDATION** = 为完整性建议（非项目事实）。禁止把推测伪装成项目规范。

## 9. 阅读顺序建议

overview → design-system → components → roles → business → api → data → pages → android → development → ai-development。

---

# For AI Coding Agents

Before writing code:

1. Read `README.md`
2. Read `00-overview/`
3. Read `01-design-system/`
4. Read `02-components/`
5. Read `03-pages/`（含每页规范）
6. Read `04-user-roles/`
7. Read `05-business/`（状态机必须先懂）
8. Read `06-api/`
9. Read `07-data/`
10. Read `08-android/`
11. Read `09-development/`
12. Read `10-ai-development/system-instructions.md`（Master Development Context）

Do not start implementation before understanding: **Design System · Roles · Permissions · API · Navigation · Business Rules**。

# AI Implementation Entry Point

- **第一步**：读 `10-ai-development/system-instructions.md` 与 `03-pages/navigation-map.md`，建立全局地图。
- **第二步**：读 `01-design-system/tokens.md` + `02-components/`，先实现 Design System 层（Theme + 基础组件），这是唯一允许先于页面写的 UI 代码。
- **第三步**：按 `09-development/implementation-order.md` 的 Phase 顺序逐页开发；每页先查 `03-pages/pages/` 规范，再查 `06-api/` 与 `04-user-roles/`。
- **必须遵守**：只用 Token 不硬编码；UI→ViewModel→Repository→API 分层；所有页面实现 Loading/Empty/Error/Success 四态；遵循状态机，禁止发明新状态。
- **禁止**：重新设计 UI / 创造新颜色间距圆角 / 绕过 Token / 页面直接调 Retrofit / 修改后端契约 / 把业务逻辑写进 UI。
- **遇到规格未覆盖的情况**：查 `99-reference/unresolved.md`；仍无答案则在 `10-ai-development/implementation-rules.md` 的默认规则下保守实现并记录。
