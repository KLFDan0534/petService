# StatList

Editorial hero statistics: a semantic `<dl>` of value/label pairs laid out in a wrapping flex row, separated by hairline dividers. Ported 1:1 from `src/views/user/Services.vue` (`.s-stats`).

Use it for supporting figures next to a `PageHeader`/hero, or as a compact overview band above a table. It is **not** a card grid — no borders, no background, no shadow.

## Anatomy

- Row: `flex flex-wrap items-end`, `0 28px` gaps (`0 20px` below 560px).
- Every item after the first: 1px `border-line` left rule + 28px left padding (20px below 560px).
- Value (`<dd>`): `font-display`, 30px / line-height 1, `-0.01em` tracking, `text-ink`, tabular numerals (26px below 560px).
- Label (`<dt>`): 12px `text-muted`, 8px top margin.

## Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `items` | `StatListItem[]` | — | Stats to render. Renders nothing when empty. |
| `loading` | `boolean` | `false` | Replaces every value with the placeholder and sets `aria-busy`. |
| `loadingPlaceholder` | `string` | `'--'` | Placeholder used while loading. |
| `label` | `string` | — | Accessible name for the list (`aria-label`). |
| `className` | `string` | `''` | Extra classes on the `<dl>` (e.g. `mt-9`). |

`StatListItem`: `{ id?: string; value: React.ReactNode; label: string }`

## Usage

```tsx
import { StatList } from 'components/StatList'

<StatList
  label="服务概览"
  className="mt-9"
  loading={loading}
  items={[
    { value: services.length, label: '可预约方案' },
    { value: categories.length, label: '服务分类' },
    { value: merchantCount, label: '覆盖门店' },
  ]}
/>
```

## Notes

- Values carry `data-numeric="true"`, so numbers stay aligned across items.
- Keep labels to 2–5 characters/short words; long labels widen a column and break the hero rhythm.
- Three to four stats is the sweet spot; the row wraps beyond that.
