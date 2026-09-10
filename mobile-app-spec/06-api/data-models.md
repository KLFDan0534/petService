# Data Models（请求/响应模型）

> 核心模型字段 EXISTING（前端消费代码反推）；其余按 `_wsh` 命名约定 DERIVED。统一包裹 `Result<T>`（见 common.md）。

## Auth

```jsonc
// POST /auth/login 请求
{ "username_wsh": "string", "password_wsh": "string" }
// 响应 data：见 authentication.md（token 对 + user + roles_wsh）
// POST /auth/refresh 请求 { "refresh_token_wsh": "jwt" } → 同登录 data
```

## User

```jsonc
{ "id_wsh": 1, "username_wsh": "", "nickname_wsh": "", "avatar_wsh": "url",
  "roles_wsh": ["OWNER"], "phone_wsh": "", "email_wsh": "",
  "real_name_status_wsh": 0, "status_wsh": 1, "created_at_wsh": "" }
```

## Pet

```jsonc
{ "id_wsh": 1, "name_wsh": "", "species_wsh": "", "breed_wsh": "",
  "gender_wsh": 0, "birth_wsh": "", "weight_wsh": 4.5,
  "avatar_wsh": "url", "images_wsh": ["url"], "note_wsh": "",
  "owner_id_wsh": 1 }
```

## ServiceItem（服务）

```jsonc
{ "id_wsh": 1, "title_wsh": "", "category_id_wsh": 1,
  "price_wsh": 128.00, "unit_wsh": "天", "status_wsh": 1,
  "description_wsh": "", "images_wsh": ["url"],
  "merchant_id_wsh": 1, "merchant_name_wsh": "",
  "rating_wsh": 4.8, "distance_wsh": 1.2 }
// availability：时段/库存结构以联调为准（unresolved）
```

## Order

```jsonc
{ "id_wsh": 1, "order_no_wsh": "", "status_wsh": "pending",
  "service_id_wsh": 1, "service_title_wsh": "", "merchant_id_wsh": 1,
  "keeper_id_wsh": null, "pet_ids_wsh": [1],
  "address_wsh": {}, "start_date_wsh": "", "end_date_wsh": "",
  "amount_wsh": 256.00, "coupon_id_wsh": null, "discount_wsh": 0,
  "pay_expire_at_wsh": "", "created_at_wsh": "" }
// 列表项含 status_label 徽章映射（App 本地 statusMaps）
```

## OrderFulfillment / CareRecord

```jsonc
{ "order_id_wsh": 1, "timeline_wsh": [ { "action_wsh": "start",
    "time_wsh": "", "images_wsh": [], "operator_wsh": "" } ],
  "daily_status_wsh": [ { "date_wsh": "", "content_wsh": "", "images_wsh": [] } ] }
```

## Wallet / Transaction / Withdrawal

```jsonc
{ "balance_wsh": 128.50 }
{ "id_wsh": 1, "type_wsh": "", "amount_wsh": "+20.00",
  "status_wsh": "success", "created_at_wsh": "" }
{ "id_wsh": 1, "amount_wsh": 100.00, "status_wsh": "pending" }
```

## Conversation / Message / Notification

```jsonc
{ "conversation_id_wsh": 1, "peer_wsh": { "id_wsh": 1, "name_wsh": "", "avatar_wsh": "" },
  "last_message_wsh": "", "unread_wsh": 2 }
{ "id_wsh": 1, "content_wsh": "", "type_wsh": "text|image", "sender_id_wsh": 1, "time_wsh": "" }
{ "id_wsh": 1, "type_wsh": "system|order|notice", "title_wsh": "", "read_wsh": 0, "created_at_wsh": "" }
```

## 校验规则

| 字段 | 规则 |
|---|---|
| 手机/邮箱/用户名 | 非空+格式；唯一性后端校验 |
| 金额 | >0，两位小数 |
| 日期 | start≤end；在 availability 内 |
| 密码/支付密码 | 一致性+强度（注册/改密页） |
| 评价/投诉/日报内容 | 非空；图片≤上传上限 |

未知字段处理：解析器忽略未知字段；必需字段缺失 → 数据异常 ErrorState。
