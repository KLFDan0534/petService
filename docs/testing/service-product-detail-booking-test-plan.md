# Service Product Detail and In-Page Booking Test Plan

Date: 2026-08-12
Scope: `docs/plans/2026-08-12-001-feat-service-product-detail-booking-plan.md`

## 1. Test Principles

1. Each implementation unit runs its focused tests before the next unit begins.
2. Backend authority is tested with tampered merchant, price, category, service, keeper, file, and version values.
3. Browser tests create run-owned data and delete only that data. Fixed pet/order IDs and shared dates are forbidden.
4. A failed check is fixed, then both the focused check and every affected broader gate are rerun.
5. A check not executed is reported as unverified with the exact blocker; it is never written as passed.
6. Product detail and management are tested at desktop 1280x720 and mobile 390x844.

## 2. Test Data Matrix

Create unique names with a run ID for every E2E execution.

| Fixture | Required state | Purpose |
|---|---|---|
| M1/S1 | Approved merchant, enabled category/product, 3 images, future booking enabled | Main happy path |
| M2/S2 | Approved merchant, same product name/category as S1, different price/images | Cross-product isolation |
| M3/S3 | Unapproved merchant with enabled product | Public visibility negative |
| S4 | Disabled product | Detail/list unavailable state |
| S5 | Product in disabled category | Category visibility negative |
| S6 | Enabled product with zero images | Empty gallery |
| S7 | Enabled product with 10 images | Media limit/order/cover |
| F1 | Image file owned by M1 user | Valid product media |
| F2 | Non-image file owned by M1 user | MIME rejection |
| F3 | Image file owned by M2 user | Cross-owner media rejection |
| P1/K1 | Owner pet and qualified keeper under M1 | Booking happy path |
| O1 | Completed order for S1 with snapshot | Snapshot stability |

## 3. Backend Test Catalog

### 3.1 Media schema and service

- B-MED-001: Save zero media rows; response gallery is empty and has no cover.
- B-MED-002: Save one owned image; order is 0 and it is the cover.
- B-MED-003: Save ten images in shuffled request order; response is contiguous 0-9 in requested order.
- B-MED-004: Duplicate file IDs reject with a stable 400 code and leave the old set unchanged.
- B-MED-005: Duplicate sort orders or gaps reject and leave the old set unchanged.
- B-MED-006: Zero covers or two covers for a non-empty set reject transactionally.
- B-MED-007: Non-image `file_record_wsh` rejects.
- B-MED-008: File owned by another merchant user rejects 403.
- B-MED-009: Eleven images reject without partial rows.
- B-MED-010: Concurrent media replacements finish with one complete valid set, never an interleaved set.
- B-MED-011: Parent aggregate lock/version serializes concurrent replacement and preserves exactly one cover.
- B-UPL-001: Product-media upload rejects spoofed MIME, SVG/HTML/polyglot, truncated, oversized-byte, and oversized-dimension content, ignores caller directory, and derives type from decoded content.
- B-UPL-002: Product-media upload removes only its own MinIO object when file-record creation fails; no client can invoke general object cleanup.
- B-UPL-003: Merchant upload is server-scoped to its merchant; admin must name one target merchant and cannot attach unrelated private file records.

### 3.2 Migration

- B-MIG-001: `a,b,c` resolves to three rows in the same order with `a` as cover.
- B-MIG-002: Blanks and repeated URLs do not create blank/duplicate rows.
- B-MIG-003: Unresolved/external, protocol-relative, data, and malformed URLs are counted in audit/remediation output and never appear in public media responses; only configured-MinIO/file-record-backed legacy values are eligible for temporary fallback.
- B-MIG-004: Existing structured rows are not duplicated or reordered.
- B-MIG-005: Run the migration twice; row counts, unique keys, order, and cover values are identical.
- B-MIG-006: H2 baseline and MySQL baseline both start cleanly with the new table.
- B-MIG-007: Rollback instructions remove only the new structure/fallback use and do not delete MinIO objects or legacy strings.
- B-MIG-008: Versioned migration ledger/readiness check rejects deployment when the expected migration version is absent and the operator command is replayed twice with identical counts.

### 3.3 Public product detail

- B-PUB-001: Enabled S1 under approved M1/enabled category returns full detail and ordered public media URLs.
- B-PUB-002: Unknown ID returns stable 404.
- B-PUB-003: Disabled/deleted product returns stable unavailable/404 without management fields.
- B-PUB-004: Enabled product under unapproved merchant is hidden.
- B-PUB-005: Enabled product under disabled category is hidden.
- B-PUB-006: Anonymous caller succeeds for visible detail.
- B-PUB-007: Response excludes file owner IDs, MinIO credentials/object secrets, other users' orders, and leave reasons.
- B-PUB-008: S1 and same-name S2 return their own merchant, price, images, rating, and service version.
- B-PUB-009: Detail query count stays bounded and does not grow per media row/keeper/rating.
- B-PUB-010: Legacy detail, public list, merchant/category list, and availability routes all enforce the same visibility invariant.
- B-PUB-011: Anonymous merchant/keeper summary DTOs omit user IDs, phones, exact coordinates/address, license/certificate URLs, internal statuses, capacity internals, leave reasons, and workflow data.
- B-PUB-012: Unpublished product media is not discoverable through product APIs; generated delivery URLs use the configured trusted origin/private delivery policy, and snapshots do not persist expiring signatures.

### 3.4 Management and authorization

- B-MGT-001: M1 creates a product for itself without trusting a forged M2 merchant ID.
- B-MGT-002: Admin can explicitly create/manage for M2.
- B-MGT-003: M1 can read/update/status-toggle/media-replace its product.
- B-MGT-004: M1 receives 403 for every M2 manage endpoint; database rows remain unchanged.
- B-MGT-005: Anonymous/owner roles receive 401/403 for management endpoints.
- B-MGT-006: Blank/oversize name, oversize introduction, negative price, invalid category, invalid status, and invalid unit reject.
- B-MGT-007: Disabled category cannot be assigned to a new public product.
- B-MGT-008: Logical product deletion retains referenced order/snapshot integrity.

### 3.5 Booking and snapshots

- B-ORD-001: S1 price is used even when request includes a different price or keeper price differs.
- B-ORD-002: Merchant is derived from S1; forged merchant is ignored/rejected.
- B-ORD-003: K1 must belong to S1's merchant and be eligible.
- B-ORD-004: Changed service version returns `PRICE_CHANGED` and creates no order/coupon/snapshot side effect.
- B-ORD-005: Off-shelf product after availability lookup rejects with no partial side effects.
- B-ORD-006: Missing merchant ID is accepted for the new product request; forged/mismatched merchant ID is ignored or rejected and never selects the merchant.
- B-ORD-007: Unsupported `次`/`课时`/`小时` units return `UNSUPPORTED_SERVICE_UNIT` and cannot create a day-based order; `天` and `day` follow the supported path.
- B-SNP-001: New snapshot contains product name/category/introduction/price/unit/version/ordered media.
- B-SNP-002: Editing S1 fields/media after O1 leaves O1 snapshot unchanged.
- B-SNP-003: Disabling/logically deleting S1 leaves O1 readable.
- B-SNP-004: Legacy snapshot without new keys renders with safe fallback.

## 4. Frontend Unit and Component Catalog

### 4.1 Merchant product editor

- F-MGT-001: Create form validates required name/category/price/unit.
- F-MGT-002: Existing management detail hydrates scalar fields and media order/cover.
- F-MGT-003: Multiple upload success stores file IDs, not only URLs.
- F-MGT-004: Partial upload failure preserves successful files, marks failures, and supports retry without duplicates.
- F-MGT-005: Move left/right or drag reorders deterministically; keyboard users can perform the same action.
- F-MGT-006: Cover selection is unique and removal of cover selects a deterministic replacement.
- F-MGT-007: Wrong MIME, oversize file, and more than 10 images are blocked with visible errors.
- F-MGT-008: Save failure preserves entered data and re-enables controls.
- F-MGT-009: Double save emits one mutation.
- F-MGT-010: Unsaved changes trigger the expected leave/close protection.

### 4.2 Product detail

- F-DET-001: Loading skeleton/status has an accessible label.
- F-DET-002: S1 renders full content and first cover image.
- F-DET-003: Thumbnail click and keyboard action change the main image without layout shift.
- F-DET-004: Zero media uses `MediaWithFallback`; malformed URL does not break page.
- F-DET-005: Ten media items remain usable on desktop/mobile.
- F-DET-006: Service 404/unavailable shows an explicit action back to catalog.
- F-DET-007: Merchant/rating partial failure does not erase core product content.
- F-DET-008: Changing route param from S1 to S2 discards stale async responses.
- F-DET-009: Long Chinese name/introduction and large price do not overflow.
- F-DET-010: Buttons, thumbnails, ratings, merchant and keeper links have accessible names and visible focus.

### 4.3 In-page booking

- F-BKG-001: Logged-in owner click opens dialog without route change.
- F-BKG-002: Cancel closes dialog and restores focus to `立即预约`.
- F-BKG-003: Anonymous click opens login prompt and stores `/services/{id}?book=1` only.
- F-BKG-004: Login return opens dialog once and clears/replaces the one-shot query.
- F-BKG-005: Failed profile requirement does not open dialog and preserves detail.
- F-BKG-006: Dialog reloads service by ID and does not trust prop name/price/merchant.
- F-BKG-007: Stale availability response cannot overwrite a newer keeper/date selection.
- F-BKG-008: Double submit sends one POST.
- F-BKG-009: `PRICE_CHANGED` reloads product/quote and requires reconfirmation.
- F-BKG-010: Success routes to `/orders/{createdId}`; error leaves dialog with safe selections.
- F-BKG-011: Unsupported-unit detail keeps full content visible but disables booking with an explanatory reason.
- F-BKG-012: Login return rejects external, protocol-relative, encoded-control, fragment, unknown-query, and nested redirect targets and falls back safely.

### 4.4 Entry convergence and order management

- F-NAV-001: ServiceGrid card, image, detail, and booking button route to detail ID.
- F-NAV-002: Dashboard product CTA routes to detail ID.
- F-NAV-003: Merchant detail product booking routes to product detail.
- F-NAV-004: Keeper surfaces do not create service-less orders; they select a merchant product or use contact/chat wording.
- F-NAV-005: Orders has no generic create button and no `create=true` watcher.
- F-NAV-006: Query strings never carry service name, price, or merchant authority.
- F-NAV-007: Legacy `/api/services/{id}` consumers receive the same public visibility behavior as detail, or are migrated to management-only access.

## 5. Browser End-to-End Catalog

### 5.1 Public and anonymous flow

1. Open catalog as anonymous, click S1, assert URL `/services/{S1}` and full product data.
2. Use gallery thumbnails, merchant link, then browser back; product page restores correctly.
3. Click `立即预约`; login prompt appears while URL remains product detail.
4. Log in and return; dialog opens once on the same detail.
5. Cancel, refresh, and assert it does not reopen unexpectedly.

### 5.2 Owner booking flow

1. Open S1 detail as owner and open dialog in place.
2. Select P1, K1, valid future dates/slots and contact data.
3. Intercept POST payload: require `service_id_wsh` and version; reject assertions if client price/total is authoritative.
4. Submit once, assert dialog closes and URL becomes `/orders/{id}`.
5. Verify displayed snapshot matches S1.
6. As merchant, edit S1 name/price/gallery; revisit order and verify snapshot did not change.

### 5.3 Product isolation

1. Open same-name S1/M1 and S2/M2 in turn.
2. Assert each detail/gallery/dialog uses its own service/merchant/price.
3. Create an order from S2 and verify no S1 identifiers appear in request or order.

### 5.4 Merchant management

1. Log in as M1 merchant and create a run-owned product with three uploaded images.
2. Reorder images, choose cover, save, preview public detail.
3. Edit name/category/introduction/price and verify detail refresh.
4. Disable product and verify public unavailable state while management still sees it.
5. Attempt M2 product/media request with M1 token through API and assert 403/no mutation.

### 5.5 Responsive and accessibility

- At 1280x720 and 390x844, capture product detail, open dialog, and merchant editor screenshots.
- Assert no horizontal page overflow and no text/button overlap.
- Tab through gallery, booking CTA, dialog, close, and editor media controls.
- Assert modal focus containment, Escape/cancel behavior, focus restoration, alt text, and reduced-motion compatibility.
- Fail on uncaught page errors, console errors, failed media/API responses not intentionally tested, or unexpected 500 responses. Record the exact browser backend build/classpath used.

### 5.6 Replayability

- Generate run ID, merchant/product/pet/file records in setup.
- Track created IDs in the worker fixture.
- Clean only tracked run-owned rows/objects in reverse dependency order in teardown.
- Execute the full Playwright suite twice consecutively. Both runs must pass without manual SQL or date edits.

## 6. Phase Gates

| Unit | Focused gate | Broader gate before commit |
|---|---|---|
| U0 | Existing service/order/rating/availability tests and non-destructive browser subset | Maven business, all Vitest, build; do not delete shared fixture/order rows or include `135` |
| U1 | Media/upload/concurrency + migration tests | Maven business, MySQL replay twice, migration ledger/readiness |
| U2 | Public/manage API, allowlist, visibility and order-contract tests | Maven business + admin |
| U3 | Merchant editor specs | All Vitest, build, merchant management desktop/mobile E2E |
| U4 | ServiceDetail specs | All Vitest, build, public detail desktop/mobile screenshots |
| U5 | Detail/dialog/unit/login-resume specs | All Vitest, build, supported booking and unsupported-unit E2E |
| U6 | Navigation and Orders specs | Static legacy-pattern scan, all Vitest, full navigation E2E |
| U7 | Snapshot backend/frontend specs | Maven business, all Vitest, snapshot browser flow |
| U8 | Full matrix | Maven business/admin, Vitest, build, MySQL replay, Playwright twice |

## 7. Final Commands

Run from repository root unless noted:

```powershell
mvn -pl pet-business -am test
mvn -pl pet-admin -am test
Set-Location frontend
npm test
npm run build
npx playwright test
npx playwright test
```

Also run the versioned migration against a temporary MySQL schema twice, record migration-ledger/version checks, before/after invariant queries and unresolved counts. Use a clean, current backend process built from the working tree; do not test against a stale `.m2` service jar.

## 8. Exit Report

The verification report must include exact test counts, command exit states, first failure and fix history, MySQL replay evidence, desktop/mobile screenshot paths, console/network findings, changed file list, commit IDs by unit, unverified items, and residual risks.
