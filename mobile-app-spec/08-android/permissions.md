# Android Permissions（系统权限）

> 仅列项目功能实际需要的（RECOMMENDATION，按页面功能推导）。

| 权限 | 用途 | 请求时机 | 被拒行为 |
|---|---|---|---|
| CAMERA | 拍照上传（开始服务图/日报/头像/评价/投诉证据） | 点击相机按钮时 | 提示去设置开启；提供相册替代路径 |
| READ_MEDIA_IMAGES (13+) / READ_EXTERNAL_STORAGE (≤12) | 相册选图上传 | 同上 | 限定系统 Photo Picker [P] |
| ACCESS_FINE/COARSE_LOCATION | 附近商家/寄养师（/nearby）、考勤打点、距离展示 | 首次进入相关页面 | 降级：隐藏距离/附近排序，用默认列表（对齐 Web 降级 [D]）；考勤打点失败提示 |
| POST_NOTIFICATIONS (13+) | SSE 掉线时的推送兜底、订单/消息通知 | 首次登录后 | App 内红点/横幅仍可用（SSE 前台） |
| INTERNET / ACCESS_NETWORK_STATE | 网络与离线检测 | 自动 | — |
| RECORD_AUDIO / BLUETOOTH 等 | **不需要**（项目无对应功能） | — | — |

## 规范

- 运行时请求一律带用途说明（in-context education [P]）
- 权限状态缓存到 Settings；「不再询问」→ 引导系统设置页
- 考勤定位（page-018）：仅 check-in/check-out 时刻前台获取，不做后台追踪 [R·隐私最小化]

## 通知渠道 [R]

`system` / `order` / `notice`（对齐 NotificationTypeMap [E]），重要性分级：order=HIGH、notice=DEFAULT、system=LOW。
