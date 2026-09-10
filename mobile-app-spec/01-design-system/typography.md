# Typography System

## 字体家族（EXISTING）

| Token | Family | Android | iOS |
|---|---|---|---|
| `font.display` | Noto Serif SC → Georgia serif | 内嵌 Noto Serif SC | 内嵌 Noto Serif SC |
| `font.body` | Inter → PingFang SC | Roboto / 思源黑体 | PingFang SC |
| `font.mono` | JetBrains Mono | 等宽默认 | SF Mono |

规则：标题/价格/统计数字=Display 衬线；正文/控件/元信息=Body；数字 `tabular-nums`（iOS `monospacedDigit()`）。

## 语义 Typography Token（数值 EXISTING，命名 DERIVED）

| Token | Font | Size(sp) | Weight | LH | LS | Usage |
|---|---|---|---|---|---|---|
| displayXL | Serif | 30 | 500 | 1.06 | -0.02em | 首屏大标题 |
| displayL | Serif | 26 | 500 | 1.12 | -0.02em | 页面主标题 |
| displayM | Serif | 23 | 500 | 1.18 | -0.01em | 区块标题 |
| displayS | Serif | 20 | 500 | 1.22 | -0.01em | 卡片标题/弹窗标题 |
| title | Serif | 18 | 500 | 1.3 | -0.01em | 列表项主标题 |
| subtitle | Sans | 15 | 400 | 1.8 | — | 副文案（compact 14） |
| bodyLarge | Sans | 14 | 400 | 1.6 | — | 次级正文 |
| body | Sans | 13.5 | 400 | 1.7 | — | 卡片描述/弹窗正文 |
| bodySmall | Sans | 13 | 400–500 | 1.5 | — | 控件文字（最高频） |
| meta | Sans | 12.5 | 400 | 1.5 | — | 元信息/面包屑 |
| caption | Sans | 12 | 400–700 | 1.5 | — | Badge/时间戳 |
| label | Sans | 10–11 | 700 | 1.2 | 0.08em uppercase | 分类/统计标签 |
| eyebrow | Sans | 9.5 | 400 | 1.0 | 0.28em uppercase | 眉题（+32px 线） |
| stat | Serif | 26–30 | 500 | 1.0 | -0.01em | 价格/数字 |
| button | Sans | 12.5/13/14 | 500 | 1.25 | — | 按钮 sm/md/lg |
| navigation | Sans | 14 | 500 | 1.4 | — | 底部导航/顶栏 |

## 平台适配（PLATFORM_ADAPTATION）

- Android：最小正文 12sp；`fontScale 1.3` 时控件不锁高，允许换行。
- iOS：Dynamic Type Body ±2pt；衬线 clamp 下限 20px。
- 中文行高比拉丁高 10–15%，LH 值已按中文实测校准，勿改小。
