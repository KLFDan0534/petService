# page-010 · 寄养师市场（Keepers）

- **Purpose**：浏览寄养师并查看资质 [E /keepers /keepers/:id]
- **Role**：登录（用户区）
- **Entry**：首页/搜索；**Exit**：KeeperDetail

## Layout

```
列表：KeeperCard（Avatar 44 + 姓名 + 评分/接单数 meta + 在线状态 Badge [E KeeperOnlineStatus]）
     支持 /keepers/nearby 附近排序（定位授权降级）
详情：头像大图 + 认证标识（资质通过 Badge）+ 简介（body）
     + 评分统计（stat）+ 服务商家隶属（→MerchantDetail）
     + 「预约Ta的服务」→ 关联 ServiceDetail
```

## API（EXISTING KeeperController）

`GET /api/keepers`（分页）· `GET /api/keepers/{id}` · `GET /api/keepers/nearby` · 资质 `GET /api/qualifications/{ownerType}/{ownerId}`。

## States

定位拒绝 → 切默认列表 + Toast 提示 [P]；空态/骨架同标准。公开浏览受限（requiresAuth:true [E]）→ 未登录先登录。
