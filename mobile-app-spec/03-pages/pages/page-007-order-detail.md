# page-007 · 订单详情（OrderDetail）

- **Purpose**：订单全量信息 + 履约追踪 + 操作中枢 [E /orders/:id → OrderDetailView]
- **Role**：登录；数据归属校验（后端 OwnershipValidator [E]）
- **Entry**：订单列表/支付成功/通知深链；**Exit**：聊天/评价/退款/打赏

## Layout

```
状态头：StatusBadge + 状态说明 + 倒计时（pending 支付超时 [E orderPaymentTimeout]）
服务与商家卡 → 商家详情
履约时间线 Timeline（GET /api/order-fulfillments/{orderId}/timeline [E]）
日报区：CareRecord 卡（日期+内容+图片）→ 图片查看器 [E]
宠物信息卡 · 服务地址卡 · 费用明细（单价/券/合计）
操作区（按状态+角色）：
  OWNER: 去支付/取消/确认接收/评价/申请退款/打赏/联系商家(→Chat)
  KEEPER/MERCHANT: 接单/拒单/配送/开始(传图)/完成/发起会话
```

## API（EXISTING）

`GET /api/orders/{id}` · `GET /api/order-fulfillments/{orderId}` · timeline/daily-status · 会话 `/api/order-fulfillments/{orderId}/conversation(+upload)` · 状态流转 `POST /api/orders/accept|reject|delivered|received|start(+upload)|complete` · 评价 `/api/ratings` · 打赏 `POST /api/tips`。

## States / 交互

- 状态机驱动按钮（见 05-business/state-machines.md），禁止前端发明状态
- SSE order-events：状态被对方变更 → 刷新 + Snackbar 提示
- 传图（开始服务/日报）：相机/相册权限 → `POST .../upload` multipart [E]
