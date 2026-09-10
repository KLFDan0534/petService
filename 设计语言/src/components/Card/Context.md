# Card

The browsable-item container, ported from the service card in `src/views/user/Services.vue` (the `.service-card` override of the shared `ServiceGrid`).

`bg-surface` on an 18px radius (`rounded-card`) with a 1px `border-line` hairline and **no shadow at rest**. On hover the border shifts to 16% ink, the card lifts `-4px` and the single sanctioned elevation (`shadow-lift`) appears — 200ms `ease-editorial`. Media inside scales to `1.06` over 300ms.

Use `Card` only for **browsable items** (services, merchants, keepers, pets). Records, settings and details belong in sections, description lists or `DataTable`.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| `media` | `ReactNode` | — | Flush top media slot on a `bg-sand` 4:3 frame. Images are cover-fit and scale on hover. |
| `onMediaClick` | `() => void` | — | Makes the media region a button. Pair with `mediaLabel`. |
| `mediaLabel` | `string` | — | Accessible name for the media button. |
| `eyebrow` | `string` | — | 11px uppercase brand label above the title. |
| `title` | `ReactNode` | — | Serif `h3`, weight 500, `-0.01em` tracking. |
| `meta` | `ReactNode` | — | Muted secondary line (merchant, distance, date). Truncates. |
| `trailing` | `ReactNode` | — | Right-aligned header content — price, rating. Rendered with tabular numerals. |
| `header` | `ReactNode` | — | Fully custom header; replaces eyebrow / title / meta / trailing. |
| `footer` | `ReactNode` | — | Footer slot divided by a hairline top border; stacks below `sm`. |
| `interactive` | `boolean` | `true` | Set `false` for static panels (no lift, no hover shadow). |
| `className` | `string` | `''` | Extra classes on the root `article`. |
| `children` | `ReactNode` | — | Body copy at 13.5px / 1.7 in `text-ink-soft`. |

## Usage

```tsx
<Card
  media={<img src={service.image} alt={`${service.name}服务图片`} />}
  mediaLabel={`查看${service.name}详情`}
  onMediaClick={() => navigate(`/services/${service.id}`)}
  eyebrow={service.categoryName}
  title={service.name}
  meta={service.merchantName}
  trailing={
    <>
      <strong className="block">¥{service.price}</strong>
      <span className="block text-meta font-body font-normal text-muted">/ 次</span>
    </>
  }
  footer={<Button variant="primary" size="sm">立即预约</Button>}
>
  <p className="ref-clamp-3">{service.description}</p>
</Card>
```

Static panel:

```tsx
<Card interactive={false} title="本月概览">…</Card>
```

## Notes

- Long titles and meta values must not break the grid — use `ref-truncate`, `ref-clamp-2`, `ref-clamp-3` on body content.
- Never add a second shadow, a gradient, or a rest-state elevation.
- Loading grids use `SkeletonCard`, empty grids use `EmptyState` — do not fake either with an empty `Card`.
