# React Native Implementation Mapping（React Native 架构映射）

> Core 规格复用本包 01-07 章；本文件只做平台技术映射。RECOMMENDATION。

## 工程结构

```
src/
├── design-system/
│   ├── tokens.ts          # colors(双主题)/typography/spacing/radius/elevation(JSON 化)
│   ├── theme.tsx          # ThemeProvider(dark mode) + useTheme()
│   └── components/        # Button/Chip/Input/Card/Badge/Empty/Error/Loading/…
├── api/                   # axios instance + interceptors + services/*(按 endpoints-detail 域)
├── stores/                # zustand：auth/settings/unread；react-query：数据获取
├── navigation/            # Root/BottomTab/RoleStacks(与 navigation-map 对齐)
├── features/              # pages/ 22 页规范对应
└── utils/                 # format(金额/时间) statusMaps 枚举
```

## Token 映射要点

| Token | RN |
|---|---|
| color.* | `tokens.colors.light/dark` + ThemeContext；wash = `hexWithAlpha()` 工具 |
| radius | tokens.radius（8/11/18/26/full） |
| typography | 自建 `Text(variant="bodySmall")` 组件（Noto Serif SC 内嵌 ttf：标题/数字） |
| elevation | `Platform.select`：Android=elevation；iOS=shadowOffset/shadowRadius 按第 8 级表 |
| spacing | tokens.spacing 常量 |

## 组件映射（02-components）

| 组件 | RN 实现 |
|---|---|
| Button | Pressable + pressed 态(bg 加深 + transform scale .98)；loading=ActivityIndicator 15 |
| Chip Rail | `ScrollView horizontal` + `showsHorizontalScrollIndicator={false}` |
| Card | View + borderWidth:1 + borderColor line；`Pressable` 按压态 elevation.1 |
| Dialog/Sheet | react-native-bottom-sheet（顶部圆角 18 + 手把）；Alert 用系统 |
| Toast | 自实现顶部 Banner(iOS)/底部 Snackbar(Android)（深底白字 3s） |
| 列表 | FlatList/InfiniteQuery（onEndReached 预加载） |
| 图片 | FastImage + fallback 占位（MediaWithFallback 语义） |

## 基础设施映射

| 领域 | RN |
|---|---|
| 网络 | axios：请求注入 Bearer；响应 401 → 单飞 refresh（锁+队列）→ 重放 [E]；timeout 30000 |
| SSE | `react-native-sse`；`?token=`；退避策略同上；后台断开回前台重连 |
| 存储 | MMKV(theme/user) + react-native-keychain(access/refresh) |
| 状态 | zustand（会话/设置）+ react-query（服务端状态/分页/重试） |
| 导航 | @react-navigation native-stack + bottom-tabs；`beforeRemove` 处理未保存；角色 guard 组件 |
| 推送 | notifee + FCM（渠道映射 NotificationType）[R] |
| 地图 | react-native-amap3d（地址选址）[E→P] |
| 权限 | react-native-permissions（相机/相册/定位/通知，见 08-android/permissions.md） |

## 与 Web 的语义对齐（EXISTING → RN）

- request.js 的 401 刷新/403 防循环逻辑 1:1 移植进 axios interceptors
- statusMaps.js 枚举直接 TS 化（`OrderStatus` 等），Badge 类别映射 tokens
- 路由守卫对齐 router/index.js 6 步（登录提示/redirect/403）
