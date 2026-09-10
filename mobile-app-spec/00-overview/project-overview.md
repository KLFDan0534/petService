# Project Overview

## 仓库结构（EXISTING）

```
petService/
├── frontend/                 # Vue 3 SPA（用户端/商家端/管理端/客服端，65 页面）
│   ├── src/api/              # 40+ API 封装（axios → /api）
│   ├── src/views/            # user/27 · merchant/ · admin/ · cs/ · error/4
│   ├── src/components/       # common 基础组件 + 布局 + 业务组件
│   ├── src/stores/           # Pinia: app/auth/notification/orderDetail/csChat/design
│   ├── src/router/index.js   # 783 行：静态路由 + 角色动态路由 + 全局守卫
│   └── src/assets/css/       # design-tokens.css + app.css
├── 设计语言/                  # React+Tailwind 设计语言镜像库（审计账本）★PRIMARY DESIGN SOURCE
├── mobile-design-language/    # 本次提取的移动端设计语言规格
├── pet-common/               # Result/PageResult/异常/状态枚举(OrderStatus等) 11 文件
├── pet-framework/            # MyBatis-Plus/RabbitMQ(1 exchange+6 队列)/MinIO/SPA 转发
├── pet-security/             # SecurityConfig/JwtUtil/JwtAuthenticationFilter（无状态 JWT）
├── pet-system/               # Auth/User/Role（14 API）
├── pet-business/             # 7 子域：宠物/寄养/订单/客服/财务/运营/AI（137 API）
├── pet-ai/                   # RAG/AI 报告/Agent（10 API）
├── pet-admin/                # 启动入口 + Admin 统计（2 API）
├── database/                 # 建库/测试/修复 SQL（55 表 schema 见 PROJECT_MAP）
└── docs/                     # api/ security/ architecture/ 文档
```

## 关键数字（EXISTING）

- Controllers 32 · API 端点 ~151 · 数据库表 55 · 前端页面 65 · 公共组件 20+
- MQ：1 个 Direct Exchange + 6 个队列（订单/消息/AI 报告/投诉等异步事件）
- 实时通道：3 个 SSE 流（chat-events / notification-events / order-events），失败降级 30s 轮询

## 全局代码约定（EXISTING）

| 约定 | 说明 |
|---|---|
| 字段后缀 `_wsh` | 所有 API 字段：`id_wsh`、`roles_wsh`、`created_at_wsh`… |
| Token 键 | access=`token`、refresh=`refreshToken`、用户=`user`（localStorage） |
| 角色 | 后端带 `ROLE_` 前缀；前端统一 `replace('ROLE_','')` → `OWNER/KEEPER/MERCHANT/ADMIN/CUSTOMER_SERVICE` |
| 软删除 | `deleted_wsh` + MyBatis-Plus 逻辑删除 + 回收站（RecycleBinController） |
| 审计 | `@LogOperation` AOP → `operation_log_wsh` 表 |
| 自动填充 | `MetaObjectHandler` 填 `created_at_wsh/updated_at_wsh` |

## 移动端范围决策（PLATFORM_ADAPTATION）

| 端 | 移动端范围 | 理由 |
|---|---|---|
| OWNER | **完整实现**（App 主场景） | C 端高频 |
| KEEPER | **完整实现**（工作台） | 移动作业场景 |
| MERCHANT | **完整实现**（工作台） | 经营场景 |
| CUSTOMER_SERVICE | 移动实现核心工作台（会话/工单/投诉）[R] | 桌面优先，移动覆盖核心 |
| ADMIN | 移动实现仪表盘+审核类操作 [R] | 表格密集型功能保留 Web |
