# Android Authentication（认证实现）

> 流程契约见 06-api/authentication.md（EXISTING）；本文件为实现要点。

## 启动流程

```
App 启动
├─ TokenStore 读取 access/refresh
├─ access 过期判断（本地 JWT exp [E isTokenExpired 语义]）
│   ├─ 有效 → 恢复 user（含 roles_wsh）→ 装配导航 → Main
│   └─ 无效 → 静默 /auth/refresh → 成功续期进 Main / 失败清态进 Login
└─ 无 token → Login（公开页仍可浏览 [E 语义]）
```

## 关键实现

- TokenStore：EncryptedDataStore；`StateFlow<AuthState>`；写操作原子化
- 刷新并发锁：synchronized + 队列等待（对齐 Web pendingRequests [E]）
- 刷新成功：更新 token 对 + **roles_wsh** → 若角色集变化，重建 NavGraph [E]
- 登出：`POST /auth/logout` → 清 Secure + 导航栈回 Login；SSE 断开
- 登录弹窗语义（LoginPromptDialog [E]）：公开页触发受限动作 → 跳 Login 并带 redirect，成功后回原目标

## 安全

- 密码输入：`PasswordVisualTransformation` + 可见性切换 [E 页面语义]
- 支付确认：支付密码（已设置 [E]）+ 生物识别快捷（BiometricPrompt，本地凭据校验后仍需服务端验证）[R]
- JWT 不落日志；崩溃上报脱敏 [R]
