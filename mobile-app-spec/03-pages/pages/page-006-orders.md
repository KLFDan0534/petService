# page-006 · 订单列表（Orders）

- **Purpose**：我的全部订单，按状态筛选追踪 [E /orders]
- **Role**：登录（用户区 OWNER/KEEPER/MERCHANT/ADMIN）
- **Entry**：Tab「订单」；**Exit**：OrderDetail / 评价 / 支付

## Layout

```
App Bar 标题「订单」
Segmented 状态筛选：全部 · 待付款 · 待送达 · 服务中 · 已完成 · 售后 [D from OrderStatus]
List → OrderCard：
├── 头行：商家名 + StatusBadge（statusMaps [E]）
├── 服务摘要（媒体缩略 44 + 名称 + 日期时段）
├── 金额（stat 衬线 tabular）
└── 操作行（上边 1px line）：按状态出按钮
    pending→去支付/取消 · delivered→确认接收 · completed→评价/再来一单 · 等
下拉刷新 + 分页（底部 spinner）
```

## API（EXISTING）

`GET /api/orders/...`（我的订单分页；keeper 维度 `/orders/my-keeper`；商家 `/orders/merchant`；可抢 `/orders/pending` [E]）· 取消 `POST /api/orders/cancel` · 确认 `POST /api/orders/received`。

## States

keepAlive 语义 → 返回保留滚动与筛选（ViewModel 缓存 [P]）；状态变化由 SSE order-events 触发刷新 [E]；空态分状态文案（「暂无待付款订单」等）。
取消订单 → CancelOrderDialog（原因必填，二次确认）[E]。
