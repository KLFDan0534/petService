# page-003 · 服务市场（Services）★视觉权威页

- **Purpose**：浏览/筛选全部上架服务 [E /services]
- **Role**：公开；下单动作需登录
- **Entry**：Tab「服务」/ 首页宫格；**Exit**：ServiceDetail

## Layout（复刻 Services.vue 视觉结构 [E]）

```
Eyebrow（9.5px + 32px 线）→ displayL 标题 → subtitle 副文案
→ Chip Rail（分类，gap8 横滚；active=ink 反白；含 count）
→ Toolbar：搜索框(44px, 清除钮) + 重置(ghost)
→ Section Band：结果标题 + 计数
→ 2 列 ServiceGrid（媒体 4:3 + 分类 label + 标题 + 描述 + 价格 stat + 距离标签 primary12% wash）
→ 底部加载更多（spinner 24px）
```

## API（EXISTING）

`GET /api/services/public`（关键词/分类/分页/排序）· `GET /api/service-categories/list` · 距离 `GET /api/geo/config` + useServiceDistance [E]。

## States

首载：卡片骨架 ×4；空：EmptyState（无匹配服务 + 清筛选操作）；错误：重试条；追加：底部 spinner。
筛选即时生效（防抖 300ms [R]）；收藏按钮乐观更新（FavoriteToggleButton [E]）。

## 权限

浏览公开；收藏/下单未登录 → LoginPromptDialog（App：跳登录并回跳 [P]）。
