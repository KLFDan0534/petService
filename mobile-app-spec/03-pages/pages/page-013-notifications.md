# page-013 · 消息通知（Notifications / Notices）

- **Purpose**：系统/订单/公告消息中心 [E /notifications /notices/:id]
- **Role**：登录（ACCOUNT_AREA）
- **Entry**：首页红点 / Tab 我的；**Exit**：NoticeDetail / OrderDetail（深链跳转）

## Layout

```
列表：ListItem（类型 Badge：system/order/notice [E NotificationTypeMap]
      + 标题 + 时间 meta + 未读圆点 8px）
      「全部已读」quiet 按钮（POST /read-all）
详情：公告正文（纯文本/结构化渲染）+ 关联跳转
全局弹窗：启动 PopupNotice（重要公告，dismiss 上报 [E]）
```

## API（EXISTING）

`GET /api/notifications/unread-count` · `PATCH /api/notifications/{id}/read` · `POST /api/notifications/read-all` · 公告 `GET /api/notices/active|unread|popup` · `POST /api/notices/{id}/read|dismiss-popup`。

## 实时（EXISTING）

SSE `GET /api/notification-events/stream?token=<jwt>`：`notification` 事件 → 更新未读数 + 应用内提示（Snackbar/Banner [P]）+ 可选系统推送 [R]。
断线：指数退避重连（2s×1.5ⁿ，≤30s，10 次）→ 失败转 30s 轮询 [E SocketManager 语义]。

## 交互

左滑标记已读 [P/R]；点击跳转按类型路由（order→OrderDetail）；红点未读=warning 色。
