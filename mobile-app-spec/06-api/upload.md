# Upload（文件上传）

## 后端（EXISTING）

- 统一入口：`POST /api/files/upload`（multipart/form-data）→ MinIO
- 商品图：`POST /api/files/product-image`
- 下载/访问：`GET /api/files/{id}/download`（前端用 URL 直接渲染 [E fileUrls]）
- 管理列表：`GET /api/files/admin-list`；删除 `DELETE /api/files/{id}`

## 业务上传端点（multipart，EXISTING）

| 端点 | 用途 | 页面 |
|---|---|---|
| `POST /api/orders/start/upload` | 开始服务传图 | page-007/018 |
| `POST /api/care-records/upload` | 照护日报图片 | page-007/018 |
| `POST /api/order-fulfillments/{orderId}/timeline/upload` | 履约时间线图 | page-007 |
| `POST /api/order-fulfillments/{orderId}/conversation/upload` | 履约会话图片 | page-007/014 |
| `POST /api/complaints/{id}/evidence` | 投诉证据 | page-017 |
| `POST /api/rag/documents/upload` | RAG 知识文档 | ADMIN |

## App 实现规范（RECOMMENDATION/P）

- 来源：相机拍照 / 相册多选；权限见 08-android/permissions.md
- 处理：压缩至长边 ≤1920px、质量 80；单文件 ≤10MB（超限提示）[R]
- 请求：`multipart/form-data`，字段名以联调为准（`file` 惯例 [D]）；带 Bearer
- 上传中：缩略图 + 进度；失败可重传；成功回填 URL
- 展示：MediaWithFallback 语义——加载中占位 surfaceVariant、失败显示占位图 [E]
- 图片查看器：全屏 + 双指缩放 + 保存到相册 [P]

## 禁止

- 绕过 `/api/files` 直传 MinIO；上传后本地 URL 拼接而不校验返回结构
