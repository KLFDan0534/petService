# Jackson Annotation Inventory

## Summary Statistics

| Annotation | Count | Purpose |
|---|---|---|
| @JsonProperty | 343 | Serialize/deserialize property name |
| @JsonAlias | 131 | Alternative deserialization names |
| @JsonGetter | 102 | Additional getter for camelCase alias |
| @JsonIgnore | 32 | Exclude field from serialization |
| @JsonIgnoreProperties | 1 | Ignore unknown properties (ChromaService) |
| **Total** | **609** | |

## Distribution by Module

### pet-system (8 entities/DTOs/VOs)

| File | @JsonProperty | @JsonAlias | @JsonGetter | @JsonIgnore |
|---|---|---|---|---|
| User.java | 15 | 0 | 0 | 2 |
| UserRole.java | 4 | 0 | 0 | 1 |
| Role.java | 5 | 0 | 0 | 1 |
| UserVO.java | 16 | 0 | 0 | 0 |
| LoginRequestDTO.java | 2 | 0 | 0 | 0 |
| RegisterRequestDTO.java | 5 | 0 | 0 | 0 |
| LoginResponse.java | 6 | 0 | 0 | 0 |
| ForgotPasswordRequestDTO.java | 1 | 0 | 0 | 0 |
| RefreshTokenRequestDTO.java | 1 | 0 | 0 | 0 |
| UserUpdateProfileRequestDTO.java | 4 | 0 | 0 | 0 |
| UserUpdatePhoneRequestDTO.java | 1 | 0 | 0 | 0 |
| UserUpdateEmailRequestDTO.java | 1 | 0 | 0 | 0 |
| UserUpdateStatusRequestDTO.java | 1 | 0 | 0 | 0 |
| UserUpdateRealNameRequestDTO.java | 2 | 0 | 0 | 0 |
| RoleCreateRequestDTO.java | 3 | 0 | 0 | 0 |
| RoleUpdateRequestDTO.java | 2 | 0 | 0 | 0 |
| RoleSetUserRolesRequestDTO.java | 1 | 0 | 0 | 0 |
| RoleListResponseDTO.java | 6 | 0 | 0 | 0 |

### pet-business (27 entities + 35 DTOs)

| Entity | @JsonProperty | @JsonAlias | @JsonGetter | @JsonIgnore |
|---|---|---|---|---|
| Pet.java | 18 | 14 | 14 | 1 |
| PetOrder.java | 69 | 0 | 20 | 1 |
| CareRecord.java | 10 | 8 | 9 | 1 |
| Keeper.java | 12 | 10 | 12 | 1 |
| Merchant.java | 8 | 10 | 10 | 1 |
| Rating.java | 19 | 14 | 14 | 1 |
| ChatMessage.java | 10 | 6 | 7 | 1 |
| Tip.java | 12 | 9 | 9 | 1 |
| Payment.java | 11 | 0 | 9 | 1 |
| Refund.java | 10 | 0 | 8 | 1 |
| Complaint.java | 12 | 0 | 0 | 1 |
| Ticket.java | 10 | 0 | 0 | 1 |
| TicketMessage.java | 5 | 0 | 0 | 1 |
| Wallet.java | 8 | 0 | 0 | 1 |
| Transaction.java | 9 | 0 | 0 | 1 |
| Withdrawal.java | 11 | 0 | 0 | 1 |
| Notice.java | 16 | 8 | 10 | 1 |
| AdoptionApplication.java | 17 | 0 | 0 | 1 |
| AdoptionPet.java | 0 | 0 | 0 | 1 |
| FileRecord.java | 7 | 0 | 1 | 0 |
| Category.java | 6 | 0 | 0 | 1 |
| Address.java | 7 | 0 | 0 | 0 |
| BusinessHours.java | 6 | 0 | 0 | 0 |
| ServiceItem.java | 7 | 0 | 0 | 0 |
| ContentReview.java | 8 | 0 | 0 | 1 |
| Favorite.java | 4 | 0 | 0 | 1 |
| OperationLog.java | 10 | 0 | 0 | 0 |
| NoticeRead.java | 4 | 0 | 0 | 0 |
| Notification.java | 6 | 0 | 0 | 1 |

### pet-ai (5 entities/DTOs)

| File | @JsonProperty | @JsonAlias | @JsonGetter | @JsonIgnore |
|---|---|---|---|---|
| AiReport.java | 6 | 0 | 0 | 1 |
| KnowledgeDocument.java | 9 | 0 | 0 | 1 |
| RagAskRequestDTO.java | 2 | 0 | 0 | 0 |
| AskResult.java | 1 | 0 | 0 | 0 |
| RagDocumentCreateRequestDTO.java | 4 | 0 | 0 | 0 |
| AiReportGenerateRequestDTO.java | 3 | 0 | 0 | 0 |
| AiReportCreateRequestDTO.java | 5 | 0 | 0 | 0 |
| AgentExecuteRequestDTO.java | 3 | 0 | 0 | 0 |
| AgentChatRequestDTO.java | 4 | 0 | 0 | 0 |
| ChromaService.java | 0 | 0 | 0 | 0 (1 @JsonIgnoreProperties) |

## Annotation Patterns

### Pattern A: `_wsh` field + `@JsonGetter` (dual output)
Entity serializes as BOTH `xxx_wsh` AND `camelCase`. Frontend reads `xxx_wsh`, legacy code reads camelCase.

Files: Pet, PetOrder, CareRecord, Keeper, Merchant, Rating, ChatMessage, Tip, Payment, Refund, Notice

### Pattern B: `_wsh` field only (single output)
Entity serializes as `xxx_wsh` only. No legacy camelCase output.

Files: User, UserRole, Role, Complaint, Ticket, TicketMessage, Wallet, Transaction, Withdrawal, AdoptionApplication, FileRecord, Category, Address, BusinessHours, ServiceItem, ContentReview, Favorite, OperationLog, NoticeRead, Notification, AiReport, KnowledgeDocument

### Pattern C: camelCase field + `@JsonProperty("_wsh")` (bridging DTO)
DTO uses clean Java names, `@JsonProperty` bridges to `_wsh` JSON.

Files: LoginResponse, OrderCancelRequestDTO, UserUpdateProfileRequestDTO, ForgotPasswordRequestDTO, RefreshTokenRequestDTO, etc.

### Pattern D: `_wsh` field + no `@JsonProperty` (clean)
Entity uses no Jackson annotations at all. Only `AdoptionPet.java`.

### Pattern E: `_wsh` field + `@JsonAlias` only (no `@JsonProperty`)
DTO uses `_wsh` field names with `@JsonAlias` for backward compat, no `@JsonProperty`.

Files: SendOrderMessageRequest, CreateCareRecordRequest
