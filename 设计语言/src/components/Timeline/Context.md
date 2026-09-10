# Timeline

Vertical list of care/activity records — each record is a hairline card with a muted
`type · datetime` meta line, body copy, and an optional photo grid.

Ported from the product's `KeeperDailyTab.vue` timeline, which was previously styled
by the consuming view (`KeeperWorkflow.vue` `:deep(.timeline*)`). Those measured
values now live inside this component: 10px item gap, 8px top margin, `--ref-line`
hairline card on `--ref-surface` at 14px radius with 14px padding, 11.5px muted meta,
13px/1.6 `ink-soft` body, 88px auto-fill photo grid capped at 420px wide.

Use it for keeper daily records, order activity feeds, and customer-service
workflow histories. It is a record list, not a browsable card grid — no shadow,
no hover lift on the item itself.

## Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `items` | `TimelineItem[]` | required | Records, newest first (order is not changed) |
| `emptyLabel` | `string` | `'暂无动态'` | Inline text when `items` is empty |
| `loading` | `boolean` | `false` | Renders shimmer skeleton rows instead of the list |
| `loadingCount` | `number` | `3` | Skeleton row count while `loading` |
| `onImageClick` | `(url, item) => void` | – | Makes photos focusable buttons (e.g. open a lightbox) |
| `className` | `string` | `''` | Extra classes on the list container |

### `TimelineItem`

| Field | Type | Description |
| --- | --- | --- |
| `id` | `string \| number` | React key |
| `type` | `string` | Raw record type (`feed`, `activity`, `medication`, `health`, `note`) mapped through `RECORD_TYPE_LABELS` |
| `typeLabel` | `string` | Explicit meta label, overrides `type` |
| `time` | `string \| number \| Date \| null` | Record time, rendered as `toLocaleString()`; falls back to `-` |
| `content` | `string` | Body copy |
| `images` | `string \| string[] \| null` | Comma-separated string (API shape) or array of urls |

Also exported: `RECORD_TYPE_LABELS`, `parseImageUrls`, `formatDateTime`.

## Usage

```tsx
import { Timeline } from 'components/Timeline'

<Timeline
  items={careRecords.map(record => ({
    id: record.id_wsh,
    type: record.type_wsh,
    time: record.record_time_wsh || record.created_at_wsh,
    content: record.content_wsh,
    images: record.images_wsh,
  }))}
  loading={loadingRecords}
  onImageClick={url => openLightbox(url)}
/>
```

Broken photos degrade to a `sand` placeholder tile rather than a torn image.
