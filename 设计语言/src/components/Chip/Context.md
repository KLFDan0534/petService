# Chip

The 44px filter control from the services catalogue. Ported 1:1 from `src/views/user/Services.vue` (`.s-chip` / `.s-rail`), with the Vue scoped CSS translated to Tailwind utilities bound to the same `--ref-*` tokens.

Use it for single-select category / sort filters that sit above a grid, table or list. It is **not** a tag or a status indicator — use `StatusBadge` for status and read-only labels.

## Anatomy

- Height 44px, `rounded-control` (11px), `0 16px` padding, 6px gap.
- Rest: `bg-surface`, `border-line` hairline, `text-ink-soft`, 13.5px / 500 (`text-control`).
- Hover: border shifts to 30% ink, fill to `bg-sand`.
- Active (selected): `bg-ink` + `border-ink` + `text-cream`.
- Optional count: 11px, 0.6 opacity, tabular numerals.
- Transitions: 150ms `ease-editorial` on colour and border only — no scaling.

## Props — `Chip`

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| `label` | `ReactNode` | — | Required. Truncates instead of wrapping. |
| `count` | `number \| string` | — | Trailing count. Pass `'--'` while loading. |
| `active` | `boolean` | `false` | Selected state; also sets `aria-pressed`. |
| `icon` | `ReactNode` | — | Optional leading lucide icon, sized to 16px. |
| `disabled` | `boolean` | `false` | Dimmed, non-interactive. |
| `className` | `string` | `''` | Appended last. |

All other `<button>` props (`onClick`, `aria-*`, …) pass through.

## Props — `ChipRail`

| Prop | Type | Notes |
| --- | --- | --- |
| `label` | `string` | Required accessible name for the `role="group"`. |
| `children` | `ReactNode` | The chips. |
| `className` | `string` | Appended last. |

Renders the source's `.s-rail`: flex row, 8px gaps, horizontal scroll with the scrollbar hidden, 4px bottom padding.

## Usage

```tsx
import { Chip, ChipRail } from 'components/Chip'

const [selected, setSelected] = useState('')

<ChipRail label="服务分类">
  <Chip
    label="全部"
    count={loading ? '--' : services.length}
    active={selected === ''}
    onClick={() => setSelected('')}
  />
  {categories.map((category) => (
    <Chip
      key={category}
      label={category}
      count={categoryCounts[category] ?? 0}
      active={selected === category}
      onClick={() => setSelected(category)}
    />
  ))}
</ChipRail>
```
