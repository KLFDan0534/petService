# Source Mapping（结论 → 证据映射）

| 规格结论 | 证据来源 |
|---|---|
| 5 角色与守卫规则 | router/index.js（USER/ACCOUNT/SUPPORT_AREA + 守卫 6 步）· permission.js ROLES |
| 用户模型 id/username/nickname/avatar/roles_wsh | stores/auth.js setAuth() |
| JWT 双 Token + 刷新并发重放 + 403 防循环 + 无 token 401 不跳登录 | utils/request.js 全文 |
| SSE 3 通道 + token query + 退避 2s×1.5ⁿ≤30s + 轮询降级 | utils/SocketManager.js |
| ~151 端点分布（32 Controller） | PROJECT_MAP.md 9.3 + Controller 注解扫描 |
| Order 10 状态 + Badge 类别 | constants/statusMaps.js OrderStatus |
| 其余全部状态机 | statusMaps.js 对应导出 |
| 按钮 42px / Chip·输入 44px 刻意区分 | 设计语言 index.css 审计账本（confirmed across 40 views） |
| 静置 1px 线零阴影 + hover lift | 审计账本 + Services.vue:517-538 |
| 圆角 8/11/18/26/full | 审计账本 confirmed 条目 |
| 动效 150/200/300 + editorial 曲线 | 审计账本 + Reveal.vue EASE |
| 弹窗 Anatomy（scrim/尺寸档/进场） | AppDialog.vue 全文 |
| 主题机制（data-theme + 唯一 Token 源） | stores/app.js 注释与实现 + design-tokens.css |
| 响应式断点 1100/900/768/560/520 | app.css / Services.vue / UserLayout.vue 媒体查询 |
| 产品原则/反面参考/无障碍目标 | PRODUCT.md |
| 55 表/6 队列/软删除/审计注解/自动填充 | PROJECT_MAP.md 模块职责与 9.4 |
| 字体家族（Serif/Sans/Mono） | index.html + 审计账本 --ref-font-* |
