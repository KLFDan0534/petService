# API Overview

## 总量（EXISTING PROJECT_MAP）

32 Controllers · ~151 端点 · 全部走 `/api` 前缀（axios baseURL；App 端用环境化 BASE_URL）。

## 模块分布（EXISTING）

| 模块 | Controllers | 端点数 |
|---|---|---|
| pet-system（Auth/User/Role） | 3 | 14 |
| pet-business 宠物域 | 3 | 14 |
| pet-business 寄养域 | 5 | 32 |
| pet-business 订单域 | 4 | 22 |
| pet-business 客服域 | 4 | 22 |
| pet-business 财务域 | 3 | 13 |
| pet-business 运营域 | 7 | 22 |
| pet-ai | 3 | 10 |
| pet-admin（Statistics） | 1 | 2 |

## App 端点使用优先级（RECOMMENDATION）

- **P0（MVP）**：Auth、User(me)、Pet、ServiceItem(public/detail/availability)、Order 核心、Payment、Wallet、Notification、File/upload、Chat、SSE×3
- **P1**：Coupon、Membership、Favorite、Rating、Refund、Tip、Address、Keeper 浏览/工作台
- **P2**：Merchant 工作台、CsWorkbench、Ticket、Complaint、Qualification
- **P3（ADMIN 移动裁剪）**：Statistics、各审核、Notice、RecycleBin、OperationLog

## 端点速查

完整清单（Controller → base path → 方法路径）见 `endpoints.md`；认证见 `authentication.md`；通用契约见 `common.md`；模型见 `data-models.md`；错误见 `errors.md`；分页见 `pagination.md`；上传见 `upload.md`；实时见 `realtime.md`。

## 版本与兼容

- 后端无版本前缀（`/api` 直接暴露）[E]；App 端以响应 `code` 契约对接，契约变更需后端同步（本规格不改后端）
- 字段命名：一律 `_wsh` 后缀；时间 `created_at_wsh/updated_at_wsh` 自动填充 [E]
