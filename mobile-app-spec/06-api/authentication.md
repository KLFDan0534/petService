# Authentication（认证）

> 流程全部 EXISTING（AuthController + request.js + auth store）。

## 机制

- **无状态 JWT**：`Authorization: Bearer <access_token_wsh>`（JwtAuthenticationFilter [E]）
- 双 Token：access + refresh；角色在 token 中（`roles_wsh`）
- SSE 鉴权：`?token=<access_token>` query 参数 [E SocketManager]

## 端点（EXISTING /api/auth）

| 端点 | 用途 |
|---|---|
| `POST /register` | 注册（可带验证码） |
| `POST /register/captcha` | 获取验证码 |
| `POST /login` | 登录 → Token 对 + 用户信息 |
| `POST /refresh` | 刷新（Body `refresh_token_wsh`）→ 新 token 对 + 最新 roles |
| `POST /forgot-password` | 找回密码 |
| `POST /logout` | 登出 |

## 登录响应 data（EXISTING auth.setAuth）

```json
{
  "access_token_wsh": "jwt",
  "refresh_token_wsh": "jwt",
  "user_id_wsh": 1,
  "username_wsh": "string",
  "nickname_wsh": "string",
  "avatar_wsh": "url",
  "roles_wsh": ["ROLE_OWNER"]
}
```

## 刷新策略（EXISTING request.js → App 平移）

1. 收到 401（HTTP 或 `code`）且本地有 token → 调 `/auth/refresh`
2. **并发去重**：刷新中其他 401 请求入队等待，成功后统一重放 [E pendingRequests]
3. 刷新成功：更新双 Token + `roles_wsh`；重放原请求
4. 刷新失败：清全部凭据 → 跳登录（带 redirect）；用户无感中断最小化
5. **无 token 的 401**：仅拒绝请求，不强制跳登录（保护公开页浏览 [E]）
6. 启动时检测本地 token 过期（`isTokenExpired`）→ 直接清登录态 [E 路由守卫]
7. 403：尝试刷新一次（`_retry403` 防循环 [E]），仍失败 → Toast「权限不足」

## App 实现要点（RECOMMENDATION）

- Token 存 EncryptedDataStore/Keychain；不写日志
- OkHttp `Authenticator` 实现刷新+重放+并发锁；SSE 连接参数每次用最新 access token
- 登出：`POST /auth/logout` → 清存储 → 清导航栈 → Login
- 多角色：登录后按 `roles_wsh` 装配导航；角色变化在 refresh 时同步 [E]
