# 01 · Design Tokens（设计令牌）

> 数值来源：`design-tokens.css` + `设计语言/src/index.css`（审计账本）+ `Services.vue`。
> 移动端只允许使用本文档中的 Token，禁止硬编码。

## 1. Color Tokens

### Light Mode [E]

| Token | Value | Usage | Source |
|---|---|---|---|
| `color.background` | `#FBF7F0` | 页面画布 / Scaffold 背景 | `--ref-canvas` |
| `color.surface` | `#FFFFFF` | 卡片 / 弹窗 / 输入框纸面 | `--ref-surface` |
| `color.surfaceVariant` | `#F1E9DB` | 媒体占位、hover 面、骨架屏基色 | `--ref-sand` |
| `color.surfaceTint` | `#F6EFE3` | 次级面 / active chip 文字底 | `--ref-cream` |
| `color.border` | `#E9DFCD` | 1px 发丝线（卡片/分隔/输入框） | `--ref-line` |
| `color.textPrimary` | `#241C14` | 标题、正文强调 | `--ref-ink` |
| `color.textSecondary` | `#55463A` | 正文、弹窗 body | `--ref-ink-soft` |
| `color.textTertiary` | `#8A7A6A` | 元信息、占位符、面包屑 | `--ref-muted` |
| `color.textDisabled` | `textTertiary @ 50% opacity` | 禁用文字 | [D]（disabled opacity .5 规则推导） |
| `color.primary` | `#BF5B2B` | 品牌主色：主按钮 / 链接 hover / 焦点环基色 | `--ref-brand` |
| `color.primaryContainer` | `primary 12% wash` | 距离标签、选中态浅底 | `color-mix(brand 12%)` [E] |
| `color.onPrimary` | `#FFFFFF` | 主按钮文字 | [E] `.btn-primary color:#fff` |
| `color.primaryDeep` | `#9A411A` | 主按钮 pressed / hover 加深 | `--ref-brand-deep` |
| `color.error` | `#B3402A` | 错误 / 危险（深锈红） | `--ref-critical` |
| `color.warning` | `#B45309` | 警告 | `--ref-caution` |
| `color.success` | `#3F5347` | 成功（苔绿） | `--ref-moss` / `--ref-positive` |
| `color.info` | `#3F5A6B` | 信息（灰蓝） | `--ref-informative` |
| `color.scrim` | `rgba(30,22,14,0.42)` + blur 2px | 弹窗 / Bottom Sheet 遮罩（暖黑） | AppDialog [E] |

### Dark Mode [E]（`html[data-theme="dark"]`，由 stores/app.js 持久化）

| Token | Value |
|---|---|
| `color.background` | `#171310` |
| `color.surface` | `#201B15` |
| `color.surfaceVariant` | `#2A241D` |
| `color.surfaceTint` | `#2E2820` |
| `color.border` | `#393129` |
| `color.textPrimary` | `#F2EBDD` |
| `color.textSecondary` | `#D8CEBB` |
| `color.textTertiary` | `#B6A892` |
| `color.primary` | `#E0823F` |
| `color.primaryDeep` | `#C4632A` |

状态色暗色 wash 配方 [E]：背景 14–16% alpha wash + 亮色文字（如 success: `rgba(52,211,153,.14)` + `#86EFAC`）。

## 2. Spacing Tokens

Base scale [E]：`--space-xs 4 / sm 8 / md 16 / lg 24 / xl 32 / 2xl 48 / 3xl 64`（gap 频率：8 > 10 > 12 > 16）。

| Token | Value | Usage |
|---|---|---|
| `space.screenPadding` | 24px（compact 16px） | 页面左右留白 [E] `--ref-gutter` |
| `space.sectionGap` | 56px（compact 44px） | 区块间距 [E] `--ref-band` |
| `space.heroGap` | 48px（compact 32px） | Hero 节奏 [E] |
| `space.cardPadding` | 20px | 卡片内边距 [E] |
| `space.cardPaddingLg` | 24/24/28px | 弹窗内边距 [E] |
| `space.listGap` | 12px | 列表项/表单行间距 [D] |
| `space.gridGap` | 16px | 卡片网格 gap [E] |
| `space.componentGap` | 8px | 控件间 / 图标-文字 gap [E] |
| `space.textGap` | 6px | 标题-说明间距 [E] |
| `space.bottomBar` | 56dp + Safe Area | 底部导航 [P] |
| `space.touchGap` | ≥8px | 相邻可点元素最小间隔 [P·R] |

## 3. Radius Tokens — 全部 [E]

| Token | Value | Usage |
|---|---|---|
| `radius.tile` | 8px | 小图标按钮 / 内嵌图标区 / 清除按钮 |
| `radius.control` | 11px | 按钮 / 输入框 / Chip / 小徽章（核心档位） |
| `radius.card` | 18px | 卡片 / 弹窗 / Empty State |
| `radius.frame` | 26px | 大图媒体框 / Hero 图 |
| `radius.full` | 9999px / 50% | 胶囊 / Badge / 头像 / Spinner |

## 4. Elevation Tokens（Shadow → 抽象高度）

| Token | 语义 | Web 源值 [E] | Android | iOS |
|---|---|---|---|---|
| `elevation.0` | 一切静置卡片/表面 | **1px 发丝线，无阴影** | 0dp + 1dp 描边 | 无 shadow + 1px border |
| `elevation.1` | hover/按压中表面 | `0 28px 60px -44px ink60%` + translateY(-4px) | 2dp | y8 blur28 op0.12 |
| `elevation.2` | 下拉 / Popover | lift | 4dp | y12 blur32 |
| `elevation.3` | Dialog | `0 40px 80px -40px ink60%` | 8dp | y18 blur44 op0.16 |
| `elevation.4` | Toast / Snackbar | `0 16px 40px rgba(15,23,42,.32)` | 12dp | y10 blur30 op0.25 |

焦点设备 [E]：`shadow.focus = 0 0 0 3px color-mix(primary 12%, transparent)`。

## 5. Motion Tokens — 全部 [E]

| Token | Value | Usage |
|---|---|---|
| `motion.fast` | 150ms | 颜色/边框过渡 |
| `motion.normal` | 200ms | 变换、弹窗进场、Toast |
| `motion.slow` | 300ms | 媒体缩放、滚动显现 |
| `motion.shimmer` | 1.3s linear infinite | 骨架屏 |
| `motion.spin` | 0.6s linear infinite | Spinner |
| `motion.toastLife` | 3000ms | Toast 自动消失 |
| `easing.editorial` | cubic-bezier(0.23, 1, 0.32, 1) | 全局统一曲线 |
| `easing.enter` | cubic-bezier(0.22, 1, 0.36, 1) | 弹窗 scale(0.98→1) |
| `easing.decel` | cubic-bezier(0.16, 1, 0.3, 1) | slideUp |

## 6. Icon Tokens [E]

- 库：Element Plus Icons（线性，stroke 2px, round cap）；`AppIcon.vue` 统一包裹。
- 尺寸：Small 14 / Medium 16–20（导航 20）/ Large 24px+；与文字 gap 6–10px；`currentColor` 继承。
- 平台映射 [P]：Android → Material Symbols Outlined；iOS → SF Symbols（`.regular`）。
