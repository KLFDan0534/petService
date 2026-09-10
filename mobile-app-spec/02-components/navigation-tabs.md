# Navigation, Tabs & Bars（导航体系）

## App 导航结构（DERIVED from router + PLATFORM_ADAPTATION）

```
Splash → (未登录) Login / (已登录) Home
Home（Bottom Navigation 5 Tab）
├── 首页 Home（Dashboard）
├── 服务 Services（市场列表）
├── 宠物 Pets
├── 订单 Orders
└── 我的 Profile
每个 Tab 是独立 Back Stack；再按角色在「我的」暴露工作台入口
```

- Web 顶导航项（EXISTING）：首页/预约服务/我的宠物/订单/收藏/消息/AI助手 → 5 Tab + 我的页聚合 + 服务/消息独立入口 [P]。
- 角色工作台入口（EXISTING 角色路由）：KEEPER→寄养工作台；MERCHANT→商家工作台；CUSTOMER_SERVICE→客服中心；ADMIN→管理后台。

## App Bar（PLATFORM）

- 默认：56dp surface 底 + 标题 displayS·nav 14 w500 + 返回箭头 + 右侧动作（图标 20）。
- 首页：Logo（logo-mark「栖」+ 栖屿宠护 [E]）+ 消息（未读红点 8px+2px 描边 [E MessageIndicator]）+ 主题切换（暗色三态 [E ThemeToggle]）。
- 详情页：大标题页滚动后显示折叠标题（iOS Large Title [P]）。
- 工作台：Tab 行（Chip 式，active=ink 底反白 [E]）或 Segmented。

## Bottom Navigation（PLATFORM_ADAPTATION）

56dp + Safe Area；surface 底 + 上边 1px line；item：图标 20/24 + navigation 14；选中=primary + primaryContainer wash；未选=textTertiary。

## Tab / Segmented

Chip 式（h44 radius 11）用于工作台模块切换；Segmented（muted 轨道+白滑块）用于订单状态切换 [D]。Web 现状：`.user-nav a.active{bg muted; color primary}` [E]。

## 页内导航（EXISTING）

- 面包屑（s-crumb meta 12.5，分隔符 border 色）→ 移动端仅保留返回+标题 [P]。
- 横向 Chip Rail = 分类导航（服务/收藏页）。
- 通知中心 → 通知详情（notices/:id）；公告 PopupNotice 弹窗 [E]。

## 返回与退出（PLATFORM）

登录/注册完成→replace 到 Home；支付成功→replace 订单详情；详情返回保持列表滚动位置（列表缓存 [E keepAlive]→ViewModel 层保留状态 [P]）。
