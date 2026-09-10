# page-012 · 个人中心（Profile / 地址 / 文件）

- **Purpose**：账户信息、安全设置、地址簿、我的文件 [E /profile(+子页) /addresses /files]
- **Role**：登录（ACCOUNT_AREA：含 CUSTOMER_SERVICE [E]）
- **Entry**：Tab「我的」；**Exit**：各子页

## Layout（我的页：聚合列表）

```
头部：Avatar 44（字母/图）+ nickname + 角色 Badge + 编辑
资产行卡：钱包余额(stat) · 优惠券 · 会员（横排 3 stat）
功能列表（ListItem 分组卡）：
  我的订单 · 收藏 · 地址管理 · 我的评价 · 退款/售后 · 投诉 · 工单 · 信用分
  消息通知 · 我的文件 · 收入中心(revenue) · 异常记录(anomaly)
  角色工作台入口（KEEPER/MERCHANT/CS/ADMIN 动态）
  AI 助手 · 申请成为寄养师 / 客服
设置组：主题切换（跟随系统/明/暗 [E]）· 修改资料 · 退出登录（danger 确认）
```

## 子页（EXISTING）

- 资料编辑：`PUT /api/users/me`；换手机 `PUT /users/me/phone`（验证码）；换邮箱 `PUT /users/me/email`；实名 `PUT /users/me/real-name`（→ RealNameReview 审核）；支付密码 `PUT /users/me/payment-password`。
- 地址簿：`GET/POST/PUT/DELETE /api/addresses` + `/default` 设默认；地址行=地图选点（高德 [P]）。
- 我的文件：`GET /api/files`（我的上传列表）+ 预览/删除。

## States

退出登录：清 DataStore + 清角色路由栈 → Login [E clearAuth 语义]。
