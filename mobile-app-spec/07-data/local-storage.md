# Local Storage（本地存储规划）

## Web 现状（EXISTING，语义基准）

| 键 | 内容 |
|---|---|
| `token` | access_token_wsh |
| `refreshToken` | refresh_token_wsh |
| `user` | JSON：id/username/nickname/avatar/roles_wsh |
| `pet-service-theme` | light/dark |

运行时：Pinia（app 主题/Toast/未读数；auth 会话；notification 未读；orderDetail/csChat 页面态）。

## App 存储分层（RECOMMENDATION）

| 层 | 技术 | 内容 |
|---|---|---|
| Secure | EncryptedDataStore / Keychain | access/refresh token、支付密码标记 |
| Preferences | DataStore / UserDefaults | 主题、语言、定位授权状态、引导标记 |
| Database | Room | 列表缓存（服务/订单/会话/通知/流水）、Paging 数据 |
| Memory | StateFlow | 当前页 UI 状态、表单草稿 |

## 缓存策略

| 数据 | 策略 |
|---|---|
| 服务列表/详情 | Room 缓存 + SWR（先缓存后刷新）；TTL 10min [R] |
| 订单列表/详情 | 内存 + 下拉刷新；SSE 失效重拉 |
| 会话/消息 | Room 持久（Paging 3）；进入会话同步已读 |
| 通知 | Room + 未读数 DataStore 同步 |
| 分类/配置（geo config 等） | DataStore，TTL 24h |
| 用户资料 | 登录时写入，refresh/profile 更新 |

## 失效规则

- 登出/refresh 失败：清 Secure 全部 + 导航状态；Room 业务缓存保留（匿名浏览 [E 公开页语义]）
- 角色变化（refresh 返回新 roles）：重建导航图，清角色相关缓存
- 主题跟随系统 + 用户覆盖三态（system/light/dark）[R，兼容 Web 二态语义]

## 禁止

- Token 落明文/日志；缓存含敏感个人信息（实名/手机号）超出会话周期
