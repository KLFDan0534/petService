# 03 · Component System（组件规范）

## 组件规范总表（开发级）

| Component | Variant | Size | Height | Radius | Padding | Typography | Icon | State |
|---|---|---|---|---|---|---|---|---|
| **Button** | primary / outline / dark / light / ghost / quiet / danger | sm/md/lg | **34/42/48** | 11px | 0 13 / 0 18 / 0 20 | 12.5/13/14 w500 | 15px, gap 8 | hover: bg加深+上浮1px；pressed: primaryDeep；disabled: opacity .5；loading: 15px spinner |
| **Chip** | default / active | md | **44px** | 11px | 0 16 | 13.5 w500 (+count 11px) | 可选 gap 6 | hover: sand底+ink30%边；active: ink底/cream字 |
| **Input / Search** | text / search(带清除) / password | md | **44px** | 11px | 0 14（含图标 0 40） | 13.5 | 15px 左置 | hover: ink25%边；focus: ink35%边 + 3px brand12%环；disabled: muted底 w75% |
| **Card** | content / media / empty / featured | — | auto | 18px（媒体 26px） | 20px | 标题 20 Serif / 描述 13.5 | 媒体 4:3 | hover: 边ink16% + lift + 上浮4px；图 hover scale 1.06 |
| **Badge / Status** | success/warning/danger/info/primary/neutral | — | ~20px | full | 2 10 | 12 w600 | — | tinted wash；暗色 14–16% alpha |
| **Dialog** | sm400/md440/wide520/lg640/xl960 | — | max 90vh | 18px | 24 24 28 | 标题 20 Serif / body 13.5 | 关闭 16px | scrim 暖黑 .42 + blur2；scale .98→1 200ms |
| **Bottom Sheet** | — | — | — | 顶部 18px | 24 24 28 | 同 Dialog | 顶部把手 [R] | Android: ModalBottomSheet / iOS: Sheet [P] |
| **Toast** | success/error/warning/info | — | ~48px | 12px | 14 20 | 15 w700 | — | 3s 自动消失；slideIn 200ms |
| **Empty** | 图标+标题+描述+操作 | — | — | 18px（虚线变体 [E]） | 64 24 | 标题 18 Serif / 描述 14 | 48px（线性图标 [R]） | — |
| **Loading** | spinner / shimmer 骨架 | — | 32px spinner | 50% | — | 14px textTertiary | — | spin 0.6s / shimmer 1.3s |
| **List Item** | leading avatar + title + meta + trailing | — | ≥56px [P] | 0–11px | 12 16 | 13–14 / meta 12.5 | 20px | pressed: sand 洗色 [P] |
| **Avatar** | 图像 / 字母 | 36/44 | — | full（Web 用 13px 圆角方 [D] 保留个性） | — | 14 w900 | — | hover 上浮 1px |
| **Tab / Segmented** | chip 式 | — | 44px | 11px | 8 16 | 14 w500 | — | active: muted 底 + primary 字 [E] |
| **Switch/Checkbox/Radio** | — | — | 44px 触控区 [P] | — | — | — | — | 选中色 = primary [R] |
| **Divider** | hairline | — | 1px | 0 | — | — | — | `color.border` [E] |
| **Image / Media** | 4:3 卡片媒体 / 全宽 Hero | — | — | 26px | — | — | — | fallback 占位 [E] MediaWithFallback；hover scale 1.06 |
| **Stat** | 数字+标签 | — | — | — | — | stat + label | — | 左竖线分隔变体 [E] |
| **Timeline** | 竖向节点 | — | — | — | — | body/meta | 节点圆点 | [E] 设计语言库 Timeline |

## Button Anatomy（可直接编码）[E]

```
Button
├── Container: height 42 · radius 11 · border 1px transparent · gap 8
├── Leading Icon: 15px（loading 时替换为 15px spinner + aria-busy）
├── Label: 13px w500 · truncate · whitespace-nowrap
└── Trailing Arrow: 15px，hover 右移 3px

primary:  bg brand → hover brand-deep · onPrimary #fff
outline:  bg surface · border line → hover 边框 ink 35%
dark:     bg ink / text cream → hover brightness 1.18
danger:   bg error 9% · 边 error 28% · 字 error → hover bg 15%
disabled: opacity .5 + not-allowed + 禁用上浮
pressed:  bg 加深一档（Android ripple=onPrimary 12%；iOS highlight=black 8%）[P]
```

## Dialog / Bottom Sheet Anatomy [E]

```
Overlay: scrim rgba(30,22,14,.42) + blur 2px · z 1000 · fadeIn 150ms
Container: surface · 1px line · radius 18 · shadow-lift(elevation.3)
           padding 24/24/28 · max-height min(90vh,720px) · overflow-y auto
           进场 scale(0.98→1) 200ms easing.enter（无飞入）
├── Header: 标题 20 Serif w500 + 描述 13px textTertiary · 右上关闭 28px tile
├── Body: 13.5px / 1.7 textSecondary
└── Footer: 右对齐 · gap 12 · 主按钮 primary + 次按钮 outline
交互: Esc/点遮罩关闭 · body 滚动锁定 · 焦点管理 · aria-modal
移动端: 小屏 footer 纵排（column-reverse）+ 按钮全宽 [E: 520px 断点]
```

## Input / Search Anatomy [E]

```
Field: h44 · radius 11 · border 1px line · bg surface · padding 0 14
Search: 左图标 15px textTertiary（left 14）· 右清除 28px tile（right 8）
placeholder: textTertiary
focus: border ink 35% + shadow focus（3px brand 12%）
disabled: bg surfaceVariant · 文字 75% · cursor not-allowed
```

## Chip / 筛选 Rail Anatomy [E]

```
Rail: 横向滚动 · gap 8 · 隐藏滚动条 · 底部 4px 余量
Chip: h44 · px16 · radius 11 · border line · bg surface · 13.5 w500
hover: bg sand + border ink30%
active: bg ink · border ink · text cream
count: 11px · opacity .6 · tabular-nums
```

## 组件生成规则 [R]

- 任何组件的高度/圆角/颜色/字号必须引用 Token；禁止在组件内硬编码。
- 状态至少实现：default / pressed / disabled / loading（可交互组件）/ empty / error（数据组件）。
- 可复用组件优先级：先查本表，存在即复用，禁止重复实现。
