# Required Annotations — Must Keep

## Principle

Annotations are required when they change behavior from the Jackson default.

---

## Category 1: Mapper Annotations (bridging camelCase ↔ `_wsh`)

These DTOs use clean camelCase Java field names. `@JsonProperty("xxx_wsh")` maps JSON `xxx_wsh` to the Java field. **Removing these would break deserialization.**

| File | Annotation | Java Field | Purpose |
|---|---|---|---|
| `LoginResponse.java` | `@JsonProperty("access_token_wsh")` | `accessToken` | JWT login response |
| `LoginResponse.java` | `@JsonProperty("refresh_token_wsh")` | `refreshToken` | JWT refresh token |
| `LoginResponse.java` | `@JsonProperty("user_id_wsh")` | `userId` | User ID in login response |
| `LoginResponse.java` | `@JsonProperty("username_wsh")` | `username` | Username in login response |
| `LoginResponse.java` | `@JsonProperty("nickname_wsh")` | `nickname` | Nickname in login response |
| `LoginResponse.java` | `@JsonProperty("roles_wsh")` | `roles` | Roles in login response |
| `ForgotPasswordRequestDTO.java` | `@JsonProperty("email_wsh")` | `email` | Email field |
| `RefreshTokenRequestDTO.java` | `@JsonProperty("refresh_token_wsh")` | `refreshToken` | Refresh token field |
| `UserUpdateProfileRequestDTO.java` | `@JsonProperty("nickname_wsh")` | `nickname` | Profile update |
| `UserUpdateProfileRequestDTO.java` | `@JsonProperty("phone_wsh")` | `phone` | Profile update |
| `UserUpdateProfileRequestDTO.java` | `@JsonProperty("avatar_wsh")` | `avatar` | Profile update |
| `UserUpdateProfileRequestDTO.java` | `@JsonProperty("gender_wsh")` | `gender` | Profile update |
| `UserUpdatePhoneRequestDTO.java` | `@JsonProperty("phone_wsh")` | `phone` | Phone update |
| `UserUpdateEmailRequestDTO.java` | `@JsonProperty("email_wsh")` | `email` | Email update |
| `UserUpdateStatusRequestDTO.java` | `@JsonProperty("status_wsh")` | `status` | Status update |
| `UserUpdateRealNameRequestDTO.java` | `@JsonProperty("real_name_wsh")` | `realName` | Real name update |
| `UserUpdateRealNameRequestDTO.java` | `@JsonProperty("id_card_no_wsh")` | `idCardNo` | ID card update |
| `OrderCancelRequestDTO.java` | `@JsonProperty("order_id_wsh")` | `orderId` | Order cancel |
| `OrderCancelRequestDTO.java` | `@JsonProperty("order_no_wsh")` | `orderNo` | Order cancel |
| `OrderDeliveredRequestDTO.java` | `@JsonProperty("order_id_wsh")` | `orderId` | Mark delivered |
| `OrderDeliveredRequestDTO.java` | `@JsonProperty("order_no_wsh")` | `orderNo` | Mark delivered |
| `OrderReceivedRequestDTO.java` | `@JsonProperty("order_no_wsh")` | `orderNo` | Confirm received |
| `OrderReceivedRequestDTO.java` | `@JsonProperty("handover_code_wsh")` | `handoverCode` | Handover code |
| `OrderReceivedRequestDTO.java` | `@JsonProperty("status_wsh")` | `status` | Status update |
| `OrderAcceptRequestDTO.java` | `@JsonProperty("order_no_wsh")` | `orderNo` | Accept order |
| `OrderCompleteRequestDTO.java` | `@JsonProperty("order_no_wsh")` | `orderNo` | Complete order |
| `OrderRejectRequestDTO.java` | `@JsonProperty("order_no_wsh")` | `orderNo` | Reject order |
| `OrderStartRequestDTO.java` | `@JsonProperty("order_no_wsh")` | `orderNo` | Start service |
| `OrderStartRequestDTO.java` | `@JsonProperty("start_photo_url_wsh")` | `startPhotoUrl` | Start photo |
| `OrderUpdateStatusRequestDTO.java` | `@JsonProperty("status_wsh")` | `status` | Force status update |
| `WithdrawalApplyRequestDTO.java` | `@JsonProperty("amount_wsh")` | `amount` | Withdrawal apply |
| `WithdrawalApplyRequestDTO.java` | `@JsonProperty("bank_card_no_wsh")` | `bankCardNo` | Bank card |
| `WithdrawalApplyRequestDTO.java` | `@JsonProperty("bank_name_wsh")` | `bankName` | Bank name |
| `WithdrawalApplyRequestDTO.java` | `@JsonProperty("account_name_wsh")` | `accountName` | Account name |
| `WithdrawalApproveRequestDTO.java` | `@JsonProperty("remark_wsh")` | `remark` | Approval remark |
| `WithdrawalRejectRequestDTO.java` | `@JsonProperty("remark_wsh")` | `remark` | Rejection remark |
| `FavoriteToggleRequestDTO.java` | `@JsonProperty("target_id_wsh")` | `targetId` | Favorite target |
| `FavoriteToggleRequestDTO.java` | `@JsonProperty("target_type_wsh")` | `targetType` | Favorite type |
| `ContentReviewReportRequestDTO.java` | `@JsonProperty("target_id_wsh")` | `targetId` | Content report |
| `ContentReviewReportRequestDTO.java` | `@JsonProperty("target_type_wsh")` | `targetType` | Content report |
| `ContentReviewReportRequestDTO.java` | `@JsonProperty("reason_wsh")` | `reason` | Report reason |
| `ContentReviewRejectRequestDTO.java` | `@JsonProperty("remark_wsh")` | `remark` | Review reject |
| `ContentReviewApproveRequestDTO.java` | `@JsonProperty("remark_wsh")` | `remark` | Review approve |
| `RatingCreateRequestDTO.java` | `@JsonProperty("order_id_wsh")` | `orderId` | Create rating |
| `RatingCreateRequestDTO.java` | `@JsonProperty("target_id_wsh")` | `targetId` | Rating target |
| `RatingCreateRequestDTO.java` | `@JsonProperty("target_type_wsh")` | `targetType` | Rating type |
| `RatingCreateRequestDTO.java` | `@JsonProperty("score_wsh")` | `score` | Rating score |
| `RatingCreateRequestDTO.java` | `@JsonProperty("content_wsh")` | `content` | Rating content |
| `RatingCreateRequestDTO.java` | `@JsonProperty("images_wsh")` | `images` | Rating images |
| `RatingReplyRequestDTO.java` | `@JsonProperty("reply_wsh")` | `reply` | Rating reply |
| `ChatSendRequestDTO.java` | `@JsonProperty("to_user_id_wsh")` | `toUserId` | Chat target |
| `ChatSendRequestDTO.java` | `@JsonProperty("order_id_wsh")` | `orderId` | Chat order |
| `ChatSendRequestDTO.java` | `@JsonProperty("content_wsh")` | `content` | Chat content |
| `ChatMarkConversationReadRequestDTO.java` | `@JsonProperty("from_user_id_wsh")` | `fromUserId` | Mark read |
| `ChatMarkConversationReadRequestDTO.java` | `@JsonProperty("order_id_wsh")` | `orderId` | Mark read |
| `AdoptionReviewRequestDTO.java` | `@JsonProperty("status_wsh")` | `status` | Review adoption |
| `AdoptionReviewRequestDTO.java` | `@JsonProperty("remark_wsh")` | `remark` | Review remark |
| `AiReportGenerateRequestDTO.java` | `@JsonProperty("order_no_wsh")` | `orderNo` | AI report |
| `AiReportGenerateRequestDTO.java` | `@JsonProperty("report_type_wsh")` | `reportType` | AI report type |
| `AiReportGenerateRequestDTO.java` | `@JsonProperty("language_wsh")` | `language` | AI report language |
| `AiReportCreateRequestDTO.java` | `@JsonProperty("order_no_wsh")` | `orderNo` | AI report create |
| `AiReportCreateRequestDTO.java` | `@JsonProperty("report_type_wsh")` | `reportType` | AI report type |
| `AiReportCreateRequestDTO.java` | `@JsonProperty("title_wsh")` | `title` | AI report title |
| `AiReportCreateRequestDTO.java` | `@JsonProperty("content_wsh")` | `content` | AI report content |
| `AiReportCreateRequestDTO.java` | `@JsonProperty("language_wsh")` | `language` | AI report language |

**Total: ~70 required @JsonProperty annotations** (bridging camelCase Java → `_wsh` JSON)

---

## Category 2: Security Annotations (@JsonIgnore)

| File | Field | Reason |
|---|---|---|
| `User.java` | `password_wsh` | Password must never be serialized |
| `User.java` | `deleted_wsh` | Internal soft-delete flag |
| `UserRole.java` | `deleted_wsh` | Internal soft-delete flag |
| `Role.java` | `deleted_wsh` | Internal soft-delete flag |
| All 28 entities with `deleted_wsh` | `deleted_wsh` | Internal soft-delete flag |

**Total: 32 required @JsonIgnore annotations** — all security/internal. Must keep.

---

## Category 3: Dual-Name Output (@JsonGetter)

10 entities use `@JsonGetter` to produce camelCase aliases alongside `_wsh` output:

| Entity | @JsonGetter Count | CamelCase Aliases Produced |
|---|---|---|
| Pet.java | 14 | id, ownerId, name, type, breed, age, weight, gender, sterilized, vaccinated, avatar, description, allergies, habits, ownerName |
| PetOrder.java | 20 | id, orderNo, ownerId, petId, keeperId, merchantId, serviceId, startDate, endDate, status, handoverCode, deliveryAddress, deliveryTime, receiverAvailableStart, receiverAvailableEnd, pickupAddress, pickupTime, startPhoto, totalAmount, finalAmount |
| CareRecord.java | 9 | id, orderId, petId, keeperId, type, content, images, recordTime |
| Keeper.java | 12 | id, merchantId, userId, name, phone, avatar, experienceYears, rating, completionRate, complaintRate, pricePerDay, maxPets, currentPets, bio, status |
| Merchant.java | 10 | id, name, phone, avatar, address, latitude, longitude, description, businessLicense, status, distance |
| Rating.java | 14 | id, orderId, targetId, targetType, score, content, images, reply, replyAt, createdAt |
| ChatMessage.java | 7 | id, fromUserId, toUserId, orderId, fileUrl, read, createdAt |
| Tip.java | 9 | id, fromUserId, toUserId, orderId, amount, message, status, createdAt |
| Payment.java | 9 | id, orderId, orderNo, payNo, amount, method, status, paidAt |
| Refund.java | 8 | id, orderId, orderNo, amount, reason, status, createdAt |
| Notice.java | 10 | id, title, content, type, imageUrl, linkUrl, sortOrder, status, createdAt, updatedAt |

**Must keep** — frontend `MerchantOrders.vue` uses camelCase keys (`orderNo`, `status`, `amount`, `createTime`). `auth.js` store has fallback `data.username || data.user_id`.

---

## Category 4: @JsonAlias (Backward Compatibility)

131 @JsonAlias entries across 10 entities providing backward compat deserialization.

**Status:** Safe to keep. Provides tolerance for mixed-format frontend requests. Candidate for removal after full frontend migration is verified, but zero risk to keep.

---

## Category 5: @JsonIgnoreProperties

| File | Annotation | Purpose |
|---|---|---|
| `ChromaService.java` (inner class) | `@JsonIgnoreProperties(ignoreUnknown = true)` | ChromaDB response may contain extra fields |

**Must keep** — functional requirement for external API deserialization.
