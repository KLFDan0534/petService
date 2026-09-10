# Breadcrumb

Quiet editorial trail that sits above a `PageHeader` to show where a route lives. Ported from the product's `.s-crumb` pattern in `src/views/user/Services.vue` (also `Merchants.vue`, `Membership.vue`).

Visual contract (do not change): 12.5px text, 8px gap, 6px vertical padding, links `--ref-muted` → `--ref-ink` on hover, separators `--ref-line`, current page `--ref-ink-soft`.

## When to use

- Merchant, admin and profile sub-routes that are more than one level deep.
- Never on the top-level home / dashboard route.
- Only one Breadcrumb per page, directly above the page title.

## Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `items` | `BreadcrumbItem[]` | — | Ordered trail, root first. The last item always renders as the current page (`aria-current="page"`), even if it has an `href`. |
| `separator` | `ReactNode` | `'›'` | Glyph between crumbs, rendered `aria-hidden`. |
| `ariaLabel` | `string` | `'面包屑'` | Accessible name for the `nav` landmark. |
| `renderLink` | `(item, className) => ReactNode` | — | Optional renderer so a router `Link` can be used instead of `<a>`. Apply the passed `className`. |
| `className` | `string` | `''` | Extra classes on the `nav`. |

`BreadcrumbItem` is `{ label: string; href?: string }`.

## Usage

```tsx
<Breadcrumb
  items={[
    { label: '首页', href: '/dashboard' },
    { label: '全部服务' }
  ]}
/>
```

With a router link:

```tsx
import { Link } from 'react-router-dom'

<Breadcrumb
  items={[
    { label: '首页', href: '/dashboard' },
    { label: '门店', href: '/merchants' },
    { label: '毛孩子的家' }
  ]}
  renderLink={(item, className) => (
    <Link to={item.href!} className={className}>
      {item.label}
    </Link>
  )}
/>
```

## Notes

- Labels use `ref-truncate`, so long service names and store names shrink rather than wrap.
- Links carry the global `:focus-visible` ring; hover/focus transitions run at 150ms with `ease-editorial`.
- Renders nothing when `items` is empty.
