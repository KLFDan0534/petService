# Logo

The product brand lockup: the raster brand mark (`/logo.png`, a rounded illustration of a dog and cat inside a house) beside the `宠物寄养平台` wordmark. Ported from the product's `UserLayout` header and footer lockups.

The mark is a **raster asset used as-is** — do not vectorise, recolour, or resize the underlying `public/logo.png`. The component only frames it (48px, 12px radius, `object-fit: cover`).

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| `src` | `string` | `'/logo.png'` | Path to the raster mark. Keep the default unless serving the asset from another host. |
| `alt` | `string` | `'宠物寄养平台'` | Accessible name. Applied to the mark only when the wordmark is hidden; otherwise the mark is decorative. |
| `wordmark` | `string` | `'宠物寄养平台'` | Text beside the mark. |
| `showWordmark` | `boolean` | `true` | Set `false` for the mark alone. |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | 32 / 48 / 56px mark. `md` matches the product header and footer. |
| `href` | `string` | – | Renders as a link (hover shifts the lockup to `text-brand`). |
| `onClick` | `() => void` | – | With `href`, fires alongside navigation (e.g. closing a mobile menu); without it, renders a button. |
| `className` | `string` | `''` | Extra classes on the lockup wrapper. |

## Usage

```tsx
// Header
<Logo href="/dashboard" onClick={closeMobileMenu} />

// Footer (same lockup, non-interactive)
<Logo />

// Compact / mobile bar
<Logo showWordmark={false} size="sm" />
```

## Behaviour

- Wordmark uses `font-body` at 600 weight with `whitespace-nowrap` + `ref-truncate`, so a longer brand name never breaks the header grid.
- If the raster mark fails to load, it falls back to a `lucide-react` paw mark on `bg-sand` in `text-brand` — never a broken image.
- Colour transitions run at 150ms on `ease-editorial`. No shadow, no gradient.
