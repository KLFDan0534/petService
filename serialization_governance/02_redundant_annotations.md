# Redundant Annotations — Confirmed for Removal

## Principle

`@JsonProperty("xxx_wsh")` is **redundant** when:
1. Java field name is also `xxx_wsh`
2. Lombok `@Getter` generates `getXxx_wsh()` → Jackson auto-derives property name `xxx_wsh`
3. OR there's an explicit `getXxx_wsh()` method

Jackson discovers the property from the getter/setter regardless of `@JsonProperty` on the field. The `@JsonProperty` changes nothing.

## Total: ~230 redundant `@JsonProperty` annotations across 50+ files

---

## pet-system

### User.java (15 redundant)
```java
@JsonProperty("id_wsh")        // field: id_wsh         → redundant
@JsonProperty("username_wsh")  // field: username_wsh   → redundant
@JsonProperty("nickname_wsh")  // field: nickname_wsh   → redundant
@JsonProperty("phone_wsh")     // field: phone_wsh      → redundant
@JsonProperty("avatar_wsh")    // field: avatar_wsh     → redundant
@JsonProperty("gender_wsh")    // field: gender_wsh     → redundant
@JsonProperty("email_wsh")     // field: email_wsh      → redundant
@JsonProperty("real_name_wsh") // field: real_name_wsh  → redundant
@JsonProperty("id_card_no_wsh")// field: id_card_no_wsh → redundant
@JsonProperty("real_name_status_wsh") // field: real_name_status_wsh → redundant
@JsonProperty("address_wsh")   // field: address_wsh    → redundant
@JsonProperty("latitude_wsh")  // field: latitude_wsh   → redundant
@JsonProperty("longitude_wsh") // field: longitude_wsh  → redundant
@JsonProperty("status_wsh")    // field: status_wsh     → redundant
@JsonProperty("created_at_wsh")// field: created_at_wsh → redundant
@JsonProperty("updated_at_wsh")// field: updated_at_wsh → redundant
```

### UserRole.java (4 redundant)
id_wsh, user_id_wsh, role_id_wsh, created_at_wsh

### Role.java (5 redundant)
id_wsh, name_wsh, code_wsh, description_wsh, created_at_wsh

### UserVO.java (16 redundant)
All fields: id_wsh, username_wsh, nickname_wsh, phone_wsh, avatar_wsh, gender_wsh, email_wsh, real_name_wsh, id_card_no_wsh, real_name_status_wsh, address_wsh, latitude_wsh, longitude_wsh, status_wsh, roles_wsh, created_at_wsh

### LoginRequestDTO.java (2 redundant)
username_wsh, password_wsh

### RegisterRequestDTO.java (5 redundant)
username_wsh, password_wsh, nickname_wsh, phone_wsh, email_wsh

### RoleCreateRequestDTO.java (3 redundant)
name_wsh, code_wsh, description_wsh

### RoleUpdateRequestDTO.java (2 redundant)
name_wsh, description_wsh

### RoleSetUserRolesRequestDTO.java (1 redundant)
role_ids_wsh

### RoleListResponseDTO.java (6 redundant)
id_wsh, name_wsh, code_wsh, description_wsh, created_at_wsh, user_count_wsh

---

## pet-business

### Entity: Pet.java (18 redundant)
All @JsonProperty: id_wsh, owner_id_wsh, name_wsh, type_wsh, breed_wsh, age_wsh, weight_wsh, gender_wsh, sterilized_wsh, vaccinated_wsh, avatar_wsh, description_wsh, allergies_wsh, habits_wsh, owner_name_wsh, created_at_wsh, updated_at_wsh

### Entity: PetOrder.java (69 redundant)
All @JsonProperty on `_wsh` named fields.

### Entity: CareRecord.java (10 redundant)
All @JsonProperty on `_wsh` named fields.

### Entity: Keeper.java (12 redundant)
All @JsonProperty on `_wsh` named fields.

### Entity: Merchant.java (8 redundant)
All @JsonProperty on `_wsh` named fields.

### Entity: Rating.java (19 redundant)
All @JsonProperty on `_wsh` named fields.

### Entity: ChatMessage.java (10 redundant)
All @JsonProperty on `_wsh` named fields.

### Entity: Tip.java (12 redundant)
All @JsonProperty on `_wsh` named fields.

### Entity: Payment.java (11 redundant)
All @JsonProperty on `_wsh` named fields.

### Entity: Refund.java (10 redundant)
All @JsonProperty on `_wsh` named fields.

### Entity: Complaint.java (12 redundant)
All @JsonProperty on `_wsh` named fields.

### Entity: Ticket.java (10 redundant)
All @JsonProperty on `_wsh` named fields.

### Entity: TicketMessage.java (5 redundant)
All @JsonProperty on `_wsh` named fields.

### Entity: Wallet.java (8 redundant)
All @JsonProperty on `_wsh` named fields (idWsh→id_wsh, etc.).

### Entity: Transaction.java (9 redundant)
All @JsonProperty on `_wsh` named fields.

### Entity: Withdrawal.java (11 redundant)
All @JsonProperty on `_wsh` named fields.

### Entity: Notice.java (16 @JsonProperty on fields — redundant)
Plus 10 @JsonProperty on @JsonGetter methods (different annotation).

### Entity: AdoptionApplication.java (17 redundant)
All @JsonProperty on `_wsh` named fields.

### Entity: FileRecord.java (7 redundant)
All @JsonProperty on `_wsh` named fields.

### Entity: Category.java (6 redundant)
All @JsonProperty on `_wsh` named fields.

### Entity: Address.java (7 redundant)
All @JsonProperty on `_wsh` named fields.

### Entity: BusinessHours.java (6 redundant)
All @JsonProperty on `_wsh` named fields.

### Entity: ServiceItem.java (7 redundant)
All @JsonProperty on `_wsh` named fields.

### Entity: ContentReview.java (8 redundant)
All @JsonProperty on `_wsh` named fields.

### Entity: Favorite.java (4 redundant)
All @JsonProperty on `_wsh` named fields.

### Entity: OperationLog.java (10 redundant)
All @JsonProperty on `_wsh` named fields.

### Entity: NoticeRead.java (4 redundant)
All @JsonProperty on `_wsh` named fields.

### Entity: Notification.java (6 redundant)
All @JsonProperty on `_wsh` named fields.

### DTO: TicketCreateRequestDTO (4 redundant)
All @JsonProperty on `_wsh` named fields.

### DTO: TicketAssignRequestDTO (1 redundant)

### DTO: TicketResolveRequestDTO (1 redundant)

### DTO: TicketAddMessageRequestDTO (2 redundant)

### DTO: ComplaintCreateRequestDTO (6 redundant)

### DTO: ComplaintResolveRequestDTO (1 redundant)

### DTO: ComplaintRejectRequestDTO (1 redundant)

### DTO: KeeperCreateRequestDTO (7 redundant)

### DTO: KeeperUpdateRequestDTO (6 redundant)

### DTO: KeeperOnlineStatusRequestDTO (1 redundant)

### DTO: ServiceItemCreateRequestDTO (6 redundant)

### DTO: ServiceItemUpdateRequestDTO (7 redundant)

### DTO: ServiceItemUpdateImagesRequestDTO (1 redundant)

### DTO: AdoptionPetCreateRequestDTO (15 redundant)

### DTO: AdoptionPetUpdateRequestDTO (15 redundant)

### DTO: AdoptionApplicationCreateRequestDTO (11 redundant)

---

## pet-ai

### AiReport.java (6 redundant)

### KnowledgeDocument.java (9 redundant)

### RagAskRequestDTO.java (2 redundant)

### AskResult.java (1 redundant)

### RagDocumentCreateRequestDTO.java (4 redundant)

### AgentExecuteRequestDTO.java (3 redundant)

### AgentChatRequestDTO.java (4 redundant)

---

## Call Chain Verification

For each redundant `@JsonProperty`, the following confirms identical behavior:

**Serialization:**
- Before: Jackson discovers property via 2 paths — `@JsonProperty("xxx_wsh")` on field AND Lombok getter `getXxx_wsh()` → produces key `xxx_wsh`
- After: Jackson discovers property via 1 path — Lombok getter `getXxx_wsh()` → produces key `xxx_wsh`
- Result: **Identical JSON output** (`"xxx_wsh": value`)

**Deserialization:**
- Before: Jackson maps JSON key `xxx_wsh` to field via field annotation AND setter `setXxx_wsh()`
- After: Jackson maps JSON key `xxx_wsh` to field via setter `setXxx_wsh()` only
- Result: **Identical deserialization** (JSON `xxx_wsh` → field `xxx_wsh`)

**No impact on:** @JsonGetter (camelCase aliases), @JsonAlias (backward compat), @JsonIgnore (security)
