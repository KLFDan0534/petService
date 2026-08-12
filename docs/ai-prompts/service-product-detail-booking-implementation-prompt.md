# AI Implementation Prompt: Service Product Detail and In-Page Booking

You are implementing a cross-layer feature in the repository root. Work autonomously until the complete Definition of Done is satisfied, but never invent a product decision that contradicts the plan.

## Authoritative Inputs

Read these files completely before editing code:

1. `AGENT_RULES.md`
2. `docs/plans/2026-08-12-001-feat-service-product-detail-booking-plan.md`
3. `docs/testing/service-product-detail-booking-test-plan.md`
4. `docs/plans/2026-08-11-001-feat-service-driven-booking-plan.md`
5. `docs/2026-08-11-service-driven-booking-verification-report.md`

The 2026-08-12 plan is the source of truth for scope, technical decisions, implementation units, verification, and Definition of Done. The test plan is the source of truth for required scenarios. Do not copy or rewrite them into a competing design.

## Mandatory Engineering Workflow

Use the repository's engineering skill before implementation. Complete its four pre-code operations explicitly:

1. Inspect the real relevant routes, components, APIs, entities, services, migrations, permissions, MinIO/file records, order snapshots, and tests.
2. State the exact in-scope/out-of-scope boundary.
3. Select the smallest implementation path that follows existing patterns.
4. Define the focused and regression proof before the first code edit.

The working tree is already dirty with the preceding service-driven booking implementation. Preserve it. First run `git status` and inspect every overlapping diff. Verify those changes against their existing plan/report and create a clearly scoped prerequisite commit only if they are intended and pass their gates. Never reset, discard, overwrite, or blindly stage user changes. If an unrelated hunk cannot be separated safely, leave it unstaged and document it.

## Execution Order

Implement U0 through U8 in dependency order from the plan. For every unit:

1. Add or update the focused tests named by that unit before or with the production change.
2. Run the unit's focused gate and observe a meaningful failure/characterization state where practical.
3. Implement only that unit's coherent change.
4. Run the focused gate, then the unit's broader gate from the test plan.
5. Review the diff for authority bypasses, IDOR, partial transactions, N+1 queries, stale async responses, layout/accessibility failures, and unrelated churn.
6. Update the relevant documentation for that unit.
7. Stage only the verified files for that unit and create a conventional commit. Do not use `git add .` or `git add -A` in this dirty tree.
8. Record the commit ID and real test result before beginning the next unit.

Do not defer tests until the end. Do not mark a unit complete if its focused or affected regression gate is red.

## Non-Negotiable Product Rules

- `pet_service_wsh.id_wsh` is the product identity and booking selection.
- Category is taxonomy, not a sellable product.
- Backend derives merchant, price, availability, eligible keeper, and final amounts from the service.
- Every product-level booking action outside the detail page goes to `/services/:id` first.
- `立即预约` opens `CreateOrderDialog` on the detail page; it does not navigate to `/orders`.
- Anonymous login returns to the same detail and opens booking once.
- Structured product media references validated product-purpose `file_record_wsh`/MinIO data; arbitrary external URLs are neither accepted nor returned publicly.
- Public detail hides disabled/deleted products, unapproved merchants, and disabled categories.
- Merchants can manage only their own products/media; admin override remains explicit.
- New order snapshots freeze the complete purchased product presentation.
- Only `day`/`天` enters the current day-based order flow; other units retain full details but expose `UNSUPPORTED_SERVICE_UNIT` and a disabled booking action.
- Existing availability, version, pricing, coupon, membership, payment, refund, rating, MQ, SSE, and fulfillment behavior must remain intact.
- Every new persisted/request/response field has the `_wsh` suffix.

## Implementation Constraints

- Reuse `ServiceItem`, `CreateOrderDialog`, category store, `MediaWithFallback`, file upload/MinIO infrastructure, ownership checks, and order snapshot infrastructure.
- Add abstractions only where they remove real duplication or enforce an aggregate invariant.
- Use structured DTOs/parsers; do not manipulate JSON or media collections with ad hoc string concatenation in new code.
- Keep the legacy `images_wsh` path only as the plan's internal-MinIO/file-record-backed temporary compatibility layer; external/unresolved values are audit-only. Remove compatibility only after the plan's measurable exit criterion.
- Keep public and management projections separate.
- Inventory every anonymous service endpoint and make detail, legacy ID, list and availability paths share one public-visibility invariant.
- Use explicit allowlisted public merchant/keeper summary DTOs; never serialize entity/management projections to anonymous callers.
- Save scalar product fields and ordered media through one aggregate transaction. Serialize concurrent replacement with a parent-row lock or checked aggregate version.
- Use a server-owned product-image upload purpose with magic-byte/decode validation, raster allowlist and byte/dimension caps. Ignore caller directories and reject SVG/HTML/polyglots.
- Scope merchant uploads to their merchant. Admin uploads must explicitly target one merchant and cannot attach unrelated users' private file records.
- Derive public media delivery from the configured trusted origin/private-bucket policy; do not snapshot expiring signed URLs.
- Do not physically delete detached product MinIO objects in this release. Only compensate the same upload request when object creation succeeds but file-record creation fails.
- Make product order creation service-first: require service ID/version, derive merchant/price, and remove client merchant authority.
- Validate login return URLs as normalized same-origin `/services/{positiveId}?book=1` routes only.
- Keep list payloads bounded; do not add full detail galleries to every list query unless the plan explicitly calls for a cover summary.
- Do not add a third-party gallery/drag library without proving the existing stack cannot meet the behavior and bundle constraints.
- No TODOs, mock production data, swallowed errors, empty pages, or unhandled 500s.
- Do not expose credentials, file ownership internals, other users' bookings, keeper leave reasons, or capacity internals.

## Testing and Browser Requirements

Execute every scenario applicable to the unit from `docs/testing/service-product-detail-booking-test-plan.md`. Final verification must include:

- Full Maven business and admin reactor tests.
- Full frontend Vitest suite.
- Production frontend build with size delta recorded.
- MySQL incremental migration on a temporary schema twice with invariant queries.
- Versioned migration ledger/readiness proof and the exact CI/CD/operator command; standalone SQL is not assumed to run at application startup.
- Deployment-order compatibility proof: schema/purpose migration, dual-read/write-new code, backfill/audit, client adoption, then separately approved legacy removal.
- Owner/admin/foreign-merchant authorization probes.
- Desktop 1280x720 and mobile 390x844 browser flows.
- Product list to detail, anonymous login resume, in-page booking, created order detail, product edit, and frozen historical snapshot.
- Two consecutive full Playwright runs using unique run-owned fixtures and automatic cleanup.
- Console errors, uncaught page errors, unexpected 4xx/5xx, broken media, overflow, overlap, focus, keyboard, and reduced-motion checks.

The current baseline evidence is Maven 121/121, Vitest 69/69, build success, and Playwright 2/3. The failed Playwright case reused fixed pet 109, which already had active order 141 for its fixed dates. Do not delete that order to make the test pass. Replace fixed-fixture behavior with isolated run-owned data.

Ensure the backend used by E2E is built from the current working tree. The existing local process may load `pet-business` from `.m2`; verify runtime classpath/API version before trusting browser results.

## Required Delivery Artifacts

Before finishing:

- Update MySQL baselines, H2 schema, idempotent migration, and database table documentation.
- Update Swagger annotations for all new/deprecated contracts.
- Update `docs/功能模块操作流程与使用说明.md` for merchant product management, public detail, and detail-page booking.
- Create a dated verification report containing exact commands, counts, failures/fixes, migration replay, browser evidence, and residual risks.
- List commits for U0-U8 and ensure each commit contains only reviewed scope.
- Remove only implementation-owned abandoned experiments, debug files, stale compatibility code outside the declared exit criterion, and generated artifacts not needed as evidence. Do not delete unknown pre-existing files such as untracked `135`.

## Completion Response

Your final response must report:

1. U0-U8 completion and the key design decisions actually implemented.
2. Changed files and database migration/rollback behavior.
3. Every test command with real pass/fail/skip counts.
4. Browser desktop/mobile flows and screenshot/evidence locations.
5. Security, concurrency, migration, snapshot, and adversarial-review findings and fixes.
6. Commit IDs by unit.
7. A strict split between automated verification, manual verification, unverified items, and residual risks.

Do not stop at an implementation summary. Continue until the plan's Definition of Done is genuinely met or a blocker changes product scope/contradicts the plan and cannot be resolved from repository evidence.
