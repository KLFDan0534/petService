# Design Principles（设计哲学 · DNA · 原则）

> 完整提取见 `../mobile-design-language/`。本目录为规格包自包含摘要，冲突时以审计账本为准。

## 设计哲学（EXISTING）

**Warm Editorial（暖调编辑部）**：米色画布 + 白色纸面 + 1px 发丝线 + 衬线大标题 + 焦糖橙品牌色。
克制、纸感、低阴影——**卡片静置零阴影，唯一高度设备是 1px 边框；阴影只在悬浮/浮层出现**。

## Design DNA（EXISTING）

| DNA | 证据 |
|---|---|
| 暖米画布 `#FBF7F0` + 墨色 `#241C14` | `--ref-canvas/--ref-ink` |
| 焦糖橙 `#BF5B2B` 唯一品牌色，按压加深 `#9A411A` | `--ref-brand/--ref-brand-deep` |
| 衬线 Display（Noto Serif SC）+ 无衬线 Body（Inter）混排 | `--ref-font-display/--ref-font-sans` |
| 1px 发丝线为主结构设备，静置无阴影 | Services.vue:517-519（审计 confirmed） |
| 阴影仅浮层/按压（ink-mix 柔和大扩散） | `0 28px 60px -44px ink 60%` |
| `color-mix()` 洗色（边框加深/状态染色/焦点环） | 全项目 100+ 处 |
| 状态色低饱和「墨水洗」，色+文成对 | StatusBadge/statusMaps.js |

## 设计原则（EXISTING PRODUCT.md + DERIVED）

1. 任务清晰优先：列表/表单/控件服务扫读与操作。
2. 一致性建立信任：状态、按钮、列表模式跨角色一致。
3. 温暖但克制：不搞大面积橙、不搞营销 Hero 进任务页。
4. 默认安全：API 内容按文本渲染。
5. 衬线承载品牌，无衬线承载信息；数字用 `tabular-nums`。
6. 一种交互一种反馈：按压=加深；选中=wash；焦点=3px 品牌环。
7. 42px 按钮 / 44px Chip·输入框是**刻意区分**，禁止"统一"（审计账本 confirmed across 40+ views）。
