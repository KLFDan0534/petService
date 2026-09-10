# Architecture Overview

## 后端架构（EXISTING）

```
Vue SPA ──/api──▶ Nginx ──▶ Spring Boot (pet-admin 启动)
                             ├─ pet-security:  JwtAuthenticationFilter（Bearer 无状态）
                             ├─ pet-system:    Auth/User/Role (RBAC)
                             ├─ pet-business:  20 Controller / 7 子域
                             ├─ pet-ai:        Spring AI (OpenAI) RAG/报告/Agent
                             ├─ pet-framework: MyBatis-Plus · RabbitMQ · MinIO
                             └─ MySQL(55 表) · Redis · RabbitMQ(6 队列) · MinIO
```

- 认证：JWT 无状态；`Authorization: Bearer <access_token_wsh>`；刷新 `POST /api/auth/refresh`
- 事件：业务事件 → RabbitMQ → 监听器处理（订单通知/消息/AI 报告/投诉等）
- 实时推送：SSE `GET /api/{chat|notification|order}-events/stream?token=<jwt>`
- 文件：统一上传入口 `POST /api/files/upload`（multipart）→ MinIO，返回文件 id/url
- 审计：`@LogOperation` AOP；软删除 + 回收站恢复

## 前端架构（EXISTING）

- Vue 3 + Pinia + Vue Router 4；请求层 axios 实例（baseURL `/api`，timeout 30s）
- 拦截器：请求注入 Bearer；响应 code=401 → 静默 refresh + 重放（并发去重 pending queue）；403 → refresh 一次后 Toast「权限不足」；code≠200 → error Toast
- 路由：`meta.requiresAuth` + `meta.roles`；登录态存 localStorage；`meta.layout` 区分 user/admin/merchant 壳
- 状态：app(主题/Toast/登录弹窗/未读数) auth(user/token) notification(未读) orderDetail csChat

## Android App 架构（RECOMMENDATION，详见 08-android/）

```
UI (Compose Screen)
  ↓ State ↑ Event
ViewModel (StateFlow<UiState>)
  ↓
UseCase (可选) → Repository (interface)
                  ↓
        Remote: Retrofit/OkHttp (+AuthInterceptor/TokenAuthenticator)
        Local:  Room (缓存) + DataStore (token/主题/设置)
DI: Hilt · 异步: Coroutines/Flow · 分页: Paging 3
```

分层铁律 [RECOMMENDATION]：UI 禁止直接调用 Retrofit；业务逻辑不进 UI；Token 存 DataStore（加密），不进日志。

## 与后端的契约

- 本规格不改后端（原任务约束）；App 全部消费既有 `/api/*` 契约
- 移动端新增项（推送/深链等）均为客户端侧能力，标记 RECOMMENDATION
