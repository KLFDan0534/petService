# IconButton

A quiet, icon-only affordance for inline actions: clearing a search field, dismissing a chip, opening a row menu. Ported from the search clear button in `src/views/user/Services.vue` (`.s-clear`): a 28px square with 8px radius, transparent at rest, muted ink that warms to `--ref-ink` over a `--ref-sand` wash on hover.

Use it for secondary, in-context actions only. Anything that is a primary or labelled action belongs in `Button`.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| `label` | `string` | — | Required accessible name (`aria-label` + `title`), since only an icon renders. |
| `children` | `ReactNode` | — | The icon element, normally a `lucide-react` icon. Sized automatically. |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | 24 / 28 / 32px square. `md` is the system default. |
| `tone` | `'default' \| 'brand' \| 'critical'` | `'default'` | Hover ink: ink, terracotta, or critical red. Background is always the sand wash. |
| `active` | `boolean` | `false` | Renders the hover treatment at rest, for toggled/selected affordances. |
| `disabled` | `boolean` | `false` | 40% opacity, no hover. |

All remaining `<button>` attributes (`onClick`, `aria-*`, `className`, …) pass through.

## Usage

```tsx
import { XIcon } from 'lucide-react'
import { IconButton } from 'components/IconButton'

<IconButton label="清除搜索" onClick={() => setQuery('')}>
  <XIcon />
</IconButton>
```

Docked inside a search input:

```tsx
<div className="relative">
  <input className="h-11 w-full rounded-control border border-line bg-surface pl-10 pr-10" />
  {query && (
    <span className="absolute right-2 top-1/2 -translate-y-1/2">
      <IconButton label="清除搜索" onClick={() => setQuery('')}>
        <XIcon />
      </IconButton>
    </span>
  )}
</div>
```

Destructive row action:

```tsx
<IconButton label="删除订单" tone="critical" onClick={remove}>
  <Trash2Icon />
</IconButton>
```

## Notes

- Colour transitions run at 150ms with `ease-editorial`; there is no scale or shadow on hover.
- Focus uses the system-wide `:focus-visible` ring from `index.css` — do not add a custom ring.
- Never place an emoji inside it; use `lucide-react`.
