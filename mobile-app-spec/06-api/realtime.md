# Realtime（实时通信：SSE）

## 现状（EXISTING SocketManager.js）

| 通道 | 端点 | 载荷事件 |
|---|---|---|
| 聊天 | `GET /api/chat-events/stream` | `notification` 风格 JSON 消息 |
| 通知 | `GET /api/notification-events/stream` | 未读/新通知 |
| 订单 | `GET /api/order-events/stream` | 订单状态变更 |

- 鉴权：`?token=<access_token>`（EventSource 无法带 Header [E]）
- 传输：SSE（`text/event-stream`）；不可用/失败 → **降级 30s 轮询**（`transport='polling'`）
- 重连：指数退避 `2s × 1.5^(n-1)`，上限 30s，最多 10 次 → 停在轮询模式
- 生命周期：登录建连、登出断开、token 刷新后用新 token 重连 [E]

## App 实现（RECOMMENDATION，OkHttp SSE）

```kotlin
class SseClient {
  // connect(url, token)：EventSource.Factory
  // onEvent → 解析 JSON → SharedFlow<RealtimeEvent>
  // onClosed → scheduleReconnect()（同上退避）
  // maxRetries=10 → 降级 polling（复用对应列表 API）
}
```

## 消费规则（DERIVED from 前端用法）

| 事件 | UI 反应 |
|---|---|
| order 变更 | 订单列表/详情重拉；Snackbar 提示；Tab 角标更新 |
| notification | 未读数 +1；App 内横幅；可选系统通知 [R] |
| chat 消息 | 消息流追加；会话列表置顶+未读；已读上报 |

## 前台/后台策略 [P]

- 前台：保持 SSE；进入对应页面才订阅对应通道（省电）[R]
- 后台：断开 SSE，靠 FCM/APNs 推送兜底 [R]；回前台重连并拉增量（unread-count）
- 登出/401：立即断开；refresh 成功后带新 token 重连

## 降级一致性

轮询间隔 30s [E]；轮询实现 = 周期调用对应 unread-count / 列表接口；两种模式对 UI 层透明（同一 Flow）。
