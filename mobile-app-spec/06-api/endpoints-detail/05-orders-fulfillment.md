# 05 · 订单 / 履约（Order · OrderFulfillment · OrderEvents）

## Order 列表（页面：page-006 / 019）
- `GET /api/orders`（我的订单，OWNER 视角，状态筛选 query）
- `GET /api/orders/merchant`（MERCHANT/ADMIN）· `GET /api/orders/my-keeper`（KEEPER）
- `GET /api/orders/pending` —— 可抢新单池（KEEPER，page-018）

## 创建（页面：page-005）
- `POST /api/orders/batch` —— 批量下单
  请求：`service_id_wsh, pet_ids_wsh[], address_id_wsh, start_date_wsh, end_date_wsh, remark_wsh?, coupon_id_wsh?`
  前置：`POST /api/coupons/quote` 与 availability 校验
  响应：订单（status=pending）+ order_no
  错误：时段不可约 · 券不可用 · 余额/参数校验

## 详情与状态机（页面：page-007；状态机见 05-business/state-machines.md）
- `GET /api/orders/{id}` 详情
- `POST /api/orders/cancel`（pending；OWNER，原因 [E CancelOrderDialog]）→ cancelled
- `POST /api/orders/accept`（MERCHANT/KEEPER；paid→confirmed）
- `POST /api/orders/reject`（accept 前；→cancelled）
- `POST /api/orders/delivered`（confirmed→delivered）
- `POST /api/orders/received`（OWNER 确认；delivered→received）
- `POST /api/orders/start`（received→in_progress）· `POST /api/orders/start/upload`（multipart 传图 [E]）
- `POST /api/orders/complete`（in_progress→completed）
- `GET /api/orders/{id}/status` 轮询状态（兜底）

## 履约（页面：page-007 / 014）
- `GET /api/order-fulfillments/{orderId}` 履约概览（timeline/daily 汇总）
- `GET /api/order-fulfillments/{orderId}/timeline` 时间线
- `GET /api/order-fulfillments/{orderId}/daily-status` 日报列表
- `POST /api/order-fulfillments/{orderId}/timeline/upload`（multipart）
- `GET /api/order-fulfillments/{orderId}/conversation` 订单会话 · `POST .../conversation` 发消息
- `POST /api/order-fulfillments/{orderId}/conversation/upload`（multipart 图片消息）
- `POST /api/order-fulfillments/{orderId}/conversation/read` 已读

## 实时
- `GET /api/order-events/stream?token=` SSE（事件 → 重拉列表/详情 [E]）
