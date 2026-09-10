# Endpoints（端点全量清单）

> EXISTING：从 55 个 Controller 源码提取的类级 base + 方法级路径。HTTP 动词按语义标注（GET/POST/PUT/DELETE，置信度中，集成时以实际联调为准——记 unresolved.md）。路径中 `{id}` 为路径参数。

## 系统 / 认证

| Base | 路径 |
|---|---|
| `/api/auth` | register · register/captcha · login · refresh · forgot-password · logout |
| `/api/users` | me(GET/PUT) · me/phone · me/email · me/real-name · me/payment-password · passwords/reset-all · {id}/status |
| `/api/roles`（base /api） | roles · roles/{id} · users/{userId}/roles |

## 宠物 / 分类 / 日报

| Base | 路径 |
|---|---|
| `/api/pets` | admin/all · merchant · {id} |
| `/api/categories` | parent/{parentId} · {id} |
| `/api/care-records` | order/{orderId} · {id} · upload(multipart) |

## 寄养域

| Base | 路径 |
|---|---|
| `/api/keepers` | pending · me · my-application · {id} · nearby · merchant/{merchantId} · {id}/approve·reject · {id}/resign · {id}/merchant-approve·reject·terminate · merchant/pending · {id}/online-status |
| `/api/keeper-attendance` | check-in · check-out · me/current · me/today · merchant/today · admin-list |
| `/api/keeper-leaves` | merchant · merchant/{id} · admin-list · {id}/approve·reject |
| `/api/merchants` | {id} · my · nearby · {id}/store-mode · {id}/approve·reject |
| `/api/merchants/{merchantId}/hours` | {id} |
| `/api/service-categories` | list · admin/list · {id} |
| `/api/services` | public · merchant/{merchantId} · {id} · {serviceId}/detail · {serviceId}/manage · {serviceId}/availability · {id}/toggle-status · category/{categoryId} · merchant/{merchantId}/manage · {id}/images |
| `/api/qualifications` | {ownerType}/{ownerId} · pending · {id}/approve·reject |
| `/api/addresses` | {id} · {id}/default |
| `/api/geo` | config |

## 订单 / 支付 / 售后

| Base | 路径 |
|---|---|
| `/api/orders` | merchant · my-keeper · pending · {id} · batch · cancel · accept · reject · delivered · received · start · start/upload(multipart) · complete · {id}/status |
| `/api/order-fulfillments` | {orderId} · {orderId}/timeline · {orderId}/daily-status · {orderId}/conversation(+read) · {orderId}/conversation/upload(multipart) |
| `/api/order-events` | stream（SSE） |
| `/api/payments` | create · pay · admin-list · order/{orderNo} |
| `/api/refunds` | all · {id}/approve · {id}/complete · {id}/reject |
| `/api/tips` | admin-list · order/{orderId} · me |

## 客服 / 消息

| Base | 路径 |
|---|---|
| `/api/chat` | conversation · assign-agent · unread · unread-count · send · read/{messageId} · read-conversation |
| `/api/chat-events` | stream（SSE） |
| `/api/complaints` | targets · all · {id}/evidence · {id}/messages · {id}/accept·resolve·reject |
| `/api/cs-workbench` | stats · merchants · conversations · threads · threads/read |
| `/api/merchant-customer-service` | applications · applications/me · merchant/applications/pending · merchant/staff · merchant/applications/{id}/approve·reject · applications/{id}/resign · merchant/staff/{id}/terminate · admin-list · admin/{id}/approve·reject |
| `/api/ratings` | my · {id}/reply |
| `/api/tickets` | me · {id} · {id}/assign·resolve·close · {id}/messages |

## 财务

| Base | 路径 |
|---|---|
| `/api/wallet` | me · recharge · admin/adjust |
| `/api/transactions` | me |
| `/api/withdrawals` | me · {id}/approve·reject·complete |

## 运营 / 营销 / AI

| Base | 路径 |
|---|---|
| `/api/coupons` | templates · templates/active · templates/{id}/status·grant·grant-all·grant-condition · templates/{id}/claim · my · available · quote |
| `/api/membership` | me · plans/active · benefits/order/quote · usages · admin/*（plans CRUD/status/users/usages/expire） |
| `/api/membership/orders` | {orderNo} · {orderNo}/cancel·pay · admin/list · admin/{orderNo}/pay |
| `/api/notices` | active · unread · {id} · popup · {id}/dismiss-popup · {id}/read |
| `/api/notifications` | unread-count · {id}/read · read-all · admin-list |
| `/api/notification-events` | stream（SSE） |
| `/api/favorites` | page · types · toggle · check · admin-list |
| `/api/files` | product-image · upload(multipart) · {id}/download · admin-list · {id} |
| `/api/reviews` | pending · {id}/approve·reject |
| `/api/operation-logs` | {id} |
| `/api/recycle-bin` | tables · restore · delete |
| `/api/statistics` | admin · user · merchant · reputation |
| `/api/ai` | chat · reports/order/{orderId} · reports/pet/{petId} · reports · care-suggestion · boarding-report · config/test |
| `/api/ai/history` | sessions · sessions/{sessionId} |
| `/api/rag` | documents · documents/upload · documents/{id} · search · ask |
| `/api/agent` | chat · execute |

## 按页面归组

见 `api-mapping` 摘要：page-001→auth；page-002→notices/statistics/services.public；page-003→services.public+categories；page-004→services.detail+availability；page-005→coupons.quote+orders.batch；page-006/007→orders.*+fulfillments；page-008→payments+wallet；page-009→pets；page-010→keepers；page-011→merchants；page-012→users.me+addresses+files；page-013→notifications+notices；page-014→chat+ai；page-015→favorites；page-016→coupons+membership；page-017→wallet+refunds+ratings+complaints+statistics；page-018→keeper-attendance+care-records+orders(pending/my-keeper)；page-019→merchant 域；page-020→审核域；page-021→cs-workbench+tickets+complaints。
