# page-020 · 管理后台（Admin Workspace）

- **Purpose**：平台治理：审核/运营/财务 [E /admin/* 动态路由，18+ Web 页]
- **Role**：ADMIN
- **移动端范围**：仪表盘 + 审批类高频操作 [PLATFORM 决策]；重表格页保留 Web [R]

## 移动端包含模块（按 Controller 归组）

```
仪表盘：GET /api/statistics/admin（订单/用户/收入 stat 卡 + 趋势 [R 简化图表]）
审核中心（待办徽标驱动）：
├─ 商家入驻 /api/merchants（pending→approve|reject）
├─ 寄养师资质 /api/qualifications/pending → /{id}/approve|reject
├─ 实名认证 /api/admin/real-name-reviews/{userId}/approve|reject
├─ 内容审核 /api/reviews/pending → approve|reject
├─ 退款仲裁 /api/refunds → approve|complete|reject
├─ 提现审批 /api/withdrawals → approve|reject|complete
└─ CS 申请 /api/merchant-customer-service/admin-list → approve|reject
运营：公告 CRUD /api/notices · Banner · 优惠券模板/发放 /api/coupons/templates
用户与角色：/api/users（封禁 /{id}/status）· /api/roles（RBAC 管理）
钱包调整：POST /api/wallet/admin/adjust
审计：操作日志 /api/operation-logs · 回收站 /api/recycle-bin(restore|delete)
文件：/api/files/admin-list
```

## 规范

统一「待办列表 + 详情 BottomSheet + 通过/拒绝（原因必填）」模式 [D]；全部写操作二次确认；danger 操作用 danger variant；列表分页 + 搜索 44px + 状态 Chip 筛选。
未覆盖的管理表格页 → 记录 unresolved.md，Phase 8 后评估 [R]。
