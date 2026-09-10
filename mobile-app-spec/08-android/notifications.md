# Android Notifications（通知实现）

## 数据源

1. **应用内（前台主通道）**：SSE notification-events [E] → 未读数 StateFlow → 顶栏红点（8px dot+2px 描边 [E]）+ App 内横幅
2. **系统推送（兜底/后台）**：FCM [RECOMMENDATION]——后端当前无推送网关（unresolved），Phase 评估后接入

## 展示映射（EXISTING NotificationTypeMap）

| 类型 | 渠道 | 点击路由 |
|---|---|---|
| order 订单 | order(HIGH) | qiyu://order/{id} → OrderDetail |
| notice 公告 | notice(DEFAULT) | NoticeDetail / 弹窗 |
| system 系统 | system(LOW) | Notifications |

## 行为规范

- 前台收到：不弹系统通知，Snackbar/Banner [P]
- 角标：未读数 = `/notifications/unread-count` 初始化 + SSE 增量 [E]
- 已读：进入详情 → `PATCH /{id}/read`；批量 `/read-all` [E]
- 聊天消息：独立未读计数（/chat/unread-count [E]）；进会话即清
- 免打扰：夜间静默 [R]
