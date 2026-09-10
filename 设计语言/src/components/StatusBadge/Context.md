# StatusBadge

The system's single status pill. Ported from the product's confirmed `.badge` / `.badge-{tone}`
family (`src/views/user/KeeperWorkflow.vue:643-655`, cross-checked with `KeeperApply.vue:489-503`
and `PetDetail.vue:674-692`).

Use it for any status, category, or priority value: order status, review status, keeper online
status, ticket category, read/unread. It is the only sanctioned way to render a status value —
do not hand-roll a coloured pill.

## Appearance

Every tone is a restrained wash of its colour into the warm surface: `color-mix()` fill, matching
deep text, and a low-opacity border. 11px / weight 500 / 4px×10px / 6px radius / 6px gap.

**Warning must stay the amber wash** (`#b45309` at 10% fill, `#b45309` text, 26% border). The
`background: var(--ref-brand); color: #fff` variants in `Complaints.vue:780`, `Payments.vue:378`,
`TicketDetail.vue:477` and `Tickets.vue:450` are deviations — migrate them to `tone="warning"`.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| `tone` | `'primary' \| 'secondary' \| 'success' \| 'warning' \| 'danger' \| 'error' \| 'info' \| 'disabled'` | `'info'` | Matches the source `.badge-{tone}` classes. `danger` and `error` are visually identical, as in the source. |
| `dot` | `boolean` | `false` | 5px tone-coloured dot before the label (online/offline/busy style). |
| `icon` | `ReactNode` | — | Optional leading `lucide-react` icon, auto-sized to 12px. |
| `children` | `ReactNode` | — | The label. Truncates with `ref-truncate` so long values never break a grid. |
| `className`, `style`, `...rest` | span attributes | — | Merged onto the root `<span>`. |

## Usage

```tsx
import { StatusBadge, toneFromBadgeClass } from 'components/StatusBadge'

<StatusBadge tone="warning">待付款</StatusBadge>
<StatusBadge tone="success" dot>在线</StatusBadge>
<StatusBadge tone="danger">已取消</StatusBadge>
```

### Migrating from `statusMaps.js`

`toneFromBadgeClass()` converts the legacy `getStatusBadge(map, key)` result (`'badge-warning'`,
`'badge-info'`, …) into a tone, defaulting to `info` exactly like the source helper:

```tsx
<StatusBadge tone={toneFromBadgeClass(getStatusBadge(OrderStatus, order.status))}>
  {getStatusLabel(OrderStatus, order.status)}
</StatusBadge>
```

## Guidance

- One badge per record row or card. Never stack multiple tones to imply severity.
- Keep labels short (2–5 characters in Chinese); the badge truncates rather than wraps.
- No solid brand fill, no gradients, no shadow — status colour is a wash, not a button.
- Dark mode is inherited: token-driven tones flip with `html[data-theme="dark"]`.
