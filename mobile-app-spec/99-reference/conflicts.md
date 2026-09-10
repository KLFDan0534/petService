# Conflicts（冲突与裁决）

> 规格内部或规格与项目事实的冲突登记。禁止静默取舍。

| # | 冲突 | A 方 | B 方 | 裁决 | 证据 |
|---|---|---|---|---|---|
| C1 | 旧 Token（--color-* 橙蓝 shadcn）vs 新 Token（--ref-* 暖调） | 旧 admin/merchant 页仍在用 | 交接文档+审计账本指定新体系 | **取 --ref-* 新体系**；移动端禁止旧值 | 旧样式改写交接文档 §1 |
| C2 | 圆角档位：25 种实际取值 vs 审计账本 4 档 + full | 页面私有 --r-btn/--r-card 等 | tile8/control11/card18/frame26 | **取 5 档**；10px 一律归 control11 | 审计账本 confirmed |
| C3 | 字号 20+ 种 vs 语义 16 级 | 页面散值（13px×357 等） | typography.md | **取语义级**；13px→bodySmall、13.5→body、12.5→meta | 频率统计 |
| C4 | 控件高度 42 vs 44 | Button=42 | Chip/Input=44 | **保留刻意区分**，禁止统一 | 审计账本 confirmed across 40 views |
| C5 | 阴影：旧 shadow-sm/md/lg/xl vs 静置零阴影+浮层 lift | 旧全局样式 | 新权威页 | **静置 1px 线零阴影**；阴影仅浮层/按压 | 审计账本 + Services.vue |
| C6 | Toast 位置：Web 右上 vs 移动端习惯 | 右上深底 | 底部 Snackbar/顶部 Banner | **Android 底部 Snackbar / iOS 顶部 Banner**，样式沿用深底白字 3s | SocketManager 无关；Toast 样式 [E] + 平台习惯 [P] |
| C7 | 空态图标：emoji（📭）vs 线性图标 | EmptyState 现状 | 图标系统一致性 | **移动端用线性图标** | PRODUCT.md 一致性原则 |
| C8 | 暗色覆盖：旧令牌区块暗色不完整 | design-tokens dark 块 | 新体系双值齐备 | 移动端全走新体系双值 | dark-mode.md |
| C9 | hover 反馈 vs 触屏 | Web 上浮+阴影 | 触屏无 hover | **触屏=加深+scale.98** | motion-interaction.md |
| C10 | 管理端移动范围：全量 vs 裁剪 | 18+ 表格页 | 移动可用性 | **裁剪为待办/审核型**；表格留 Web | page-020 范围决策 |

> 新增冲突：实现过程中发现规格 vs 后端实际不符时，追加条目并给出裁决与证据。
