# Coding Guidelines（编码规范）

## Kotlin/Compose

- 官方 Kotlin Coding Conventions；Compose 参数顺序：modifier 第一、lambda 最后
- 单 Activity + Navigation-Compose；Screen 无业务逻辑、无数据源依赖
- VM 状态不可变（data class）；Effect 用 Channel/SharedFlow
- DI 全 Hilt；时间 `java.time`；金额 `BigDecimal`

## 设计系统纪律

- UI 值仅来自 `QiyuTheme`（colors/typography/spacing/shape/elevation）；code review 检查裸 dp/sp/color
- 文案 `strings.xml`（zh-CN 默认 [E]）；占位/时间/金额统一格式化工具

## 数据层纪律

- DTO（`_wsh`）与 Domain Model 分离；mapper 单向
- Repository 返回 Flow/Result；不暴露 suspend 直接调用链给 UI
- 错误统一 `ApiException(code,message)` → UiState.Error 映射（06-api/errors.md）

## 通用

- 每文件单一职责；命名与 03-pages 术语表一致
- 注释解释「为什么」；对 EXISTING 契约的行为在 Repository 注释来源
- Git：feature 分支 + PR；提交信息 `feat(orders): ...`

## 安全（见 security.md）

Token 不打印；API 内容纯文本渲染（PRODUCT.md [E]）；Web 上 unsafe HTML 禁令在 App 对应为「不启用任何富文本解释」。
