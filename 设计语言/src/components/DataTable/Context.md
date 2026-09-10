# DataTable

The admin / merchant workspace table. Ported from the product's `src/components/common/DataTable.vue`, so the public contract is unchanged: `columns` + `data`, badge cells detected by an allow-list, and a trailing 操作 action slot. What the port adds are the states the Vue version lacked — **loading**, **error + retry** — and the responsive restructure required by the brand guidelines (scrollable table at `md` and up, label/value lists below it).

Use it for records: orders, users, merchants, payments, tickets. Do not use `Card` grids for tabular data.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| `columns` | `(string \| { key, label?, align?, width? })[]` | `[]` | A bare string is both the row key and the header, matching the Vue source. |
| `data` | `T[]` | `[]` | Array of plain row objects. |
| `actions` | `(row, index) => ReactNode` | – | React equivalent of the Vue default slot. When present, a right-aligned action column is appended. |
| `actionsLabel` | `string` | `'操作'` | Header for the action column. |
| `loading` | `boolean` | `false` | Renders shimmer rows instead of data. Sets `aria-busy`. |
| `loadingRows` | `number` | `4` | Shimmer row count. |
| `error` | `string \| null` | `null` | Replaces the body with an alert region. Takes precedence over the empty state. |
| `onRetry` | `() => void` | – | Shows the retry button inside the error region. |
| `retryLabel` | `string` | `'重试'` | |
| `emptyText` | `string` | `'暂无数据'` | Inline empty message. |
| `caption` | `string` | – | Accessible table name. |
| `captionHidden` | `boolean` | `true` | Set `false` to show the caption as a meta line above the rows. |
| `className` | `string` | `''` | Applied to the outer frame. |

## Cell rendering

- **Badge cells** — a cell value shaped `{ badge, label }` renders as a pill. `badge` must be one of `ALLOWED_BADGES`: `badge-primary`, `badge-secondary`, `badge-success`, `badge-warning`, `badge-danger`, `badge-error`, `badge-info`, `badge-disabled`. Anything else falls through to text.
- **Text cells** — `null` / `''` render as `-`; objects fall back to `label` then `JSON.stringify`; booleans render 是/否.
- Numeric cells get `data-numeric="true"` for tabular numerals.
- Long values wrap with `overflow-wrap: anywhere` so order ids and usernames never break the grid.

Exports: `DataTable`, `ALLOWED_BADGES`, `isBadgeCell`, `formatCell`, plus the `DataTableProps` / `DataTableColumn` / `DataTableBadgeCell` types.

## Usage

```tsx
<DataTable
  columns={[
    { key: 'id_wsh', label: '订单号', width: '140px' },
    { key: 'user_wsh', label: '用户' },
    { key: 'amount_wsh', label: '金额', align: 'right' },
    { key: 'status_label_wsh', label: '状态' },
  ]}
  data={orders}
  loading={isLoading}
  error={error}
  onRetry={refetch}
  caption="订单列表"
  actions={(row) => <Button size="sm" variant="ghost">详情</Button>}
/>
```

Plain-key columns, as used across the existing admin views:

```tsx
<DataTable columns={['name_wsh', 'type_wsh', 'breed_wsh']} data={pets} />
```
