# Modal

The single dialog primitive for the product. Consolidates the `.dlg-*` family that was duplicated across the order and pet dialogs (`CreateOrderDialog`, `ReviewDialog`, `TipDialog`, `PetFormDialog`) plus the user views (`Pets`, `Addresses`, `Profile`, `Tickets`, `Complaints`): overlay → panel → head → body → foot → close.

Anatomy and styling are ported 1:1 from the source dialogs — warm scrim (`rgba(10,8,6,0.5)` + 2px blur), surface panel on a hairline border with the single sanctioned lift, serif 20px title, 24px gutters, right-aligned footer. Entrance is `animate-fade` (overlay) + `animate-pop` (panel), never a fly-in.

Behaviour handled once, here: Escape to dismiss, overlay press-to-dismiss (press must start *and* land on the overlay), `<body>` scroll lock with a shared counter so stacked dialogs restore correctly, focus moved into the panel on open and returned to the trigger on close, and `role="dialog" aria-modal="true"` labelled by the title.

Existing dialogs keep their own props and emit contracts — they only swap their markup/CSS for this primitive.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| `open` | `boolean` | — | Unmounted while closed. |
| `onClose` | `() => void` | — | Fired by ✕, Escape and the overlay. |
| `title` | `ReactNode` | — | Head title; also labels the dialog. |
| `description` | `ReactNode` | — | Supporting copy at the top of the body. |
| `children` | `ReactNode` | — | Body content. |
| `footer` | `ReactNode` | — | Actions, right aligned. Omit for no footer. |
| `size` | `'sm' \| 'md' \| 'wide' \| 'lg' \| 'xl'` | `'md'` | Max width 400 / 440 / 520 / 640 / 960px. |
| `hideClose` | `boolean` | `false` | Hide the ✕ control. |
| `closeOnOverlayClick` | `boolean` | `true` | |
| `closeOnEsc` | `boolean` | `true` | |
| `ariaLabel` | `string` | — | Accessible name when there is no visible `title`. |
| `className` | `string` | `''` | Extra panel classes. |

Size mapping from the source: `dlg-panel-sm` → `sm`, `dlg-panel` → `md`, `dlg-panel-wide` → `wide`, `dlg-panel-lg` → `lg`, `order-modal` → `xl`.

## Usage

```tsx
import { Modal } from 'components/Modal'

<Modal
  open={confirmOpen}
  onClose={() => setConfirmOpen(false)}
  title="删除这份宠物档案？"
  description="删除后无法再用于新的预约，此操作不可撤销。"
  size="sm"
  footer={
    <>
      <Button variant="secondary" onClick={() => setConfirmOpen(false)}>取消</Button>
      <Button variant="danger" onClick={handleDelete}>确认删除</Button>
    </>
  }
/>
```

Form dialog:

```tsx
<Modal open={open} onClose={close} title="添加宠物" size="wide" footer={<SaveActions />}>
  <FieldGroup>…</FieldGroup>
</Modal>
```

## Guidance

- Long bodies scroll inside the panel (`max-h: min(90vh, 720px)`); never let the page scroll behind a dialog.
- Put the primary action last in `footer`, cancel first.
- Use `size="sm"` for confirms, `wide` for single-column forms, `lg` for detail records, `xl` only for the multi-step create-order flow.
- Always pass `ariaLabel` when the dialog has no visible title.
