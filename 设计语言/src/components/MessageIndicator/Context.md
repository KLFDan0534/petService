# MessageIndicator

Global unread-message control for layout navigation bars (user, merchant, admin). A 44×44 icon button that shows a critical dot — or an optional numeric badge — when there are unread notifications.

Ported from `src/components/common/MessageIndicator.vue`. The source's Element Plus icon and global `--color-*` variables are expressed here with `lucide-react` and the design system's editorial-warm tokens (`text-ink`, `hover:text-brand`, `bg-critical`, `rounded-control`).

## Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `unreadCount` | `number` | `0` | Unread notifications; the indicator appears when `> 0`. |
| `showCount` | `boolean` | `false` | Render the number instead of a plain dot. |
| `max` | `number` | `99` | Cap before rendering `{max}+`. |
| `label` | `string` | `'消息提醒'` | Tooltip and accessible name base. |
| `onClick` | `() => void` | — | Wire to the notifications route. |
| `disabled` | `boolean` | `false` | Non-interactive state. |
| `className` | `string` | `''` | Extra classes on the button. |

## Usage

```tsx
import { MessageIndicator } from 'components/MessageIndicator'

const navigate = useNavigate()

<MessageIndicator
  unreadCount={unreadCount}
  onClick={() => navigate('/notifications')}
/>
```

Numeric variant:

```tsx
<MessageIndicator unreadCount={128} showCount onClick={goNotifications} />
```

## Notes

- Sits inline with other nav controls; no wrapper padding is applied.
- The count uses `data-numeric="true"` for tabular numerals.
- Hover is a colour + 10% brand tint only — no shadow, no scale, per the motion rules.
