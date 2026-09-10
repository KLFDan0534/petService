# ReviewDialog

Modal dialog for submitting a three-dimension service review on a completed order: **商家 / 看护人 / 服务**. Ported from `src/components/order/ReviewDialog.vue` with its logic preserved 1:1 (dimension derivation, first-undone tab selection, form reset on open, submit guards).

Each dimension is reviewed separately: pick a tab, choose a score (5→1), optionally write a comment, submit. Dimensions already reviewed (`doneTypes`) or missing a target id are surfaced as disabled with a short note.

## Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `visible` | `boolean` | — | Controls visibility. Renders nothing when false. Opening resets score to 5, clears the comment, and selects the first un-reviewed dimension with a valid target. |
| `order` | `ReviewDialogOrder \| null` | — | The order being reviewed. Reads `id_wsh`, `merchant_id_wsh` / `merchant_name_wsh`, `keeper_id_wsh` / `keeper_name_wsh`, `service_id_wsh` / `service_name_wsh`. Falls back to `商家 #12`-style labels when a name is missing. |
| `doneTypes` | `ReviewDimensionType[]` | `[]` | Dimension types already reviewed. Marked 已评价 and not submittable. |
| `onClose` | `() => void` | — | Fired by the scrim, 取消, and Escape. |
| `onReviewed` | `(payload: ReviewSubmitPayload) => void \| Promise<unknown>` | — | Fired on submit. If it returns a promise the button stays in 提交中... until it settles. |

`ReviewSubmitPayload`: `{ orderId, targetType, targetId, score, content }`.
`ReviewDimensionType`: `'merchant' | 'keeper' | 'service'`.

## Usage

```tsx
const [reviewOrder, setReviewOrder] = useState<ReviewDialogOrder | null>(null)

<ReviewDialog
  visible={!!reviewOrder}
  order={reviewOrder}
  doneTypes={reviewedDims}
  onClose={() => setReviewOrder(null)}
  onReviewed={async payload => {
    await api.createReview(payload)
    refreshOrders()
  }}
/>
```

## Design notes

- Editorial Warm tokens only: `bg-surface`, `border-line`, `text-ink` / `text-ink-soft` / `text-muted`, `bg-brand` for the active tab and primary action.
- Entrance is `animate-fade` on the scrim plus `animate-pop` on the panel — no fly-in.
- Caps at `92vh` and docks as a bottom sheet below `sm`; `rounded-card` dialog on desktop.
- Score select carries `data-numeric="true"` for tabular numerals; long target names truncate via `ref-truncate`.
