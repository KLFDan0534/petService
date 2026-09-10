# Dialogs, Bottom Sheets & Toast（浮层与反馈）

## AppDialog（EXISTING，Web 已有完整 Anatomy）

- 5 尺寸档：sm400 / md440 / wide520 / lg640 / xl960（`SIZE_MAP` [E]）；移动端 compact 全部退化为全宽-24 边距 [E: 520 断点]。
- 结构：Header（displayS 20 衬线 + 描述 13 muted + 关闭 28 tile）→ Body（13.5/1.7 inkSoft）→ Footer（右对齐 gap12；compact 纵排全宽 [E]）。
- 容器：surface · 1px line · radius 18 · elevation.3 · maxH 90vh · padding 24/24/28 · scale(.98→1) 200ms。
- 行为：scrim 暖黑 .42+blur2 · 点遮罩关（可配）· Esc 关 · body 滚动锁 · 焦点管理 [E] → Android Dialog / iOS Sheet 或 Alert [P]。

## BottomSheet（PLATFORM_ADAPTATION，复用 Dialog 档位）

把手 36×4 [R] · 顶部 radius 18 · 内容区同 Dialog Body · 下拉关闭 · Android ModalBottomSheet / iOS Sheet。用于：筛选、选择器（分类/日期/地址）、更多操作。

## 确认 Dialog 场景（EXISTING 业务弹窗 → 移动映射）

| Web 组件 | 场景 | 移动 |
|---|---|---|
| CreateOrderDialog | 创建订单（日期/宠物/地址/券/报价） | 全屏页 [P]（表单长） |
| CancelOrderDialog | 取消订单（原因） | Dialog + danger 确认 |
| PetFormDialog | 宠物新增/编辑 | 全屏页 [P] |
| ReviewDialog | 评价（星级+内容） | BottomSheet |
| TipDialog | 打赏（金额选择） | BottomSheet |
| LoginPromptDialog | 未登录访问受限 | Dialog → 登录页（redirect） |
| PopupNotice | 全局公告 | 启动时 Dialog（可dismiss [E dismiss-popup]） |
| AmapAddressPicker | 地图选址 | 全屏地图页 [P] |

## Toast → Snackbar（EXISTING Toast 规格平移）

Web [E]：右上深底（#1F2937 / 状态实色 success #065F46 error #991B1B warning #B45309 info #1D4ED8）白字 15 w700 · radius 12 · padding 14 20 · 3s · slideIn 200ms。
移动 [P]：**Android Snackbar 底部** / **iOS 顶部 Banner**；保留深底白字与 3s；错误类可带「重试」动作；同屏最多 1 条（Web 是队列 [E]，移动端收敛）[P]。

## Loading / Empty / Error States

- Loading：首载=骨架屏（卡片布局 shimmer）；操作=按钮 spinner；追加=底部 24px spinner。
- Empty：EmptyState（线性图标 48 [R 替代 Web emoji] + displayS 衬线标题 + body 描述 + primary 操作）· 卡内变体=虚线边 [E]。
- Error：请求失败=Snackbar + 原地重试按钮；页面级=ErrorState（插画/图标+说明+重试）；403=无权限 EmptyState；网络断=离线横幅+重试 [P]。
