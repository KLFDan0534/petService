# MediaWithFallback

Site-wide image primitive with a built-in placeholder / error fallback. Ported from the product's `src/components/common/MediaWithFallback.vue`, which nearly every card, avatar and cover image uses.

It always fills its parent (`100% × 100%`, `object-fit: cover`) and clips overflow — so the **parent** owns aspect ratio and radius. Pair it with `MediaFrame` (or any sized, rounded wrapper) for framing.

## Behaviour

- Empty / whitespace `src` → placeholder.
- Image `onError` → placeholder.
- Changing `src` resets the error state and retries.
- Local MinIO URLs (`localhost` / `127.0.0.1` with a `/minio/` path) are rewritten to a relative path so they resolve through the frontend proxy. Exported as `normalizeMediaUrl`.
- The placeholder is exposed as `role="img"` with the `alt` text when `alt` is provided; otherwise it stays decorative.

## Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `src` | `string` | `''` | Image source. Empty or failing sources render the fallback. |
| `alt` | `string` | `''` | Accessible description; also labels the fallback. |
| `placeholder` | `string` | `''` | Optional caption inside the fallback (e.g. `暂无图片`). Omit for a bare icon. |
| `loading` | `'lazy' \| 'eager'` | `'lazy'` | Native image loading strategy. |
| `className` | `string` | `''` | Extra classes on the frame element. |

## Usage

```tsx
import { MediaWithFallback } from 'components/MediaWithFallback'

// Card cover — parent sets ratio + radius
<div className="h-48 w-full overflow-hidden rounded-card border border-line">
  <MediaWithFallback src={service.cover} alt={service.name} placeholder="暂无图片" />
</div>

// Avatar tile
<div className="h-16 w-16 overflow-hidden rounded-tile">
  <MediaWithFallback src={pet.avatar} alt={`${pet.name} 的照片`} />
</div>

// Above-the-fold hero image
<MediaWithFallback src={heroImage} alt="照护服务" loading="eager" />
```

## Notes

- Resting backdrop is `bg-sand`; the fallback is a 10% `--ref-brand` tint over `--ref-surface` with a `text-brand` icon and `text-muted` caption — no gradients, no shadow.
- The source's `--color-*` variables come from a global product stylesheet not present here, so those values are reproduced with this system's `--ref-*` tokens.
