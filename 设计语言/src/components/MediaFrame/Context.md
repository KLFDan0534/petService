# MediaFrame

The single sanctioned way to present photography in Editorial Warm. Ported from the product's hero frame (`src/views/user/Services.vue` `.s-hero-frame`) plus the shared `MediaWithFallback` loader.

Photography is the decoration in this system — always frame it with `MediaFrame` rather than a bare `<img>`.

## Behaviour

- 4/3 aspect ratio by default, `rounded-frame` (26px), 1px `border-line`, `bg-surface`, `overflow-hidden`.
- Image is `object-cover` and scales to `1.06` over 300ms `ease-editorial` on hover of the frame **or** of a parent element with the `group` class (so cards can drive the zoom).
- `loading` renders the `ref-shimmer` skeleton — never a bare "Loading...".
- Empty or failing `src` renders the sand placeholder with a brand `ImageIcon` and optional label.
- Local MinIO URLs are rewritten to same-origin paths (`normalizeMediaUrl`), matching the product loader.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| `src` | `string` | `''` | Empty or failing sources show the fallback |
| `alt` | `string` | `''` | Pass `''` for decorative media |
| `ratio` | `'4/3' \| '1/1' \| '16/9' \| '3/2' \| '3/4' \| 'auto'` | `'4/3'` | `auto` lets content define height |
| `radius` | `'frame' \| 'card' \| 'control' \| 'tile' \| 'none'` | `'frame'` | Only system radii |
| `loading` | `boolean` | `false` | Shimmer skeleton |
| `imgLoading` | `'lazy' \| 'eager'` | `'lazy'` | Native image loading |
| `zoomOnHover` | `boolean` | `true` | 1.06 hover scale |
| `fallbackLabel` | `string` | – | Text under the placeholder icon |
| `children` | `ReactNode` | – | Overlay content (badges, captions) |
| `className` | `string` | `''` | Extra classes on the frame |

## Usage

```tsx
// Page hero
<MediaFrame src={heroImage} alt="照护服务" imgLoading="eager" />

// Loading state
<MediaFrame loading alt="照护服务" />

// Card media — zoom driven by the card's `group`
<article className="group rounded-card border border-line bg-surface overflow-hidden">
  <MediaFrame src={service.cover} alt={service.name} radius="none" />
  <div className="p-5">…</div>
</article>

// Avatar
<MediaFrame src={pet.avatar} alt={`${pet.name} 的照片`} ratio="1/1" radius="control" fallbackLabel="暂无照片" />
```

## Don't

- Don't add a resting shadow — only `shadow-lift` on hover, and only on the card, not the frame.
- Don't invent radii or ratios outside the props above.
- Don't render a blank box for missing images; always let the fallback show.
