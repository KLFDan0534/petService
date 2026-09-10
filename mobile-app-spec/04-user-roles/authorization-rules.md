# Authorization Rules（授权实现规则）

## Web 端机制（EXISTING，App 端语义对齐）

1. **路由守卫**：`requiresAuth` + `meta.roles`（any-of 匹配）；失败 → /403 [E]
2. **动态路由注入**：登录后按角色 addRoute；登出 resetRoute [E]
3. **指令级**：`v-permission` 指令 + `hasRole/hasAnyRole/hasPermission` 工具 [E]
4. **后端裁决**：JWT 中的角色 + `@PreAuthorize` + 资源归属校验（OwnershipValidator [E]）——前端检查仅是 UX，不是安全边界

## App 实现（RECOMMENDATION，语义 1:1 平移）

- 启动时从 DataStore 恢复 `user{roles_wsh}`；导航图按角色装配（Navigation Graph 分区）
- 页面级：路由/Tab 按 `roles` 集合注册；无权 → Forbidden 页（page-022）
- 组件级：`PermissionGate(roles=[...])` Composable/Widget 包裹动作按钮
- API 层：401 走 TokenAuthenticator 静默刷新；403 → Snackbar「权限不足」并阻止重试循环（Web `_retry403` [E]）
- **禁止**：仅靠隐藏 UI 实现权限（后端仍会拒绝）；本地伪造角色

## 敏感操作附加规则（DERIVED）

| 操作 | 附加要求 |
|---|---|
| 支付/充值 | 支付密码（`PUT /users/me/payment-password` 设置 [E]）[R 移动端生物识别快捷确认] |
| 退款/提现审批 | 二次确认 Dialog + 原因（reject 必填 [D]） |
| 用户封禁/角色变更 | danger 按钮二次确认 + 操作日志自动记录 [E @LogOperation] |
| 资源访问 | 全部走归属校验，UI 不假设可见即合法 |
