# page-001 · 登录 / 注册 / 找回密码（Auth）

- **Purpose**：账号进入与找回 [EXISTING 路由]
- **Role**：公开；已登录访问 → 重定向 Dashboard [E]
- **Entry**：受保护页 redirect / LoginPromptDialog；**Exit**：Dashboard（replace）
- **Navigation**：三页互链；登录页含协议入口 [R]

## Layout（Warm Editorial 应用）

- 画布 background；顶部 Logo（logo-mark「栖」+ 品牌名，displayL）
- 卡片式表单：surface + 1px line + radius 18 + padding 24/24/28
- 字段：用户名/手机号/邮箱、密码（可见性切换）、（注册）确认密码、（找回）验证码
- 主按钮 lg 全宽 primary；次操作 TextLink（ghost）
- 错误：字段下方 12px error 文案 + 输入框 error 边

## API（EXISTING AuthController）

| 动作 | 端点 | Body |
|---|---|---|
| 注册 | `POST /api/auth/register` | 用户资料（+captcha） |
| 获取验证码 | `POST /api/auth/register/captcha` | 账号标识 |
| 登录 | `POST /api/auth/login` | 凭据 |
| 找回 | `POST /api/auth/forgot-password` | 账号+验证码+新密码 |
| 登出 | `POST /api/auth/logout` | — |

**成功响应 data**：`access_token_wsh` `refresh_token_wsh` `user_id_wsh` `username_wsh` `nickname_wsh` `avatar_wsh` `roles_wsh[]` [E auth.setAuth]。App 侧存 DataStore（见 07-data/local-storage.md）。

## States

Submitting（按钮 spinner）→ 失败：字段级错误 / Snackbar；成功：注入角色 → replace Dashboard。
未登录 401 拦截：无 token 的 401 不强制跳登录（保护公开浏览 [E]）——App 同语义：公开页不弹登录。

## Android/iOS

密码管理器集成 [R]；键盘类型随字段；返回手势在 Auth 流中禁用滑返（iOS）[P]。
