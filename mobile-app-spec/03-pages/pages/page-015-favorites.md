# page-015 · 收藏（Favorites）

- **Purpose**：商家/寄养师/服务三类收藏聚合 [E /favorites]
- **Role**：登录（用户区）
- **Entry**：我的页 / 卡片心形按钮

## Layout

```
Segmented/Chip 类型切换（merchant / keeper / service [E FavoriteTargetTypeMap]）
→ 对应卡片列表（MerchantCard / KeeperCard / ServiceCard 2 列）
→ 点击进入详情；卡上心形按钮取消收藏（二次确认 [R]）
```

## API（EXISTING FavoriteController）

`GET /api/favorites/page?targetType=` · `GET /api/favorites/types`（含计数）· `POST /api/favorites/toggle` · `GET /api/favorites/check?targetType&targetId`（详情页回显）。

## 交互

toggle 乐观更新（失败回滚 + Toast [E FavoriteToggleButton 语义]）；空态按类型出文案与引导。
