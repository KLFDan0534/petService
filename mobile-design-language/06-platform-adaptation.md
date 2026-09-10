# 06 · Platform Adaptation（Core → Android / iOS）

## 分层模型

```
Core Design Language（品牌 · 颜色 · 字体 · 间距 · 圆角 · 组件 DNA · Motion DNA）
        ↓ 统一继承
Android Adaptation Layer（Material 习惯）
iOS Adaptation Layer（HIG 习惯）
```

## Core（跨平台统一）[E]

颜色双主题 Token、Typography 语义体系、Spacing/Radius/Elevation/Motion Token、
组件结构（Button 7 变体 / Chip 44px / Card 18px 线框 / Dialog 24/24/28）、
状态洗色配方（12% 品牌环 / 14–16% 状态 wash）、"静置零阴影 + 1px 线" 的高度语言。

## Android Adaptation [P]

| 主题 | 规格 |
|---|---|
| Navigation | Top App Bar（surface 底 + ink 标题 + 1px 线）+ Bottom Navigation（56dp，≤5 个入口：首页/服务/宠物/订单/我的） |
| Back | 系统返回 + Predictive Back；返回优先级：Sheet/Dialog > 页面 |
| Dialog | Material Dialog（radius 18、scrim 暖黑 .42） |
| Bottom Sheet | ModalBottomSheet（顶部 radius 18） |
| Snackbar | 底部、深底白字、状态色、3s（对应 Web Toast 内容） |
| Touch | ripple = onPrimary 12%（primary 按钮）/ ink 8%（中性） |
| Edge-to-edge | 状态栏/导航栏透明，Scaffold 处理 insets；画布色 = background |
| 状态栏 | 图标色随主题：light→ink、dark→cream |
| 字体 | Roboto（body）+ 内嵌 Noto Serif SC（display）；sp 缩放 |

## iOS Adaptation [P]

| 主题 | 规格 |
|---|---|
| Navigation | Navigation Bar + **Large Title**（displayXL 衬线，滚动折叠居中）；边缘滑返回 |
| Tab Bar | 49pt + Home Indicator Safe Area；毛玻璃（对应 Web 顶栏 blur20/saturate1.35 语言） |
| Sheet | Sheet + grabber（顶部 radius 18） |
| Alert | 居中、按钮纵排（对应 Dialog footer 小屏纵排规则） |
| Action Sheet | 列表长按/更多操作 |
| Safe Area | 状态栏 + Home Indicator 全程尊重；底栏 padding 34pt |
| Dynamic Type | Body ±2pt，衬线 clamp 下限 20px |
| 字体 | PingFang SC（body）+ Noto Serif SC（display） |

## Cross-Platform Mapping（组件映射表）

| Design Component | Core（本项目 Token） | Android | iOS |
|---|---|---|---|
| App Navigation | 64px sticky 顶栏 + 毛玻璃 [E] | Top App Bar | Nav Bar + Large Title |
| Primary Nav | user-nav 项 [E]（首页/服务/宠物/订单/收藏/消息/AI） | Bottom Navigation | Tab Bar |
| Back | Web 无（浏览器） | System Back | 边缘滑 + Back 按钮 |
| Dialog | AppDialog [E] | Material Dialog | UIAlertController / Sheet |
| Bottom Sheet | Dialog sm/md 档位复用 [D] | ModalBottomSheet | Sheet + grabber |
| Toast | 右上角深底 3s [E] | Snackbar（底部） | 顶部 Banner |
| Chip 筛选 Rail | 44px chip + active 反白 [E] | FilterChip | Segmented / Capsule |
| Card | 1px 线 + 18px + 零静置阴影 [E] | Surface 0dp + outline | 无 shadow + border |
| 状态色 | critical/caution/moss/informative [E] | error/warning/success/info | 语义色对齐 |
| 深色 | `data-theme="dark"` 双值 [E] | Theme 双 Palette | traitCollection 双 Asset |

## Dark Mode [E→P]

- 触发：`html[data-theme="dark"]`（Web）→ Android 双 Palette / iOS 双 Asset。
- 全部颜色走 Token（第 01 节 Dark 表）；wash 配方不换算，直接用对应亮色文字。
- 阴影不换色（本设计语言静置无阴影，天然适配）。
- 图片/媒体占位用 `surfaceVariant`。
