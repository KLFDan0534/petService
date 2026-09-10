# page-011 · 商家页（Merchants）

- **Purpose**：商家列表与门店详情（服务聚合）[E /merchants /merchants/:id]
- **Role**：登录（用户区）
- **Entry**：首页/搜索/服务详情；**Exit**：ServiceDetail / 聊天

## Layout

```
列表：MerchantCard（媒体 4:3 + 店名 + 评分 + 营业状态 Badge + 距离）
详情：
├── Hero 媒体（frame 26）
├── 店名（displayL）+ StoreMode/StoreStatus Badge [E] + 营业时间
├── 收藏（FavoriteToggleButton）
├── 该店服务：ServiceGrid 2 列
├── 评价区：RatingCard + 商家回复（reply）
└── 底部操作：联系商家（→Chat 会话）
```

## API（EXISTING MerchantController）

`GET /api/merchants` · `GET /api/merchants/{id}` · `GET /api/merchants/nearby` · 服务 `GET /api/services/merchant/{merchantId}` · 评价 `GET /api/ratings/...` · 营业时间 `GET /api/merchants/{merchantId}/hours`。

## States

关店/休息 → 服务可浏览但下单入口禁用（Badge 说明）[D]；收藏乐观更新。
