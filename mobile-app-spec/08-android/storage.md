# Android Storage（存储实现）

## 分层（见 07-data/local-storage.md 规划）

| 层 | 实现 |
|---|---|
| Secure | EncryptedDataStore：`token` `refreshToken` `user{roles_wsh}` [E 键语义] |
| Settings | DataStore：theme(system/light/dark)、引导、定位授权标记 |
| Cache | Room：`service_cache` `order_list` `conversation` `message` `notification` `transaction` |
| Paging | RoomRemoteMediator（会话/通知/流水） |

## Room 摘要

- Entity 命名与 API 模型对齐（去 `_wsh` 后缀转 camelCase，DTO 层映射 [D]）
- 索引：conversation(peerId)、message(conversationId,time)、notification(read,created)
- 类型转换：枚举 string、金额 BigDecimal、时间 Instant

## 主题持久化

键 `pet-service-theme` 语义平移 [E]；默认跟随系统；切换即时生效（Compose `QiyuTheme(darkTheme=…)`），200ms 过渡 [E]。

## 清理

- 登出：清 Secure + Settings(保留主题) + Room 业务表 [D]
- 版本升级迁移：Room Migration 强制；缓存表可 fallback 清空
