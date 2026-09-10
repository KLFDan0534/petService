# Mobile Design Language Specification · 栖屿宠护

> 从 petService 项目逆向提取的移动端设计语言（Mobile Design Language）。
> 目标：任何开发者（Android / iOS / Flutter / React Native）或 AI Coding Agent 仅凭本文件夹，
> 即可实现与当前项目视觉高度一致、交互统一的双平台移动端 UI。

## 证据等级标记（全文适用）

| 标记 | 含义 |
|---|---|
| **[E]** Existing | 项目代码中明确存在 |
| **[D]** Derived | 从多页面/组件重复规律推导 |
| **[P]** Platform Adaptation | 为 Android/iOS 落地所做的平台适配 |
| **[R]** Recommendation | 项目无明确证据，建议补充 |

## 事实来源（Source of Truth）

1. `frontend/src/assets/css/design-tokens.css` — 双主题 Token（light + `html[data-theme="dark"]`）
2. `frontend/src/assets/css/app.css` — 全局基础样式、控件、弹窗、Toast、响应式断点
3. `frontend/src/views/user/Services.vue` — **视觉权威参考页**（审计账本 confirmed：任何视图间冲突以它为准）
4. `设计语言/`（React 镜像库）— 已固化的 `--ref-*` Token、Button 组件解剖、Motion Token、审计账本
5. `frontend/旧样式改写交接文档.md` — 新旧 Token 迁移映射（旧 `--color-*` → 新 `--ref-*`）
6. 全项目频率统计 — radius/shadow/font-size/gap/padding 实际出现次数

## 阅读顺序

1. `01-design-tokens.md` — Color / Spacing / Radius / Elevation / Motion / Icon 全部 Token
2. `02-typography.md` — 语义化字体体系
3. `03-components.md` — 组件规范表与 Component Anatomy
4. `04-layout-responsive.md` — 布局、栅格、响应式规则
5. `05-motion-interaction-accessibility.md` — 动效、交互状态、无障碍
6. `06-platform-adaptation.md` — Core → Android / iOS 适配层、Dark Mode、跨平台映射
7. `07-issues-and-reference.md` — 项目设计问题清单、Do/Don't、Source Mapping

## 一句话设计语言

> **Warm Editorial（暖调编辑部）**：米色画布 `#FBF7F0` + 白色纸面 + 1px 发丝线 + 衬线大标题 + 焦糖橙 `#BF5B2B`。
> 卡片静置零阴影（唯一高度设备是 1px 边框），阴影只在悬浮/浮层出现；状态色用低饱和「墨水洗」而非高饱和色块。
