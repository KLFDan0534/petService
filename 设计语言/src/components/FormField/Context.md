# FormField

Form row primitive: label → 44px control → hint/error. Consolidates the duplicated
`.auth-field / .auth-input / .auth-error` (Register, Login, ForgetPassword) and
`.ka-field / .ka-input / .ka-textarea / .ka-hint` (KeeperApply) scoped CSS.

Presentational only — it owns no state. Keep `value` / `onChange` and all validation
logic in the consuming view, and mirror the validation result into `error` (plus
`invalid` on the control).

## Exports

- `FormField` — the label + control + message wrapper.
- `FormInput` — 44px tall input (`h-11`), ported from `.auth-input` / `.ka-input`.
- `FormTextarea` — min 96px, vertically resizable, same border/focus treatment.

## Props

### FormField

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| `label` | `ReactNode` | — | Required. Rendered as a real `<label>`. |
| `htmlFor` | `string` | — | id of the control; always pass it for a11y. |
| `required` | `boolean` | `false` | Appends the terracotta `*` marker. |
| `hint` | `ReactNode` | — | Quiet helper copy; hidden while `error` is set. |
| `error` | `ReactNode` | — | Validation message in `brand-deep`, `role="alert"`. |
| `trailing` | `ReactNode` | — | Inline action right of the control (captcha row). |
| `wide` | `boolean` | `false` | `col-span-full` inside a form grid. |
| `muted` | `boolean` | `false` | Dims the field for derived / read-only values. |
| `className` | `string` | — | Extra classes on the wrapper. |

### FormInput / FormTextarea

All native `input` / `textarea` props, plus `invalid?: boolean` which paints the
error border and sets `aria-invalid`.

## Usage

```tsx
<FormField label="手机号" htmlFor="phone" required error={errors.phone}>
  <FormInput
    id="phone"
    type="tel"
    inputMode="numeric"
    maxLength={11}
    value={form.phone}
    invalid={Boolean(errors.phone)}
    onChange={(e) => setPhone(e.target.value.replace(/\D/g, ''))}
    placeholder="用于接收订单与照护提醒"
  />
</FormField>
```

Captcha row:

```tsx
<FormField
  label="短信验证码"
  htmlFor="captcha"
  required
  error={errors.captcha}
  hint={captchaHint}
  trailing={
    <button type="button" className="h-11 rounded-[10px] border border-line bg-surface px-4 text-[13px] font-medium text-ink-soft">
      获取验证码
    </button>
  }
>
  <FormInput id="captcha" inputMode="numeric" maxLength={6} placeholder="6 位数字" />
</FormField>
```

## Notes

- Focus uses the system-wide `shadow-focus` ring plus a `brand` border.
- Radius is the product's 10px control radius (`--r-btn`), not `rounded-control`.
- Place fields in a `grid gap-5` (single column) or `grid-cols-2 gap-5` parent; use
  `wide` for textareas and full-bleed rows.
