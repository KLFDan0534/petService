# OrderCard

A single customer order row for the user-facing order list (`/orders`). Ported 1:1 from the product's `src/components/order/OrderCard.vue` (used by `src/views/user/Orders.vue`).

It renders, in one card:

1. A clickable summary button — service cover with pet-avatar badge, order no., status badge, service name, pet/date/merchant meta, and an amount column with billing text and discount.
2. An optional payment countdown band (15-minute window from `created_at_wsh`) for `pending` orders.
3. A four-phase service progress rail (已下单 → 已支付 → 寄养中 → 已完成), replaced by a "flow stopped" note for `cancelled` / `refunding` / `refunded`.
4. A status-driven action footer.

> **Scope:** this is currently a user-order-list component. Only `Orders.vue` consumes it in the product — keep it feature-local unless a second surface (merchant / keeper / CS / admin) adopts it.

## Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `order` | `OrderRecord` | — | **Required.** Raw order record with the API's `*_wsh` fields. |
| `nowMs` | `number` | `Date.now()` | Shared clock. Pass one ticking value from the list so all countdowns update together. |
| `processing` | `boolean` | `false` | Disables mutating actions (cancel, pay, deliver, tip, review) while a request is in flight. |
| `onCancel` | `(order) => void` | — | 取消订单 (pending). |
| `onPay` | `(method) => void` | — | 余额支付 (pending). Called with `'balance'`. |
| `onDeliver` | `(order) => void` | — | 确认已送达 (confirmed). |
| `onReview` | `(order) => void` | — | 评价服务 (completed, no feedback yet). |
| `onTip` | `(order) => void` | — | 打赏照护师 (completed). |
| `onViewDetail` | `(order) => void` | — | Card body click + 订单详情 / 查看服务动态. |
| `onRebook` | `(order) => void` | — | 再次预约. In the product this routes to `/services/:id?book=1`. |

## Status behaviour

| `status_wsh` | Badge | Progress | Actions |
| --- | --- | --- | --- |
| `pending` | 待付款 (brand fill) | phase 0 + countdown band | 取消订单 · 余额支付 |
| `paid` | 已支付 (outline) | phase 1 | note + 订单详情 |
| `confirmed` | 待送达 (outline) | phase 1 | 订单详情 · 确认已送达 |
| `delivered` / `received` / `in_progress` | 已送达 / 已接收 / 服务中 (green tint) | phase 2 | 联系门店 (tel) · 查看服务动态 |
| `completed` | 已完成 (quiet) | phase 3 | 再次预约 · 打赏照护师 · 评价服务 / 订单详情 |
| `cancelled` / `refunding` / `refunded` | 已取消 / 退款中 / 已退款 | flow-stopped note | 订单详情 · 再次预约 |

`completed` orders without `has_feedback_wsh` also show a 待反馈 tag next to the status badge.

## Usage

```tsx
import { OrderCard } from 'components/OrderCard'

<div className="grid gap-4">
  {orders.map((o) => (
    <OrderCard
      key={o.id_wsh}
      order={o}
      nowMs={nowMs}
      processing={processingOrderId === o.id_wsh}
      onCancel={handleCancel}
      onPay={(method) => handlePay(o, method)}
      onDeliver={handleDeliver}
      onReview={openReview}
      onTip={setTipOrder}
      onViewDetail={handleViewDetail}
      onRebook={(order) => navigate(`/services/${order.service_id_wsh}?book=1`)}
    />
  ))}
</div>
```

## Exported helpers

- `ORDER_PHASES` — the four progress phases with their `*_wsh` timestamp fields.
- `ORDER_PAYMENT_TIMEOUT_MS`, `getPaymentTimeoutRemaining(order, now)`, `formatPaymentTimeoutRemaining(ms)` — payment-window logic, mirroring `src/utils/orderPaymentTimeout.js`.
- `billingText(order)` — human-readable billing description ("4 天"、"1 次（60 分钟）"), mirroring `src/domain/BookingUnit.js`.

## Notes

- All colours come from the `--ref-*` tokens via Tailwind (`bg-surface`, `border-line`, `text-brand-deep`, …). No new palette.
- Amounts, order numbers, dates and countdowns carry `data-numeric="true"` for tabular numerals.
- The amount column collapses below 900px and reappears inline under the pet/date meta.
- Missing cover / pet avatar fall back to a warm placeholder and the pet name's first character; a broken image URL falls back too.
- Emoji icons from the Vue source (🕐 / ⛔ / ↗) are replaced with `lucide-react` equivalents per the Editorial Warm guidelines.
