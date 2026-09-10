# Design Tokens（主 Token 表）

> 所有值 EXISTING（来源 `design-tokens.css` + `设计语言/src/index.css` 审计账本）。移动端禁止硬编码。

## Color Tokens — Light

| Token | Value | Usage |
|---|---|---|
| `color.background` | `#FBF7F0` | 页面画布 |
| `color.surface` | `#FFFFFF` | 卡片/弹窗/输入框 |
| `color.surfaceVariant` | `#F1E9DB` | 媒体占位/按压面/骨架基色 |
| `color.surfaceTint` | `#F6EFE3` | 次级面 |
| `color.border` | `#E9DFCD` | 1px 发丝线 |
| `color.divider` | = border（1px） | 分隔线 |
| `color.textPrimary` | `#241C14` | 标题/强调 |
| `color.textSecondary` | `#55463A` | 正文 |
| `color.textTertiary` | `#8A7A6A` | 元信息/占位符 |
| `color.textDisabled` | textTertiary + 50% opacity | 禁用 |
| `color.primary` | `#BF5B2B` | 主按钮/链接/焦点环 |
| `color.primaryContainer` | primary 12% wash | 选中浅底/距离标签 |
| `color.onPrimary` | `#FFFFFF` | 主按钮文字 |
| `color.primaryDeep` | `#9A411A` | 按压态 |
| `color.error` | `#B3402A` | 错误/危险 |
| `color.warning` | `#B45309` | 警告 |
| `color.success` | `#3F5347` | 成功 |
| `color.info` | `#3F5A6B` | 信息 |
| `color.scrim` | `rgba(30,22,14,.42)` + blur2 | 遮罩 |

## Color Tokens — Dark（EXISTING `html[data-theme="dark"]`）

| Token | Value |
|---|---|
| background / surface / surfaceVariant / surfaceTint | `#171310` / `#201B15` / `#2A241D` / `#2E2820` |
| border | `#393129` |
| textPrimary / Secondary / Tertiary | `#F2EBDD` / `#D8CEBB` / `#B6A892` |
| primary / primaryDeep | `#E0823F` / `#C4632A` |

状态暗色 wash：14–16% alpha 背景 + 亮色文字（success 例：`rgba(52,211,153,.14)`+`#86EFAC`）。

## Spacing

| Token | Value | Usage |
|---|---|---|
| screenPadding | 24（compact 16） | 页面左右留白 |
| sectionGap | 56（compact 44） | 区块间距 |
| cardPadding | 20 | 卡片内边距 |
| cardPaddingLg | 24/24/28 | 弹窗内边距 |
| gridGap | 16（compact 12） | 网格 gap |
| listGap | 12 | 列表/表单行 |
| componentGap | 8 | 控件间/图标-文字 |
| textGap | 6 | 标题-说明 |
| bottomBar | 56dp + Safe Area | 底部导航 |
| touchGap | ≥8 | 相邻可点元素间隔 |

## Radius（EXISTING 5 档）

`tile 8px`（小图标按钮）· `control 11px`（按钮/输入框/Chip）· `card 18px`（卡片/弹窗）· `frame 26px`（大图/Hero）· `full 9999px`（胶囊/头像）。

## Elevation（Shadow → 抽象 5 级）

| 级 | 语义 | Android | iOS |
|---|---|---|---|
| 0 | 静置表面=1px 线无阴影 | 0dp+描边 | 无 shadow+border |
| 1 | 按压/悬浮面 | 2dp | y8 b28 o.12 |
| 2 | 菜单/Popover | 4dp | y12 b32 |
| 3 | Dialog | 8dp | y18 b44 o.16 |
| 4 | Toast/Snackbar | 12dp | y10 b30 o.25 |

焦点环 [EXISTING]：`0 0 0 3px primary 12%`。

## Motion

`fast 150ms`（颜色/边框）· `normal 200ms`（变换/弹窗）· `slow 300ms`（媒体/显现）· shimmer 1.3s · spin 0.6s · toastLife 3000ms ·
`easing.editorial cubic-bezier(.23,1,.32,1)`（全局）· `easing.enter cubic-bezier(.22,1,.36,1)`（弹窗 scale.98→1）。

## Icons

线性图标 stroke2/round（Element Plus → Android Material Symbols Outlined / iOS SF Symbols `.regular`）；Small 14 / Medium 16–20 / Large 24+；与文字 gap 6–10；`currentColor`。
