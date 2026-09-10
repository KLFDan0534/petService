# Layout & Responsive

## 屏幕结构（EXISTING→PLATFORM_ADAPTATION）

```
Screen（Safe Area 全程尊重）
├── App Bar: 56dp（Android）/ Nav Bar + Large Title（iOS）
├── Content: padding=screenPadding，全宽
│   ├── Eyebrow（9.5px 眉题 + 32px 线）
│   ├── Page Title（displayL 衬线）
│   ├── Chip Rail（横向筛选 gap8 隐藏滚动条）
│   ├── Card Grid（2 列）或 List
│   ├── Section Band（56px 节奏）
│   └── （详情页）底部操作栏：surface 底 + 上边 1px line + Safe Area padding
└── Bottom Navigation: 56dp + Safe Area 底
```

## 网格（EXISTING minmax → DERIVED 移动规则）

| 场景 | Web 源 | 移动 |
|---|---|---|
| 统计卡 | minmax(180,1fr) | 2 列固定 |
| 服务卡 | minmax(240,1fr) | 2 列（compact） |
| 宠物卡 | minmax(280,1fr) | 2 列（compact） |
| 表单 | 双列 gap16 | 单列 |

网格 gap 16（compact 12）。

## 窗口类（EXISTING 断点 1100/900/768/560/520 → PLATFORM）

| Class | 尺寸 | 规则 |
|---|---|---|
| Compact | <600 | gutter16 · 2 列 · Bottom Tab · displayXL=30 |
| Medium | 600–840 | gutter24 · 2–3 列 · Navigation Rail（平板） |
| Expanded | >840 | 沿用 Web 双栏 |

## 信息密度模式（DERIVED）

- 列表卡：媒体 4:3 + 分类 label + 标题 + 描述 2 行 + 价格 + 操作行。
- 详情：Hero 媒体（frame 26）→ 标题区 → 信息卡 → 内容 → sticky 底部操作栏。
- 工作台：Filter Toolbar（搜索 44 + 重置）+ 列表 + StatusBadge。
- 底部操作栏内容示例：订单详情 CTA、创建订单确认条。
