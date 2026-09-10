# Flutter Implementation Mapping（Flutter 架构映射）

> Core 语言与页面/权限/业务规格完全复用本包 01-07 章；本文件只做平台技术映射。RECOMMENDATION。

## 工程结构

```
lib/
├── core/
│   ├── designsystem/
│   │   ├── qiyu_colors.dart      # 双主题 Token（tokens.md 逐项）
│   │   ├── qiyu_typography.dart  # 16 级语义字体
│   │   ├── qiyu_dimens.dart      # spacing/radius/elevation
│   │   └── theme.dart            # ThemeData装配 + ThemeExtension<QiyuTokens>
│   ├── network/                  # dio + interceptor + sse_client
│   ├── storage/                  # flutter_secure_storage(token) + shared_prefs
│   └── components/               # QiyuButton/Chip/Input/Card/Badge/Empty/Error/Loading
├── data/                         # dto(_wsh) + repository + api_service(dio)
├── domain/                       # model + 枚举(statusMaps)
└── features/                     # 与 03-pages 22 页一一对应
```

## Token 映射要点

| Token | Flutter |
|---|---|
| color.* | `ColorScheme.fromSeed` 不用——手工 `ColorScheme(light:…)` 双份；wash 配方 = `primary.withValues(alpha:.12)` |
| radius.control 11 等 | `BorderRadius.circular(11)` 常量表 + `RoundedRectangleBorder` |
| type.* | `TextTheme` 自定义 16 style；衬线标题 `fontFamily: 'NotoSerifSC'`（pubspec 内嵌） |
| elevation 0-4 | 自定义 `QiyuElevation`（BoxShadow 精确复刻 iOS/Web 数值，不使用 Material elevation） |
| spacing | `SizedBox(w: QiyuSpacing.md)` 常量 |

## 组件映射（02-components）

| 组件 | Flutter 实现 |
|---|---|
| Button 7 变体 | `QiyuButton(variant,size,loading)` → FilledButton 定制样式；pressed=brandDeep |
| Chip/输入/搜索 | 自绘 Container + InkWell（ripple=onPrimary 12%）+ TextField(44 逻辑像素) |
| Card | `Container(decoration: border 1px)` 零阴影；hover 转按压 |
| Dialog/BottomSheet | `showDialog`(scale 动画自定) / `showModalBottomSheet`(RoundedRectangle top 18) |
| Toast→Snackbar | `ScaffoldMessenger.showSnackBar`（深底白字 3s，behavior: floating） |
| EmptyState/ErrorState/骨架 | 直接复刻 02-components 规格（shimmer 用 shimmer 包） |

## 基础设施映射

| 领域 | Flutter |
|---|---|
| 网络 | dio：Interceptor 注 Bearer；401 → 单飞刷新（Lock）→ 重试队列 [E request.js 语义] |
| SSE | `package:http` streamed request 或 `sse_client`；`?token=`；退避 2s×1.5ⁿ≤30s→轮询 [E] |
| 分页 | `infinite_scroll_pagination` + PageResult 映射 |
| 状态 | Riverpod（`AsyncNotifier` = UiState 五态）；或 Bloc |
| 路由 | go_router：ShellRoute(5 Tab) + 角色重定向（对齐守卫 [E]） |
| 推送 | firebase_messaging（渠道 system/order/notice）[R] |
| 地图 | amap_flutter_map（地址选址 [E AmapAddressPicker]） |

## 验收

同 10-ai-development/verification-checklist.md；视觉对照 `../mobile-design-language/`。
