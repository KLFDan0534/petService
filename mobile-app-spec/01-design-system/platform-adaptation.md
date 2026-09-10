# Android Adaptation（平台适配层）

> Core 语言（颜色/字体/间距/圆角/组件 DNA/Motion DNA）跨平台统一，本文件定义 Android 特有映射。

| 主题 | 规格 |
|---|---|
| Navigation | Top App Bar（surface 底 + ink 标题 + 1px 线）+ **Bottom Navigation** 56dp，5 Tab：首页/服务/宠物/订单/我的 [DERIVED from Web nav 项] |
| Back | 系统返回 + Predictive Back；优先级 Sheet/Dialog > 页面 > 退出 |
| Dialog | Material Dialog：radius 18、scrim `rgba(30,22,14,.42)`、标题 displayS 衬线 |
| Bottom Sheet | ModalBottomSheet，顶部 radius 18，把手 [R] |
| Snackbar | 底部，elevation.4，深底白字/状态色，3s，可带操作（Web Toast 内容平移） |
| Touch | ripple：primary 按钮=onPrimary 12%；中性=ink 8% |
| Edge-to-edge | 状态栏/导航栏透明，Scaffold contentWindowInsets 处理；画布=background |
| 状态栏 | 图标色随主题（light 模式 ink / dark 模式 cream） |
| 字体 | Roboto + 内嵌 Noto Serif SC；sp 跟随系统缩放 |
| 通知 | FCM [RECOMMENDATION]：兜底 SSE 掉线场景，通知渠道分 system/order/notice（对应 NotificationTypeMap [E]） |
| 深链 [R] | `qiyu://order/{id}`、`qiyu://service/{id}`、`https://…/orders/{id}` App Links |
| 权限 | 见 08-android/permissions.md（相机/定位/存储/通知） |

## iOS Adaptation

| 主题 | 规格 |
|---|---|
| Navigation | Nav Bar + **Large Title**（displayXL 衬线，滚动折叠居中）；边缘滑返回 |
| Tab Bar | 49pt + Home Indicator；毛玻璃（对应 Web 顶栏 blur20/saturate1.35 语言） |
| Sheet | Sheet + grabber，顶部 radius 18 |
| Alert | 居中纵排按钮（对应 Dialog footer 小屏 column-reverse [E]） |
| Action Sheet | 长按/更多操作 |
| Safe Area | 顶 47/59pt + 底 34pt 全程尊重 |
| Dynamic Type | Body ±2pt |
| 字体 | PingFang SC + Noto Serif SC |
| 推送 [R] | APNs，同上分渠道 |

## 不强行统一项

返回方式、Sheet/Alert 系统组件、通知系统 UI、字体缩放机制、日期/选择器——按平台习惯，视觉 Token 仍统一。
