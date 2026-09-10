# page-019 · 商家工作台（Merchant Workspace）

- **Purpose**：商家经营域：服务/订单/寄养师/统计/营业时间 [E /merchant/* 动态路由]
- **Role**：MERCHANT（ADMIN 可见）
- **Entry**：我的页工作台入口；**Exit**：订单/服务详情

## Layout（底部/顶部 Tab 切换 [D]）

```
概览：今日订单/收入 stat 卡（GET /api/statistics/merchant）+ 待办（待接单/待审核徽标）
服务管理：我的服务列表（GET /api/services/merchant/{merchantId}）
  ├─ 新建/编辑服务（全屏表单：名称/分类/价格/时段/描述/图片 POST /api/services/{id}/images）
  ├─ 上架/下架 POST /api/services/{id}/toggle-status
  └─ 管理详情 GET /api/services/{serviceId}/manage
订单管理：GET /api/orders/merchant + 状态筛选 → accept/reject/delivered/complete
  （派单：指派寄养师；寄养师管理：/keepers/merchant/{merchantId}
    待审 merchant-approve|reject / 在线状态 / 请假审批 /keeper-leaves）
宠物寄养档案：GET /api/pets/merchant
营业时间：GET/PUT /api/merchants/{merchantId}/hours + 开/关店 store-mode [E]
经营统计：订单/收入/评分图表 [R 移动端用简单 stat+条形]
门店资料：GET/PUT /api/merchants/my
客服团队：申请审批（merchant/applications）+ 员工管理 staff/terminate [E]
```

## 规则

全部操作走既有 API 契约；表格类 Web 页在移动端转为「列表+筛选 Chip + 详情 BottomSheet」[D]；审核类操作必须二次确认 Dialog。
