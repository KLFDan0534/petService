# page-021 · 客服中心（Support Workspace）

- **Purpose**：客服坐席工作台：会话/工单/投诉 [E /merchant/support/*（MERCHANT|ADMIN|CS）+ CsWorkbench]
- **Role**：CUSTOMER_SERVICE（MERCHANT/ADMIN 可进）
- **Entry**：我的页「客服中心」；**Exit**：工单详情/会话页

## Layout

```
工作台首页：GET /api/cs-workbench/stats → 待接待/今日会话/工单 stat 卡
Tab 切换 [D]：
├── 会话：GET /api/cs-workbench/conversations（未读徽标排序）→ 聊天页（同 page-014 结构）
├── 工单：GET /api/tickets/... → 详情（状态/优先级 Badge [E]）
│   动作：assign / resolve / close + 消息往来 GET/POST /tickets/{id}/messages
├── 投诉：GET /api/complaints/all → 详情（证据查看 /{id}/evidence）
│   动作：accept(受理) / resolve / reject + 消息 /{id}/messages
└── 商家维度：GET /api/cs-workbench/merchants（管辖商家切换 [D]）
```

## API（EXISTING）

CsWorkbenchController（stats/merchants/conversations/threads/threads-read）· TicketController · ComplaintController · 消息通道同 ChatController。

## 规则

工单状态机 pending→processing→resolved→closed；投诉 pending→processing→resolved/rejected（见 05-business）。
坐席在线状态与分配由后端 assign-agent [E]；移动端复用聊天组件，禁止另写消息渲染。
