# 04 · 商家 / 营业时间 / 服务 / 分类 / 地址 / 定位

## Merchant（页面：page-011 / 019）
- `GET /api/merchants` 商家列表（分页）
- `GET /api/merchants/nearby` 附近（定位参数；拒绝降级 [D]）
- `GET /api/merchants/{id}` 详情（store_mode/store_status Badge [E]）
- `GET /api/merchants/my` 我的商家（MERCHANT，页面 019）
- `PUT /api/merchants/{id}` 更新（ADMIN/归属）
- `PATCH /api/merchants/{id}/store-mode` 开/关店（0 自动 1 开 2 关 [E]）
- `POST /api/merchants/{id}/approve | /reject` 入驻审核（ADMIN；MerchantStatus 0→1/2 [E]）

## 营业时间
- `GET /api/merchants/{merchantId}/hours`（页面 011/019）
- `POST /api/merchants/{merchantId}/hours` 批量保存 · `PUT .../hours/{id}`（MERCHANT）

## 服务 ServiceItem（页面：page-003/004/019）
- `GET /api/services/public` —— 公开列表。Query：`keyword, categoryId, page, size, 排序`。响应：PageResult<ServiceItem>
- `GET /api/services/{id}` · `GET /api/services/{serviceId}/detail` 详情
- `GET /api/services/{serviceId}/availability` —— 可约时段（下单前置校验 [E O3]）
- `GET /api/services/merchant/{merchantId}` 商家在售
- `GET /api/services/merchant/{merchantId}/manage` 管理视图（MERCHANT）
- `POST /api/services` 创建 · `PUT /api/services/{id}` 更新 · `DELETE /api/services/{id}`（软删）
- `POST /api/services/{id}/toggle-status` 上/下架（ServiceStatus 0/1 [E]）
- `GET /api/services/category/{categoryId}` 分类下服务
- `POST /api/services/{id}/images` 更新媒体图（或引用 files/upload [D]）

## 服务分类
- `GET /api/service-categories/list`（公开）· `GET /api/service-categories/admin/list` · `POST/PUT/DELETE .../{id}`（ADMIN）

## 地址（页面：page-005/012）
- `GET /api/addresses` 我的地址簿 · `POST /api/addresses` 新增
- `PUT /api/addresses/{id}` · `DELETE /api/addresses/{id}` · `PATCH /api/addresses/{id}/default` 设默认

## 定位配置
- `GET /api/geo/config` —— 地图 Key/服务半径等（页面 003 距离展示 [E useServiceDistance]）
