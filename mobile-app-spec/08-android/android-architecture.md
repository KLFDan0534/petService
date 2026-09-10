# Android Architecture（推荐架构）

> 全部 RECOMMENDATION（项目无 Android 现状）；语义对齐 Web 既有分层（request.js 拦截器 / Pinia / router 守卫）。

## 技术栈

Kotlin · Jetpack Compose · Material3（主题映射 Warm Editorial Token）· MVVM + 轻量 Clean · Coroutines/Flow · Navigation-Compose · Retrofit/OkHttp + kotlinx.serialization · Room · DataStore(+Encrypted) · Paging 3 · Hilt · Coil · OkHttp-SSE。

## 分层

```
:app
 ├─ ui/            Screen(Compose) + ViewModel(UiState/Event) + Navigation
 ├─ domain/        UseCase（复杂业务才建，如下单报价）/ Model
 ├─ data/
 │   ├─ remote/    ApiService(Retrofit) + DTO + ApiException
 │   ├─ local/     Room DAO/Entity + DataStore
 │   └─ repository/ 接口 + 实现（解包 Result、错误映射、缓存编排）
 └─ core/          designsystem(Token/Theme/组件) · network(拦截器/SSE) · common
```

## 铁律

1. UI 不直接依赖 Retrofit；Screen 只见 UiState
2. Repository 是唯一数据出口；DTO 与 UI Model 分离
3. 所有颜色/字体/尺寸来自 DesignSystem 单一来源（tokens）
4. 状态机在 ViewModel 层校验（展示白名单），真实裁决永远在后端
5. 全部异步 Flow；统一 `UiState<T> = Loading|Success|Empty|Error`

## 模块化（可选演进）

按域拆 `:feature:home/:services/:orders/:chat/:keeper/:merchant/:support/:admin` + `:core:designsystem/:network/:data` [R]。
