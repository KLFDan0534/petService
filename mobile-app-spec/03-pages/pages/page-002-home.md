# page-002 · 首页（Home / Dashboard）

- **Purpose**：平台门面：Banner、服务入口、推荐、公告 [E Dashboard.vue]
- **Role**：公开（部分区块登录后个性化）
- **Entry**：启动默认页 / Tab「首页」；**Exit**：各列表与详情

## Layout

- 顶栏：Logo（「栖」mark + 品牌名）+ 消息图标（未读红点）+ 主题切换 [E]
- BannerCarousel（轮播图 [E]）
- Eyebrow + displayL 标题区
- 服务宫格 ServiceGrid（2 列卡片，媒体 4:3 + label + 标题 + 价格）[E]
- 推荐区块（SectionHeading + 卡片横滑/纵列）
- 未读公告 → PopupNotice 弹窗（可 dismiss [E]）

## API（EXISTING）

| 数据 | 端点 |
|---|---|
| Banner | 公告/横幅接口（NoticeController active/popup） |
| 服务列表 | `GET /api/services/public`（分页+分类） |
| 附近商家 | `GET /api/merchants/nearby`（定位） |
| 未读公告 | `GET /api/notices/unread` · 弹窗 `GET /api/notices/popup` |
| 未读消息数 | `GET /api/notifications/unread-count` |
| 用户统计 | `GET /api/statistics/user`（登录） |

## States / 交互

- 首载骨架屏（Banner+宫格 shimmer）；下拉刷新 [P]
- 定位拒绝 → 附近区块降级为热门 [R]
- 消息红点实时：SSE notification-events + 轮询兜底 [E]
