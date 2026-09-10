# DetailList

Key/value description list ported 1:1 from the product's order detail view
(`src/views/order/OrderDetailView.vue` — `.od-facts` / `.od-fact` and
`.od-money` / `.od-money-row`). This is the sanctioned way to present record
data: **records, settings and details use lists, not cards.**

Two variants:

- **`facts`** (default) — a responsive grid of micro-labelled facts. `dt` is 11px
  uppercase with `0.14em` tracking in `--ref-muted`; `dd` is 13.5px/1.5 in
  `--ref-ink-soft` with a 7px top margin and `word-break`. `amount` items switch
  the value to the serif 21px ink treatment.
- **`money`** — stacked amount rows, label left / value right on a shared
  baseline. `dt` 13.5px ink-soft, `dd` 14px ink; the `total` row adds a hairline,
  a medium-weight ink label, and (with `amount`) the serif 24px total.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| `items` | `DetailListItem[]` | — | The rows. |
| `variant` | `'facts' \| 'money'` | `'facts'` | Layout mode. |
| `columns` | `1 \| 2 \| 3 \| 4` | `4` | `facts` only. Collapses to 2 (or 1) below `lg`. |
| `divided` | `boolean` | `false` | `facts` only. Adds the `.od-facts-inner` top hairline for use inside a panel. |
| `className` | `string` | — | Extra classes on the `<dl>`. |
| `aria-label` | `string` | — | Label the list when it has no visible heading. |

### `DetailListItem`

| Field | Type | Notes |
| --- | --- | --- |
| `label` | `ReactNode` | `dt` content. |
| `value` | `ReactNode` | `dd` content. |
| `amount` | `boolean` | Serif amount treatment (21px in `facts`, 24px total in `money`). |
| `currency` | `boolean` | Prefixes the small muted `¥` glyph. |
| `numeric` | `boolean` | Tabular numerals. Defaults to `true` in `money`. |
| `total` | `boolean` | `money` only — closing total row. |
| `tone` | `'default' \| 'discount' \| 'muted' \| 'ink'` | Value accent; `discount` is `--ref-brand-deep`. |
| `key` | `string` | Optional stable React key. |

## Usage

```tsx
import { DetailList } from 'components/DetailList'

<DetailList
  aria-label="订单概要"
  items={[
    { label: '实付金额', value: '386.00', amount: true, currency: true, numeric: true },
    { label: '宠物', value: '柯基 · 团子' },
    { label: '寄养师', value: '待分配', tone: 'muted' },
    { label: '服务周期', value: '03-04 → 03-09', numeric: true },
  ]}
/>
```

```tsx
<DetailList
  variant="money"
  items={[
    { label: '服务金额', value: '¥430.00' },
    { label: '优惠减免', value: '−¥44.00', tone: 'discount' },
    { label: '实付金额', value: '386.00', total: true, amount: true, currency: true },
  ]}
/>
```

## Guidance

- Inside a `Section` / panel, use `divided` so the facts read as a continuation
  rather than a floating block.
- Keep `columns={4}` for page-level headers; drop to `1`–`2` inside cards.
- Always pass `numeric` for prices, ids, dates, durations and counts.
- Use `tone="muted"` for placeholder values (`待分配`, `—`) instead of a
  different font size.
