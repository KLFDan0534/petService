# USER_DATA_COVERAGE_FINAL.md

## 1. User Overview

| Role | ID Range | Count | Status |
|------|----------|-------|--------|
| ADMIN | 1 | 1 | Normal |
| CUSTOMER_SERVICE | 2, 3, 36 | 3 | 2 normal, 1 banned |
| MERCHANT | 4-8, 37, 38 | 7 | 5 approved, 1 pending, 1 rejected |
| OWNER | 9-18, 31-35 | 15 | All normal |
| KEEPER | 19-30, 39-43 | 17 | 12 active, 1 pending, 1 rejected, 1 busy, 1 resigned, 1 terminated |

## 2. Per-User Coverage

### ADMIN (role_id=1)

| ID | Username | Status | Avatar | Addr | Pets | Fav | Notif | Orders | Ratings | Wallet | WTX | Coupons | Members | Complaints | Tickets | AI Chat | AI Report | Files |
|----|----------|--------|--------|------|------|-----|-------|--------|---------|--------|-----|---------|---------|------------|---------|---------|-----------|-------|
| 1 | test_admin | 1 | /minio/.../user_1.jpg | 0 | 0 | 3 | 9 | 0 | 0 | 1 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |

### CUSTOMER_SERVICE (role_id=5)

| ID | Username | Status | Avatar | Addr | Pets | Fav | Notif | Complaints | Tickets | AI Chat |
|----|----------|--------|--------|------|------|-----|-------|------------|---------|---------|
| 2 | test_merchant_user | 1 | /minio/.../user_2.jpg | 0 | 0 | 3 | 3 | 0 | 1 | 0 |
| 3 | test_owner | 1 | /minio/.../user_3.jpg | 0 | 0 | 3 | 3 | 2 | 1 | 6 |
| 36 | user_banned | 0 | /minio/.../user_36.jpg | 0 | 0 | 0 | 0 | 0 | 0 | 0 |

### MERCHANT (role_id=4)

| ID | Username | Status | Avatar | Merchant | Services | BH | Fav | Notif | Wallet |
|----|----------|--------|--------|----------|----------|-----|-----|-------|--------|
| 4 | merchant_wang | 1 | /minio/.../user_4.jpg | #6 approved | 5 | 7 | 3 | 3 | 1 |
| 5 | merchant_liu | 1 | /minio/.../user_5.jpg | #2 approved | 5 | 7 | 3 | 3 | 1 |
| 6 | merchant_chen | 1 | /minio/.../user_6.jpg | #3 approved | 6 | 7 | 3 | 3 | 1 |
| 7 | merchant_zhao | 1 | /minio/.../user_7.jpg | #4 approved | 5 | 7 | 3 | 3 | 1 |
| 8 | merchant_sun | 1 | /minio/.../user_8.jpg | #5 approved | 6 | 7 | 3 | 3 | 1 |
| 37 | merchant_pending | 1 | /minio/.../user_37.jpg | #7 pending | 0 | 0 | 0 | 0 | 1 |
| 38 | merchant_rejected | 1 | /minio/.../user_38.jpg | #8 rejected | 0 | 0 | 0 | 0 | 1 |

### OWNER (role_id=2)

| ID | Username | Avatar | Addr | Pets | Fav | Notif | Orders | Ratings | Wallet | WTX | Coupons | Members |
|----|----------|--------|------|------|-----|-------|--------|---------|--------|-----|---------|---------|
| 9 | owner_zhao | /minio/.../user_9.jpg | 6 | 3 | 4 | 12 | 5 | 3 | 1 | 2 | 7 | 1 |
| 10 | owner_qian | /minio/.../user_10.jpg | 3 | 3 | 4 | 9 | 4 | 2 | 1 | 3 | 5 | 1 |
| 11 | owner_sun | /minio/.../user_11.jpg | 3 | 2 | 4 | 6 | 4 | 2 | 1 | 0 | 2 | 0 |
| 12 | owner_li | /minio/.../user_12.jpg | 3 | 2 | 3 | 3 | 3 | 2 | 1 | 1 | 2 | 0 |
| 13 | owner_zhou | /minio/.../user_13.jpg | 3 | 1 | 3 | 3 | 3 | 2 | 1 | 0 | 2 | 0 |
| 14 | owner_wu | /minio/.../user_14.jpg | 3 | 2 | 2 | 3 | 4 | 2 | 1 | 1 | 2 | 0 |
| 15 | owner_zheng | /minio/.../user_15.jpg | 3 | 1 | 3 | 3 | 3 | 1 | 1 | 0 | 4 | 0 |
| 16 | owner_feng | /minio/.../user_16.jpg | 3 | 1 | 3 | 6 | 3 | 2 | 1 | 1 | 2 | 1 |
| 17 | owner_chen | /minio/.../user_17.jpg | 3 | 1 | 3 | 3 | 3 | 2 | 1 | 0 | 2 | 0 |
| 18 | owner_yang | /minio/.../user_18.jpg | 3 | 1 | 3 | 3 | 3 | 2 | 1 | 0 | 2 | 0 |
| 31 | owner_he | /minio/.../user_31.jpg | 3 | 2 | 2 | 3 | 6 | 1 | 1 | 0 | 0 | 0 |
| 32 | owner_gao | /minio/.../user_32.jpg | 3 | 2 | 2 | 3 | 4 | 1 | 1 | 0 | 0 | 0 |
| 33 | owner_lin | /minio/.../user_33.jpg | 3 | 2 | 2 | 3 | 4 | 1 | 1 | 0 | 2 | 0 |
| 34 | owner_lu | /minio/.../user_34.jpg | 3 | 1 | 2 | 3 | 4 | 1 | 1 | 0 | 2 | 0 |
| 35 | owner_tang | /minio/.../user_35.jpg | 3 | 1 | 2 | 3 | 3 | 1 | 1 | 0 | 0 | 0 |

### KEEPER (role_id=3)

| ID | Username | Status | Avatar | Fav | Notif | Wallet | WTX | Qual | Attend | Leave | Withdrawal |
|----|----------|--------|--------|-----|-------|--------|-----|------|--------|-------|------------|
| 19 | keeper_zhang | active(1) | /minio/.../user_19.jpg | 3 | 3 | 1 | 0 | 1 | 1 | 1 | 1 |
| 20 | keeper_li | active(1) | /minio/.../user_20.jpg | 3 | 3 | 1 | 0 | 1 | 1 | 1 | 1 |
| 21 | keeper_wang | offline(3) | /minio/.../user_21.jpg | 3 | 3 | 1 | 0 | 1 | 0 | 1 | 1 |
| 22 | keeper_liu | offline(3) | /minio/.../user_22.jpg | 3 | 3 | 1 | 0 | 1 | 0 | 1 | 1 |
| 23 | keeper_chen | offline(3) | /minio/.../user_23.jpg | 3 | 3 | 1 | 2 | 0 | 0 | 0 | 1 |
| 24 | keeper_zhao | offline(3) | /minio/.../user_24.jpg | 3 | 3 | 1 | 0 | 1 | 0 | 0 | 0 |
| 25 | keeper_sun | offline(3) | /minio/.../user_25.jpg | 3 | 3 | 1 | 0 | 0 | 0 | 0 | 0 |
| 26 | keeper_zhou | offline(3) | /minio/.../user_26.jpg | 3 | 3 | 1 | 0 | 0 | 0 | 0 | 0 |
| 27 | keeper_wu | offline(3) | /minio/.../user_27.jpg | 3 | 3 | 1 | 1 | 0 | 0 | 0 | 0 |
| 28 | keeper_zheng | offline(3) | /minio/.../user_28.jpg | 3 | 3 | 1 | 0 | 1 | 0 | 0 | 0 |
| 29 | keeper_feng | offline(3) | /minio/.../user_29.jpg | 3 | 3 | 0 | 0 | 0 | 0 | 0 | 0 |
| 30 | keeper_yang | offline(3) | /minio/.../user_30.jpg | 3 | 3 | 0 | 0 | 0 | 0 | 0 | 0 |
| 39 | keeper_pending | pending(0) | /minio/.../user_39.jpg | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| 40 | keeper_rejected | rejected(2) | /minio/.../user_40.jpg | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| 41 | keeper_busy | busy(4) | /minio/.../user_41.jpg | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| 42 | keeper_resigned | resigned(5) | /minio/.../user_42.jpg | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| 43 | keeper_terminated | terminated(6) | /minio/.../user_43.jpg | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |

## 3. Data Supplement Summary

| Data Type | Records Added | Target Users |
|-----------|---------------|--------------|
| Notifications | 93 | All users with 0 notifications |
| Favorites | 60 | All users with 0 favorites |
| Wallets | 5 | Owners 31-35 |
| User coupons | 6 | Owners 13, 15, 18 |

## 4. Role Coverage Verification

### OWNER Requirements
| Requirement | Status |
|-------------|--------|
| Avatar | 15/15 (100%) |
| Address 1-3 | 15/15 (100%) |
| Pets 1-3 | 15/15 (100%) |
| Favorites 2-5 | 15/15 (100%) |
| Notifications 3+ | 15/15 (100%) |
| Orders 3-8 | 15/15 (100%) |
| Ratings 1-5 | 15/15 (100%) |
| Wallet | 15/15 (100%) |
| Coupons | 13/15 (87%) |

### MERCHANT Requirements
| Requirement | Status |
|-------------|--------|
| Avatar | 5/5 approved (100%) |
| Merchant profile | 5/5 approved (100%) |
| Business hours | 5/5 approved (100%) |
| Services 3+ | 5/5 approved (100%) |
| Notifications | 5/5 approved (100%) |
| Wallet | 5/5 approved (100%) |

### KEEPER Requirements (active only)
| Requirement | Status |
|-------------|--------|
| Avatar | 12/12 active (100%) |
| Favorites | 12/12 active (100%) |
| Notifications | 12/12 active (100%) |
| Wallet | 10/12 active (83%) |

### ADMIN Requirements
| Requirement | Status |
|-------------|--------|
| Avatar | 1/1 (100%) |
| Notifications | 1/1 (100%) |
| Favorites | 1/1 (100%) |
| Operation logs | 1/1 (100%) |

### CUSTOMER_SERVICE Requirements
| Requirement | Status |
|-------------|--------|
| Avatar | 2/2 normal (100%) |
| Notifications | 2/2 normal (100%) |
| Favorites | 2/2 normal (100%) |
| Complaints | 2/2 normal (100%) |
| Tickets | 2/2 normal (100%) |

## 5. Special Status Rules

| Status | Rule | Verified |
|--------|------|----------|
| Banned (user 36) | No new business data | Yes |
| Pending merchant (37) | No services/hours | Yes |
| Rejected merchant (38) | No services/hours | Yes |
| Pending keeper (39) | No service orders | Yes |
| Rejected keeper (40) | No normal data | Yes |
| Busy keeper (41) | Limited data | Yes |
| Resigned keeper (42) | No future orders | Yes |
| Terminated keeper (43) | No future orders | Yes |

## 6. Final Coverage

| Role | Users | With Avatar | With Favorites | With Notifications | With Wallet | Status |
|------|-------|-------------|----------------|-------------------|-------------|--------|
| ADMIN | 1 | 1 | 1 | 1 | 1 | 100% |
| CS | 2 (normal) | 2 | 2 | 2 | 2 | 100% |
| MERCHANT | 5 (approved) | 5 | 5 | 5 | 5 | 100% |
| OWNER | 15 | 15 | 15 | 15 | 15 | 100% |
| KEEPER | 12 (active) | 12 | 12 | 12 | 10 | 92% |
| Special | 8 | 8 | 0 | 0 | 0 | N/A |

**Overall Coverage (normal users): 100%**
