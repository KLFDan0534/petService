# Cards & Lists（卡片与列表规范）

## Card（EXISTING Services.vue .service-card）

```
Container: bg surface · border 1px line · radius 18 · shadow 无（静置）
Media: 4:3 · radius frame 26（顶部裁切）· bg surfaceVariant 占位 · fallback 图 [E MediaWithFallback]
Body: padding 20
├── 分类 label：11px w700 · 0.08em uppercase · primary
├── 标题：displayS 衬线 20/1.3（max 2 行）
├── 描述：body 13.5/1.7 textSecondary（2-3 行）
└── Actions: 上边 1px line · 价格 stat 衬线 + 操作按钮/链接
Pressed: border ink16% + elevation.1（触屏按压转换）[P]
```

变体：`content`（无媒体）/`media`/`empty`（虚线边 [E empty-services]）/`featured`（2px primary 边，Web pricing-card [E]）。

业务卡：ServiceCard · PetCard（72px 圆头像+18px 衬线名 [E pet-card]）· OrderCard（状态徽章+时间线入口）· StatCard（stat 数字+label，可选左竖线 [E s-stats]）。

## ListItem（DERIVED，Web 行模式移动化）

```
Height ≥56 · padding 12 16 · radius control（可分组卡内 0）
Leading: Avatar 36-44 / 图标 20
Title: 14 w500 · Meta: meta 12.5 textTertiary
Trailing: StatusBadge / 箭头 / 操作按钮
Pressed: surfaceVariant wash；分隔：1px divider（卡内）或 8px 间隙（分组卡）
```

## 状态 Badge 映射（EXISTING statusMaps.js）

| Badge 类 | Light | Dark |
|---|---|---|
| success | `#D1FAE5`/`#065F46` | `rgba(52,211,153,.14)`/`#86EFAC` |
| warning | `#FEF3C7`/`#92400E` | `rgba(251,191,36,.16)`/`#FDE68A` |
| danger | `#FEE2E2`/`#991B1B` | `rgba(248,113,113,.14)`/`#FCA5A5` |
| info | `#DBEAFE`/`#1E40AF` | `rgba(96,165,250,.16)`/`#93C5FD` |
| primary | `#FFEDD5`/`#9A3412` | `rgba(245,158,11,.16)`/`#FCD34D` |
| neutral(secondary) | `#E2E8F0`/`#334155` | `rgba(148,163,184,.16)`/`#CBD5E1` |

规格：pill · padding 2 10 · 12 w600 · 色必伴文字 [RECOMMENDATION: wash 值 Token 化]。

## Chip（EXISTING .s-chip）

h44 · px16 · radius 11 · border line · 13.5 w500；active=bg ink/text cream；count 11px 60%；Rail：gap8 横滚隐藏滚动条。用于：服务分类筛选、订单状态筛选、收藏类型筛选。

## Divider / Image / Timeline

Divider=1px border；Image=4:3 容器+fallback（MediaWithFallback [E]）+点击查看器 [P]；Timeline=竖向节点+连接线，节点圆点 primary/完成 success，用于订单履约时间线（OrderFulfillment timeline [E]）。
