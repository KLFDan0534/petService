# page-004 · 服务详情（ServiceDetail）

- **Purpose**：服务完整信息与下单入口 [E /services/:id]
- **Role**：公开浏览；下单需登录（用户区角色）
- **Entry**：Services/首页/收藏/商家页；**Exit**：CreateOrder / MerchantDetail

## Layout

```
Hero 媒体（全宽 4:3+ · radius frame 26 · 可横向多图 + 查看器）
标题区：displayL 衬线标题 · 分类 label · 价格 stat（衬线 tabular）
信息卡（surface+1px line）：商家（头像+名→MerchantDetail）· 营业状态 Badge · 距离
描述区：body 13.5/1.7（富文本按纯文本+结构化渲染 [E 原则]）
评价预览：RatingCard 横滑 → 全部评价
底部操作栏（sticky）：收藏 IconButton + 价格摘要 + 「立即预约」primary lg
```

## API（EXISTING）

`GET /api/services/{id}` 或 `/api/services/{serviceId}/detail` · 时段 `GET /api/services/{serviceId}/availability` · 商家 `GET /api/merchants/{id}` · 评价 `GET /api/ratings/...` · 收藏 `POST /api/favorites/toggle`。

## States / 交互

- 「立即预约」→ CreateOrder（选日期受 availability 约束，禁用非营业日 [E]）
- 未登录点击 → 登录（redirect 回来）
- 收藏失败回滚 + Toast
- 图片失败 fallback 占位（MediaWithFallback [E]）
