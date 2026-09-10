# PopupNotice

Global announcement dialog, ported from `src/components/common/PopupNotice.vue`. Mounted once near the app root (the source mounts it in `App.vue`) and shows the popup-delivery notice queue one item at a time until every notice has been dismissed.

Presentational only — the caller owns loading and persistence. Fetch the queue (`getPopupNotices()` for signed-in users, `getActiveNotices({ type: 'notice' })` otherwise), filter it with the exported `isPopupNotice` guard, and persist dismissal inside `onDismiss` (`dismissPopup(id)` or a `localStorage` read flag for anonymous visitors).

## Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `notices` | `PopupNoticeItem[]` | required | Popup queue. Renders nothing when empty. |
| `onDismiss` | `(notice: PopupNoticeItem, index: number) => void` | — | Fired for the close button, the primary action, `Escape`, and a scrim click. Persist the dismissal here. |
| `onDismissAll` | `() => void` | — | Fired once the queue is emptied. |
| `badgeLabel` | `string` | `'公告'` | Label in the terracotta pill above the title. |

### `PopupNoticeItem`

Field names match the API payload: `id_wsh`, `title_wsh`, `content_wsh`, and the optional `type_wsh` / `delivery_type_wsh` used by `isPopupNotice`.

## Behaviour

- One notice visible at a time; dismissing removes it from the queue and advances, clamping the index at the end.
- Counter (`1 / n`) and a `下一条` action appear only when more than one notice remains; a single notice shows `我知道了`.
- Content preserves line breaks (`whitespace-pre-wrap`) and scrolls at `300px`.
- Scrim click (target must be the scrim itself) and `Escape` dismiss the current notice.

## Usage

```tsx
import { PopupNotice, isPopupNotice, type PopupNoticeItem } from 'components/PopupNotice'

const [notices, setNotices] = useState<PopupNoticeItem[]>([])

useEffect(() => {
  getPopupNotices().then((r) => {
    setNotices((r.data ?? []).filter(isPopupNotice))
  })
}, [])

<PopupNotice
  notices={notices}
  onDismiss={(notice) => dismissPopup(notice.id_wsh)}
/>
```

## Notes

The source's scoped CSS relied on `--color-card` / `--color-primary` variables from a global stylesheet that isn't present here, so those rules are reproduced with this system's tokens (`bg-surface`, `text-ink`, `bg-brand`, `text-ink-soft`, `rounded-tile`, `shadow-lift`) at the original measurements. Entrance uses the shared `animate-pop` / `animate-fade` keyframes, so it collapses under `prefers-reduced-motion`.
