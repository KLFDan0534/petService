# Implementation Order（实现顺序）

## Phase 1 · 工程基础
Gradle/Hilt/骨架 · BASE_URL flavor · 混淆 · CI 占位

## Phase 2 · Design System
Theme（双主题 Token 全量）→ Typography → 基础组件（Button/Chip/Input/Card/Badge/Avatar/Divider）→ 反馈组件（Loading/Empty/Error/Dialog/BottomSheet/Snackbar）→ Reveal 动效
**退出标准**：组件 Demo 页与 Token 表逐项对齐

## Phase 3 · 网络与导航
Retrofit+Authenticator+SSE → Room/DataStore → NavHost 全图 + 角色装配 + 守卫

## Phase 4 · Authentication
page-001 全部 + 登录弹窗语义 + refresh 并发 + 登出

## Phase 5 · 核心组件业务化
ServiceCard/PetCard/OrderCard/StatusBadge/收藏按钮/FilterToolbar/图片组件/查看器

## Phase 6 · C 端核心页
page-002 首页 → 003 服务 → 004 详情 → 009 宠物 → 005 下单 → 008 支付 → 006/007 订单

## Phase 7 · C 端次级页
010-017（寄养师/商家/我的/通知/IM+AI/收藏/券会员/钱包售后）

## Phase 8 · 角色工作台
018 寄养师 → 019 商家 → 021 客服 → 020 管理裁剪版

## Phase 9 · 全局态补全
离线横幅/重试统一/错误页 page-022/骨架全覆盖/空态文案

## Phase 10 · 打磨
动效（Reveal/转场/按压）· 暗色全面走查 · 无障碍（48dp/TalkBack）· 深链 · 性能

## Phase 11 · 测试
单元/导航/流程 E2E（testing.md）

## Phase 12 · 发布
签名/混淆映射/崩溃上报/灰度 [R]

> 依赖规则：严格顺序；跨 Phase 复用组件必须先落 Phase 2/5。
