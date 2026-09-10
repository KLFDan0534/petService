# 09 · 运营（公告 / 通知 / 收藏 / 文件 / 审核 / 审计 / 回收站 / 统计）

## Notice 公告（页面：page-002 / 013）
- `GET /api/notices/active` 生效公告 · `GET /api/notices/unread` 未读
- `GET /api/notices/popup` 弹窗公告（启动 PopupNotice [E]）
- `POST /api/notices/{id}/read` 已读 · `POST /api/notices/{id}/dismiss-popup` 关闭弹窗（不再弹 [E]）
- `GET /api/notices/{id}` 详情
- `POST /api/notices` · `PUT/DELETE /api/notices/{id}`（ADMIN，BannerStatus 0/1 [E]）

## Notification 通知（页面：page-013）
- `GET /api/notifications/unread-count` 未读数（红点初始化 [E]）
- `PATCH /api/notifications/{id}/read` 单条已读
- `POST /api/notifications/read-all` 全部已读
- `GET /api/notifications/admin-list`（ADMIN）
- `GET /api/notification-events/stream?token=` SSE（红点增量 [E]）

## Favorite 收藏（页面：page-015）
- `GET /api/favorites/page?targetType=merchant|keeper|service` 分页收藏
- `GET /api/favorites/types` 各类型计数（Tab 徽标 [D]）
- `POST /api/favorites/toggle` 切换收藏（乐观更新+失败回滚 [E]）
- `GET /api/favorites/check?targetType&targetId` 详情页回显
- `GET /api/favorites/admin-list`（ADMIN）

## File 文件（页面：全局上传；upload.md）
- `POST /api/files/upload`（multipart 统一入口）→ MinIO，返回 id/url
- `POST /api/files/product-image` 商品图
- `GET /api/files/{id}/download` · `DELETE /api/files/{id}`（我的文件 page-012）
- `GET /api/files/admin-list`（ADMIN）

## ContentReview 内容审核（ADMIN；page-020）
- `GET /api/reviews/pending` 待审 → `POST /api/reviews/{id}/approve | /reject`（ReviewStatus [E]）

## OperationLog（ADMIN）
- `GET /api/operation-logs/{id}`（及列表端点 [D]）—— 审计查询

## RecycleBin 回收站（ADMIN）
- `GET /api/recycle-bin/tables` 可回收表 · `POST /api/recycle-bin/restore` 恢复 · `POST /api/recycle-bin/delete` 彻底删除（均 danger 确认 [D]）

## Statistics（页面：page-002 / 017 / 019 / 020）
- `GET /api/statistics/admin` 管理仪表盘 · `GET /api/statistics/merchant` 商家统计
- `GET /api/statistics/user` 用户统计 · `GET /api/statistics/reputation` 信用分
