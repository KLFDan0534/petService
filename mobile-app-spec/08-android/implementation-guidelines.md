# Android Implementation Guidelines（实现总则）

## 每个页面的实现顺序（强制）

```
1 查 03-pages/pages/page-xxx.md → 2 选取/新建组件(02-components)
3 ViewModel(UiState/Event/Effect) → 4 Repository+ApiService(06-api)
5 四态 UI(Loading/Empty/Error/Success) → 6 权限(04-user-roles)
7 动效(01-design-system/motion) → 8 无障碍(01-design-system/accessibility)
9 自测(09-development/testing 清单)
```

## 编码红线

- 禁止硬编码颜色/字号/圆角/间距 —— 一律 Token（core/designsystem）
- 禁止 Screen 直接调 Retrofit；禁止 UI 层写业务规则（状态白名单来自 VM）
- 禁止重复实现已有组件；禁止绕过 Repository
- 状态流转以服务端为准；本地只做展示白名单
- 所有用户可见文案走 strings.xml；错误 message 优先服务端 [E]

## 复用清单（先查再写）

按钮/Chip/输入/卡片/列表项/徽章/头像/弹窗/BottomSheet/Empty/Error/Loading/图片组件/收藏按钮/聊天气泡/时间线 —— 全部在 02-components 有规格。

## 联调注意

- 字段 `_wsh` 后缀；`Result` 包裹；分页 PageResult
- SSE `?token=`；multipart 上传端点见 06-api/upload.md
- 支付成功以 `/payments/order/{orderNo}` 轮询确认 [E]
