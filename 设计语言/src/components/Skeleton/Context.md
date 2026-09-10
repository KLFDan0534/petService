# Skeleton

The only sanctioned loading placeholder in Editorial Warm. Never ship a bare `加载中...` / `Loading...` string — render a Skeleton region that matches the shape of the content that will replace it.

Ported from the product's repeated shimmer blocks (`src/views/user/Services.vue` `.s-hero-skel` / `.service-skeleton`, `src/views/user/ServiceDetail.vue` `.d-skel-media` / `.d-skel-info`, `src/components/dashboard/ServiceGrid.vue` markup). The fill is the source declaration verbatim:

```
linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%)
background-size: 200% 100%
animation: ref-shimmer 1.3s linear infinite
```

That declaration now exists **once** as the global `.ref-shimmer` utility in `index.css`, together with a single `@keyframes ref-shimmer` and the `prefers-reduced-motion: reduce` guard — do not re-declare it in a view.

## Exports

| Export | Use |
| --- | --- |
| `Skeleton` | Primitive shape: text line, block, media, circle |
| `SkeletonText` | Stack of ragged shimmer lines |
| `SkeletonCard` | Browsable-item placeholder (media + body lines) |
| `SkeletonList` | Loading region for a card grid, with `role="status"` |

## `Skeleton` props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| `variant` | `'line' \| 'block' \| 'media' \| 'circle'` | `'line'` | Shape preset; sets default height/radius |
| `width` | `string \| number` | `100%` (`40px` for circle) | Numbers become px |
| `height` | `string \| number` | per variant (`line` 14px, `block` 120px) | Overrides `aspect` |
| `radius` | `'none' \| 'sm' \| 'tile' \| 'control' \| 'card' \| 'frame' \| 'full'` | per variant | Token radii only |
| `aspect` | `string` | `'16 / 10'` for `media` | e.g. `'4 / 3'`, `'1 / 1'` |
| `tone` | `'shimmer' \| 'static'` | `'shimmer'` | `static` = flat `bg-sand` panel (source `.d-skel-info`) |
| `className` | `string` | — | Extra layout classes |

All primitives are `aria-hidden`; put the accessible label on the region (`SkeletonList` does this for you).

## `SkeletonText` props

`lines` (3), `gap` (14px), `lastLineWidth` (`'76%'`), `className`.

## `SkeletonCard` props

`aspect` (`'16 / 10'`), `lines` (3), `className`.

## `SkeletonList` props

`count` (6), `label` (`'加载中'`), `gridClassName`, `className`.

## Usage

```tsx
import { Skeleton, SkeletonText, SkeletonList } from 'components/Skeleton'

// Region-level: a loading service catalogue
{loading ? <SkeletonList count={6} label="服务加载中" /> : <ServiceGrid items={items} />}

// Detail hero: shimmer media beside a flat secondary panel
<div className="grid grid-cols-1 gap-gutter md:grid-cols-[minmax(0,1.35fr)_minmax(0,1fr)]">
  <Skeleton variant="media" aspect="16 / 10" radius="frame" />
  <Skeleton variant="block" height={320} tone="static" />
</div>

// Inline copy
<SkeletonText lines={2} gap={10} lastLineWidth="42%" />
```

## Rules

- Match the real content's shape and rhythm — same aspect ratio, same line count, same radii.
- Use `tone="static"` for secondary panels so only one element shimmers per region.
- Skeletons are for first load. Use an inline spinner or optimistic state for refreshes of already-visible data.
- Pair with `EmptyState` (no results) and `ErrorState` (failure); a data region must handle all three.
