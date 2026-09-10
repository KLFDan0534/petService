# ServiceGrid

Responsive catalog grid of pet-service cards. Ported from `src/components/dashboard/ServiceGrid.vue`, with the Editorial Warm appearance that `src/views/user/Services.vue` (514-613) previously forced on via `:deep()` overrides now baked into the component — so Dashboard and Services render identically without page-level overrides.

Handles all three data states itself: loading skeletons, populated grid, empty state.

## Anatomy

- **Card** — `rounded-card` (18px) hairline `border-line` on `bg-surface`, no shadow at rest; hover lifts `-4px` with `shadow-lift` and a darker border.
- **Media** — 4/3 button on `bg-sand`, image scales to `1.06` on hover, `ImageIcon` + service name fallback on missing/broken images.
- **Body** — 20px padding; 11px/700 uppercase brand category, 20px `font-display` heading, merchant line with a 12%-brand distance chip, right-aligned 22px `font-display` price with unit.
- **Action bar** — hairline top border, text "查看详情" link with nudging arrow, optional favorite toggle, and an 11px-radius brand primary "立即预约" button. Stacks full-width below `sm`.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| `services` | `ServiceGridItem[]` | `[]` | Empty → empty state. |
| `loading` | `boolean` | `false` | Renders 6 skeleton cards. |
| `isLoggedIn` | `boolean` | `false` | Shows the favorite toggle (source: `authStore.isLoggedIn`). |
| `favoriteIds` | `(string \| number)[]` | `[]` | Ids rendered as favorited. |
| `onSelect` | `(id) => void` | – | Media button + 查看详情 (source pushed `/services/:id`). |
| `onBook` | `(service) => void` | – | 立即预约 (source pushed `/services/:id?book=1`). |
| `onToggleFavorite` | `(id) => void` | – | Favorite toggle. |
| `emptyTitle` / `emptyDescription` | `string` | 没有找到匹配的服务 / 调整关键词或服务分类后再试试。 | Empty-state copy. |
| `className` | `string` | `''` | Extra classes on the root. |

`ServiceGridItem`: `id_wsh`, `name_wsh`, `firstImage?`, `category_name_wsh?`, `merchant_name_wsh?`, `distance_m_wsh?`, `price_wsh?`, `unit_wsh?`, `description_wsh?`.

Also exports the source helpers `formatServiceDistance`, `formatMoney`, and `unitLabel`.

## Usage

```tsx
<ServiceGrid
  services={filteredServices}
  loading={loading}
  isLoggedIn={auth.isLoggedIn}
  favoriteIds={favorites}
  onSelect={id => navigate(`/services/${id}`)}
  onBook={service => navigate(`/services/${service.id_wsh}?book=1`)}
  onToggleFavorite={toggleFavorite}
/>
```

## Notes

- Columns: 1 → 2 (`sm`) → 3 (`lg`), 18px gap on mobile and 24px (`gap-gutter`) above.
- Prices and distances carry `data-numeric="true"` for tabular numerals.
- Long merchant names truncate; descriptions clamp to 3 lines with a 72px min height so action bars stay aligned.
- Motion collapses under `prefers-reduced-motion` via the global guard in `index.css`.
