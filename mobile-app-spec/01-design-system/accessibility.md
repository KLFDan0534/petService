# Accessibility（无障碍）

## 项目已具备（EXISTING）

`role=dialog aria-modal`、`aria-label/expanded`、Loading `aria-live=polite`、全局 focus-visible 3px 环、弹窗 body 滚动锁定 + Esc + 焦点管理、`prefers-reduced-motion` 守卫、`tabular-nums`、状态**色+文**成对（StatusBadge 集中映射，禁止纯色传义）、WCAG AA 目标（PRODUCT.md EXISTING）。

## 移动端强制项（PLATFORM_ADAPTATION）

1. **Touch Target**：≥48dp（Android）/44pt（iOS）。42px 按钮、44px 控件视觉不变，热区扩展；28px 小按钮扩到 44。
2. 对比度：ink/canvas ≈13:1 ✓；textTertiary 仅用于 ≥12sp 非正文；禁用态仍 ≥3:1。
3. TalkBack/VoiceOver：所有图标按钮必须 contentDescription/accessibilityLabel（参考 Web aria-label 文案，如「关闭」「打开导航菜单」）。
4. 焦点顺序：弹窗聚焦容器→关闭钮；表单 label 显式关联。
5. 动态字体：fontScale 1.3 不截断、允许换行；Bottom Sheet 高度自适应。
6. Reduce Motion：关闭 Reveal/shimmer/转场，直接呈现终态。
7. 色觉安全：所有语义色伴随文字标签或图标 [E 原则]。
8. 内容安全：API 文本一律纯文本渲染（PRODUCT.md 原则）。

## 检查清单（QA 用）

- [ ] 每个可点元素热区 ≥48dp
- [ ] 图标按钮均有语义标签
- [ ] 动态字体 130% 无破版
- [ ] TalkBack 可完成：登录→下单→支付→查看订单
- [ ] 暗色模式对比度抽查
