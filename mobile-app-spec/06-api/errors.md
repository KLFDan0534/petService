# Errors（错误处理规范）

## 错误来源（EXISTING）

1. HTTP 层：网络失败 / 超时（Web timeout 30s [E]）/ 5xx
2. 响应 `code`：401 认证 · 403 权限 · 其他业务码（`Result.code≠200`）
3. `GlobalExceptionHandler` + `BusinessException`：业务错误 message 面向用户 [E]

## 统一映射（Web request.js 语义 → App）

| 错误 | 处理 | App UI |
|---|---|---|
| 无网络/超时 | 拒绝并 reject | 离线横幅 + 原地重试；列表保留缓存数据 |
| 401（有 token） | 静默 refresh + 重放（并发排队）[E] | 无感；失败 → 清登录态 → Login + Snackbar「登录已过期」 |
| 401（无 token） | 仅拒绝 [E] | 公开页不弹登录；受限动作 → Login |
| 403 | refresh 一次（防循环 [E `_retry403`]）→ 仍失败 | Snackbar「权限不足」；页面级 → 403 页 |
| 500 | — | Snackbar「服务器错误，请稍后重试」[E 文案] |
| code≠200 | message 透传 | Snackbar(message, error)；表单内联时字段级显示 |
| 数据解析失败 | — | ErrorState（数据异常）+ 重试 |

## 页面级状态策略

| 场景 | 策略 |
|---|---|
| 首屏加载失败 | 全屏 ErrorState（图标+说明+重试按钮） |
| 列表加载失败 | 保留旧数据 + 顶部错误条 + 重试 |
| 提交失败 | Snackbar + 表单保留输入 |
| 静默刷新失败 | 下次操作再试，不强制登出（除 401 链路） |

## 重试规则 [P]

- 只重试幂等 GET；POST 失败需用户手动重提（防重复下单/支付）
- 自动重试：仅 SSE 重连（指数退避 2s×1.5ⁿ ≤30s，10 次后转轮询 [E]）
- Snackbar 可携带「重试」动作重放最后失败请求

## 禁止

- 吞掉异常不提示；把原始堆栈展示给用户；对业务 message 二次包装改写
