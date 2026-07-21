# Refactoring Plan

## Goal

Remove all redundant `@JsonProperty("xxx_wsh")` annotations where Java field name already matches `xxx_wsh`. No behavioral change. No frontend impact. No Swagger impact.

## Scope

- **Remove:** ~230 `@JsonProperty("xxx_wsh")` annotations where field name = annotation value
- **Keep:** ~70 `@JsonProperty("xxx_wsh")` on camelCase fields (bridging purpose)
- **Keep:** All 32 `@JsonIgnore` (security)
- **Keep:** All 102 `@JsonGetter` (frontend compatibility)
- **Keep:** All 131 `@JsonAlias` (backward compatibility)
- **Keep:** @JsonIgnoreProperties in ChromaService

## File-by-File Edit Plan

### pet-system (12 files, ~77 edits)

| File | Edits | Strategy |
|---|---|---|
| `User.java` | 16 | Remove all @JsonProperty lines on `_wsh` fields |
| `UserRole.java` | 4 | Same |
| `Role.java` | 5 | Same |
| `UserVO.java` | 16 | Same |
| `LoginRequestDTO.java` | 2 | Same |
| `RegisterRequestDTO.java` | 5 | Same |
| `RoleCreateRequestDTO.java` | 3 | Same |
| `RoleUpdateRequestDTO.java` | 2 | Same |
| `RoleSetUserRolesRequestDTO.java` | 1 | Same |
| `RoleListResponseDTO.java` | 6 | Same |

### pet-business (26+ entity/DTO files, ~150 edits)

| File | Edits | Notes |
|---|---|---|
| `Pet.java` | 17 | Keep @JsonAlias, @JsonIgnore |
| `PetOrder.java` | 69 | Keep @JsonGetter, @JsonIgnore |
| `CareRecord.java` | 10 | Keep @JsonAlias, @JsonIgnore |
| `Keeper.java` | 12 | Keep @JsonAlias, @JsonIgnore |
| `Merchant.java` | 8 | Keep @JsonAlias, @JsonIgnore |
| `Rating.java` | 19 | Keep @JsonAlias, @JsonIgnore |
| `ChatMessage.java` | 10 | Keep @JsonAlias, @JsonIgnore |
| `Tip.java` | 12 | Keep @JsonAlias, @JsonIgnore |
| `Payment.java` | 11 | Keep @JsonAlias, @JsonIgnore |
| `Refund.java` | 10 | Keep @JsonAlias, @JsonIgnore |
| `Complaint.java` | 12 | Keep @JsonIgnore |
| `Ticket.java` | 10 | Keep @JsonIgnore |
| `TicketMessage.java` | 5 | Keep @JsonIgnore |
| `Wallet.java` | 8 | Keep @JsonIgnore |
| `Transaction.java` | 9 | Keep @JsonIgnore |
| `Withdrawal.java` | 11 | Keep @JsonIgnore |
| `Notice.java` | 16 | Keep @JsonProperty on getters, @JsonIgnore |
| `AdoptionApplication.java` | 17 | Keep @JsonIgnore |
| `FileRecord.java` | 7 | Keep @JsonGetter |
| `Category.java` | 6 | Keep @JsonIgnore |
| `Address.java` | 7 | No @JsonIgnore |
| `BusinessHours.java` | 6 | No @JsonIgnore |
| `ServiceItem.java` | 7 | No @JsonIgnore |
| `ContentReview.java` | 8 | Keep @JsonIgnore |
| `Favorite.java` | 4 | Keep @JsonIgnore |
| `OperationLog.java` | 10 | No @JsonIgnore |
| `NoticeRead.java` | 4 | No @JsonIgnore |
| `Notification.java` | 6 | Keep @JsonIgnore |
| Various Request DTOs | ~50 | Remove only where field name = `_wsh` |

### pet-ai (7 files, ~32 edits)

| File | Edits | Notes |
|---|---|---|
| `AiReport.java` | 6 | Keep @JsonIgnore |
| `KnowledgeDocument.java` | 9 | Keep @JsonIgnore |
| `RagAskRequestDTO.java` | 2 | No others |
| `AskResult.java` | 1 | No others |
| `RagDocumentCreateRequestDTO.java` | 4 | No others |
| `AgentExecuteRequestDTO.java` | 3 | No others |
| `AgentChatRequestDTO.java` | 4 | No others |

## Execution Order

```
Phase 1: pet-system (7 files, safest — no @JsonGetter entanglement)
Phase 2: pet-ai (7 files, simple entities)
Phase 3: pet-business entities without @JsonGetter (15+ files)
Phase 4: pet-business entities with @JsonGetter (11 files — verify @JsonProperty removed but NOT @JsonGetter)
Phase 5: pet-business DTOs with _wsh field names (15+ files)
```

## Validation Steps

After each phase:
1. **Compile**: `mvn compile` — must pass
2. **JSON compare**: Verify serialization output unchanged
3. **No frontend changes needed**: Verify by checking API responses

## Rollback Plan

If any issue found:
- Revert specific file changes via `git checkout -- <file>`
- All changes are per-file, easy to roll back individually
