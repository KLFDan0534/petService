# page-022 · 错误页与全局状态（Errors & Global States）

## 错误页（EXISTING /403 /404 /500 /503）

| 场景 | 触发 | 移动呈现 |
|---|---|---|
| 403 Forbidden | 路由 meta.roles 不满足 [E] | 全屏 EmptyState：图标+「无权限访问」+ 返回首页 primary |
| 404 NotFound | 未匹配路由 | 「页面不存在」+ 返回首页 |
| 500 ServerError | 服务端异常 | 「服务开小差了」+ 重试 + 返回 |
| 503 ServiceUnavailable | 维护 | 「系统维护中」+ 稍后重试 |

视觉：居中，图标 48（线性）+ displayS 衬线标题 + body 描述 + 操作按钮；画布 background [D from EmptyState]。

## 全局状态映射（EXISTING request.js → 移动语义）

| 错误 | 后端行为 | App UI |
|---|---|---|
| HTTP/`code` 401 | 静默 refresh → 重放；无 token 的 401 仅拒绝 [E] | 用户无感；refresh 失败 → 清登录态 → Login（Snackbar「登录已过期」） |
| 403 | refresh 一次后仍 403 | Snackbar「权限不足」；页面级 → 403 页 |
| 500 | — | Snackbar「服务器错误，请稍后重试」[E 文案] |
| `code≠200` 业务错误 | Result.message | Toast/Snackbar 透传 message [E] |
| 网络失败/超时(30s) | — | 离线横幅 + 原地重试；列表页顶部黄条 [P] |
| 校验错误(4xx) | message | 字段级 error + Snackbar |

## 全局 UI 设施 [P]

- 离线监测：Connectivity → 顶/底横幅「网络不可用」
- 全局 Loading 遮罩仅用于不可取消的同步操作（支付/提交订单）；其余用局部骨架
- 统一重试：`Retry` 按钮 = 重放最后一次失败请求
