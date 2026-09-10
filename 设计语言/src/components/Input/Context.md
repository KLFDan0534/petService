# Input

The single text field for the Editorial Warm system, ported 1:1 from the services toolbar search field (`src/views/user/Services.vue`, `.s-search input`).

- 44px tall (`h-11`), `rounded-control` (11px), 1px `border-line` hairline on `bg-surface`
- `text-control` (13.5px) ink text, `text-muted` placeholder
- Hover: border `color-mix(in srgb, var(--ref-ink) 25%, transparent)`
- Focus: border `35%` ink, `outline: none`, ring `0 0 0 3px color-mix(in srgb, var(--ref-brand) 12%, transparent)`
- Leading icon sits 14px from the left; trailing clear button sits 8px from the right (28px, `rounded-tile`, hover `bg-sand`)

Transitions are 150ms `ease-editorial` on border-color and box-shadow only — no transforms, no shadow at rest.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| `leadingIcon` | `ReactNode` | – | Rendered at 14px left, muted, 15px box. Adds `pl-10`. |
| `clearable` | `boolean` | `false` | Shows the trailing clear button when the field has a value. Adds `pr-10`. |
| `onClear` | `() => void` | – | Fired after the value is cleared and focus returns to the input. |
| `invalid` | `boolean` | `false` | Critical border + critical focus ring, sets `aria-invalid`. |
| `clearLabel` | `string` | `'Clear'` | Accessible label for the clear button. |
| `containerClassName` | `string` | – | Wrapper class — use it for width (`max-w-[380px]`, `w-full`). |
| `className` | `string` | – | Merged onto the `<input>`. |
| …rest | `InputHTMLAttributes` | – | `type`, `value`, `onChange`, `placeholder`, `disabled`, etc. |

Works controlled or uncontrolled; clearing dispatches a native `input` event so controlled consumers receive the empty value.

`SearchInput` is a preset: `type="search"`, `autoComplete="off"`, `clearable`, and a `SearchIcon` leading slot.

## Usage

```tsx
import { Input, SearchInput } from 'components/Input'

// Services toolbar
<SearchInput
  containerClassName="max-w-[380px]"
  placeholder="搜索服务名称或内容"
  aria-label="搜索服务"
  value={query}
  onChange={(e) => setQuery(e.target.value)}
/>

// Form field
<Input
  type="email"
  leadingIcon={<MailIcon />}
  placeholder="you@example.com"
  invalid={!!error}
/>
```

Pair with `Field` for labels, help text and error copy. Set `data-numeric="true"` when the field holds prices, ids or counts.
