# Error Handling（错误处理实现总纲）

> 语义契约见 06-api/errors.md；本文件是代码落点。

## 异常类型

```kotlin
sealed class AppError {
  object Network / Timeout / Offline
  data class Auth(401 链路终态)
  data class Forbidden
  data class Server(code,message)
  data class Business(code,message)   // message 直接展示 [E]
  data class Parse / Unknown
}
```

## 展示决策树

```
Repository 捕获 → 映射 AppError → UiState.Error
├─ 页面首屏 → ErrorState 全屏 + Retry
├─ 列表追加 → 底部错误条 + Retry
├─ 提交动作 → Snackbar(message)（message=服务端 [E]）
├─ 静默操作 → 记录 + 可选轻提示
└─ 401 终态 → 全局：清登录态 → Login（Snackbar「登录已过期」）
```

## 组件职责

| 层 | 责任 |
|---|---|
| TokenAuthenticator | 401 刷新重放、并发锁、终态广播 |
| Repository | code≠200 → Business(message)；解析失败 → Parse |
| ViewModel | AppError → UiState；保留上帧数据（列表） |
| UI | 只渲染，不自定文案覆盖服务端 message |

## 特例

- 支付/提交订单失败：表单页保留 + 输入不丢
- SSE 断线：非错误提示（静默降级轮询 [E]）
- 404 实体：详情页 ErrorState「内容不存在」+ 返回
