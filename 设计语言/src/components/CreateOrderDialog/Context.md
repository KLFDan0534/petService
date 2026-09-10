# CreateOrderDialog

The full booking dialog from the customer app (ported from `src/components/order/CreateOrderDialog.vue`). It walks the user through four steps — ① 宠物与看护人 ② 预约时间 ③ 宠物送达地址 ④ 联系信息与备注 — then emits the exact API payload the backend expects.

The Vue original called `@/api/*` directly. This port keeps **all** booking logic, validation order, estimate math and payload shapes 1:1, but receives lists/quotes as props and hands the finished payload to `onSubmit`, so it stays a pure design-system component.

## Modes (derived, not configured)

| Condition | Mode |
| --- | --- |
| `service` present, unit `day` / `booking_mode_wsh: 'date_range'` | **批量模式** — keeper first, then one row per pet (max `BATCH_MAX_PETS` = 10), each with delivery/pickup date + slot |
| `service` present, unit `session` / `hour` (`booking_mode_wsh: 'slot'`) | **单笔时段** — start slot only; `hour` also shows 服务数量（小时） |
| no `service` | **非服务模式** — merchant is selectable, times fall back to `datetime-local` |

Batch submits with a single pet reuse the single-order endpoint payload (identical to the legacy path); two or more pets produce `{ ...shared, items }`.

## Props

| Prop | Type | Notes |
| --- | --- | --- |
| `visible` | `boolean` | Required. Renders nothing when false; flipping to true resets the form. |
| `service` | `ServiceSummary \| null` | Presence enables availability + slot pickers and locks the merchant. |
| `pets` / `merchants` / `keepers` | arrays | Options for the selects. Merchants/keepers are expected to be pre-filtered to bookable ones. |
| `availabilityDays` | `AvailabilityDay[]` | `days_wsh` from the availability API; only `bookable_wsh` days can be submitted. |
| `availableCoupons` | `Coupon[]` | Best coupon is auto-selected until the user touches the select. |
| `couponQuote` / `membershipQuote` | quote objects | Applied only when their base amount matches the current estimate. |
| `maxBookingDays` | `number` | Authoritative booking window (inclusive); falls back to `AVAILABILITY_REQUEST_WINDOW_DAYS`. |
| `merchantLoading`, `keeperLoading`, `availabilityLoading`, `couponLoading`, `availabilityError`, `submitting` | `boolean` | Drive placeholders, hints and disabled states. |
| `initialMerchantId`, `initialKeeperId` | `string` | Pre-selection when opened from a service detail page. |
| `deliveryDistanceMeters`, `distanceLoading`, `distanceError` | distance chip | Shown once the address has coordinates. |
| `addressPicker` | `ReactNode` | Slot for the real Amap picker; a plain text field is used otherwise. |
| `onClose` | `() => void` | Backdrop mousedown and 取消. |
| `onSubmit` | `(payload) => void` | Receives `CreateOrderPayload` or `CreateOrdersBatchPayload`. |
| `onNotify` | `(message, 'warning' \| 'error') => void` | Every validation message the Vue version sent to the toast store. |
| `onMerchantChange`, `onKeeperChange`, `onCouponChange` | `(id: string) => void` | Hooks to refetch keepers / availability / quotes. |
| `onRetryDistance` | `() => void` | 重试定位 button. |

## Exported helpers

`normalizeUnit`, `dayEstimate`, `buildServiceDates`, `slotToDateTime`, `toApiDateTime`, `toDateOnly`, `diffDays`, `addDays`, `isSameDate`, `displaySlot`, `formatDistanceMeters`, `couponText`, `couponDiscountEstimate`, `pickBestCoupon`, `createPetRow`, `EMPTY_FORM`, `BATCH_MAX_PETS`, `AVAILABILITY_REQUEST_WINDOW_DAYS`.

## Usage

```tsx
<CreateOrderDialog
  visible={open}
  service={service}
  pets={pets}
  merchants={merchants}
  keepers={keepers}
  availabilityDays={availability.days_wsh}
  availableCoupons={coupons}
  couponQuote={couponQuote}
  membershipQuote={membershipQuote}
  maxBookingDays={availability.booking_window_days_wsh}
  keeperLoading={keeperLoading}
  submitting={submitting}
  onClose={() => setOpen(false)}
  onNotify={(message, level) => addToast(message, level)}
  onKeeperChange={keeperId => loadAvailability(keeperId)}
  onSubmit={async payload => {
    const res = 'items' in payload ? await createOrdersBatch(payload) : await createOrder(payload)
    if (res.code === 200) onCreated(res.data)
  }}
/>
```

## Notes

- Prices, quantities and step numbers carry `data-numeric="true"` for tabular numerals.
- The estimate band is solid `bg-cream` with a `border-brand/30` hairline (the source gradient is dropped per the brand rules); amounts, discount lines and the platform-subsidy note match the original.
- The default address input marks the source as unconfirmed, so submitting still requires 商家位置 or a real picker result — same guard as production.
