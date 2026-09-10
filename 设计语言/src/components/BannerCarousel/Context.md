# BannerCarousel

Auto-rotating promotional banner for the platform's dashboard and services pages. Ported from `src/components/dashboard/BannerCarousel.vue` (`.campaign-banner`).

Full-bleed photography inside a 26px (`rounded-frame`) media frame, with a legibility scrim, an editorial serif headline, prev/next arrows and dot controls. Rotation is 6s, pauses on hover and keyboard focus, and never starts under `prefers-reduced-motion: reduce`.

## When to use

- Platform-run promotions above a dashboard or services list.
- Only for editorial/marketing content. Never for records, settings, or data regions.

## Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `banners` | `Banner[]` | `[]` | Banner records. Entries without `id_wsh`, or with `status_wsh === 0`, are filtered out. Renders nothing when the result is empty. |
| `interval` | `number` | `6000` | Auto-rotate interval in ms. |
| `className` | `string` | `''` | Extra classes on the root `<section>`. |

### `Banner`

| Field | Type | Description |
| --- | --- | --- |
| `id_wsh` | `string \| number` | Stable key. Required. |
| `title_wsh` | `string?` | Headline. Falls back to `平台精选活动`. |
| `image_url_wsh` | `string?` | Banner image. Missing or failing images show a warm placeholder. |
| `link_url_wsh` | `string?` | Only `http:`/`https:` URLs are honoured (`toSafeUrl`); anything else drops the link and the call-to-action. |
| `status_wsh` | `number?` | `0` hides the banner. |

Also exported: `toSafeUrl(value)` and `normalizeMediaUrl(value)` (rewrites local MinIO URLs to relative paths), matching the product helpers.

## Behaviour

- Crossfade between slides (220ms, `ease-editorial`) via framer-motion.
- With a single slide, arrows and dots are hidden and no timer runs.
- Layout: `16/11` aspect with top-right arrows below `md`; `4/1` aspect with vertically centred side arrows above it.
- Linked banners open in a new tab with `noopener noreferrer` and expose a Chinese `aria-label`.

## Usage

```tsx
import { BannerCarousel } from 'components/BannerCarousel'

<BannerCarousel banners={banners} />
<BannerCarousel banners={banners} interval={8000} className="mb-section" />
```
