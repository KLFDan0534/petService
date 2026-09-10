# Android Project Structure（工程组织建议）

## 包结构（RECOMMENDATION）

```
com.qiyu.pet/
├── QiyuApp.kt                    # @HiltAndroidApp；SSE/通知初始化
├── MainActivity.kt               # 单 Activity + Compose
├── core/
│   ├── designsystem/
│   │   ├── theme/                # Color.kt(双主题 Token) Type.kt Shape.kt Elevation.kt
│   │   └── components/           # QiyuButton/Chip/Input/Card/Badge/EmptyState/ErrorState/...
│   ├── network/                  # AuthInterceptor · TokenAuthenticator · SseClient · ApiClient
│   ├── common/                   # Result 包装 · 时间/金额格式 · 扩展
│   └── storage/                  # TokenStore(DataStore) · SettingsStore
├── data/
│   ├── remote/dto/               # _wsh 字段 DTO（data-models.md）
│   ├── local/                    # Room: entities/daos + Converters
│   └── repository/               # AuthRepo OrderRepo ServiceRepo ChatRepo ...
├── domain/model/                 # UI 模型 + 枚举(statusMaps 映射)
└── feature/
    ├── auth/ home/ services/ service_detail/ order_create/ orders/ order_detail/
    ├── payment/ pets/ keepers/ merchants/ profile/ notifications/ chat/ ai/
    ├── favorites/ coupons/ membership/ wallet/ aftersales/
    ├── keeper/ merchant/ support/ admin/
    └── error/                    # 403/404/500/503
```

## 命名与约定

- Screen：`XxxScreen` + `XxxViewModel` + `XxxUiState`；Route 常量集中 `Routes.kt`
- 资源：颜色/文案入 `strings.xml`（中文为默认 locale [E 品牌语言]）
- 每个 feature 内自洽（Screen/VM/组件），跨 feature 组件上移 designsystem

## 构建配置

- `minSdk 26` · target 最新 [R]； flavors：dev/prod（BASE_URL 注入 [D `/api` 契约]）
- 依赖注入全部 Hilt；混淆规则覆盖 serialization/枚举
