# TipDialog

Small modal for tipping a keeper after an order. Ported from `src/components/order/TipDialog.vue`; the logic is unchanged — both fields reset every time the dialog opens, and submission is blocked unless an amount and an order are present. The source used bare bootstrap-ish classes (`.modal`, `.btn`, `.form-control`) that don't exist here, so the visuals are rebased on the Editorial Warm tokens: `bg-surface` panel, `border-line` hairlines, `rounded-card`, `shadow-lift`, `animate-pop`, brand-filled confirm.

## Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `open` | `boolean` | — | Whether the dialog is visible. Returns `null` when false. |
| `order` | `TipDialogOrder \| null` | `null` | Order being tipped. `id_wsh` is passed back on confirm; optional `serviceName` shows as context. |
| `onClose` | `() => void` | — | Scrim click, cancel button, or Escape. |
| `onTip` | `(orderId, amount: number, message: string) => void` | — | Fired on 确认打赏. |
| `submitting` | `boolean` | `false` | Disables confirm and shows 打赏中…. |

## Usage

```tsx
const [tipOrder, setTipOrder] = useState<TipDialogOrder | null>(null)

<TipDialog
  open={!!tipOrder}
  order={tipOrder}
  onClose={() => setTipOrder(null)}
  onTip={(id, amount, message) => submitTip(id, amount, message)}
/>
```

## Notes

- Amount input carries `data-numeric="true"` for tabular numerals; `min` 0.01 / `step` 0.01 as in the source.
- Docks as a bottom sheet under `sm`, centred card above it; caps at `92vh` and scrolls.
- Focus moves to the amount field on open; Escape and scrim mousedown both dismiss.
