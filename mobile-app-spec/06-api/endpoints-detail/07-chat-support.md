# 07 · IM / 投诉 / 客服工作台 / 评价 / 工单

## Chat（页面：page-014）
- `GET /api/chat/conversation` —— 会话列表/会话消息（peer 参数 [D]）
- `POST /api/chat/send` 发送文本消息（乐观上屏+失败重发 [P]）
- `POST /api/chat/assign-agent` 分配客服坐席
- `GET /api/chat/unread` · `GET /api/chat/unread-count` 未读（列表徽标）
- `PATCH /api/chat/read/{messageId}` 单条已读 · `POST /api/chat/read-conversation` 会话已读（进入即上报 [D]）

## ChatEvents
- `GET /api/chat-events/stream?token=` SSE 实时消息（断线退避→轮询 [E]）

## Complaint（状态机 pending→processing→resolved/rejected [E]；页面 page-017 / 021）
- `GET /api/complaints/targets` 可投诉目标（订单/商家/寄养师 [D]）
- `GET /api/complaints/all` 列表（维度按角色）
- `POST /api/complaints` 发起（描述；权限：OWNER）
- `POST /api/complaints/{id}/evidence`（multipart 证据）
- `GET /api/complaints/{id}/evidence` 证据查看
- `GET/POST /api/complaints/{id}/messages` 沟通消息
- `POST /api/complaints/{id}/accept`（受理 → processing）· `/resolve`（→resolved）· `/reject`（→rejected；原因）

## CsWorkbench（页面：page-021；MERCHANT/ADMIN/CS）
- `GET /api/cs-workbench/stats` 工作台统计卡
- `GET /api/cs-workbench/merchants` 管辖商家切换
- `GET /api/cs-workbench/conversations` 会话列表（未读排序 [D]）
- `GET /api/cs-workbench/threads` 工单/投诉线程 · `POST /api/cs-workbench/threads/read` 批量已读

## MerchantCustomerService（客服申请；页面 page-019 / 020）
- `POST /api/merchant-customer-service/applications` 用户申请
- `GET .../applications/me` 我的申请状态（CsApplicationStatus [E]）
- `GET .../merchant/applications/pending` + `POST .../merchant/applications/{id}/approve|reject`（MERCHANT）
- `GET .../merchant/staff` 坐席列表 · `POST .../merchant/staff/{id}/terminate` 终止
- `GET .../admin-list` + `POST .../admin/{id}/approve|reject`（ADMIN）
- `POST .../applications/{id}/resign` 坐席离职

## Rating（页面：page-007 / 011 / 017）
- `GET /api/ratings/my` 我的评价
- `POST /api/ratings`（或订单侧提交端点）评价：星级+内容+图片（completed 后 [E O5]）
- `POST /api/ratings/{id}/reply` 商家回复

## Ticket（状态机 pending→processing→resolved→closed [E]；页面 page-017 / 021）
- `GET /api/tickets/me` 我的工单 · `GET /api/tickets/{id}` 详情
- `POST /api/tickets` 创建（分类 complaint/question/suggestion/other + 优先级 [E]）
- `POST /api/tickets/{id}/assign`（CS 接单 → processing）
- `POST /api/tickets/{id}/resolve` → resolved · `POST /api/tickets/{id}/close` → closed
- `GET/POST /api/tickets/{id}/messages` 沟通
