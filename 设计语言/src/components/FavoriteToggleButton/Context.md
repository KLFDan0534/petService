# FavoriteToggleButton

Optimistic favourite toggle for services, merchants and keepers. Ported from
`src/components/common/FavoriteToggleButton.vue` + `useFavoriteState`: it owns its
own request handling (initial check, toggle, revert on failure) and exposes the
store side-effects (auth gate, toasts) as callbacks.

Geometry follows the icon-button standard: 40px tall, fully rounded, 20px star,
`muted` at rest and `caution` when favourited. `showText` adds the label inline.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| `targetId` | `number \| string` | — | Must resolve to a positive number |
| `targetType` | `string` | — | `merchant` \| `keeper` \| `service` |
| `showText` | `boolean` | `true` | Hide for card / media overlays |
| `favoritedLabel` | `string` | `'已收藏'` | Also the `aria-label` when active |
| `unfavoritedLabel` | `string` | `'收藏'` | Also the `aria-label` when inactive |
| `defaultFavorited` | `boolean` | `false` | Used until `onCheckFavorite` resolves |
| `onCheckFavorite` | `(id: number, type: string) => Promise<boolean> \| boolean` | — | Runs on mount and when the target changes; shows the loading spinner |
| `onToggle` | `(next: boolean, id: number, type: string) => Promise<unknown>` | — | Rejecting reverts the optimistic flip |
| `isAuthenticated` | `boolean` | `true` | When false, clicks call `onRequireLogin` |
| `onRequireLogin` | `() => void` | — | Open the login prompt / store the redirect path |
| `onNotify` | `(message: string, tone: 'success' \| 'error') => void` | — | Toast hook (`已收藏`, `已取消收藏`, `操作失败`, `不支持的收藏类型`) |
| `disabled` | `boolean` | `false` | Forces the disabled state |
| `className` | `string` | `''` | Extra classes |

## States

- **Loading / toggling** — spinner replaces the star, button disabled.
- **Disabled** — 50% opacity, no hover lift; also applied when the target is invalid.
- **Error** — state reverts and `onNotify('操作失败', 'error')` fires.

## Usage

```tsx
<FavoriteToggleButton
  targetId={service.id}
  targetType="service"
  showText={false}
  isAuthenticated={auth.isLoggedIn}
  onRequireLogin={() => openLoginPrompt(location.pathname)}
  onCheckFavorite={checkFavorite}
  onToggle={(next, id, type) => toggleFavorite(id, type)}
  onNotify={(message, tone) => toast[tone](message)}
/>
```

Clicks call `stopPropagation`, so the button is safe inside a linked card.

## Helpers

`FAVORITE_TARGET_TYPES`, `FAVORITE_TARGET_TYPE_LABELS`,
`normalizeFavoriteTargetType`, `isFavoriteTargetType`,
`getFavoriteTargetTypeLabel` are re-exported for favourite lists and filters.
