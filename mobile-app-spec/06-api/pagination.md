# Pagination（分页规范）

## 后端（EXISTING pet-common）

- 请求：`PageRequestDTO`（`current`/`size` + 业务筛选参数；MyBatis-Plus 分页）
- 响应：`PageResult<T>`（`records`/`total`/`size`/`current`，字段名以联调为准）

## App 统一模型（RECOMMENDATION）

```kotlin
data class Page<T>(val items: List<T>, val total: Long, val page: Int, val hasMore: Boolean)
```

## UI 行为

| 场景 | 规格 |
|---|---|
| 首屏 | Loading 骨架；失败见 errors.md |
| 追加 | 底部 24px spinner；触底预加载（剩 3 屏触发）[P] |
| 无更多 | meta 文案「没有更多了」 |
| 空结果 | EmptyState |
| 下拉刷新 | 回到第 1 页；保留筛选条件 |
| 筛选/搜索变更 | 重置页码 + 防抖 300ms |

## 排序与筛选约定（DERIVED）

- 列表默认按 `created_at_wsh` 倒序
- 服务列表：分类（category）+ 关键词 + 排序（距离/评分 [D]）
- 订单列表：状态枚举筛选
- 全部筛选为查询参数，不缓存到全局（会话内保留 [D]）

## 大列表

- 会话/通知/流水：Paging 3 + Room 缓存 [R]
- 管理端列表：搜索 44px + 状态 Chip（page-020）
