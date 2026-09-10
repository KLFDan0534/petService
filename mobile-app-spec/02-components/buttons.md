# Button（按钮规范）

## Anatomy（EXISTING，设计语言 Button/index.tsx）

```
Button
├── Container: height 42 · radius 11 · border 1px transparent · gap 8
├── Leading Icon: 15px（loading 时替换为 15px spinner，aria-busy）
├── Label: 13px w500 · truncate · whitespace-nowrap
└── Trailing Arrow: 15px（Web hover 右移 3px → 移动端保留为静态箭头 [P]）
```

## 规格

| Size | Height | HPadding | Typography |
|---|---|---|---|
| sm | 34 | 13 | 12.5 w500 |
| md（默认） | 42 | 18 | 13 w500 |
| lg | 48 | 20 | 14 w500（全宽） |

## Variants（EXISTING）

| Variant | Rest | Pressed | 用途 |
|---|---|---|---|
| primary | bg brand / text #fff | bg brandDeep | 主操作 |
| outline | bg surface · border line · text ink | border ink35% | 次操作 |
| dark | bg ink / text cream | brightness 1.18 | 强调次级 |
| light | bg #F5EFE7 / text #17130F | bg #fff | 深色区上的浅按钮 |
| ghost | transparent / text inkSoft | text brand | 行内操作 |
| quiet | transparent / text inkSoft | text ink | 弱操作 |
| danger | bg error9% · border error28% · text error | bg error15% | 删除/取消订单 |

## States（EXISTING + P）

disabled: opacity .5 + 无 ripple；loading: spinner 15px + 禁点 + 保持宽度；pressed: bg 加深 + scale .98（200ms editorial）；focused（键盘/遥控）: 3px primary12% 环。

## 用法规则（DERIVED）

- 每屏一个 primary；Dialog footer 右对齐（小屏纵排全宽）；lg 默认全宽。
- 破坏性操作（取消订单/退款/删除）必须二次确认（Dialog，danger 按钮在右）。
- 触控热区 ≥48dp（热区扩展，视觉不变）。
