# LoadingSpinner

Small inline busy indicator: a warm terracotta ring over a hairline track, with a short status message beneath it. Ported from the product's `src/components/common/LoadingSpinner.vue`.

Use it for short, scoped waits (a panel, a dialog body, a submit action). For content that has a known shape — lists, cards, tables — prefer `Skeleton` / `SkeletonList` instead, per the Editorial Warm guidelines.

## Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `text` | `string` | `'加载中...'` | Message shown under the spinner; also used as the `aria-label`. |
| `className` | `string` | — | Extra classes on the wrapper (e.g. to tighten the padding). |

## Behaviour

- Renders `role="status"` with `aria-live="polite"` so screen readers announce the wait.
- Ring uses `border-line` with a `border-t-brand` head and the system `animate-spin` (`ref-spin`) keyframes.
- Collapses automatically under `prefers-reduced-motion: reduce` via the global guard in `index.css`.

## Usage

```tsx
import { LoadingSpinner } from 'components/LoadingSpinner'

{loading && <LoadingSpinner text="加载商家资料..." />}
```

Inside a surface:

```tsx
<div className="rounded-card border border-line bg-surface">
  {loading ? <LoadingSpinner text="加载订单列表..." /> : <OrderList orders={orders} />}
</div>
```
