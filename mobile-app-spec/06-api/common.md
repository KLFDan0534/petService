# Common（通用契约）

## 响应包裹 Result<T>（EXISTING pet-common）

```json
{ "code": 200, "message": "success", "data": { } }
```

- `code=200` 成功；`code=401/403` 认证/权限（与 HTTP 状态码双轨，拦截器两者都处理 [E]）
- 其他非 200 → 业务错误，`message` 直接面向用户展示（Web 直接 Toast message [E]）
- App 规则 [P]：Repository 层解包：code==200 → data；否则抛 `ApiException(code,message)`

## PageResult<T>（EXISTING pet-common）

```json
{ "code": 200, "message": "success",
  "data": { "records": [ ... ], "total": 100, "size": 10, "current": 1 } }
```
（字段名以实际响应为准；App 分页统一映射为 `items/totalCount` —— 详见 pagination.md）

## 命名约定（EXISTING）

| 约定 | 示例 |
|---|---|
| 字段后缀 `_wsh` | `id_wsh` `status_wsh` `roles_wsh` `access_token_wsh` |
| 审计字段 | `created_at_wsh` `updated_at_wsh`（自动填充） |
| 软删除 | `deleted_wsh`（列表默认过滤） |
| 前端合成标签 | `status_label_wsh`（App 端改为本地 statusMaps 映射 [P]） |

## 幂等与并发（DERIVED）

- 写操作（提交订单/支付/审核）提交中禁点（loading 状态即幂等护栏）
- 支付以 `orderNo` 为键查询结果（`/payments/order/{orderNo}`）
- 状态操作失败大多为状态机冲突 → 重新拉详情

## 时间与数字（P）

- 时间：ISO 字符串 → 本地时区展示；订单倒计时用服务端时间校准 [D orderPaymentTimeout]
- 金额：字符串/数值两位小数；展示 `¥` + `tabular-nums`
