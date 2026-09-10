# 03 · 寄养师 / 考勤 / 请假 / 资质（Keeper · Attendance · Leave · Qualification）

## Keeper（页面：page-010 / 018 / 019）
- `GET /api/keepers` 寄养师列表（分页；评分/接单数 meta [D]）
- `GET /api/keepers/nearby` 附近（经纬度参数；定位拒绝降级 [D]）
- `GET /api/keepers/{id}` 详情（资质标识/简介/统计）
- `GET /api/keepers/me` · `GET /api/keepers/my-application` 我的资料/申请（KEEPER）
- `GET /api/keepers/pending` 待审核（ADMIN）→ `POST /api/keepers/{id}/approve | /reject`
- `GET /api/keepers/merchant/{merchantId}` 商家在雇（MERCHANT）
- `GET /api/keepers/merchant/pending` 商家待审 → `POST /api/keepers/{id}/merchant-approve | /merchant-reject`
- `POST /api/keepers/{id}/merchant-terminate` 商家终止雇佣
- `POST /api/keepers/{id}/resign` 寄养师离职
- `PATCH /api/keepers/{id}/online-status` 在线状态（body：1|3|4 [E KeeperOnlineStatus]）

## 申请入驻（用户 → 寄养师）
- `POST /api/keepers`（或 /keeper-apply 提交端点，含资质材料 file 引用 [D]）
- 页面：page-018 申请页；状态：KeeperReviewStatus 0/1/2 [E]

## 考勤（页面：page-018）
- `POST /api/keeper-attendance/check-in` —— 上班打卡（可带经纬度 [P·R]）。错误：重复打卡
- `POST /api/keeper-attendance/check-out` —— 下班打卡。错误：未打卡
- `GET /api/keeper-attendance/me/current` 当前状态卡
- `GET /api/keeper-attendance/me/today` 今日记录
- `GET /api/keeper-attendance/merchant/today`（MERCHANT）· `GET /api/keeper-attendance/admin-list`（ADMIN）

## 请假
- `GET /api/keeper-leaves/merchant` · `POST /api/keeper-leaves/merchant` 商家发起/记录
- `PUT /api/keeper-leaves/merchant/{id}` 修改
- `GET /api/keeper-leaves/admin-list`（ADMIN）
- `POST /api/keeper-leaves/{id}/approve | /reject`（LeaveApprovalStatus [E]）

## 资质
- `GET /api/qualifications/{ownerType}/{ownerId}` 资质列表（ownerType：keeper/merchant [D]）
- `GET /api/qualifications/pending` 待审（ADMIN）
- `POST /api/qualifications/{id}/approve | /reject`（原因必填 [D]）
