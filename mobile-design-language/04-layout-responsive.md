# 04 · Layout & Responsive System（布局与响应式）

## 屏幕结构 [E→P]

Web 现状 [E]：64px sticky 顶栏 + 内容区（`max-width 1180px` + gutter 24）+ Footer。

移动端结构 [P]：

```
Screen
├── Safe Area 顶（状态栏）
├── App Bar: 56dp（Android）/ Nav Bar + Large Title（iOS）
├── Content: padding screenPadding（全宽，无 max-width）
│   ├── Eyebrow 眉题（9.5px + 32px 前置线）
│   ├── Hero / Page Title（displayXL/L 衬线）
│   ├── Chip Rail（横向筛选，gap 8，隐藏滚动条）
│   ├── Card Grid（手机 2 列 / 宽屏 auto-fill）
│   ├── Section Band（56px 节奏）
│   └── List（全宽列表项）
└── Bottom Navigation: 56dp + Safe Area 底 [P·R]
```

## 栅格与网格 [E→D]

| 场景 | Web 源值 | 移动端 |
|---|---|---|
| 统计卡 | `minmax(180px,1fr)` | 2 列固定 |
| 服务卡 | `minmax(240px,1fr)` | 2 列（compact）/ auto-fill |
| 宠物卡 | `minmax(280px,1fr)` | 2 列（compact）/ 1–2 列 |
| 表单双列 | `1fr 1fr` gap 16 | 单列（对应 768px 断点行为 [E]） |
| 网格 gap | 16px | 12px（compact） |

## 响应式断点 [E]

项目真实断点：**1100px**（头部折叠汉堡）→ **900px**（Hero 单列）→ **768px**（padding 16 / 图标化侧栏）→ **560px**（gutter 16 / 字号降档）→ **520px**（单列 / 弹窗全宽按钮）。

移动端窗口类映射 [P]：

| Class | 尺寸 | 规则 |
|---|---|---|
| Compact | < 600dp/pt | gutter 16 · 2 列网格 · Bottom Tab Bar · displayXL=30px |
| Medium | 600–840 | gutter 24 · 2–3 列 · Navigation Rail（平板） |
| Expanded | > 840 | 沿用 Web 双栏布局 |

## 页面信息密度 [D]

- 列表页：卡片网格 2 列，每卡含 媒体(4:3) + 分类 label + 标题 + 描述 2 行 + 价格 + 操作行。
- 详情页：Hero 媒体（radius frame 26）→ 标题区 → 信息卡 → 内容区 → 底部操作栏（sticky，elevation.1 上边线）[P·R]。
- 管理台（平板）：Filter Toolbar（搜索 44px + 重置）+ 表格/列表 + 状态 Badge。
- 底部操作栏模式 [D]：Web 页脚操作区（如订单详情 CTA）→ 移动端固定底栏，背景 surface、上边 1px line、Safe Area padding。
