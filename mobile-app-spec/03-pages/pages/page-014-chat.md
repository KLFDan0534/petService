# page-014 · IM 聊天与 AI 助手（Chat / AI）

- **Purpose**：用户↔商家 IM、订单履约会话、AI 问答 [E /chat /ai /ai/chat]
- **Role**：登录（用户区）
- **Entry**：商家/订单详情「联系」/ 首页入口；**Exit**：—

## 布局

```
会话列表：Avatar + 对方名 + 最后消息 meta + 未读数 Badge + 时间
聊天页：
├── 头部：对方名 + 在线状态（KeeperOnlineStatus [E]）
├── 消息流：ChatBubble（我=primary 底白字右；对方=surface+line 左；系统=居中 meta）
├── 输入区：输入框(44) + 相机（图片消息）+ 发送 primary
└── 已读回执（read/read-conversation [E]）
AI 助手页：
├── 历史会话（GET /api/ai/history/sessions [E]）
├── 问答流（同 Bubble 风格，AI=cream 底）
└── 输入区（RAG 检索 /rag/ask；普通 /api/ai/chat）
```

## API（EXISTING）

会话 `GET /api/chat/conversation?...` · 发送 `POST /api/chat/send` · 未读 `/api/chat/unread(-count)` · 已读 `PATCH .../read/{messageId}` `POST read-conversation`。
订单会话 `GET/POST /api/order-fulfillments/{orderId}/conversation(+upload|read)`。
SSE `GET /api/chat-events/stream?token=<jwt>` 实时收发 [E]；图片消息 `POST .../upload` multipart。
AI：`POST /api/ai/chat` · `GET/DELETE /api/ai/history/sessions[/{sessionId}]` · `POST /api/rag/ask`。

## 规则

发送乐观上屏（失败标红可重发 [P/R]）；进入会话即上报已读；长按消息复制 [R]；AI 回复流式/分段渲染 [D]。
