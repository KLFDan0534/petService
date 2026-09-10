# Android Networking（网络层）

## 组成（RECOMMENDATION，语义对齐 request.js [E]）

```
OkHttp
├── AuthInterceptor      注入 Bearer access_token（存在时）
├── TokenAuthenticator   401 → 刷新（并发锁+队列等待）→ 重放；失败 → 登出事件
├── LoggingInterceptor   仅 debug；禁止打印 token/密码
└── 超时 30s [E]
Retrofit + kotlinx.serialization（ignoreUnknownKeys=true [D 数据异常策略]）
```

## 响应解包

```kotlin
suspend fun <T> api(call: ApiResponse<T>): T =
    if (call.code == 200) call.data ?: error(...)
    else throw ApiException(call.code, call.message)   // message 可直接展示 [E]
// 401/403 交由 Authenticator/拦截器链 [E 语义]
```

## SSE

OkHttp `EventSource`（见 06-api/realtime.md）：3 通道、`?token=`、指数退避、降级轮询、登录建连/登出断开/换 token 重连。

## 其他

- BASE_URL：flavor 注入（dev/prod）
- 重试：仅 GET 自动 1 次 [R]；POST 不自动（幂等护栏，见 06-api/errors.md）
- 文件上传：独立 Retrofit service（带进度回调）
- 弱网：列表请求取消策略——新请求取消旧请求（筛选场景）
