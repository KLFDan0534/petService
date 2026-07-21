# Serialization Comparison: Before vs After

## Test Case: User.java (Pattern B — no @JsonGetter)

### Before (with redundant @JsonProperty)

```java
@JsonProperty("id_wsh")
private Long id_wsh;
// → JSON: "id_wsh": 1

@JsonProperty("username_wsh")
private String username_wsh;
// → JSON: "username_wsh": "john"
```

### After (without redundant @JsonProperty)

```java
private Long id_wsh;
// → JSON: "id_wsh": 1  (auto-discovered from getter getId_wsh())

private String username_wsh;
// → JSON: "username_wsh": "john"  (auto-discovered from getter getUsername_wsh())
```

### JSON Output Comparison
```json
// BEFORE & AFTER — IDENTICAL
{
  "id_wsh": 1,
  "username_wsh": "john",
  "nickname_wsh": "Johnny",
  "phone_wsh": "13800138000",
  "created_at_wsh": "2026-06-28 12:00:00"
}
```

**Result: 100% identical** ✓

---

## Test Case: Pet.java (Pattern A — @JsonProperty + @JsonGetter)

### Before (with redundant @JsonProperty)

```java
@JsonProperty("id_wsh")
private Long id_wsh;
// → produces "id_wsh": 1 (from @JsonProperty on field)
// Also: @JsonGetter("id") → produces "id": 1

@JsonProperty("name_wsh")
private String name_wsh;
// → produces "name_wsh": "Fluffy"
// Also: @JsonGetter("name") → produces "name": "Fluffy"
```

### After (without redundant @JsonProperty)

```java
private Long id_wsh;
// → produces "id_wsh": 1 (auto-discovered from getter getId_wsh())
// Also: @JsonGetter("id") → produces "id": 1

private String name_wsh;
// → produces "name_wsh": "Fluffy" (auto-discovered from getter getName_wsh())
// Also: @JsonGetter("name") → produces "name": "Fluffy"
```

### JSON Output Comparison
```json
// BEFORE & AFTER — IDENTICAL
{
  "id_wsh": 1,
  "id": 1,
  "name_wsh": "Fluffy",
  "name": "Fluffy",
  "type_wsh": "dog",
  "type": "dog"
}
```

**Result: 100% identical** ✓ (both `_wsh` and camelCase names preserved)

---

## Test Case: OrderCancelRequestDTO (Pattern C — camelCase field)

### Before

```java
@JsonProperty("order_id_wsh")
private Long orderId;
// → JSON: "order_id_wsh": 123

@JsonProperty("order_no_wsh")
private String orderNo;
// → JSON: "order_no_wsh": "ORD001"
```

### After (NOT modified — required annotation)

```java
@JsonProperty("order_id_wsh")
private Long orderId;

@JsonProperty("order_no_wsh")
private String orderNo;
```

**Result: No change** ✓ (this @JsonProperty is required)

---

## Test Case: AdoptionPet.java (Pattern D — no annotations)

### Before & After

```java
// No @JsonProperty — already clean
private Long id_wsh;
// → JSON: "id_wsh": 1 (auto-discovered)
```

**Result: No change** ✓

---

## Overall Comparison Summary

| Entity/DTO Type | Before JSON | After JSON | Match? |
|---|---|---|---|
| Pattern B (no @JsonGetter) | `"xxx_wsh"` | `"xxx_wsh"` | ✅ Identical |
| Pattern A (with @JsonGetter) | `"xxx_wsh"` + `"xxx"` | `"xxx_wsh"` + `"xxx"` | ✅ Identical |
| Pattern C (camelCase DTO) | `"xxx_wsh"` | `"xxx_wsh"` | ✅ No change |
| Pattern D (no annotations) | `"xxx_wsh"` | `"xxx_wsh"` | ✅ No change |

**All patterns: Before = After. Zero behavioral change.**
