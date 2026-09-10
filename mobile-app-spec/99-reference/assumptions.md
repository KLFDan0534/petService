# Assumptions（假设记录）

> 以下为 DERIVED/RECOMMENDATION 级别的关键假设，若被推翻需回溯影响面。

| # | 假设 | 依据 | 被推翻时影响 |
|---|---|---|---|
| A1 | 一个账号可持多角色并共存生效 | roles_wsh 为数组 + auth 多角色 computed [E] | 导航装配逻辑 |
| A2 | 订单状态流转仅由后端 Controller 暴露的动作驱动 | Controller 端点语义 [D] | 状态机/按钮白名单 |
| A3 | 公开页（Dashboard/Services/ServiceDetail）允许匿名浏览，受限动作才要求登录 | requiresAuth:false + 401 语义 [E] | 全站守卫 |
| A4 | 钱包为唯一支付通道（无第三方支付证据） | PaymentController + Recharge [E/D] | 支付页/对账 |
| A5 | SSE token 走 query 参数且刷新后需重连 | SocketManager [E] | 实时层 |
| A6 | 列表页 keepAlive 语义 = 返回保留筛选与滚动 | router keepAlive [E] | 导航状态 |
| A7 | 管理端表格页可不移动化（Web 覆盖） | page-020 范围决策 [P] | Phase 8 范围 |
| A8 | Android 技术栈采用 Kotlin/Compose/MVVM | 项目无移动端 → RECOMMENDATION | 全部 08-android |
| A9 | 中文为唯一 UI 语言 | 品牌与页面文案 [E] | i18n 基建 |
| A10 | 上传图片需客户端压缩（Web 无压缩证据，移动网络需要） | 最佳实践 [R] | upload 管道 |
