# 02 · 宠物 / 分类 / 照护日报（Pet · Category · CareRecord）

## GET /api/pets —— 我的宠物列表
权限：OWNER（用户区）。页面：page-009
响应：PageResult<Pet>（data-models Pet）。排序：created_at_wsh 倒序 [D]

## GET /api/pets/{id} —— 宠物详情
权限：归属校验 [E OwnershipValidator]。错误：404/无权

## POST /api/pets —— 创建宠物
请求：`name_wsh*` `species_wsh*` `breed_wsh? gender_wsh? birth_wsh? weight_wsh? avatar_wsh? images_wsh[]? note_wsh?`
校验：名字/物种必填；体重>0 [D]

## PUT /api/pets/{id} / DELETE /api/pets/{id}
删除=软删除 [E deleted_wsh]；被订单引用时后端拒绝（错误透传）

## GET /api/pets/merchant —— 商家在养宠物
权限：MERCHANT。页面：page-019

## GET /api/pets/admin/all —— 全量宠物
权限：ADMIN。页面：page-020

## 分类（两级树）
- `GET /api/categories/parent/{parentId}` 子分类（parentId=0 为根 [D]）
- `GET /api/categories/{id}` · `POST/PUT/DELETE /api/categories/{id}`（ADMIN）
页面：page-003 筛选 Rail 数据源

## 照护日报 CareRecord
- `GET /api/care-records/order/{orderId}` —— 订单全部日报。页面：page-007 日报区
- `POST /api/care-records/upload`（multipart）—— 新增日报（内容+图片 [E]）。权限：KEEPER/MERCHANT（订单执行方）
- `GET /api/care-records/{id}` · `PUT /api/care-records/{id}` · `DELETE /api/care-records/{id}`（当日可改 [D]）
错误：非执行方无权 · 图片超限（见 upload.md）
