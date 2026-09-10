# API Integration Rules（API 集成规则）

## 调用链（强制）

```
UI(Screen) → ViewModel → (UseCase) → Repository → ApiService(Retrofit)
```

禁止：Composable/Screen 直接持有 Retrofit；禁止在 Repository 写 UI 语义（Toast/导航）。

## 集成步骤（每个端点）

1. 在 endpoints.md 确认路径与归属 Controller
2. data-models.md 定义/复用 DTO（`_wsh` 字段，`ignoreUnknownKeys`）
3. Repository 解包 `Result`：code==200 → data；否则 `ApiException`；401/403 走拦截器链
4. ViewModel 映射 AppError → UiState（errors.md）
5. 分页端点接 Paging 3（pagination.md）
6. 写操作加防重（Submitting 态）；资金操作后拉取权威状态（如 `/payments/order/{orderNo}`）

## 特殊端点规则

| 类型 | 规则 |
|---|---|
| auth/refresh | 仅 TokenAuthenticator 调用；并发锁；失败广播登出 |
| SSE stream | SseClient 统一管理（realtime.md）；不进 Repository 分页体系 |
| multipart 上传 | 独立 service + 进度；压缩后提交（upload.md） |
| 状态流转（orders/*） | 调用后重拉详情，不本地改状态 |
| quote 类（券/会员） | 输入变化防抖后重算；展示不缓存为最终价 |

## 契约差异处理

联调发现字段/行为与规格不符 → 停止猜测：以真实响应修正 DTO，并在 99-reference/conflicts.md 记录（规格值 vs 实际值 vs 采用）。禁止静默改规格。
