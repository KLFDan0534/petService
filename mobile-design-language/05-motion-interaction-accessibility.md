# 05 · Motion · Interaction · Accessibility

## Motion Language — 全部 [E]

| 场景 | 规格 |
|---|---|
| 弹窗进场 | scrim fadeIn 150ms + 容器 scale(0.98→1) 200ms `easing.enter`（**无飞入**） |
| 列表/区块显现 | Reveal：y +18px + 6px blur → 归位，300ms `easing.editorial`，分组 `delay = index × 0.05s`，仅一次 |
| 卡片 hover | 边框 ink16% + lift 阴影 + translateY(-4px)，200ms |
| 媒体 hover | scale(1.06)，300ms |
| 按钮 hover | translateY(-1px)；箭头图标右移 3px |
| Toast | slideIn 200ms → 3000ms 后移除 |
| 骨架屏 | sand→surface→sand 渐变位移，1.3s linear infinite |
| Spinner | 32px 圆环（border 3px，顶色 primary），0.6s linear |
| 页面转场 [P] | Android: 共享轴 X 300ms / iOS: push 250–350ms，复用 `easing.editorial` |
| Bottom Sheet [P] | iOS spring（天然）/ Android slide-up 250ms + scrim fade |
| Reduce Motion | 全局守卫 [E]：时长压至 0.01ms、shimmer 停止 → Android `animator duration scale` / iOS `accessibilityReduceMotion` |

## Interaction States（交互反馈语言）

| 交互 | Web 现状 [E] | 移动端转换 [P] |
|---|---|---|
| Tap/Press | hover：上浮 1–4px + lift 阴影 | 背景/文字加深一档 + scale 0.98；**无 hover 上浮** |
| Long Press | 无 | 列表项上下文菜单 / contextMenu [R] |
| Swipe | 无 | 列表删除/标记（对应 Web 行内按钮）[R] |
| Focus | `:focus-visible` 3px brand 12% 环（全局）[E] | 键盘/遥控 focus 保留；触摸端不显示 |
| Disabled | opacity .5 + not-allowed | 同 opacity，去 ripple |
| Pull to Refresh | 无 [P] | 指示色 = primary |
| Scroll | 8px 自定义滚动条；rail 隐藏滚动条 | 系统滚动条；rail 边缘渐隐 [R] |
| Keyboard | 无自定义 | iOS 下推内容 / Android `adjustResize` [P] |
| System Back | 无 | Android 系统返回 + 预测式返回；关闭 Sheet/Dialog 优先于退出页面 [P] |

## Accessibility

[E] 项目已具备：`role="dialog" aria-modal`、`aria-label/expanded`、`aria-live="polite"`（Loading）、全局 focus-visible、body 滚动锁定、Esc 关闭、reduce-motion 守卫、`tabular-nums`、状态色总是伴随文字标签（StatusBadge 集中映射）。

[P] 移动端强制项：

1. **Touch Target ≥ 48dp（Android）/ 44pt（iOS）**——42px 按钮视觉保持，触控热区扩至 48；28px 关闭按钮扩到 44。
2. 对比度：ink/canvas ≈ 13:1 ✓；textTertiary 仅用于 ≥12px 非正文。
3. TalkBack/VoiceOver：沿用 aria 语义；图片需补 alt/label [R]。
4. Dynamic Type / 字体缩放 1.3x：不锁死控件高度，允许换行。
5. 色盲安全：状态语义 = 色 + 文，禁止仅用颜色传达 [E 原则继承]。
