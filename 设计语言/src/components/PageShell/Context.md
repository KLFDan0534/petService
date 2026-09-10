# PageShell

The single page container for every route. Ported from `src/views/user/Services.vue`
(`.svc-page` + `.svc-shell`), a pattern duplicated as a per-view `*-shell` class in
nearly every refined view — use this component instead of re-declaring it.

Geometry (1:1 with the source):

- outer page region: `width: 100%`, `padding: 6px 0 72px`
- inner shell: `max-width: 1180px`, `margin: 0 auto`, `padding: 0 24px`
- below 560px the gutter collapses to `16px`

## Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `width` | `'narrow' \| 'default' \| 'wide'` | `'default'` | `default` = 1180px product shell, `narrow` = 1080px record shell (order detail), `wide` = 1440px admin data workspace. |
| `as` | `'main' \| 'div' \| 'section' \| 'article'` | `'main'` | Element for the outer page region. |
| `padded` | `boolean` | `true` | Vertical page padding (`6px 0 72px`). Set `false` when nesting. |
| `shellClassName` | `string` | – | Extra classes on the inner max-width shell. |
| `className` | `string` | – | Extra classes on the outer page region. |
| `children` | `ReactNode` | – | Page content. |

All other `HTMLAttributes` (e.g. `id`, `aria-*`) pass through to the outer element.

## Usage

```tsx
import { PageShell } from 'components/PageShell'

export function ServicesPage() {
  return (
    <PageShell>
      <PageHeader eyebrow="Services" title="为你的爱宠，找到合适的照护服务" />
      <section className="mt-section">{/* catalogue */}</section>
    </PageShell>
  )
}
```

Admin / merchant data workspace:

```tsx
<PageShell width="wide">
  <DataTable {...props} />
</PageShell>
```

## Guidance

- Exactly one `PageShell` per route; never nest a padded shell inside another.
- Section rhythm inside the shell is 44 / 56 / 80px (`mt-section` = 56px).
- Do not add a background or shadow here — the page background comes from `bg-canvas` on `body`.
