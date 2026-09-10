# PetFormDialog

Create / edit dialog for a pet profile. Ported 1:1 from the product's
`src/components/pet/PetFormDialog.vue` — same field order, same labels, same
payload shape (`*_wsh` keys), same avatar-upload behaviour. Styling that relied
on the product's global `app.css` (`.modal-overlay`, `.modal`, `.btn*`,
`--color-*`) is reproduced with Editorial Warm tokens (`surface`, `line`, `ink`,
`muted`, `sand`, `brand`, `critical`, `rounded-control`, `shadow-lift`,
`animate-pop`).

Controlled component: the parent owns `visible`, `saving` and `serverError`, and
performs the actual persistence in `onSave`.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| `visible` | `boolean` | `false` | Renders nothing when false; form resets on close. |
| `pet` | `Pet \| null` | `null` | `null` → 添加宠物 (create). A record → 编辑宠物 (edit), form hydrated from it. |
| `saving` | `boolean` | `false` | Disables footer actions, primary label becomes `保存中...`. |
| `serverError` | `string` | `''` | Shown in the error banner (`role="alert"`). |
| `onSave` | `(payload: PetFormPayload) => void` | — | Fired on submit after local validation. |
| `onClose` | `() => void` | — | Scrim click, close button, Escape. |
| `onUploadAvatar` | `(file: File) => Promise<string>` | — | Should resolve with the uploaded avatar URL. When omitted the file is previewed locally as a data URL. |
| `onToast` | `(message, type) => void` | — | Upload failures route here; without it they fall back to the error banner. |

## Behaviour

- **Validation preserved**: `宠物名称` is `required`; submitting it empty shows
  `请输入宠物名称` and blocks `onSave`. Text fields trim on blur.
- **Payload normalisation** (`toPayload`): empty strings/undefined → `null`,
  `gender_wsh` coerced to a number, `sterilized_wsh` / `vaccinated_wsh` coerced
  to `0 | 1`. Type is lower-cased on hydrate.
- **Avatar**: 64px round preview with paw-print placeholder, spinner overlay
  while uploading, `移除` action, broken-image fallback.
- **States**: loading (saving + upload spinner), error banner, disabled
  controls, visible focus ring on every control.
- **Responsive**: two-column rows collapse to one below `sm`; the dialog docks
  as a bottom sheet on mobile and caps at `92vh`.

## Usage

```tsx
import { PetFormDialog, type PetFormPayload } from 'components/PetFormDialog'

<PetFormDialog
  visible={open}
  pet={editing}
  saving={saving}
  serverError={error}
  onClose={() => setOpen(false)}
  onSave={async (payload: PetFormPayload) => {
    setSaving(true)
    try {
      await savePet(payload)
      setOpen(false)
    } catch (e) {
      setError('保存失败，请稍后重试')
    } finally {
      setSaving(false)
    }
  }}
  onUploadAvatar={async file => {
    const body = new FormData()
    body.append('file', file)
    const r = await apiPost('/api/files/upload?directory=pets', body)
    return r.data.url_wsh
  }}
/>
```

## Exports

`PetFormDialog`, `petTypeOptions`, `createEmptyPetForm`, `toPayload`, and the
types `Pet`, `PetFormValues`, `PetFormPayload`, `PetFormDialogProps`,
`PetGenderValue`, `PetFlagValue`.
