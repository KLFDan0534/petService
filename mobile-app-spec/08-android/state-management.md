# Android State Management（状态管理）

## 统一 UiState（RECOMMENDATION）

```kotlin
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data object Empty : UiState<Nothing>
    data class Error(val cause: ErrorCause, val message: String) : UiState<Nothing>
}
// 扩展：Refreshing（下拉）、Submitting（提交中）、Offline
```

## Trigger → UI → Action（全页面标准，对应 page-index 模板）

| State | Trigger | UI | Action |
|---|---|---|---|
| Loading | 首次进入/切筛选 | 骨架屏（卡片 shimmer） | 自动请求 |
| Success | 请求成功 | 内容渲染 | — |
| Empty | Success 且空 | EmptyState（引导操作） | 跳转引导 |
| Error | 请求失败 | 页面级 ErrorState / 列表错误条 | Retry 重放 |
| Refreshing | 下拉 | 刷新指示 | 拉第 1 页 |
| Submitting | 表单提交 | 按钮 spinner，全局禁点 | 提交 |
| Offline | 连接回调 | 顶部/底部横幅 | 恢复后自动重试 [R] |

## ViewModel 模式

- 每屏一个 ViewModel；输入：事件（sealed Event）；输出：`StateFlow<UiState>` + `SharedFlow<Effect>`（Toast/导航）
- 列表：Paging 3 `cachedIn(viewModelScope)`；筛选变更 → 重置数据源
- 订单/会话实时：SSE Flow → merge 手动刷新；**不在 VM 里预测状态机**（重拉详情）
- 会话内保留：筛选/滚动位置由 VM 持有（对齐 Web keepAlive [E]）

## 跨屏共享

- 未读数：全局单例 Repository（SSE + unread-count）→ 首页/我的 Badge
- 登录态：AuthRepository（StateFlow<User?>）驱动导航图重建
