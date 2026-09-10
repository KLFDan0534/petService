# 07 · Issues · Do/Don't · Source Mapping

## 发现的设计问题（仅记录，不改代码）

1. **双 Token 体系并存** [E]：旧 `--color-*`（shadcn 橙蓝）与新 `--ref-*`（暖调 Editorial）共存；admin/merchant/cs 页面未迁移（交接文档已列清单）。移动端只取 `--ref-*` 体系。
2. **圆角碎片化** [E]：25 种取值（66×`var(--r-card)`、48×`var(--r-btn)`、57×`10px` 硬编码等）。移动端统一 5 档。
3. **字号碎片化** [E]：20+ 种 px 字号（13px 出现 357 次），无语义层 → 用第 02 节语义 Token。
4. **暗色模式不完整** [E]：旧令牌区块部分覆盖。
5. **Toast 为 Web 专用形态** [E]：右上角、硬编码深色值 → 移动端 Snackbar/Banner。
6. **触控目标不足** [E]：28px 关闭按钮、34px 小按钮 → 热区扩至 44/48。
7. **交互依赖 hover** [E]：上浮+阴影是鼠标语言 → 触屏改按压加深。
8. **图标双轨** [E]：Element 图标 + emoji（EmptyState `📭`）→ 统一线性图标。
9. **Badge 色值硬编码** [E]（`#D1FAE5` 等）→ 状态 wash Token 化。

## Do / Don't

**Do**：全部取用 Token；卡片静置 1px 线零阴影；hover 转 touch 加深；衬线只用于标题/数字；状态用 色+文 wash；弹窗进场 scale 0.98；42px 按钮 vs 44px Chip/输入是刻意区分（审计账本 confirmed，勿"统一"）。
**Don't**：不引入旧 `--color-*` 冷灰蓝体系；不把旧 `shadow-sm/md/lg/xl` 用于静置卡片；不新造 8/11/18/26 之外的圆角档；不把 box-shadow 原样搬 Android elevation；EmptyState 不用 emoji；不破坏 focus 环与 aria 语义。

## Source Mapping（证据来源）

| 结论 | 来源 |
|---|---|
| 全部颜色 Token | `frontend/src/assets/css/design-tokens.css`；`设计语言/src/index.css:18-171` |
| 视觉权威/冲突裁决 | `设计语言` 审计账本：`--rule-services-vue-is-the-primary-visual-reference...` → `src/views/user/Services.vue` |
| Button 7 变体/尺寸/状态 | `设计语言/src/components/Button/index.tsx`（移植自 `.cta` 家族：CustomerServiceApply.vue:479 等 40+ 视图） |
| Chip/Search/Card/Hero/Stats | `Services.vue:235-660`（rail 398、search 457-478、card 517-549、hero 287-363） |
| Dialog Anatomy | `frontend/src/components/common/AppDialog.vue`（全文件） |
| Old→New Token 映射 | `frontend/旧样式改写交接文档.md` 第 1 节 |
| Motion/Reveal | `Reveal.vue`（ease 0.23,1,0.32,1 / y18 / blur6 / delay 0.05s）、`app.css:520-523` keyframes |
| 响应式断点 | `app.css:746-831`、`Services.vue:632-660`、`UserLayout.vue:730-828` |
| 暗色模式 | `design-tokens.css:105-148` + `stores/app.js`（localStorage `pet-service-theme` + prefers-color-scheme） |
| 状态 wash 配方 | `app.css:365-387`（badge light/dark） |
| 频率统计 | 全项目 `*.vue/*.css` 正则统计（radius/shadow/font-size/gap/padding） |

## Implementation Guidelines（落地索引）

- **Android**：colors.xml/type.xml/dimens.xml/shape.xml 按第 01-03 节生成；Compose `MaterialTheme(primary=#BF5B2B)` + 自定义 `LocalQiyuColors`。
- **iOS**：Asset Catalog 双外观色 + UIFont 扩展（Serif title / Sans body）+ 按 06 节映射系统组件。
- **Flutter**：`ThemeExtension<QiyuTokens>`；`color-mix` 洗色用 `Color.lerp` / `withValues(alpha:)`。
- **React Native**：`tokens.ts`（JSON 化）+ `Platform.select` 处理 elevation/shadowOffset 双写。
- **验收**：对照第 03 节组件规范表逐列核对 Height/Radius/Padding/Typography/State。
