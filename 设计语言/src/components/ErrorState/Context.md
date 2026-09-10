# ErrorState

The third data-region state. `rules/motion-states-and-resilience.md` requires loading / empty / error on every data region; the system had `Skeleton` and `EmptyState` but nothing for failure, so views fell back to toasts or swallowed the error entirely.

Evidence for adding it (CONFIRMED in the module audit):
- Toast-only failure with no retry: `MerchantOrders` is the *only* view with an inline retry; BusinessHours / Services / Keepers / CS, `AdminStatistics:177`, `AdminWallets:137`, `AdminMembershipUsers:152`, `AdminComplaints:245`, `AdminUsers:69`, `AdminPets:87`, `TicketDetail:190` all toast and stop.
- Silently swallowed: `MerchantDashboard:29`, `MerchantStatistics:22`, `MerchantPets:38`, `AdminDashboard:109`, `AdminOperationLogs:84`, `CsChat:359,404`, `Tickets:247,275`.
- `src/views/error/ServerError.vue:6` and `ServiceUnavailable.vue:6` tell the user "请稍后重试" but offer only 返回首页 / 返回上一页 — no retry.

## Variants

| Variant | Use for |
| --- | --- |
| `inline` (default) | A failed region — list, table, stat grid, panel. Replaces the region's contents, keeps the page shell. |
| `page` | The route surfaces `/403`, `/404`, `/500`, `/503`. |

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| `variant` | `'inline' \| 'page'` | `'inline'` | |
| `code` | `string \| number` | — | `page` only. Rendered `aria-hidden` — the `<h1>` carries the meaning. |
| `title` | `string` | `加载失败` | |
| `description` | `string` | — | What to do next. |
| `detail` | `string` | — | Server/technical message. Small, muted, clamped to 2 lines. |
| `onRetry` | `() => void` | — | Omit and no retry button renders. |
| `retryLabel` | `string` | `重试` | |
| `retrying` | `boolean` | `false` | Puts the retry `Button` in its loading state. |
| `icon` | `ReactNode` | warning mark | |
| `children` | `ReactNode` | — | Secondary actions (返回首页, 联系客服 …). |
| `className` | `string` | `''` | |

## Usage

```tsx
// Failed region
{error ? (
  <ErrorState description="网络请求失败，请检查连接后重试。" onRetry={reload} />
) : loading ? (
  <SkeletonList count={6} />
) : items.length === 0 ? (
  <EmptyState title="暂无数据" />
) : (
  <List items={items} />
)}

// Route surface
<ErrorState variant="page" code={500} title="服务出现异常" retryLabel="重新加载" onRetry={reload}>
  <Button variant="outline" size="sm" href="/">返回首页</Button>
</ErrorState>
```

## Design notes

- Inline uses a restrained `critical/25` hairline over a 4% critical wash — no solid red fill, no shadow at rest.
- `page` numeral is `clamp(56px, 13vw, 104px)` in `text-line`, fixing the audited fixed `96px` that filled a 320px viewport.
- Action rows are `flex-wrap`, fixing the audited non-wrapping `.error-actions` row.
- Inline carries `role="alert"`; the numeral is `aria-hidden` so the `<h1>` is what screen readers announce (the audited pages had the code as a `<div>` and the real heading at 24px).
- Composes the DS `Button`, so retry/secondary actions inherit the 42px CTA contract and the focus ring.

## Vue port note

Mirror this API on the Vue side as `src/components/common/ErrorState.vue` with `variant / code / title / description / detail / retryLabel / retrying` props and a `retry` emit, plus a default slot for secondary actions. It is a **new** shared component (no existing `.vue` counterpart) — see `rules/design-system-artifact-boundary.md`, group B.
