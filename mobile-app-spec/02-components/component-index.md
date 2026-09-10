# Component Index（组件索引）

> 详细解剖见 `../mobile-design-language/03-components.md`（同仓库）。本目录规定组件清单与移动端复用规则。

## 基础组件（Phase 2 必须先实现）

| 组件 | 变体 | 源（EXISTING） |
|---|---|---|
| Button | primary/outline/dark/light/ghost/quiet/danger × sm/md/lg | 设计语言 Button/index.tsx（.cta 家族） |
| IconButton | 44dp 热区 | UserLayout header 控件 |
| Chip | default/active（+count） | Services.vue .s-chip |
| Input | text/search(清除)/password/textarea | Services.vue .s-search + app.css |
| Select | 44px + 底部弹层 [P] | — |
| Switch/Checkbox/Radio | primary 选中 [R] | — |
| Card | content/media/empty/featured | Services.vue .service-card |
| ListItem | leading+title+meta+trailing | 列表页通用 |
| StatusBadge | success/warning/danger/info/primary/neutral | StatusBadge.vue + statusMaps.js |
| Avatar | 图像/字母，36/44 | UserLayout .user-avatar |
| Divider | 1px border | app.css |
| Media | 4:3 + fallback | MediaWithFallback.vue |
| ImageViewer | 全屏查看 [P] | — |

## 反馈组件

LoadingSpinner（32px 圆环）· ShimmerSkeleton（sand 渐变）· EmptyState（图标+衬线标题+描述+操作，虚线变体）· ErrorState（错误页模式）· Toast→Snackbar [P] · AppDialog（5 尺寸档 400/440/520/640/960）· BottomSheet [P] · Reveal（显现动画）· PullRefresh [P]。

## 业务组件（页面间共享）

FavoriteToggleButton（收藏切换，乐观更新）· OrderCard · ServiceCard · PetCard · StatCard（数字衬线+tabular-nums）· SectionHeading（眉题+标题）· TextLink · Timeline（订单时间线）· FilterToolbar · Stepper（数量/日期选择）· AddressPicker（高德选址 → 移动端地图选点 [P]）· ChatBubble · UnreadDot（8px 红点+2px 描边 [E]）。

## 复用规则（RECOMMENDATION → 强制）

1. 先查本索引，存在即复用；重复实现视为缺陷。
2. 组件只引用 Token；不接受裸色值/裸字号 props。
3. 可交互组件必须含 default/pressed/disabled(/loading) 态；数据组件必须含 loading/empty/error 态。
4. 组件文件位置建议见 08-android/project-structure.md。
