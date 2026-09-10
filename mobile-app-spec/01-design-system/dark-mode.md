# Dark Mode（暗色模式）

## 现状（EXISTING）

- Web 触发：`html[data-theme="dark"]` + `.dark` + `color-scheme`，由 Pinia `app.setTheme()` 统一写入；持久化 localStorage `pet-service-theme`；初始值 = 本地存储 > 系统 `prefers-color-scheme`；提供 ThemeToggle 组件。
- Token 权威唯一来源是 `design-tokens.css`（app store **不在运行时注入 Token**，避免双重覆盖——该架构决策必须继承）。

## Dark Token 值（EXISTING）

background `#171310` · surface `#201B15` · surfaceVariant `#2A241D` · surfaceTint `#2E2820` · border `#393129` · ink `#F2EBDD` · ink-soft `#D8CEBB` · muted `#B6A892` · brand `#E0823F` · brand-deep `#C4632A`。

状态 wash（暗色配方 EXISTING）：背景 14–16% alpha + 亮色文字（success `rgba(52,211,153,.14)`+`#86EFAC`、warning `rgba(251,191,36,.16)`+`#FDE68A`、danger `rgba(248,113,113,.14)`+`#FCA5A5`、info `rgba(96,165,250,.16)`+`#93C5FD`、primary `rgba(245,158,11,.16)`+`#FCD34D`）。

## Android 实现（RECOMMENDATION）

双 Palette（light/dark）+ `isSystemInDarkTheme()`；用户设置存 DataStore（system/light/dark 三选，默认跟随系统）；状态栏图标色随主题切换。

## iOS 实现（RECOMMENDATION）

Asset Catalog 双外观 + `preferredColorScheme` 绑定用户设置。

## 规则

- 全部颜色走 Token，禁止运行时改色；
- 静置零阴影 → 暗色无需阴影适配；
- 媒体占位用 surfaceVariant；
- 主题切换动画：200ms color transition（对应 Web body transition [E]）。
