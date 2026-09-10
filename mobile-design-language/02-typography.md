# 02 · Typography System（语义字体体系）

## 字体家族 [E]

| Token | Family | 移动端替代 |
|---|---|---|
| `font.display` | 'Noto Serif SC', 'Songti SC', Georgia, serif | 内嵌 Noto Serif SC（标题/衬线数字） |
| `font.body` | 'Inter', system-ui, 'PingFang SC', sans-serif | Android: Roboto / iOS: PingFang SC |
| `font.mono` | 'JetBrains Mono', ui-monospace, monospace | 平台等宽字体 |

规则 [E]：标题 / 价格 / 统计数字用 Display（衬线）；正文 / 控件 / 元信息用 Body；数字使用 `tabular-nums`。

## 语义 Typography Token [D]（数值全部来自项目实测）

| Token | Font | Size | Weight | LH | LS | Usage |
|---|---|---|---|---|---|---|
| `type.displayXL` | Serif | clamp(30→46)，移动端 30px | 500 | 1.06 | -0.02em | 首屏大标题 |
| `type.displayL` | Serif | clamp(26→36)，移动端 26px | 500 | 1.12 | -0.02em | 页面主标题 |
| `type.displayM` | Serif | clamp(23→30)，移动端 23px | 500 | 1.18 | -0.01em | 区块标题 |
| `type.displayS` | Serif | clamp(20→26)，移动端 20px | 500 | 1.22 | -0.01em | 卡片标题、弹窗标题 |
| `type.title` | Serif | 18px | 500 | 1.3 | -0.01em | 列表项主标题 |
| `type.subtitle` | Sans | 15px | 400 | 1.8 | — | Hero 副文案（compact 14px） |
| `type.bodyLarge` | Sans | 14px | 400 | 1.6 | — | 次级正文 |
| `type.body` | Sans | 13.5px | 400 | 1.7 | — | 卡片描述、弹窗正文 |
| `type.bodySmall` | Sans | 13px | 400–500 | 1.5 | — | 控件文字（全项目最高频字号，357×） |
| `type.meta` | Sans | 12.5px | 400 | 1.5 | — | 元信息、面包屑 |
| `type.caption` | Sans | 12px | 400–700 | 1.5 | — | Badge、脚注、时间戳 |
| `type.label` | Sans | 10–11px | 700 | 1.2 | 0.08em uppercase | 卡片分类、统计标签 |
| `type.eyebrow` | Sans | 9.5px | 400 | 1.0 | 0.28em uppercase | 区块眉题（配 32px 前置线） |
| `type.stat` | Serif | 26–30px | 500 | 1.0 | -0.01em | 价格 / 数字（tabular-nums） |
| `type.button` | Sans | 12.5/13/14px | 500 | 1.25 | — | 按钮 sm/md/lg |
| `type.navigation` | Sans | 14px | 500 | 1.4 | — | 底部导航 / 顶栏 [P] |

## 平台注意事项 [P]

- Android：正文最小 12sp；Dynamic 字体缩放 1.3x 时 44px 控件高度允许放宽、文字换行。
- iOS：Dynamic Type 下 Body 各级 ±2pt；衬线标题 clamp 下限 20px；`monospacedDigit()` 对应 tabular-nums。
- 中文渲染：Android 思源黑体兜底；iOS PingFang SC；`-webkit-font-smoothing: antialiased` → 两平台默认开启亚像素渲染，无需额外处理。
