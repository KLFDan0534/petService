# 01 · 认证 / 用户 / 角色（Auth · User · Role · RealName）

## POST /api/auth/register —— 注册
权限：公开。页面：page-001
请求：`username_wsh, password_wsh, nickname_wsh?, captcha_wsh?`（需验证码时先取码）
响应：同登录 data（token 对 + user + roles_wsh [E]）
错误：用户名已存在 · 验证码错误/过期

## POST /api/auth/register/captcha —— 获取注册验证码
权限：公开。请求：账号标识（手机/邮箱）。响应：发送结果。错误：频控（429 语义）

## POST /api/auth/login —— 登录
权限：公开。页面：page-001
请求：`username_wsh, password_wsh`
响应 data [E]：`access_token_wsh, refresh_token_wsh, user_id_wsh, username_wsh, nickname_wsh, avatar_wsh, roles_wsh[]`
错误：账号或密码错误 · 封禁（UserStatus=0 → message 透传）

## POST /api/auth/refresh —— 刷新令牌
权限：持 refreshToken。请求：`refresh_token_wsh`。响应：同登录 data（roles_wsh 可能更新 → 重建导航 [E]）。错误：refresh 无效/过期 → 清登录态

## POST /api/auth/forgot-password —— 找回密码
权限：公开。请求：账号 + 验证码 + 新密码。响应：成功标志。错误：验证码错误

## POST /api/auth/logout —— 登出
权限：登录。响应：成功标志。App：清存储 → Login

## GET /api/users/me —— 我的资料
权限：登录。响应：User 模型（data-models.md）。页面：page-012

## PUT /api/users/me —— 更新资料
请求：`nickname_wsh, avatar_wsh` 等。错误：校验失败

## PUT /api/users/me/phone —— 换绑手机
请求：新手机 + 验证码。页面：/profile/phone

## PUT /api/users/me/email —— 换绑邮箱
请求：新邮箱 + 验证码。页面：/profile/email

## PUT /api/users/me/real-name —— 提交实名认证
请求：`real_name_wsh, id_no_wsh, 证件图 file`。响应：进入 RealNameStatus=1 审核中 [E]。页面：/profile/real-name

## PUT /api/users/me/payment-password —— 设置/修改支付密码
请求：`old_password_wsh?, new_password_wsh, confirm_wsh`。页面：/profile/payment-password

## POST /api/users/me/passwords/reset-all —— 重置全部密码
权限：登录（安全操作）。请求：验证凭据。错误：验证失败

## PATCH /api/users/{id}/status —— 封禁/解封
权限：ADMIN。请求：`status_wsh: 0|1` [E UserStatus]。错误：不可封禁管理员

## 角色管理（base /api/roles，ADMIN）
- `GET /api/roles` 角色列表 · `POST /api/roles` 创建 · `PUT/DELETE /api/roles/{id}`
- `GET /api/users/{userId}/roles` · `PUT /api/users/{userId}/roles` 分配角色（RBAC [E]）

## 实名审核（base /api/admin/real-name-reviews，ADMIN）
- `POST .../{userId}/approve` · `POST .../{userId}/reject`（原因）[E]
