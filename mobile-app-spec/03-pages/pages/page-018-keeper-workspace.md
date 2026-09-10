# page-018 · 寄养师工作台（Keeper Workspace）

- **Purpose**：寄养师作业闭环：申请→接单→配送→照护→完成 [E /keeper-workflow /keeper/profile /keeper-apply]
- **Role**：KEEPER（工作台）；申请页对所有用户区角色开放；KEEPER|ADMIN 进入 workflow [E]
- **Entry**：我的页工作台入口；**Exit**：订单详情

## Layout

```
申请页 KeeperApply：资质表单（材料上传 POST /api/files/upload）→ 提交 Qualification
  → 状态追踪（pending→approved/rejected [E]）→ 我的申请 /keepers/my-application
工作台（Chip Tab 切换 [D]）：
├── 今日概览：考勤状态卡（当前状态 GET /keeper-attendance/me/current + 今日记录 /me/today）
│   └─ 上班 check-in / 下班 check-out（定位打点 [P·R]）
├── 可抢订单：GET /api/orders/pending → accept / reject
├── 我的任务：GET /api/orders/my-keeper → 按状态操作
│   （delivered 配送→received 接收→start(传图)→in_progress→complete）
├── 在线状态：PATCH /keepers/{id}/online-status（1 在线/3 离线/4 忙碌 [E]）
└── 个人资料：/keeper/profile（GET /api/keepers/me）
```

## API 汇总（EXISTING）

`POST /api/keeper-attendance/check-in|check-out` · `GET /me/current|me/today` · 日报 `POST /api/care-records/upload`（multipart）· 履约会话 `.../conversation` · 离职 `POST /keepers/{id}/resign`。

## 规则

- 未 check-in 时接单操作提示 [D]；任务操作严格走订单状态机
- 日报必须当日可传图；照片压缩后上传 [P·R]
- 全程 SSE order-events 驱动任务刷新
