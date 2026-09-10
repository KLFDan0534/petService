# Motion & Interaction（动效与交互语言）

## Motion Token（全 EXISTING）

fast 150ms · normal 200ms · slow 300ms · easing.editorial `cubic-bezier(.23,1,.32,1)` · easing.enter `(.22,1,.36,1)` · easing.decel `(.16,1,.3,1)` · shimmer 1.3s · spin 0.6s · toast 3s。

## 动效场景

| 场景 | 规格（EXISTING → 平台适配） |
|---|---|
| 弹窗进场 | scrim fade150 + 容器 scale(.98→1) 200ms enter，**无飞入** |
| 列表显现 | Reveal：y+18 + blur6 → 归位 300ms，分组 delay=index×0.05s，仅一次 |
| 按压 | 背景/文字加深一档 + scale .98（Web hover 上浮的触屏转换） |
| 媒体 | 无 hover；改用点击进入查看器 |
| 页面转场 | Android 共享轴 X 300ms / iOS push 250–350ms [P] |
| Bottom Sheet | iOS spring / Android slide-up 250 + scrim fade [P] |
| Snackbar | 底部滑入 200ms，3s [P] |
| 骨架屏 | sand→surface 位移 1.3s |
| Reduce Motion | 全局守卫：0.01ms + shimmer 停止（EXISTING）→ 平台无障碍同步 [P] |

## Interaction States

| 交互 | 规格 |
|---|---|
| Tap | 按压加深（primary→primaryDeep / 中性→ink8% wash）+ scale.98 |
| Long Press | 列表项上下文菜单 [R] |
| Swipe | 列表行删除/标记 [R] |
| Focus | 3px primary12% 环（键盘/遥控显示，触摸不显示）[EXISTING] |
| Disabled | opacity .5，去 ripple |
| Pull Refresh | 指示色 primary [P] |
| System Back | Sheet/Dialog > 页面（Android 预测式返回）[P] |
| Keyboard | adjustResize / iOS 下推 [P] |
| Scroll | rail 隐藏滚动条+边缘渐隐 [D/R] |

## 手势汇总（PLATFORM_ADAPTATION）

下拉刷新（列表页）· 左滑行操作（订单/通知）· 边缘滑返回（iOS）· 预测式返回（Android 13+）· 双击 Tab 回顶并刷新 [R]。
