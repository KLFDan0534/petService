# FilterToolbar

The catalogue filter row ported from `src/views/user/Services.vue` (`.s-toolbar` / `.s-search` / `.s-clear` / `.s-reset`). A wrapping flex row with a 12px gap and 28px top margin: a flexible 220–380px search field plus 44px-tall action controls. Below 560px it stacks full-width with `align-items: stretch` and the reset control centres itself.

Use it directly under a category rail or section heading, above a results heading — never inside a `Card`.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| `value` | `string` | — | Controlled search value. Omit for uncontrolled use. |
| `onValueChange` | `(value: string) => void` | — | Fires on keystrokes and on clear. |
| `defaultValue` | `string` | `''` | Initial value when uncontrolled. |
| `placeholder` | `string` | `'搜索服务名称或内容'` | |
| `searchLabel` | `string` | `'搜索服务'` | `aria-label` for the input. |
| `ariaLabel` | `string` | `'筛选预约服务'` | `aria-label` for the `role="search"` region. |
| `showReset` | `boolean` | `false` | Mirrors the source's `hasFilters` guard. |
| `resetLabel` | `string` | `'清除筛选'` | |
| `onReset` | `() => void` | — | Clears all active filters. |
| `children` | `ReactNode` | — | Extra 44px controls (sort selects, chips) placed after the field. |
| `className` | `string` | `''` | Appended to the row. |

## Behaviour

- The inline clear button (28px, `rounded-tile`) only appears while the query is non-empty.
- Input states match the source: hairline `border-line` at rest, ink-tinted border on hover/focus, and a 3px brand-tinted focus ring.
- Reset is a text-only control that shifts to `text-brand` on hover.

## Usage

```tsx
const [query, setQuery] = useState('')
const [category, setCategory] = useState('')
const hasFilters = query !== '' || category !== ''

<FilterToolbar
  value={query}
  onValueChange={setQuery}
  showReset={hasFilters}
  onReset={() => {
    setQuery('')
    setCategory('')
  }}
/>
```
