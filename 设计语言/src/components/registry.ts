// AUTO-GENERATED — do not edit manually.
// Re-run the design system builder to regenerate.

import type { ComponentPreviewModule } from './previewTypes';
import __AmapAddressPickerPreviews from './AmapAddressPicker/AmapAddressPicker.previews';
import __BannerCarouselPreviews from './BannerCarousel/BannerCarousel.previews';
import __BreadcrumbPreviews from './Breadcrumb/Breadcrumb.previews';
import __ButtonPreviews from './Button/Button.previews';
import __CardPreviews from './Card/Card.previews';
import __ChipPreviews from './Chip/Chip.previews';
import __CreateOrderDialogPreviews from './CreateOrderDialog/CreateOrderDialog.previews';
import __DataTablePreviews from './DataTable/DataTable.previews';
import __DetailListPreviews from './DetailList/DetailList.previews';
import __EmptyStatePreviews from './EmptyState/EmptyState.previews';
import __ErrorStatePreviews from './ErrorState/ErrorState.previews';
import __EyebrowPreviews from './Eyebrow/Eyebrow.previews';
import __FavoriteToggleButtonPreviews from './FavoriteToggleButton/FavoriteToggleButton.previews';
import __FilterToolbarPreviews from './FilterToolbar/FilterToolbar.previews';
import __FormFieldPreviews from './FormField/FormField.previews';
import __IconButtonPreviews from './IconButton/IconButton.previews';
import __InputPreviews from './Input/Input.previews';
import __LoadingSpinnerPreviews from './LoadingSpinner/LoadingSpinner.previews';
import __LoginPromptDialogPreviews from './LoginPromptDialog/LoginPromptDialog.previews';
import __LogoPreviews from './Logo/Logo.previews';
import __MediaFramePreviews from './MediaFrame/MediaFrame.previews';
import __MediaWithFallbackPreviews from './MediaWithFallback/MediaWithFallback.previews';
import __MessageIndicatorPreviews from './MessageIndicator/MessageIndicator.previews';
import __ModalPreviews from './Modal/Modal.previews';
import __OrderCardPreviews from './OrderCard/OrderCard.previews';
import __PageHeaderPreviews from './PageHeader/PageHeader.previews';
import __PageShellPreviews from './PageShell/PageShell.previews';
import __PetFormDialogPreviews from './PetFormDialog/PetFormDialog.previews';
import __PetListPreviews from './PetList/PetList.previews';
import __PopupNoticePreviews from './PopupNotice/PopupNotice.previews';
import __RevealPreviews from './Reveal/Reveal.previews';
import __ReviewDialogPreviews from './ReviewDialog/ReviewDialog.previews';
import __SectionBandPreviews from './SectionBand/SectionBand.previews';
import __SectionHeadingPreviews from './SectionHeading/SectionHeading.previews';
import __ServiceGridPreviews from './ServiceGrid/ServiceGrid.previews';
import __SkeletonPreviews from './Skeleton/Skeleton.previews';
import __StatListPreviews from './StatList/StatList.previews';
import __StatusBadgePreviews from './StatusBadge/StatusBadge.previews';
import __TextLinkPreviews from './TextLink/TextLink.previews';
import __ThemeTogglePreviews from './ThemeToggle/ThemeToggle.previews';
import __TimelinePreviews from './Timeline/Timeline.previews';
import __TipDialogPreviews from './TipDialog/TipDialog.previews';
const __contextMd: Record<string, string> = {
  "AmapAddressPicker": `# AmapAddressPicker

Amap (高德地图) address field: debounced keyword search with a suggestion panel, one-tap browser geolocation, and a full map-picking dialog with click / marker-drag reverse geocoding.

Ported from the product's \`src/components/common/AmapAddressPicker.vue\` (structure, behaviour, copy and result shapes kept 1:1). Its global \`.form-control\` / \`.btn\` / \`--color-*\` styling doesn't exist in this artifact, so that chrome is expressed with Editorial Warm tokens (\`bg-surface\`, \`border-line\`, \`rounded-control\`, \`text-brand\`, \`shadow-lift\`) matching the warm override the product applies in \`views/user/Addresses.vue\`.

## Usage

\`\`\`tsx
import { AmapAddressPicker } from 'components/AmapAddressPicker'
import type { AmapAddressSelection } from 'components/AmapAddressPicker'

const [selection, setSelection] = useState<AmapAddressSelection>({
  address: '', latitude: null, longitude: null, source: '',
})

<AmapAddressPicker
  value={selection.address}
  latitude={selection.latitude}
  longitude={selection.longitude}
  source={selection.source}
  onChange={setSelection}
  amapConfig={{ apiKey: GAO_MAP_API_KEY, securityCode: GAO_MAP_SECURITY_CODE }}
  onNotify={(message, level) => toast(message, level)}
/>
\`\`\`

Omit \`value\` to run the component uncontrolled (it keeps the selection internally and still calls \`onChange\`).

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| \`value\` | \`string\` | – | Address text. Omit for uncontrolled mode. |
| \`latitude\` / \`longitude\` | \`number \\| string \\| null\` | \`null\` | Current coordinate; cleared whenever the user types. |
| \`source\` | \`string\` | \`''\` | \`merchant\` \\| \`amap_geolocation\` \\| \`amap_map\` \\| \`amap_tip\` … drives the confirmation caption. |
| \`placeholder\` | \`string\` | \`'搜索地址或打开地图选址'\` | |
| \`showMapButton\` / \`mapButtonText\` | \`boolean\` / \`string\` | \`true\` / \`'地图选址'\` | Opens the map dialog. |
| \`showLocateButton\` / \`locateButtonText\` | \`boolean\` / \`string\` | \`true\` / \`'定位'\` | Browser geolocation. |
| \`disabled\` | \`boolean\` | \`false\` | Locks the field and both actions. |
| \`amapConfig\` | \`{ apiKey: string; securityCode?: string }\` | – | Required for live search / map. In the product this comes from \`/api/geo/config\`. |
| \`searchAddress\` | \`(keyword, options) => Promise<AmapPlace[]>\` | built-in | Override for mocks or a server proxy. |
| \`getCurrentAddress\` | \`() => Promise<AmapPlace>\` | built-in | Override geolocation. |
| \`reverseGeocodeDetail\` | \`(lng, lat, options) => Promise<AmapPlace>\` | built-in | Used by map click / marker drag. |
| \`loadAmap\` | \`() => Promise<AmapNamespace>\` | built-in | Override the JS SDK loader. |
| \`onChange\` | \`(selection: AmapAddressSelection) => void\` | – | Replaces the Vue \`update:modelValue / latitude / longitude / source\` emits. |
| \`onSelected\` | \`(place: AmapPlace) => void\` | – | Fires with the full Amap record on every confirmed pick. |
| \`onNotify\` | \`(message: string, level: 'error' \\| 'warning') => void\` | – | Replaces \`appStore.addToast\`. |
| \`className\` | \`string\` | \`''\` | Extra classes on the wrapper. |

## Behaviour

- Typing clears the coordinate and re-searches after 500ms; suggestions need at least 2 characters.
- Suggestions show name, full address, and a distance chip (\`match_label_wsh\`); picking one emits address + coordinate + \`location_source_wsh\`.
- Map dialog: keyword search (debounced, Enter to search immediately), result list, click or drag the marker to reverse geocode, and 确认选择 is enabled only once an address is resolved. Escape or the scrim closes it.
- States covered: loading (\`地图加载中...\`, \`搜索中\`, \`定位中\`), empty (\`未找到匹配地点\`), error (map load failure inside the map area plus \`onNotify\`), and reverse-geocode failure (\`当前选中\` falls back to the error text).
- Below \`md\` the field stacks above its buttons, the result list collapses above the map, and the footer becomes a column.

## Helpers

\`./amapLocation\` exports the ported Amap layer — \`loadAmap\`, \`searchAmapAddress\`, \`reverseGeocodeDetail\`, \`getCurrentAddress\` — plus the \`AmapPlace\` / \`AmapPoint\` / \`AmapConfig\` types. REST calls use \`fetch\` (no axios) and take the key from \`amapConfig\` instead of the product's \`/api/geo/config\` endpoint.
`,
  "BannerCarousel": `# BannerCarousel

Auto-rotating promotional banner for the platform's dashboard and services pages. Ported from \`src/components/dashboard/BannerCarousel.vue\` (\`.campaign-banner\`).

Full-bleed photography inside a 26px (\`rounded-frame\`) media frame, with a legibility scrim, an editorial serif headline, prev/next arrows and dot controls. Rotation is 6s, pauses on hover and keyboard focus, and never starts under \`prefers-reduced-motion: reduce\`.

## When to use

- Platform-run promotions above a dashboard or services list.
- Only for editorial/marketing content. Never for records, settings, or data regions.

## Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| \`banners\` | \`Banner[]\` | \`[]\` | Banner records. Entries without \`id_wsh\`, or with \`status_wsh === 0\`, are filtered out. Renders nothing when the result is empty. |
| \`interval\` | \`number\` | \`6000\` | Auto-rotate interval in ms. |
| \`className\` | \`string\` | \`''\` | Extra classes on the root \`<section>\`. |

### \`Banner\`

| Field | Type | Description |
| --- | --- | --- |
| \`id_wsh\` | \`string \\| number\` | Stable key. Required. |
| \`title_wsh\` | \`string?\` | Headline. Falls back to \`平台精选活动\`. |
| \`image_url_wsh\` | \`string?\` | Banner image. Missing or failing images show a warm placeholder. |
| \`link_url_wsh\` | \`string?\` | Only \`http:\`/\`https:\` URLs are honoured (\`toSafeUrl\`); anything else drops the link and the call-to-action. |
| \`status_wsh\` | \`number?\` | \`0\` hides the banner. |

Also exported: \`toSafeUrl(value)\` and \`normalizeMediaUrl(value)\` (rewrites local MinIO URLs to relative paths), matching the product helpers.

## Behaviour

- Crossfade between slides (220ms, \`ease-editorial\`) via framer-motion.
- With a single slide, arrows and dots are hidden and no timer runs.
- Layout: \`16/11\` aspect with top-right arrows below \`md\`; \`4/1\` aspect with vertically centred side arrows above it.
- Linked banners open in a new tab with \`noopener noreferrer\` and expose a Chinese \`aria-label\`.

## Usage

\`\`\`tsx
import { BannerCarousel } from 'components/BannerCarousel'

<BannerCarousel banners={banners} />
<BannerCarousel banners={banners} interval={8000} className="mb-section" />
\`\`\`
`,
  "Breadcrumb": `# Breadcrumb

Quiet editorial trail that sits above a \`PageHeader\` to show where a route lives. Ported from the product's \`.s-crumb\` pattern in \`src/views/user/Services.vue\` (also \`Merchants.vue\`, \`Membership.vue\`).

Visual contract (do not change): 12.5px text, 8px gap, 6px vertical padding, links \`--ref-muted\` → \`--ref-ink\` on hover, separators \`--ref-line\`, current page \`--ref-ink-soft\`.

## When to use

- Merchant, admin and profile sub-routes that are more than one level deep.
- Never on the top-level home / dashboard route.
- Only one Breadcrumb per page, directly above the page title.

## Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| \`items\` | \`BreadcrumbItem[]\` | — | Ordered trail, root first. The last item always renders as the current page (\`aria-current="page"\`), even if it has an \`href\`. |
| \`separator\` | \`ReactNode\` | \`'›'\` | Glyph between crumbs, rendered \`aria-hidden\`. |
| \`ariaLabel\` | \`string\` | \`'面包屑'\` | Accessible name for the \`nav\` landmark. |
| \`renderLink\` | \`(item, className) => ReactNode\` | — | Optional renderer so a router \`Link\` can be used instead of \`<a>\`. Apply the passed \`className\`. |
| \`className\` | \`string\` | \`''\` | Extra classes on the \`nav\`. |

\`BreadcrumbItem\` is \`{ label: string; href?: string }\`.

## Usage

\`\`\`tsx
<Breadcrumb
  items={[
    { label: '首页', href: '/dashboard' },
    { label: '全部服务' }
  ]}
/>
\`\`\`

With a router link:

\`\`\`tsx
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
\`\`\`

## Notes

- Labels use \`ref-truncate\`, so long service names and store names shrink rather than wrap.
- Links carry the global \`:focus-visible\` ring; hover/focus transitions run at 150ms with \`ease-editorial\`.
- Renders nothing when \`items\` is empty.
`,
  "Button": `# Button

The consolidated \`.cta\` family that was duplicated across ~30 product views. This is the only button primitive — do not re-declare local \`.cta\` / \`.btn\` rules in a screen.

Ported from \`src/views/user/CustomerServiceApply.vue:479\`, \`KeeperWorkflow.vue:631-639\` and \`Login.vue:277-289\`. The per-view \`--r-btn: 10px\` alias is normalised to the canonical **11px** (\`rounded-control\`).

## Anatomy

- 42px height, \`0 18px\` padding, 11px radius, 13px / weight 500 (\`md\`, canonical)
- \`inline-flex\` with an 8px gap, 1px transparent border so outline/danger don't shift
- Hover: \`translateY(-1px)\` plus the variant's colour change, 150ms \`ease-editorial\`
- Disabled: \`opacity: .5\`, \`cursor: not-allowed\`, transform cancelled
- Focus-visible: the system focus ring (\`shadow-focus\`)
- 42px is deliberately distinct from the 44px chip/input scale

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| \`variant\` | \`'primary' \\| 'outline' \\| 'dark' \\| 'light' \\| 'ghost' \\| 'quiet' \\| 'danger'\` | \`'primary'\` | See table below |
| \`size\` | \`'sm' \\| 'md' \\| 'lg'\` | \`'md'\` | 34px / 42px / 48px |
| \`arrow\` | \`boolean\` | \`false\` | Trailing arrow that nudges 3px on hover (\`.cta-arrow\`) |
| \`icon\` | \`ReactNode\` | — | Leading slot; pass a \`lucide-react\` icon at 15px |
| \`loading\` | \`boolean\` | \`false\` | Spinner in the leading slot, control disabled, \`aria-busy\` |
| \`fullWidth\` | \`boolean\` | \`true\` when \`size="lg"\` | Stretch to container |
| \`href\` | \`string\` | — | Renders an \`<a>\` instead of a \`<button>\` |
| \`disabled\`, \`type\`, \`onClick\`, … | native button props | — | Forwarded |

## Variants

| Variant | Use |
| --- | --- |
| \`primary\` | The one committing action per view — \`brand\` on white, hover \`brand-deep\` |
| \`outline\` | Secondary action — \`surface\` fill, \`line\` border, border darkens on hover |
| \`dark\` | Auth and full-bleed submits — \`ink\` on \`cream\`, hover brightens |
| \`light\` | Only over dark sections — fixed cream on fixed paper, hover white |
| \`ghost\` | Tertiary/back links — transparent, text goes \`brand\` on hover |
| \`quiet\` | Dismissals next to a primary (Cancel) — transparent, text goes \`ink\` |
| \`danger\` | Destructive — tinted \`critical\` fill, \`critical\` text and border |

## Usage

\`\`\`tsx
import { Button } from 'components/Button'

<Button variant="primary" arrow onClick={book}>立即预约</Button>
<Button variant="outline" size="sm">查看详情</Button>

// Auth submit
<Button variant="dark" size="lg" type="submit" loading={submitting} arrow>
  {submitting ? '登录中…' : '登录'}
</Button>

// Destructive pairing
<div className="flex items-center gap-3">
  <Button variant="quiet" onClick={close}>取消</Button>
  <Button variant="danger" onClick={remove}>删除</Button>
</div>
\`\`\`

## Rules

- One \`primary\` per view. Everything else is \`outline\`, \`ghost\` or \`quiet\`.
- Never scale a button on hover, add a shadow, or use a gradient fill.
- Use \`loading\` rather than swapping the button for a spinner — the control keeps its footprint.
- Icons come from \`lucide-react\`; never an emoji glyph.
`,
  "Card": `# Card

The browsable-item container, ported from the service card in \`src/views/user/Services.vue\` (the \`.service-card\` override of the shared \`ServiceGrid\`).

\`bg-surface\` on an 18px radius (\`rounded-card\`) with a 1px \`border-line\` hairline and **no shadow at rest**. On hover the border shifts to 16% ink, the card lifts \`-4px\` and the single sanctioned elevation (\`shadow-lift\`) appears — 200ms \`ease-editorial\`. Media inside scales to \`1.06\` over 300ms.

Use \`Card\` only for **browsable items** (services, merchants, keepers, pets). Records, settings and details belong in sections, description lists or \`DataTable\`.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| \`media\` | \`ReactNode\` | — | Flush top media slot on a \`bg-sand\` 4:3 frame. Images are cover-fit and scale on hover. |
| \`onMediaClick\` | \`() => void\` | — | Makes the media region a button. Pair with \`mediaLabel\`. |
| \`mediaLabel\` | \`string\` | — | Accessible name for the media button. |
| \`eyebrow\` | \`string\` | — | 11px uppercase brand label above the title. |
| \`title\` | \`ReactNode\` | — | Serif \`h3\`, weight 500, \`-0.01em\` tracking. |
| \`meta\` | \`ReactNode\` | — | Muted secondary line (merchant, distance, date). Truncates. |
| \`trailing\` | \`ReactNode\` | — | Right-aligned header content — price, rating. Rendered with tabular numerals. |
| \`header\` | \`ReactNode\` | — | Fully custom header; replaces eyebrow / title / meta / trailing. |
| \`footer\` | \`ReactNode\` | — | Footer slot divided by a hairline top border; stacks below \`sm\`. |
| \`interactive\` | \`boolean\` | \`true\` | Set \`false\` for static panels (no lift, no hover shadow). |
| \`className\` | \`string\` | \`''\` | Extra classes on the root \`article\`. |
| \`children\` | \`ReactNode\` | — | Body copy at 13.5px / 1.7 in \`text-ink-soft\`. |

## Usage

\`\`\`tsx
<Card
  media={<img src={service.image} alt={\`\${service.name}服务图片\`} />}
  mediaLabel={\`查看\${service.name}详情\`}
  onMediaClick={() => navigate(\`/services/\${service.id}\`)}
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
\`\`\`

Static panel:

\`\`\`tsx
<Card interactive={false} title="本月概览">…</Card>
\`\`\`

## Notes

- Long titles and meta values must not break the grid — use \`ref-truncate\`, \`ref-clamp-2\`, \`ref-clamp-3\` on body content.
- Never add a second shadow, a gradient, or a rest-state elevation.
- Loading grids use \`SkeletonCard\`, empty grids use \`EmptyState\` — do not fake either with an empty \`Card\`.
`,
  "Chip": `# Chip

The 44px filter control from the services catalogue. Ported 1:1 from \`src/views/user/Services.vue\` (\`.s-chip\` / \`.s-rail\`), with the Vue scoped CSS translated to Tailwind utilities bound to the same \`--ref-*\` tokens.

Use it for single-select category / sort filters that sit above a grid, table or list. It is **not** a tag or a status indicator — use \`StatusBadge\` for status and read-only labels.

## Anatomy

- Height 44px, \`rounded-control\` (11px), \`0 16px\` padding, 6px gap.
- Rest: \`bg-surface\`, \`border-line\` hairline, \`text-ink-soft\`, 13.5px / 500 (\`text-control\`).
- Hover: border shifts to 30% ink, fill to \`bg-sand\`.
- Active (selected): \`bg-ink\` + \`border-ink\` + \`text-cream\`.
- Optional count: 11px, 0.6 opacity, tabular numerals.
- Transitions: 150ms \`ease-editorial\` on colour and border only — no scaling.

## Props — \`Chip\`

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| \`label\` | \`ReactNode\` | — | Required. Truncates instead of wrapping. |
| \`count\` | \`number \\| string\` | — | Trailing count. Pass \`'--'\` while loading. |
| \`active\` | \`boolean\` | \`false\` | Selected state; also sets \`aria-pressed\`. |
| \`icon\` | \`ReactNode\` | — | Optional leading lucide icon, sized to 16px. |
| \`disabled\` | \`boolean\` | \`false\` | Dimmed, non-interactive. |
| \`className\` | \`string\` | \`''\` | Appended last. |

All other \`<button>\` props (\`onClick\`, \`aria-*\`, …) pass through.

## Props — \`ChipRail\`

| Prop | Type | Notes |
| --- | --- | --- |
| \`label\` | \`string\` | Required accessible name for the \`role="group"\`. |
| \`children\` | \`ReactNode\` | The chips. |
| \`className\` | \`string\` | Appended last. |

Renders the source's \`.s-rail\`: flex row, 8px gaps, horizontal scroll with the scrollbar hidden, 4px bottom padding.

## Usage

\`\`\`tsx
import { Chip, ChipRail } from 'components/Chip'

const [selected, setSelected] = useState('')

<ChipRail label="服务分类">
  <Chip
    label="全部"
    count={loading ? '--' : services.length}
    active={selected === ''}
    onClick={() => setSelected('')}
  />
  {categories.map((category) => (
    <Chip
      key={category}
      label={category}
      count={categoryCounts[category] ?? 0}
      active={selected === category}
      onClick={() => setSelected(category)}
    />
  ))}
</ChipRail>
\`\`\`
`,
  "CreateOrderDialog": `# CreateOrderDialog

The full booking dialog from the customer app (ported from \`src/components/order/CreateOrderDialog.vue\`). It walks the user through four steps — ① 宠物与看护人 ② 预约时间 ③ 宠物送达地址 ④ 联系信息与备注 — then emits the exact API payload the backend expects.

The Vue original called \`@/api/*\` directly. This port keeps **all** booking logic, validation order, estimate math and payload shapes 1:1, but receives lists/quotes as props and hands the finished payload to \`onSubmit\`, so it stays a pure design-system component.

## Modes (derived, not configured)

| Condition | Mode |
| --- | --- |
| \`service\` present, unit \`day\` / \`booking_mode_wsh: 'date_range'\` | **批量模式** — keeper first, then one row per pet (max \`BATCH_MAX_PETS\` = 10), each with delivery/pickup date + slot |
| \`service\` present, unit \`session\` / \`hour\` (\`booking_mode_wsh: 'slot'\`) | **单笔时段** — start slot only; \`hour\` also shows 服务数量（小时） |
| no \`service\` | **非服务模式** — merchant is selectable, times fall back to \`datetime-local\` |

Batch submits with a single pet reuse the single-order endpoint payload (identical to the legacy path); two or more pets produce \`{ ...shared, items }\`.

## Props

| Prop | Type | Notes |
| --- | --- | --- |
| \`visible\` | \`boolean\` | Required. Renders nothing when false; flipping to true resets the form. |
| \`service\` | \`ServiceSummary \\| null\` | Presence enables availability + slot pickers and locks the merchant. |
| \`pets\` / \`merchants\` / \`keepers\` | arrays | Options for the selects. Merchants/keepers are expected to be pre-filtered to bookable ones. |
| \`availabilityDays\` | \`AvailabilityDay[]\` | \`days_wsh\` from the availability API; only \`bookable_wsh\` days can be submitted. |
| \`availableCoupons\` | \`Coupon[]\` | Best coupon is auto-selected until the user touches the select. |
| \`couponQuote\` / \`membershipQuote\` | quote objects | Applied only when their base amount matches the current estimate. |
| \`maxBookingDays\` | \`number\` | Authoritative booking window (inclusive); falls back to \`AVAILABILITY_REQUEST_WINDOW_DAYS\`. |
| \`merchantLoading\`, \`keeperLoading\`, \`availabilityLoading\`, \`couponLoading\`, \`availabilityError\`, \`submitting\` | \`boolean\` | Drive placeholders, hints and disabled states. |
| \`initialMerchantId\`, \`initialKeeperId\` | \`string\` | Pre-selection when opened from a service detail page. |
| \`deliveryDistanceMeters\`, \`distanceLoading\`, \`distanceError\` | distance chip | Shown once the address has coordinates. |
| \`addressPicker\` | \`ReactNode\` | Slot for the real Amap picker; a plain text field is used otherwise. |
| \`onClose\` | \`() => void\` | Backdrop mousedown and 取消. |
| \`onSubmit\` | \`(payload) => void\` | Receives \`CreateOrderPayload\` or \`CreateOrdersBatchPayload\`. |
| \`onNotify\` | \`(message, 'warning' \\| 'error') => void\` | Every validation message the Vue version sent to the toast store. |
| \`onMerchantChange\`, \`onKeeperChange\`, \`onCouponChange\` | \`(id: string) => void\` | Hooks to refetch keepers / availability / quotes. |
| \`onRetryDistance\` | \`() => void\` | 重试定位 button. |

## Exported helpers

\`normalizeUnit\`, \`dayEstimate\`, \`buildServiceDates\`, \`slotToDateTime\`, \`toApiDateTime\`, \`toDateOnly\`, \`diffDays\`, \`addDays\`, \`isSameDate\`, \`displaySlot\`, \`formatDistanceMeters\`, \`couponText\`, \`couponDiscountEstimate\`, \`pickBestCoupon\`, \`createPetRow\`, \`EMPTY_FORM\`, \`BATCH_MAX_PETS\`, \`AVAILABILITY_REQUEST_WINDOW_DAYS\`.

## Usage

\`\`\`tsx
<CreateOrderDialog
  visible={open}
  service={service}
  pets={pets}
  merchants={merchants}
  keepers={keepers}
  availabilityDays={availability.days_wsh}
  availableCoupons={coupons}
  couponQuote={couponQuote}
  membershipQuote={membershipQuote}
  maxBookingDays={availability.booking_window_days_wsh}
  keeperLoading={keeperLoading}
  submitting={submitting}
  onClose={() => setOpen(false)}
  onNotify={(message, level) => addToast(message, level)}
  onKeeperChange={keeperId => loadAvailability(keeperId)}
  onSubmit={async payload => {
    const res = 'items' in payload ? await createOrdersBatch(payload) : await createOrder(payload)
    if (res.code === 200) onCreated(res.data)
  }}
/>
\`\`\`

## Notes

- Prices, quantities and step numbers carry \`data-numeric="true"\` for tabular numerals.
- The estimate band is solid \`bg-cream\` with a \`border-brand/30\` hairline (the source gradient is dropped per the brand rules); amounts, discount lines and the platform-subsidy note match the original.
- The default address input marks the source as unconfirmed, so submitting still requires 商家位置 or a real picker result — same guard as production.
`,
  "DataTable": `# DataTable

The admin / merchant workspace table. Ported from the product's \`src/components/common/DataTable.vue\`, so the public contract is unchanged: \`columns\` + \`data\`, badge cells detected by an allow-list, and a trailing 操作 action slot. What the port adds are the states the Vue version lacked — **loading**, **error + retry** — and the responsive restructure required by the brand guidelines (scrollable table at \`md\` and up, label/value lists below it).

Use it for records: orders, users, merchants, payments, tickets. Do not use \`Card\` grids for tabular data.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| \`columns\` | \`(string \\| { key, label?, align?, width? })[]\` | \`[]\` | A bare string is both the row key and the header, matching the Vue source. |
| \`data\` | \`T[]\` | \`[]\` | Array of plain row objects. |
| \`actions\` | \`(row, index) => ReactNode\` | – | React equivalent of the Vue default slot. When present, a right-aligned action column is appended. |
| \`actionsLabel\` | \`string\` | \`'操作'\` | Header for the action column. |
| \`loading\` | \`boolean\` | \`false\` | Renders shimmer rows instead of data. Sets \`aria-busy\`. |
| \`loadingRows\` | \`number\` | \`4\` | Shimmer row count. |
| \`error\` | \`string \\| null\` | \`null\` | Replaces the body with an alert region. Takes precedence over the empty state. |
| \`onRetry\` | \`() => void\` | – | Shows the retry button inside the error region. |
| \`retryLabel\` | \`string\` | \`'重试'\` | |
| \`emptyText\` | \`string\` | \`'暂无数据'\` | Inline empty message. |
| \`caption\` | \`string\` | – | Accessible table name. |
| \`captionHidden\` | \`boolean\` | \`true\` | Set \`false\` to show the caption as a meta line above the rows. |
| \`className\` | \`string\` | \`''\` | Applied to the outer frame. |

## Cell rendering

- **Badge cells** — a cell value shaped \`{ badge, label }\` renders as a pill. \`badge\` must be one of \`ALLOWED_BADGES\`: \`badge-primary\`, \`badge-secondary\`, \`badge-success\`, \`badge-warning\`, \`badge-danger\`, \`badge-error\`, \`badge-info\`, \`badge-disabled\`. Anything else falls through to text.
- **Text cells** — \`null\` / \`''\` render as \`-\`; objects fall back to \`label\` then \`JSON.stringify\`; booleans render 是/否.
- Numeric cells get \`data-numeric="true"\` for tabular numerals.
- Long values wrap with \`overflow-wrap: anywhere\` so order ids and usernames never break the grid.

Exports: \`DataTable\`, \`ALLOWED_BADGES\`, \`isBadgeCell\`, \`formatCell\`, plus the \`DataTableProps\` / \`DataTableColumn\` / \`DataTableBadgeCell\` types.

## Usage

\`\`\`tsx
<DataTable
  columns={[
    { key: 'id_wsh', label: '订单号', width: '140px' },
    { key: 'user_wsh', label: '用户' },
    { key: 'amount_wsh', label: '金额', align: 'right' },
    { key: 'status_label_wsh', label: '状态' },
  ]}
  data={orders}
  loading={isLoading}
  error={error}
  onRetry={refetch}
  caption="订单列表"
  actions={(row) => <Button size="sm" variant="ghost">详情</Button>}
/>
\`\`\`

Plain-key columns, as used across the existing admin views:

\`\`\`tsx
<DataTable columns={['name_wsh', 'type_wsh', 'breed_wsh']} data={pets} />
\`\`\`
`,
  "DetailList": `# DetailList

Key/value description list ported 1:1 from the product's order detail view
(\`src/views/order/OrderDetailView.vue\` — \`.od-facts\` / \`.od-fact\` and
\`.od-money\` / \`.od-money-row\`). This is the sanctioned way to present record
data: **records, settings and details use lists, not cards.**

Two variants:

- **\`facts\`** (default) — a responsive grid of micro-labelled facts. \`dt\` is 11px
  uppercase with \`0.14em\` tracking in \`--ref-muted\`; \`dd\` is 13.5px/1.5 in
  \`--ref-ink-soft\` with a 7px top margin and \`word-break\`. \`amount\` items switch
  the value to the serif 21px ink treatment.
- **\`money\`** — stacked amount rows, label left / value right on a shared
  baseline. \`dt\` 13.5px ink-soft, \`dd\` 14px ink; the \`total\` row adds a hairline,
  a medium-weight ink label, and (with \`amount\`) the serif 24px total.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| \`items\` | \`DetailListItem[]\` | — | The rows. |
| \`variant\` | \`'facts' \\| 'money'\` | \`'facts'\` | Layout mode. |
| \`columns\` | \`1 \\| 2 \\| 3 \\| 4\` | \`4\` | \`facts\` only. Collapses to 2 (or 1) below \`lg\`. |
| \`divided\` | \`boolean\` | \`false\` | \`facts\` only. Adds the \`.od-facts-inner\` top hairline for use inside a panel. |
| \`className\` | \`string\` | — | Extra classes on the \`<dl>\`. |
| \`aria-label\` | \`string\` | — | Label the list when it has no visible heading. |

### \`DetailListItem\`

| Field | Type | Notes |
| --- | --- | --- |
| \`label\` | \`ReactNode\` | \`dt\` content. |
| \`value\` | \`ReactNode\` | \`dd\` content. |
| \`amount\` | \`boolean\` | Serif amount treatment (21px in \`facts\`, 24px total in \`money\`). |
| \`currency\` | \`boolean\` | Prefixes the small muted \`¥\` glyph. |
| \`numeric\` | \`boolean\` | Tabular numerals. Defaults to \`true\` in \`money\`. |
| \`total\` | \`boolean\` | \`money\` only — closing total row. |
| \`tone\` | \`'default' \\| 'discount' \\| 'muted' \\| 'ink'\` | Value accent; \`discount\` is \`--ref-brand-deep\`. |
| \`key\` | \`string\` | Optional stable React key. |

## Usage

\`\`\`tsx
import { DetailList } from 'components/DetailList'

<DetailList
  aria-label="订单概要"
  items={[
    { label: '实付金额', value: '386.00', amount: true, currency: true, numeric: true },
    { label: '宠物', value: '柯基 · 团子' },
    { label: '寄养师', value: '待分配', tone: 'muted' },
    { label: '服务周期', value: '03-04 → 03-09', numeric: true },
  ]}
/>
\`\`\`

\`\`\`tsx
<DetailList
  variant="money"
  items={[
    { label: '服务金额', value: '¥430.00' },
    { label: '优惠减免', value: '−¥44.00', tone: 'discount' },
    { label: '实付金额', value: '386.00', total: true, amount: true, currency: true },
  ]}
/>
\`\`\`

## Guidance

- Inside a \`Section\` / panel, use \`divided\` so the facts read as a continuation
  rather than a floating block.
- Keep \`columns={4}\` for page-level headers; drop to \`1\`–\`2\` inside cards.
- Always pass \`numeric\` for prices, ids, dates, durations and counts.
- Use \`tone="muted"\` for placeholder values (\`待分配\`, \`—\`) instead of a
  different font size.
`,
  "EmptyState": `# EmptyState

The empty-region placeholder for any list, table, or card grid that has no data. Ported from \`src/components/common/EmptyState.vue\` — same API (\`icon\` / \`title\` / \`description\` + default slot), so all existing consumers work unchanged. Two presentational fixes: the hardcoded \`📭\` emoji default is replaced with a line-art \`lucide-react\` mark, and the measured shell from \`Services.vue\` (\`--ref-surface\` fill, 1px dashed \`--ref-line\`, 18px radius, muted body, display-font heading, brand-tinted icon) is now the component's own styling.

Never leave a data region blank or print a bare \`暂无数据\` — use this component.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| \`icon\` | \`ReactNode\` | line-art inbox icon | Pass a \`lucide-react\` icon. No emoji. |
| \`title\` | \`string\` | — | Serif display heading (\`text-display-xs\`, ink). Omitted when absent. |
| \`description\` | \`string\` | — | 14px muted line, clamped to \`max-w-prose\`. |
| \`children\` | \`ReactNode\` | — | Action slot below the copy (usually one \`Button\`). |
| \`className\` | \`string\` | \`''\` | Extra classes on the outer shell. |

## Usage

\`\`\`tsx
import { EmptyState } from 'components/EmptyState'

<EmptyState title="暂无服务" description="换个筛选条件或稍后再看看。" />

<EmptyState title="还没有订单" description="下单后，订单会显示在这里。">
  <Button>浏览服务</Button>
</EmptyState>

<EmptyState
  icon={<SearchXIcon strokeWidth={1.25} />}
  title="没有匹配结果"
  description="试试更短的关键词。"
/>
\`\`\`

## Notes

- Pair with \`Skeleton\` (loading) and \`ErrorState\` (failure) so every data region covers all three states.
- Keep copy short: one headline, one next step.
`,
  "ErrorState": `# ErrorState

The third data-region state. \`rules/motion-states-and-resilience.md\` requires loading / empty / error on every data region; the system had \`Skeleton\` and \`EmptyState\` but nothing for failure, so views fell back to toasts or swallowed the error entirely.

Evidence for adding it (CONFIRMED in the module audit):
- Toast-only failure with no retry: \`MerchantOrders\` is the *only* view with an inline retry; BusinessHours / Services / Keepers / CS, \`AdminStatistics:177\`, \`AdminWallets:137\`, \`AdminMembershipUsers:152\`, \`AdminComplaints:245\`, \`AdminUsers:69\`, \`AdminPets:87\`, \`TicketDetail:190\` all toast and stop.
- Silently swallowed: \`MerchantDashboard:29\`, \`MerchantStatistics:22\`, \`MerchantPets:38\`, \`AdminDashboard:109\`, \`AdminOperationLogs:84\`, \`CsChat:359,404\`, \`Tickets:247,275\`.
- \`src/views/error/ServerError.vue:6\` and \`ServiceUnavailable.vue:6\` tell the user "请稍后重试" but offer only 返回首页 / 返回上一页 — no retry.

## Variants

| Variant | Use for |
| --- | --- |
| \`inline\` (default) | A failed region — list, table, stat grid, panel. Replaces the region's contents, keeps the page shell. |
| \`page\` | The route surfaces \`/403\`, \`/404\`, \`/500\`, \`/503\`. |

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| \`variant\` | \`'inline' \\| 'page'\` | \`'inline'\` | |
| \`code\` | \`string \\| number\` | — | \`page\` only. Rendered \`aria-hidden\` — the \`<h1>\` carries the meaning. |
| \`title\` | \`string\` | \`加载失败\` | |
| \`description\` | \`string\` | — | What to do next. |
| \`detail\` | \`string\` | — | Server/technical message. Small, muted, clamped to 2 lines. |
| \`onRetry\` | \`() => void\` | — | Omit and no retry button renders. |
| \`retryLabel\` | \`string\` | \`重试\` | |
| \`retrying\` | \`boolean\` | \`false\` | Puts the retry \`Button\` in its loading state. |
| \`icon\` | \`ReactNode\` | warning mark | |
| \`children\` | \`ReactNode\` | — | Secondary actions (返回首页, 联系客服 …). |
| \`className\` | \`string\` | \`''\` | |

## Usage

\`\`\`tsx
// Failed region
{error ? (
  <ErrorState description="网络请求失败，请检查连接后重试。" onRetry={reload} />
) : loading ? (
  <SkeletonList count={6} />
) : items.length === 0 ? (
  <EmptyState title="暂无数据" />
) : (
  <List items={items} />
)}

// Route surface
<ErrorState variant="page" code={500} title="服务出现异常" retryLabel="重新加载" onRetry={reload}>
  <Button variant="outline" size="sm" href="/">返回首页</Button>
</ErrorState>
\`\`\`

## Design notes

- Inline uses a restrained \`critical/25\` hairline over a 4% critical wash — no solid red fill, no shadow at rest.
- \`page\` numeral is \`clamp(56px, 13vw, 104px)\` in \`text-line\`, fixing the audited fixed \`96px\` that filled a 320px viewport.
- Action rows are \`flex-wrap\`, fixing the audited non-wrapping \`.error-actions\` row.
- Inline carries \`role="alert"\`; the numeral is \`aria-hidden\` so the \`<h1>\` is what screen readers announce (the audited pages had the code as a \`<div>\` and the real heading at 24px).
- Composes the DS \`Button\`, so retry/secondary actions inherit the 42px CTA contract and the focus ring.

## Vue port note

Mirror this API on the Vue side as \`src/components/common/ErrorState.vue\` with \`variant / code / title / description / detail / retryLabel / retrying\` props and a \`retry\` emit, plus a default slot for secondary actions. It is a **new** shared component (no existing \`.vue\` counterpart) — see \`rules/design-system-artifact-boundary.md\`, group B.
`,
  "Eyebrow": `# Eyebrow

The small uppercase kicker that sits above page and section titles in Editorial Warm: a 32×1px \`--ref-line\` hairline, a 10px gap, then a 9.5px uppercase label at 0.28em tracking in \`--ref-muted\`.

Ported 1:1 from \`.s-eyebrow\` in \`src/views/user/Services.vue\` (lines 256–272), where it appears above the hero title and the catalogue section heading.

## Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| \`children\` | \`ReactNode\` | — | The label text. Rendered uppercase. |
| \`line\` | \`boolean\` | \`true\` | Show the leading 32px hairline. |
| \`tone\` | \`'muted' \\| 'brand'\` | \`'muted'\` | Label colour. \`brand\` for card eyebrows. |
| \`as\` | \`'div' \\| 'p' \\| 'span'\` | \`'div'\` | Rendered element. Use \`p\` inside section headers. |
| \`decorative\` | \`boolean\` | \`false\` | Sets \`aria-hidden\` on the whole eyebrow — use when the label duplicates the adjacent heading. |
| \`className\` | \`string\` | \`''\` | Extra classes for layout only. |

## Usage

\`\`\`tsx
<Eyebrow decorative>Services</Eyebrow>
<h1 className="mt-5 font-display text-display-xl text-ink">
  为你的爱宠，找到合适的照护服务
</h1>
\`\`\`

Section header, label-only, and brand variants:

\`\`\`tsx
<Eyebrow as="p">Catalogue</Eyebrow>
<Eyebrow line={false}>Membership</Eyebrow>
<Eyebrow tone="brand">Featured</Eyebrow>
\`\`\`

## Notes

- Keep labels short (one or two words) — long labels truncate rather than wrap.
- Never use it as a decorative numbering device (\`01 / 02 / 03\`); that pattern is banned by the brand rules.
- Pair with \`SectionHeading\` / \`PageHeader\` for the standard 12–20px gap to the title.
`,
  "FavoriteToggleButton": `# FavoriteToggleButton

Optimistic favourite toggle for services, merchants and keepers. Ported from
\`src/components/common/FavoriteToggleButton.vue\` + \`useFavoriteState\`: it owns its
own request handling (initial check, toggle, revert on failure) and exposes the
store side-effects (auth gate, toasts) as callbacks.

Geometry follows the icon-button standard: 40px tall, fully rounded, 20px star,
\`muted\` at rest and \`caution\` when favourited. \`showText\` adds the label inline.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| \`targetId\` | \`number \\| string\` | — | Must resolve to a positive number |
| \`targetType\` | \`string\` | — | \`merchant\` \\| \`keeper\` \\| \`service\` |
| \`showText\` | \`boolean\` | \`true\` | Hide for card / media overlays |
| \`favoritedLabel\` | \`string\` | \`'已收藏'\` | Also the \`aria-label\` when active |
| \`unfavoritedLabel\` | \`string\` | \`'收藏'\` | Also the \`aria-label\` when inactive |
| \`defaultFavorited\` | \`boolean\` | \`false\` | Used until \`onCheckFavorite\` resolves |
| \`onCheckFavorite\` | \`(id: number, type: string) => Promise<boolean> \\| boolean\` | — | Runs on mount and when the target changes; shows the loading spinner |
| \`onToggle\` | \`(next: boolean, id: number, type: string) => Promise<unknown>\` | — | Rejecting reverts the optimistic flip |
| \`isAuthenticated\` | \`boolean\` | \`true\` | When false, clicks call \`onRequireLogin\` |
| \`onRequireLogin\` | \`() => void\` | — | Open the login prompt / store the redirect path |
| \`onNotify\` | \`(message: string, tone: 'success' \\| 'error') => void\` | — | Toast hook (\`已收藏\`, \`已取消收藏\`, \`操作失败\`, \`不支持的收藏类型\`) |
| \`disabled\` | \`boolean\` | \`false\` | Forces the disabled state |
| \`className\` | \`string\` | \`''\` | Extra classes |

## States

- **Loading / toggling** — spinner replaces the star, button disabled.
- **Disabled** — 50% opacity, no hover lift; also applied when the target is invalid.
- **Error** — state reverts and \`onNotify('操作失败', 'error')\` fires.

## Usage

\`\`\`tsx
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
\`\`\`

Clicks call \`stopPropagation\`, so the button is safe inside a linked card.

## Helpers

\`FAVORITE_TARGET_TYPES\`, \`FAVORITE_TARGET_TYPE_LABELS\`,
\`normalizeFavoriteTargetType\`, \`isFavoriteTargetType\`,
\`getFavoriteTargetTypeLabel\` are re-exported for favourite lists and filters.
`,
  "FilterToolbar": `# FilterToolbar

The catalogue filter row ported from \`src/views/user/Services.vue\` (\`.s-toolbar\` / \`.s-search\` / \`.s-clear\` / \`.s-reset\`). A wrapping flex row with a 12px gap and 28px top margin: a flexible 220–380px search field plus 44px-tall action controls. Below 560px it stacks full-width with \`align-items: stretch\` and the reset control centres itself.

Use it directly under a category rail or section heading, above a results heading — never inside a \`Card\`.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| \`value\` | \`string\` | — | Controlled search value. Omit for uncontrolled use. |
| \`onValueChange\` | \`(value: string) => void\` | — | Fires on keystrokes and on clear. |
| \`defaultValue\` | \`string\` | \`''\` | Initial value when uncontrolled. |
| \`placeholder\` | \`string\` | \`'搜索服务名称或内容'\` | |
| \`searchLabel\` | \`string\` | \`'搜索服务'\` | \`aria-label\` for the input. |
| \`ariaLabel\` | \`string\` | \`'筛选预约服务'\` | \`aria-label\` for the \`role="search"\` region. |
| \`showReset\` | \`boolean\` | \`false\` | Mirrors the source's \`hasFilters\` guard. |
| \`resetLabel\` | \`string\` | \`'清除筛选'\` | |
| \`onReset\` | \`() => void\` | — | Clears all active filters. |
| \`children\` | \`ReactNode\` | — | Extra 44px controls (sort selects, chips) placed after the field. |
| \`className\` | \`string\` | \`''\` | Appended to the row. |

## Behaviour

- The inline clear button (28px, \`rounded-tile\`) only appears while the query is non-empty.
- Input states match the source: hairline \`border-line\` at rest, ink-tinted border on hover/focus, and a 3px brand-tinted focus ring.
- Reset is a text-only control that shifts to \`text-brand\` on hover.

## Usage

\`\`\`tsx
const [query, setQuery] = useState('')
const [category, setCategory] = useState('')
const hasFilters = query !== '' || category !== ''

<FilterToolbar
  value={query}
  onValueChange={setQuery}
  showReset={hasFilters}
  onReset={() => {
    setQuery('')
    setCategory('')
  }}
/>
\`\`\`
`,
  "FormField": `# FormField

Form row primitive: label → 44px control → hint/error. Consolidates the duplicated
\`.auth-field / .auth-input / .auth-error\` (Register, Login, ForgetPassword) and
\`.ka-field / .ka-input / .ka-textarea / .ka-hint\` (KeeperApply) scoped CSS.

Presentational only — it owns no state. Keep \`value\` / \`onChange\` and all validation
logic in the consuming view, and mirror the validation result into \`error\` (plus
\`invalid\` on the control).

## Exports

- \`FormField\` — the label + control + message wrapper.
- \`FormInput\` — 44px tall input (\`h-11\`), ported from \`.auth-input\` / \`.ka-input\`.
- \`FormTextarea\` — min 96px, vertically resizable, same border/focus treatment.

## Props

### FormField

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| \`label\` | \`ReactNode\` | — | Required. Rendered as a real \`<label>\`. |
| \`htmlFor\` | \`string\` | — | id of the control; always pass it for a11y. |
| \`required\` | \`boolean\` | \`false\` | Appends the terracotta \`*\` marker. |
| \`hint\` | \`ReactNode\` | — | Quiet helper copy; hidden while \`error\` is set. |
| \`error\` | \`ReactNode\` | — | Validation message in \`brand-deep\`, \`role="alert"\`. |
| \`trailing\` | \`ReactNode\` | — | Inline action right of the control (captcha row). |
| \`wide\` | \`boolean\` | \`false\` | \`col-span-full\` inside a form grid. |
| \`muted\` | \`boolean\` | \`false\` | Dims the field for derived / read-only values. |
| \`className\` | \`string\` | — | Extra classes on the wrapper. |

### FormInput / FormTextarea

All native \`input\` / \`textarea\` props, plus \`invalid?: boolean\` which paints the
error border and sets \`aria-invalid\`.

## Usage

\`\`\`tsx
<FormField label="手机号" htmlFor="phone" required error={errors.phone}>
  <FormInput
    id="phone"
    type="tel"
    inputMode="numeric"
    maxLength={11}
    value={form.phone}
    invalid={Boolean(errors.phone)}
    onChange={(e) => setPhone(e.target.value.replace(/\\D/g, ''))}
    placeholder="用于接收订单与照护提醒"
  />
</FormField>
\`\`\`

Captcha row:

\`\`\`tsx
<FormField
  label="短信验证码"
  htmlFor="captcha"
  required
  error={errors.captcha}
  hint={captchaHint}
  trailing={
    <button type="button" className="h-11 rounded-[10px] border border-line bg-surface px-4 text-[13px] font-medium text-ink-soft">
      获取验证码
    </button>
  }
>
  <FormInput id="captcha" inputMode="numeric" maxLength={6} placeholder="6 位数字" />
</FormField>
\`\`\`

## Notes

- Focus uses the system-wide \`shadow-focus\` ring plus a \`brand\` border.
- Radius is the product's 10px control radius (\`--r-btn\`), not \`rounded-control\`.
- Place fields in a \`grid gap-5\` (single column) or \`grid-cols-2 gap-5\` parent; use
  \`wide\` for textareas and full-bleed rows.
`,
  "IconButton": `# IconButton

A quiet, icon-only affordance for inline actions: clearing a search field, dismissing a chip, opening a row menu. Ported from the search clear button in \`src/views/user/Services.vue\` (\`.s-clear\`): a 28px square with 8px radius, transparent at rest, muted ink that warms to \`--ref-ink\` over a \`--ref-sand\` wash on hover.

Use it for secondary, in-context actions only. Anything that is a primary or labelled action belongs in \`Button\`.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| \`label\` | \`string\` | — | Required accessible name (\`aria-label\` + \`title\`), since only an icon renders. |
| \`children\` | \`ReactNode\` | — | The icon element, normally a \`lucide-react\` icon. Sized automatically. |
| \`size\` | \`'sm' \\| 'md' \\| 'lg'\` | \`'md'\` | 24 / 28 / 32px square. \`md\` is the system default. |
| \`tone\` | \`'default' \\| 'brand' \\| 'critical'\` | \`'default'\` | Hover ink: ink, terracotta, or critical red. Background is always the sand wash. |
| \`active\` | \`boolean\` | \`false\` | Renders the hover treatment at rest, for toggled/selected affordances. |
| \`disabled\` | \`boolean\` | \`false\` | 40% opacity, no hover. |

All remaining \`<button>\` attributes (\`onClick\`, \`aria-*\`, \`className\`, …) pass through.

## Usage

\`\`\`tsx
import { XIcon } from 'lucide-react'
import { IconButton } from 'components/IconButton'

<IconButton label="清除搜索" onClick={() => setQuery('')}>
  <XIcon />
</IconButton>
\`\`\`

Docked inside a search input:

\`\`\`tsx
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
\`\`\`

Destructive row action:

\`\`\`tsx
<IconButton label="删除订单" tone="critical" onClick={remove}>
  <Trash2Icon />
</IconButton>
\`\`\`

## Notes

- Colour transitions run at 150ms with \`ease-editorial\`; there is no scale or shadow on hover.
- Focus uses the system-wide \`:focus-visible\` ring from \`index.css\` — do not add a custom ring.
- Never place an emoji inside it; use \`lucide-react\`.
`,
  "Input": `# Input

The single text field for the Editorial Warm system, ported 1:1 from the services toolbar search field (\`src/views/user/Services.vue\`, \`.s-search input\`).

- 44px tall (\`h-11\`), \`rounded-control\` (11px), 1px \`border-line\` hairline on \`bg-surface\`
- \`text-control\` (13.5px) ink text, \`text-muted\` placeholder
- Hover: border \`color-mix(in srgb, var(--ref-ink) 25%, transparent)\`
- Focus: border \`35%\` ink, \`outline: none\`, ring \`0 0 0 3px color-mix(in srgb, var(--ref-brand) 12%, transparent)\`
- Leading icon sits 14px from the left; trailing clear button sits 8px from the right (28px, \`rounded-tile\`, hover \`bg-sand\`)

Transitions are 150ms \`ease-editorial\` on border-color and box-shadow only — no transforms, no shadow at rest.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| \`leadingIcon\` | \`ReactNode\` | – | Rendered at 14px left, muted, 15px box. Adds \`pl-10\`. |
| \`clearable\` | \`boolean\` | \`false\` | Shows the trailing clear button when the field has a value. Adds \`pr-10\`. |
| \`onClear\` | \`() => void\` | – | Fired after the value is cleared and focus returns to the input. |
| \`invalid\` | \`boolean\` | \`false\` | Critical border + critical focus ring, sets \`aria-invalid\`. |
| \`clearLabel\` | \`string\` | \`'Clear'\` | Accessible label for the clear button. |
| \`containerClassName\` | \`string\` | – | Wrapper class — use it for width (\`max-w-[380px]\`, \`w-full\`). |
| \`className\` | \`string\` | – | Merged onto the \`<input>\`. |
| …rest | \`InputHTMLAttributes\` | – | \`type\`, \`value\`, \`onChange\`, \`placeholder\`, \`disabled\`, etc. |

Works controlled or uncontrolled; clearing dispatches a native \`input\` event so controlled consumers receive the empty value.

\`SearchInput\` is a preset: \`type="search"\`, \`autoComplete="off"\`, \`clearable\`, and a \`SearchIcon\` leading slot.

## Usage

\`\`\`tsx
import { Input, SearchInput } from 'components/Input'

// Services toolbar
<SearchInput
  containerClassName="max-w-[380px]"
  placeholder="搜索服务名称或内容"
  aria-label="搜索服务"
  value={query}
  onChange={(e) => setQuery(e.target.value)}
/>

// Form field
<Input
  type="email"
  leadingIcon={<MailIcon />}
  placeholder="you@example.com"
  invalid={!!error}
/>
\`\`\`

Pair with \`Field\` for labels, help text and error copy. Set \`data-numeric="true"\` when the field holds prices, ids or counts.
`,
  "LoadingSpinner": `# LoadingSpinner

Small inline busy indicator: a warm terracotta ring over a hairline track, with a short status message beneath it. Ported from the product's \`src/components/common/LoadingSpinner.vue\`.

Use it for short, scoped waits (a panel, a dialog body, a submit action). For content that has a known shape — lists, cards, tables — prefer \`Skeleton\` / \`SkeletonList\` instead, per the Editorial Warm guidelines.

## Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| \`text\` | \`string\` | \`'加载中...'\` | Message shown under the spinner; also used as the \`aria-label\`. |
| \`className\` | \`string\` | — | Extra classes on the wrapper (e.g. to tighten the padding). |

## Behaviour

- Renders \`role="status"\` with \`aria-live="polite"\` so screen readers announce the wait.
- Ring uses \`border-line\` with a \`border-t-brand\` head and the system \`animate-spin\` (\`ref-spin\`) keyframes.
- Collapses automatically under \`prefers-reduced-motion: reduce\` via the global guard in \`index.css\`.

## Usage

\`\`\`tsx
import { LoadingSpinner } from 'components/LoadingSpinner'

{loading && <LoadingSpinner text="加载商家资料..." />}
\`\`\`

Inside a surface:

\`\`\`tsx
<div className="rounded-card border border-line bg-surface">
  {loading ? <LoadingSpinner text="加载订单列表..." /> : <OrderList orders={orders} />}
</div>
\`\`\`
`,
  "LoginPromptDialog": `# LoginPromptDialog

Global auth-gate dialog. Mount it once near the app root and show it whenever a guest triggers a members-only action (favouriting, booking, checkout). Ported from \`src/components/common/LoginPromptDialog.vue\`.

- Renders through a portal into \`document.body\` (the Vue \`<Teleport to="body">\` equivalent).
- Centered, 400px-max card on a blurred scrim; lock glyph, one line of copy, dismiss + login actions.
- Closes on scrim click, \`Escape\`, or the dismiss button. Confirm closes first, then calls \`onLogin\` with the resolved login path.
- \`safeRedirect\` (also exported) rejects non-path and protocol-relative values, falling back to \`/dashboard\` — keeps the redirect same-origin (F-NAV-006).

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| \`visible\` | \`boolean\` | — | Controls mounting/visibility. |
| \`onClose\` | \`() => void\` | — | Scrim, Escape, and dismiss button. |
| \`onLogin\` | \`(loginPath: string) => void\` | — | Receives \`/login?redirect=<encoded>\`; wire to your router. |
| \`loginRedirectPath\` | \`string\` | \`/dashboard\` | Path to return to after sign-in; sanitised. |
| \`message\` | \`string\` | \`登录后即可使用该功能\` | Body copy. |
| \`cancelLabel\` | \`string\` | \`暂不登录\` | Dismiss label. |
| \`confirmLabel\` | \`string\` | \`去登录\` | Confirm label. |

## Usage

\`\`\`tsx
import { useNavigate } from 'react-router-dom'
import { LoginPromptDialog } from 'components/LoginPromptDialog'

const navigate = useNavigate()

<LoginPromptDialog
  visible={app.showLoginPrompt}
  onClose={app.closeLoginPrompt}
  loginRedirectPath={app.loginRedirectPath}
  onLogin={path => navigate(path)}
/>
\`\`\`

## Guidelines

- One instance per app; drive it from app state rather than rendering it per feature.
- Keep the copy to a single sentence — this is an interruption, not a marketing surface.
- Confirm action is the only brand-filled button; dismiss stays outlined.
`,
  "Logo": `# Logo

The product brand lockup: the raster brand mark (\`/logo.png\`, a rounded illustration of a dog and cat inside a house) beside the \`宠物寄养平台\` wordmark. Ported from the product's \`UserLayout\` header and footer lockups.

The mark is a **raster asset used as-is** — do not vectorise, recolour, or resize the underlying \`public/logo.png\`. The component only frames it (48px, 12px radius, \`object-fit: cover\`).

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| \`src\` | \`string\` | \`'/logo.png'\` | Path to the raster mark. Keep the default unless serving the asset from another host. |
| \`alt\` | \`string\` | \`'宠物寄养平台'\` | Accessible name. Applied to the mark only when the wordmark is hidden; otherwise the mark is decorative. |
| \`wordmark\` | \`string\` | \`'宠物寄养平台'\` | Text beside the mark. |
| \`showWordmark\` | \`boolean\` | \`true\` | Set \`false\` for the mark alone. |
| \`size\` | \`'sm' \\| 'md' \\| 'lg'\` | \`'md'\` | 32 / 48 / 56px mark. \`md\` matches the product header and footer. |
| \`href\` | \`string\` | – | Renders as a link (hover shifts the lockup to \`text-brand\`). |
| \`onClick\` | \`() => void\` | – | With \`href\`, fires alongside navigation (e.g. closing a mobile menu); without it, renders a button. |
| \`className\` | \`string\` | \`''\` | Extra classes on the lockup wrapper. |

## Usage

\`\`\`tsx
// Header
<Logo href="/dashboard" onClick={closeMobileMenu} />

// Footer (same lockup, non-interactive)
<Logo />

// Compact / mobile bar
<Logo showWordmark={false} size="sm" />
\`\`\`

## Behaviour

- Wordmark uses \`font-body\` at 600 weight with \`whitespace-nowrap\` + \`ref-truncate\`, so a longer brand name never breaks the header grid.
- If the raster mark fails to load, it falls back to a \`lucide-react\` paw mark on \`bg-sand\` in \`text-brand\` — never a broken image.
- Colour transitions run at 150ms on \`ease-editorial\`. No shadow, no gradient.
`,
  "MediaFrame": `# MediaFrame

The single sanctioned way to present photography in Editorial Warm. Ported from the product's hero frame (\`src/views/user/Services.vue\` \`.s-hero-frame\`) plus the shared \`MediaWithFallback\` loader.

Photography is the decoration in this system — always frame it with \`MediaFrame\` rather than a bare \`<img>\`.

## Behaviour

- 4/3 aspect ratio by default, \`rounded-frame\` (26px), 1px \`border-line\`, \`bg-surface\`, \`overflow-hidden\`.
- Image is \`object-cover\` and scales to \`1.06\` over 300ms \`ease-editorial\` on hover of the frame **or** of a parent element with the \`group\` class (so cards can drive the zoom).
- \`loading\` renders the \`ref-shimmer\` skeleton — never a bare "Loading...".
- Empty or failing \`src\` renders the sand placeholder with a brand \`ImageIcon\` and optional label.
- Local MinIO URLs are rewritten to same-origin paths (\`normalizeMediaUrl\`), matching the product loader.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| \`src\` | \`string\` | \`''\` | Empty or failing sources show the fallback |
| \`alt\` | \`string\` | \`''\` | Pass \`''\` for decorative media |
| \`ratio\` | \`'4/3' \\| '1/1' \\| '16/9' \\| '3/2' \\| '3/4' \\| 'auto'\` | \`'4/3'\` | \`auto\` lets content define height |
| \`radius\` | \`'frame' \\| 'card' \\| 'control' \\| 'tile' \\| 'none'\` | \`'frame'\` | Only system radii |
| \`loading\` | \`boolean\` | \`false\` | Shimmer skeleton |
| \`imgLoading\` | \`'lazy' \\| 'eager'\` | \`'lazy'\` | Native image loading |
| \`zoomOnHover\` | \`boolean\` | \`true\` | 1.06 hover scale |
| \`fallbackLabel\` | \`string\` | – | Text under the placeholder icon |
| \`children\` | \`ReactNode\` | – | Overlay content (badges, captions) |
| \`className\` | \`string\` | \`''\` | Extra classes on the frame |

## Usage

\`\`\`tsx
// Page hero
<MediaFrame src={heroImage} alt="照护服务" imgLoading="eager" />

// Loading state
<MediaFrame loading alt="照护服务" />

// Card media — zoom driven by the card's \`group\`
<article className="group rounded-card border border-line bg-surface overflow-hidden">
  <MediaFrame src={service.cover} alt={service.name} radius="none" />
  <div className="p-5">…</div>
</article>

// Avatar
<MediaFrame src={pet.avatar} alt={\`\${pet.name} 的照片\`} ratio="1/1" radius="control" fallbackLabel="暂无照片" />
\`\`\`

## Don't

- Don't add a resting shadow — only \`shadow-lift\` on hover, and only on the card, not the frame.
- Don't invent radii or ratios outside the props above.
- Don't render a blank box for missing images; always let the fallback show.
`,
  "MediaWithFallback": `# MediaWithFallback

Site-wide image primitive with a built-in placeholder / error fallback. Ported from the product's \`src/components/common/MediaWithFallback.vue\`, which nearly every card, avatar and cover image uses.

It always fills its parent (\`100% × 100%\`, \`object-fit: cover\`) and clips overflow — so the **parent** owns aspect ratio and radius. Pair it with \`MediaFrame\` (or any sized, rounded wrapper) for framing.

## Behaviour

- Empty / whitespace \`src\` → placeholder.
- Image \`onError\` → placeholder.
- Changing \`src\` resets the error state and retries.
- Local MinIO URLs (\`localhost\` / \`127.0.0.1\` with a \`/minio/\` path) are rewritten to a relative path so they resolve through the frontend proxy. Exported as \`normalizeMediaUrl\`.
- The placeholder is exposed as \`role="img"\` with the \`alt\` text when \`alt\` is provided; otherwise it stays decorative.

## Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| \`src\` | \`string\` | \`''\` | Image source. Empty or failing sources render the fallback. |
| \`alt\` | \`string\` | \`''\` | Accessible description; also labels the fallback. |
| \`placeholder\` | \`string\` | \`''\` | Optional caption inside the fallback (e.g. \`暂无图片\`). Omit for a bare icon. |
| \`loading\` | \`'lazy' \\| 'eager'\` | \`'lazy'\` | Native image loading strategy. |
| \`className\` | \`string\` | \`''\` | Extra classes on the frame element. |

## Usage

\`\`\`tsx
import { MediaWithFallback } from 'components/MediaWithFallback'

// Card cover — parent sets ratio + radius
<div className="h-48 w-full overflow-hidden rounded-card border border-line">
  <MediaWithFallback src={service.cover} alt={service.name} placeholder="暂无图片" />
</div>

// Avatar tile
<div className="h-16 w-16 overflow-hidden rounded-tile">
  <MediaWithFallback src={pet.avatar} alt={\`\${pet.name} 的照片\`} />
</div>

// Above-the-fold hero image
<MediaWithFallback src={heroImage} alt="照护服务" loading="eager" />
\`\`\`

## Notes

- Resting backdrop is \`bg-sand\`; the fallback is a 10% \`--ref-brand\` tint over \`--ref-surface\` with a \`text-brand\` icon and \`text-muted\` caption — no gradients, no shadow.
- The source's \`--color-*\` variables come from a global product stylesheet not present here, so those values are reproduced with this system's \`--ref-*\` tokens.
`,
  "MessageIndicator": `# MessageIndicator

Global unread-message control for layout navigation bars (user, merchant, admin). A 44×44 icon button that shows a critical dot — or an optional numeric badge — when there are unread notifications.

Ported from \`src/components/common/MessageIndicator.vue\`. The source's Element Plus icon and global \`--color-*\` variables are expressed here with \`lucide-react\` and the design system's editorial-warm tokens (\`text-ink\`, \`hover:text-brand\`, \`bg-critical\`, \`rounded-control\`).

## Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| \`unreadCount\` | \`number\` | \`0\` | Unread notifications; the indicator appears when \`> 0\`. |
| \`showCount\` | \`boolean\` | \`false\` | Render the number instead of a plain dot. |
| \`max\` | \`number\` | \`99\` | Cap before rendering \`{max}+\`. |
| \`label\` | \`string\` | \`'消息提醒'\` | Tooltip and accessible name base. |
| \`onClick\` | \`() => void\` | — | Wire to the notifications route. |
| \`disabled\` | \`boolean\` | \`false\` | Non-interactive state. |
| \`className\` | \`string\` | \`''\` | Extra classes on the button. |

## Usage

\`\`\`tsx
import { MessageIndicator } from 'components/MessageIndicator'

const navigate = useNavigate()

<MessageIndicator
  unreadCount={unreadCount}
  onClick={() => navigate('/notifications')}
/>
\`\`\`

Numeric variant:

\`\`\`tsx
<MessageIndicator unreadCount={128} showCount onClick={goNotifications} />
\`\`\`

## Notes

- Sits inline with other nav controls; no wrapper padding is applied.
- The count uses \`data-numeric="true"\` for tabular numerals.
- Hover is a colour + 10% brand tint only — no shadow, no scale, per the motion rules.
`,
  "Modal": `# Modal

The single dialog primitive for the product. Consolidates the \`.dlg-*\` family that was duplicated across the order and pet dialogs (\`CreateOrderDialog\`, \`ReviewDialog\`, \`TipDialog\`, \`PetFormDialog\`) plus the user views (\`Pets\`, \`Addresses\`, \`Profile\`, \`Tickets\`, \`Complaints\`): overlay → panel → head → body → foot → close.

Anatomy and styling are ported 1:1 from the source dialogs — warm scrim (\`rgba(10,8,6,0.5)\` + 2px blur), surface panel on a hairline border with the single sanctioned lift, serif 20px title, 24px gutters, right-aligned footer. Entrance is \`animate-fade\` (overlay) + \`animate-pop\` (panel), never a fly-in.

Behaviour handled once, here: Escape to dismiss, overlay press-to-dismiss (press must start *and* land on the overlay), \`<body>\` scroll lock with a shared counter so stacked dialogs restore correctly, focus moved into the panel on open and returned to the trigger on close, and \`role="dialog" aria-modal="true"\` labelled by the title.

Existing dialogs keep their own props and emit contracts — they only swap their markup/CSS for this primitive.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| \`open\` | \`boolean\` | — | Unmounted while closed. |
| \`onClose\` | \`() => void\` | — | Fired by ✕, Escape and the overlay. |
| \`title\` | \`ReactNode\` | — | Head title; also labels the dialog. |
| \`description\` | \`ReactNode\` | — | Supporting copy at the top of the body. |
| \`children\` | \`ReactNode\` | — | Body content. |
| \`footer\` | \`ReactNode\` | — | Actions, right aligned. Omit for no footer. |
| \`size\` | \`'sm' \\| 'md' \\| 'wide' \\| 'lg' \\| 'xl'\` | \`'md'\` | Max width 400 / 440 / 520 / 640 / 960px. |
| \`hideClose\` | \`boolean\` | \`false\` | Hide the ✕ control. |
| \`closeOnOverlayClick\` | \`boolean\` | \`true\` | |
| \`closeOnEsc\` | \`boolean\` | \`true\` | |
| \`ariaLabel\` | \`string\` | — | Accessible name when there is no visible \`title\`. |
| \`className\` | \`string\` | \`''\` | Extra panel classes. |

Size mapping from the source: \`dlg-panel-sm\` → \`sm\`, \`dlg-panel\` → \`md\`, \`dlg-panel-wide\` → \`wide\`, \`dlg-panel-lg\` → \`lg\`, \`order-modal\` → \`xl\`.

## Usage

\`\`\`tsx
import { Modal } from 'components/Modal'

<Modal
  open={confirmOpen}
  onClose={() => setConfirmOpen(false)}
  title="删除这份宠物档案？"
  description="删除后无法再用于新的预约，此操作不可撤销。"
  size="sm"
  footer={
    <>
      <Button variant="secondary" onClick={() => setConfirmOpen(false)}>取消</Button>
      <Button variant="danger" onClick={handleDelete}>确认删除</Button>
    </>
  }
/>
\`\`\`

Form dialog:

\`\`\`tsx
<Modal open={open} onClose={close} title="添加宠物" size="wide" footer={<SaveActions />}>
  <FieldGroup>…</FieldGroup>
</Modal>
\`\`\`

## Guidance

- Long bodies scroll inside the panel (\`max-h: min(90vh, 720px)\`); never let the page scroll behind a dialog.
- Put the primary action last in \`footer\`, cancel first.
- Use \`size="sm"\` for confirms, \`wide\` for single-column forms, \`lg\` for detail records, \`xl\` only for the multi-step create-order flow.
- Always pass \`ariaLabel\` when the dialog has no visible title.
`,
  "OrderCard": `# OrderCard

A single customer order row for the user-facing order list (\`/orders\`). Ported 1:1 from the product's \`src/components/order/OrderCard.vue\` (used by \`src/views/user/Orders.vue\`).

It renders, in one card:

1. A clickable summary button — service cover with pet-avatar badge, order no., status badge, service name, pet/date/merchant meta, and an amount column with billing text and discount.
2. An optional payment countdown band (15-minute window from \`created_at_wsh\`) for \`pending\` orders.
3. A four-phase service progress rail (已下单 → 已支付 → 寄养中 → 已完成), replaced by a "flow stopped" note for \`cancelled\` / \`refunding\` / \`refunded\`.
4. A status-driven action footer.

> **Scope:** this is currently a user-order-list component. Only \`Orders.vue\` consumes it in the product — keep it feature-local unless a second surface (merchant / keeper / CS / admin) adopts it.

## Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| \`order\` | \`OrderRecord\` | — | **Required.** Raw order record with the API's \`*_wsh\` fields. |
| \`nowMs\` | \`number\` | \`Date.now()\` | Shared clock. Pass one ticking value from the list so all countdowns update together. |
| \`processing\` | \`boolean\` | \`false\` | Disables mutating actions (cancel, pay, deliver, tip, review) while a request is in flight. |
| \`onCancel\` | \`(order) => void\` | — | 取消订单 (pending). |
| \`onPay\` | \`(method) => void\` | — | 余额支付 (pending). Called with \`'balance'\`. |
| \`onDeliver\` | \`(order) => void\` | — | 确认已送达 (confirmed). |
| \`onReview\` | \`(order) => void\` | — | 评价服务 (completed, no feedback yet). |
| \`onTip\` | \`(order) => void\` | — | 打赏照护师 (completed). |
| \`onViewDetail\` | \`(order) => void\` | — | Card body click + 订单详情 / 查看服务动态. |
| \`onRebook\` | \`(order) => void\` | — | 再次预约. In the product this routes to \`/services/:id?book=1\`. |

## Status behaviour

| \`status_wsh\` | Badge | Progress | Actions |
| --- | --- | --- | --- |
| \`pending\` | 待付款 (brand fill) | phase 0 + countdown band | 取消订单 · 余额支付 |
| \`paid\` | 已支付 (outline) | phase 1 | note + 订单详情 |
| \`confirmed\` | 待送达 (outline) | phase 1 | 订单详情 · 确认已送达 |
| \`delivered\` / \`received\` / \`in_progress\` | 已送达 / 已接收 / 服务中 (green tint) | phase 2 | 联系门店 (tel) · 查看服务动态 |
| \`completed\` | 已完成 (quiet) | phase 3 | 再次预约 · 打赏照护师 · 评价服务 / 订单详情 |
| \`cancelled\` / \`refunding\` / \`refunded\` | 已取消 / 退款中 / 已退款 | flow-stopped note | 订单详情 · 再次预约 |

\`completed\` orders without \`has_feedback_wsh\` also show a 待反馈 tag next to the status badge.

## Usage

\`\`\`tsx
import { OrderCard } from 'components/OrderCard'

<div className="grid gap-4">
  {orders.map((o) => (
    <OrderCard
      key={o.id_wsh}
      order={o}
      nowMs={nowMs}
      processing={processingOrderId === o.id_wsh}
      onCancel={handleCancel}
      onPay={(method) => handlePay(o, method)}
      onDeliver={handleDeliver}
      onReview={openReview}
      onTip={setTipOrder}
      onViewDetail={handleViewDetail}
      onRebook={(order) => navigate(\`/services/\${order.service_id_wsh}?book=1\`)}
    />
  ))}
</div>
\`\`\`

## Exported helpers

- \`ORDER_PHASES\` — the four progress phases with their \`*_wsh\` timestamp fields.
- \`ORDER_PAYMENT_TIMEOUT_MS\`, \`getPaymentTimeoutRemaining(order, now)\`, \`formatPaymentTimeoutRemaining(ms)\` — payment-window logic, mirroring \`src/utils/orderPaymentTimeout.js\`.
- \`billingText(order)\` — human-readable billing description ("4 天"、"1 次（60 分钟）"), mirroring \`src/domain/BookingUnit.js\`.

## Notes

- All colours come from the \`--ref-*\` tokens via Tailwind (\`bg-surface\`, \`border-line\`, \`text-brand-deep\`, …). No new palette.
- Amounts, order numbers, dates and countdowns carry \`data-numeric="true"\` for tabular numerals.
- The amount column collapses below 900px and reappears inline under the pet/date meta.
- Missing cover / pet avatar fall back to a warm placeholder and the pet name's first character; a broken image URL falls back too.
- Emoji icons from the Vue source (🕐 / ⛔ / ↗) are replaced with \`lucide-react\` equivalents per the Editorial Warm guidelines.
`,
  "PageHeader": `# PageHeader

The editorial page hero, ported from the Services view (\`src/views/user/Services.vue\`). Left-aligned, asymmetric: an eyebrow + display title + capped subtitle + optional stat row and action cluster, with an optional framed media column on the right.

Use exactly one per route, above the page's sections.

## Layout

- Two columns \`minmax(0, 1.2fr) / minmax(0, 1fr)\` with a 48px gap above 900px; collapses to one column with a 32px gap below it.
- Padding: \`48px 0 40px\` desktop, \`34px 0 28px\` mobile.
- Title \`clamp(34px, 4.4vw, 52px)\`, serif, weight 500, \`-0.01em\`, \`text-wrap: balance\`.
- Subtitle 15px / 1.8 (14px under 560px), capped at 560px, \`ink-soft\` at 82% opacity.
- Stats: serif 30px (26px mobile) tabular numerals, 12px muted labels, hairline divider before every stat after the first.
- Media frame: 4/3, \`rounded-frame\` (26px), 1px \`line\` border, images cover.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| \`title\` | \`ReactNode\` | — | Required. Pass a fragment with \`<br className="hidden min-[900px]:block" />\` to control desktop-only line breaks. |
| \`eyebrow\` | \`string\` | — | Uppercase kicker after a 32px hairline. |
| \`subtitle\` | \`ReactNode\` | — | Supporting copy, capped at 560px. |
| \`stats\` | \`{ value: ReactNode; label: string }[]\` | — | Optional stat row. |
| \`statsLabel\` | \`string\` | \`'概览'\` | Accessible name for the \`<dl>\`. |
| \`actions\` | \`ReactNode\` | — | Action cluster under the stats. |
| \`media\` | \`ReactNode\` | — | Right column content (usually an \`<img>\` or \`MediaFrame\`). Omit for a single-column header. |
| \`breadcrumb\` | \`ReactNode\` | — | Rendered in a \`<nav aria-label="面包屑">\` above the hero. |
| \`loading\` | \`boolean\` | \`false\` | Stats render \`--\`; the media frame shows the \`ref-shimmer\` skeleton. |
| \`className\` | \`string\` | \`''\` | Applied to the outer \`<header>\`. |

## Usage

\`\`\`tsx
<PageHeader
  eyebrow="Services"
  title={<>为你的爱宠，<br className="hidden min-[900px]:block" />找到合适的照护服务</>}
  subtitle="按分类浏览门店在售方案，价格与档期实时同步。"
  stats={[
    { value: services.length, label: '可预约方案' },
    { value: categories.length, label: '服务分类' },
    { value: merchantCount, label: '覆盖门店' },
  ]}
  statsLabel="服务概览"
  media={<img src={heroImage} alt="照护服务" />}
  loading={loading}
/>
\`\`\`

Copy-only header for records and settings pages:

\`\`\`tsx
<PageHeader
  eyebrow="Orders"
  title="订单管理"
  subtitle="查看全部预约订单的状态、金额与门店归属。"
  actions={<Button>新建订单</Button>}
/>
\`\`\`

## Don't

- Don't center the stack or add a background gradient/glow — the hero is left-aligned, canvas-on-canvas.
- Don't set the title to weight 700+, or use a second heading font.
- Don't decorate with \`01 / 02\` numbering; business numbers belong in \`stats\`.
`,
  "PageShell": `# PageShell

The single page container for every route. Ported from \`src/views/user/Services.vue\`
(\`.svc-page\` + \`.svc-shell\`), a pattern duplicated as a per-view \`*-shell\` class in
nearly every refined view — use this component instead of re-declaring it.

Geometry (1:1 with the source):

- outer page region: \`width: 100%\`, \`padding: 6px 0 72px\`
- inner shell: \`max-width: 1180px\`, \`margin: 0 auto\`, \`padding: 0 24px\`
- below 560px the gutter collapses to \`16px\`

## Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| \`width\` | \`'narrow' \\| 'default' \\| 'wide'\` | \`'default'\` | \`default\` = 1180px product shell, \`narrow\` = 1080px record shell (order detail), \`wide\` = 1440px admin data workspace. |
| \`as\` | \`'main' \\| 'div' \\| 'section' \\| 'article'\` | \`'main'\` | Element for the outer page region. |
| \`padded\` | \`boolean\` | \`true\` | Vertical page padding (\`6px 0 72px\`). Set \`false\` when nesting. |
| \`shellClassName\` | \`string\` | – | Extra classes on the inner max-width shell. |
| \`className\` | \`string\` | – | Extra classes on the outer page region. |
| \`children\` | \`ReactNode\` | – | Page content. |

All other \`HTMLAttributes\` (e.g. \`id\`, \`aria-*\`) pass through to the outer element.

## Usage

\`\`\`tsx
import { PageShell } from 'components/PageShell'

export function ServicesPage() {
  return (
    <PageShell>
      <PageHeader eyebrow="Services" title="为你的爱宠，找到合适的照护服务" />
      <section className="mt-section">{/* catalogue */}</section>
    </PageShell>
  )
}
\`\`\`

Admin / merchant data workspace:

\`\`\`tsx
<PageShell width="wide">
  <DataTable {...props} />
</PageShell>
\`\`\`

## Guidance

- Exactly one \`PageShell\` per route; never nest a padded shell inside another.
- Section rhythm inside the shell is 44 / 56 / 80px (\`mt-section\` = 56px).
- Do not add a background or shadow here — the page background comes from \`bg-canvas\` on \`body\`.
`,
  "PetFormDialog": `# PetFormDialog

Create / edit dialog for a pet profile. Ported 1:1 from the product's
\`src/components/pet/PetFormDialog.vue\` — same field order, same labels, same
payload shape (\`*_wsh\` keys), same avatar-upload behaviour. Styling that relied
on the product's global \`app.css\` (\`.modal-overlay\`, \`.modal\`, \`.btn*\`,
\`--color-*\`) is reproduced with Editorial Warm tokens (\`surface\`, \`line\`, \`ink\`,
\`muted\`, \`sand\`, \`brand\`, \`critical\`, \`rounded-control\`, \`shadow-lift\`,
\`animate-pop\`).

Controlled component: the parent owns \`visible\`, \`saving\` and \`serverError\`, and
performs the actual persistence in \`onSave\`.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| \`visible\` | \`boolean\` | \`false\` | Renders nothing when false; form resets on close. |
| \`pet\` | \`Pet \\| null\` | \`null\` | \`null\` → 添加宠物 (create). A record → 编辑宠物 (edit), form hydrated from it. |
| \`saving\` | \`boolean\` | \`false\` | Disables footer actions, primary label becomes \`保存中...\`. |
| \`serverError\` | \`string\` | \`''\` | Shown in the error banner (\`role="alert"\`). |
| \`onSave\` | \`(payload: PetFormPayload) => void\` | — | Fired on submit after local validation. |
| \`onClose\` | \`() => void\` | — | Scrim click, close button, Escape. |
| \`onUploadAvatar\` | \`(file: File) => Promise<string>\` | — | Should resolve with the uploaded avatar URL. When omitted the file is previewed locally as a data URL. |
| \`onToast\` | \`(message, type) => void\` | — | Upload failures route here; without it they fall back to the error banner. |

## Behaviour

- **Validation preserved**: \`宠物名称\` is \`required\`; submitting it empty shows
  \`请输入宠物名称\` and blocks \`onSave\`. Text fields trim on blur.
- **Payload normalisation** (\`toPayload\`): empty strings/undefined → \`null\`,
  \`gender_wsh\` coerced to a number, \`sterilized_wsh\` / \`vaccinated_wsh\` coerced
  to \`0 | 1\`. Type is lower-cased on hydrate.
- **Avatar**: 64px round preview with paw-print placeholder, spinner overlay
  while uploading, \`移除\` action, broken-image fallback.
- **States**: loading (saving + upload spinner), error banner, disabled
  controls, visible focus ring on every control.
- **Responsive**: two-column rows collapse to one below \`sm\`; the dialog docks
  as a bottom sheet on mobile and caps at \`92vh\`.

## Usage

\`\`\`tsx
import { PetFormDialog, type PetFormPayload } from 'components/PetFormDialog'

<PetFormDialog
  visible={open}
  pet={editing}
  saving={saving}
  serverError={error}
  onClose={() => setOpen(false)}
  onSave={async (payload: PetFormPayload) => {
    setSaving(true)
    try {
      await savePet(payload)
      setOpen(false)
    } catch (e) {
      setError('保存失败，请稍后重试')
    } finally {
      setSaving(false)
    }
  }}
  onUploadAvatar={async file => {
    const body = new FormData()
    body.append('file', file)
    const r = await apiPost('/api/files/upload?directory=pets', body)
    return r.data.url_wsh
  }}
/>
\`\`\`

## Exports

\`PetFormDialog\`, \`petTypeOptions\`, \`createEmptyPetForm\`, \`toPayload\`, and the
types \`Pet\`, \`PetFormValues\`, \`PetFormPayload\`, \`PetFormDialogProps\`,
\`PetGenderValue\`, \`PetFlagValue\`.
`,
  "PetList": `# PetList

Pet roster grid for the user 宠物档案 views. Ported from \`src/components/pet/PetList.vue\` — same card structure (avatar + name, 3-up specs strip, health flags, notes, actions), same props and events, restyled onto the Editorial Warm tokens.

Grid: 1 column, 2 at \`md\`, 3 at \`xl\`. Each card is fully clickable (\`onView\`) with inner buttons stopping propagation.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| \`pets\` | \`Pet[]\` | required | Roster. Field names keep the API's \`*_wsh\` suffixes. |
| \`onView\` | \`(id) => void\` | – | Card click, pet name button, and 查看档案. |
| \`onEdit\` | \`(pet: Pet) => void\` | – | 编辑 — receives the full record. |
| \`onDelete\` | \`(id) => void\` | – | 删除. |
| \`loading\` | \`boolean\` | \`false\` | Renders 3 shimmer skeleton cards instead of the grid. |
| \`emptyAction\` | \`ReactNode\` | – | Slot inside the empty state (e.g. an “添加宠物” button). |
| \`className\` | \`string\` | \`''\` | Extra classes on the root grid/panel. |

## Pet shape

\`\`\`ts
interface Pet {
  id_wsh: string | number
  name_wsh: string
  type_wsh?: 'dog' | 'cat' | 'rabbit' | 'bird' | 'fish' | 'hamster' | 'other' | string
  breed_wsh?: string | null
  avatar_wsh?: string | null
  age_wsh?: number | string | null      // months → “26 个月”
  weight_wsh?: number | string | null   // kg   → “11.4 kg”
  gender_wsh?: number | null            // 0 未知 · 1 公 · 2 母
  vaccinated_wsh?: number | null        // 1 = 已免疫
  sterilized_wsh?: number | null        // 1 = 已绝育
  description_wsh?: string | null       // absence ⇒ “档案待完善” badge
  habits_wsh?: string | null
  allergies_wsh?: string | null
}
\`\`\`

## Behaviour

- Missing \`avatar_wsh\` falls back to a species glyph (dog / cat / rabbit / paw) on a \`bg-sand\` disc.
- Missing \`description_wsh\` shows the brand-tinted \`档案待完善\` badge.
- Empty age / weight / unknown gender render \`—\` in \`text-muted\`; numeric cells use \`data-numeric="true"\`.
- Vaccination and sterilization render as dot flags — \`positive\` / \`informative\` when set, \`line\` when not.
- \`allergies_wsh\` is always surfaced in \`text-critical\` with a warning icon.
- Hover: \`-4px\` lift, \`shadow-lift\`, brand border, and the trailing arrow fades in. Nothing at rest.

## Usage

\`\`\`tsx
import { PetList } from 'components/PetList'

<PetList
  pets={pets}
  loading={isLoading}
  onView={id => navigate(\`/pets/\${id}\`)}
  onEdit={pet => openPetForm(pet)}
  onDelete={id => confirmDelete(id)}
  emptyAction={<Button onClick={openPetForm}>添加宠物</Button>}
/>
\`\`\`

## Exported helpers

\`petTypeLabel\`, \`genderLabel\`, \`formatAge\`, \`formatWeight\`, \`normalizeType\`, \`profileComplete\` — kept from the source so detail views format pet fields identically.
`,
  "PopupNotice": `# PopupNotice

Global announcement dialog, ported from \`src/components/common/PopupNotice.vue\`. Mounted once near the app root (the source mounts it in \`App.vue\`) and shows the popup-delivery notice queue one item at a time until every notice has been dismissed.

Presentational only — the caller owns loading and persistence. Fetch the queue (\`getPopupNotices()\` for signed-in users, \`getActiveNotices({ type: 'notice' })\` otherwise), filter it with the exported \`isPopupNotice\` guard, and persist dismissal inside \`onDismiss\` (\`dismissPopup(id)\` or a \`localStorage\` read flag for anonymous visitors).

## Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| \`notices\` | \`PopupNoticeItem[]\` | required | Popup queue. Renders nothing when empty. |
| \`onDismiss\` | \`(notice: PopupNoticeItem, index: number) => void\` | — | Fired for the close button, the primary action, \`Escape\`, and a scrim click. Persist the dismissal here. |
| \`onDismissAll\` | \`() => void\` | — | Fired once the queue is emptied. |
| \`badgeLabel\` | \`string\` | \`'公告'\` | Label in the terracotta pill above the title. |

### \`PopupNoticeItem\`

Field names match the API payload: \`id_wsh\`, \`title_wsh\`, \`content_wsh\`, and the optional \`type_wsh\` / \`delivery_type_wsh\` used by \`isPopupNotice\`.

## Behaviour

- One notice visible at a time; dismissing removes it from the queue and advances, clamping the index at the end.
- Counter (\`1 / n\`) and a \`下一条\` action appear only when more than one notice remains; a single notice shows \`我知道了\`.
- Content preserves line breaks (\`whitespace-pre-wrap\`) and scrolls at \`300px\`.
- Scrim click (target must be the scrim itself) and \`Escape\` dismiss the current notice.

## Usage

\`\`\`tsx
import { PopupNotice, isPopupNotice, type PopupNoticeItem } from 'components/PopupNotice'

const [notices, setNotices] = useState<PopupNoticeItem[]>([])

useEffect(() => {
  getPopupNotices().then((r) => {
    setNotices((r.data ?? []).filter(isPopupNotice))
  })
}, [])

<PopupNotice
  notices={notices}
  onDismiss={(notice) => dismissPopup(notice.id_wsh)}
/>
\`\`\`

## Notes

The source's scoped CSS relied on \`--color-card\` / \`--color-primary\` variables from a global stylesheet that isn't present here, so those rules are reproduced with this system's tokens (\`bg-surface\`, \`text-ink\`, \`bg-brand\`, \`text-ink-soft\`, \`rounded-tile\`, \`shadow-lift\`) at the original measurements. Entrance uses the shared \`animate-pop\` / \`animate-fade\` keyframes, so it collapses under \`prefers-reduced-motion\`.
`,
  "Reveal": `# Reveal

Scroll-reveal wrapper ported 1:1 from the product's \`src/components/common/Reveal.vue\`. It fades its children in with an 18px rise and a light blur when they enter the viewport, using an \`IntersectionObserver\` with \`rootMargin: '-10% 0px -12% 0px'\`.

This is the ONLY sanctioned scroll-reveal in Editorial Warm. Do not hand-roll another one.

## Behaviour

- Hidden at rest: \`opacity: 0\`, \`translateY(y)\`, \`blur(6px)\`.
- Revealed: \`opacity: 1\`, \`translateY(0)\`, \`blur(0)\` over 300ms on \`ease-editorial\` (\`cubic-bezier(0.23, 1, 0.32, 1)\`).
- \`prefers-reduced-motion: reduce\` → content renders visible immediately, no observer is created.
- No \`IntersectionObserver\` support → content renders visible immediately.
- Observer is disconnected on unmount.

## Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| \`tag\` | \`keyof JSX.IntrinsicElements\` | \`'div'\` | Element to render (\`div\`, \`span\`, \`li\`, \`section\`…). |
| \`delay\` | \`number\` | \`0\` | Transition delay in **seconds**. Use \`index * 0.05\`–\`0.07\` for staggered groups. |
| \`y\` | \`number\` | \`18\` | Vertical offset (px) before entering. |
| \`blur\` | \`boolean\` | \`true\` | Apply a 6px micro-blur before entering. |
| \`once\` | \`boolean\` | \`true\` | Play only once; when \`false\` it re-hides on exit and replays. |
| \`className\` | \`string\` | — | Classes on the rendered element. |
| \`style\` | \`CSSProperties\` | — | Merged after the reveal styles. |
| \`children\` | \`ReactNode\` | — | Content to reveal. |

## Usage

\`\`\`tsx
import { Reveal } from 'components/Reveal'

<Reveal delay={0.05}>
  <PageHeader title="服务" />
</Reveal>

// Staggered list
{items.map((item, index) => (
  <Reveal key={item.id} tag="li" delay={index * 0.07}>
    <ServiceRow item={item} />
  </Reveal>
))}

// Inline, larger offset, no blur
<Reveal tag="span" y={30} blur={false} delay={0.17}>
  <span>都被认真照护。</span>
</Reveal>
\`\`\`

## Notes

- The wrapper is \`display: block\` even as a \`span\` (matching the source's \`.rvl\` rule) — wrap inline text in an inner element if you need real inline flow.
- Keep total page entrance under ~500ms: header → primary → secondary → actions, roughly 70ms apart.
`,
  "ReviewDialog": `# ReviewDialog

Modal dialog for submitting a three-dimension service review on a completed order: **商家 / 看护人 / 服务**. Ported from \`src/components/order/ReviewDialog.vue\` with its logic preserved 1:1 (dimension derivation, first-undone tab selection, form reset on open, submit guards).

Each dimension is reviewed separately: pick a tab, choose a score (5→1), optionally write a comment, submit. Dimensions already reviewed (\`doneTypes\`) or missing a target id are surfaced as disabled with a short note.

## Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| \`visible\` | \`boolean\` | — | Controls visibility. Renders nothing when false. Opening resets score to 5, clears the comment, and selects the first un-reviewed dimension with a valid target. |
| \`order\` | \`ReviewDialogOrder \\| null\` | — | The order being reviewed. Reads \`id_wsh\`, \`merchant_id_wsh\` / \`merchant_name_wsh\`, \`keeper_id_wsh\` / \`keeper_name_wsh\`, \`service_id_wsh\` / \`service_name_wsh\`. Falls back to \`商家 #12\`-style labels when a name is missing. |
| \`doneTypes\` | \`ReviewDimensionType[]\` | \`[]\` | Dimension types already reviewed. Marked 已评价 and not submittable. |
| \`onClose\` | \`() => void\` | — | Fired by the scrim, 取消, and Escape. |
| \`onReviewed\` | \`(payload: ReviewSubmitPayload) => void \\| Promise<unknown>\` | — | Fired on submit. If it returns a promise the button stays in 提交中... until it settles. |

\`ReviewSubmitPayload\`: \`{ orderId, targetType, targetId, score, content }\`.
\`ReviewDimensionType\`: \`'merchant' | 'keeper' | 'service'\`.

## Usage

\`\`\`tsx
const [reviewOrder, setReviewOrder] = useState<ReviewDialogOrder | null>(null)

<ReviewDialog
  visible={!!reviewOrder}
  order={reviewOrder}
  doneTypes={reviewedDims}
  onClose={() => setReviewOrder(null)}
  onReviewed={async payload => {
    await api.createReview(payload)
    refreshOrders()
  }}
/>
\`\`\`

## Design notes

- Editorial Warm tokens only: \`bg-surface\`, \`border-line\`, \`text-ink\` / \`text-ink-soft\` / \`text-muted\`, \`bg-brand\` for the active tab and primary action.
- Entrance is \`animate-fade\` on the scrim plus \`animate-pop\` on the panel — no fly-in.
- Caps at \`92vh\` and docks as a bottom sheet below \`sm\`; \`rounded-card\` dialog on desktop.
- Score select carries \`data-numeric="true"\` for tabular numerals; long target names truncate via \`ref-truncate\`.
`,
  "SectionBand": `# SectionBand

The editorial section separator + heading block. Ported from
\`src/views/user/Services.vue\` (\`.s-results-head\` / \`.s-cat-title\` / \`.s-results-note\`):
44px top margin, 28px top padding, a 1px \`var(--ref-line)\` hairline on top, a
display title at \`clamp(20px, 2.6vw, 26px)\` (\`text-display-sm\`) and a 13px muted note.

Use it to break a long page into labelled regions — results headings, "按分类浏览",
sub-sections inside a catalogue — where a full \`PageHeader\` would be too loud and a
\`Card\` would be wrong. Structure comes from the hairline + spacing + type scale, per the
Editorial Warm guidelines; there is no background, border box or shadow.

## Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| \`title\` | \`ReactNode\` | — | Display title (required). Clamps to 2 lines. |
| \`note\` | \`ReactNode\` | — | 13px muted note under the title, e.g. a result count. |
| \`eyebrow\` | \`string\` | — | Optional uppercase eyebrow with a 32px hairline above the title. |
| \`liveNote\` | \`boolean\` | \`false\` | Adds \`aria-live="polite"\` to the note — use for counts that change with filters. |
| \`action\` | \`ReactNode\` | — | Trailing controls (sort, view switch), right-aligned, wraps on narrow viewports. |
| \`headingLevel\` | \`'h1' \\| 'h2' \\| 'h3' \\| 'h4'\` | \`'h2'\` | Heading element for the title; keep the page's heading order correct. |
| \`titleId\` | \`string\` | — | Id on the heading, for \`aria-labelledby\` on the parent \`<section>\`. |
| \`className\` | \`string\` | — | Extra classes on the band wrapper (e.g. spacing overrides). |

## Usage

\`\`\`tsx
import { SectionBand } from 'components/SectionBand'

<section aria-labelledby="service-results-title">
  <SectionBand
    titleId="service-results-title"
    title={selectedCategory || '全部照护方案'}
    note={loading ? '正在加载方案' : \`共 \${filtered.length} 套方案 · 价格与档期实时同步门店\`}
    liveNote
  />
  <ServiceGrid services={filtered} loading={loading} />
</section>
\`\`\`

With an eyebrow and a trailing control:

\`\`\`tsx
<SectionBand
  eyebrow="Catalogue"
  title="按分类浏览"
  note="选择分类，下方方案将同步更新"
  action={<Button variant="ghost">按价格排序</Button>}
/>
\`\`\`

## Notes

- Numbers inside \`note\` that need aligned digits should carry \`data-numeric="true"\`.
- Do not add decorative section numbering (\`01 / 02\`) to the title — business numbers are fine.
- The first section on a page should use \`PageHeader\`; \`SectionBand\` is for the breaks after it.
`,
  "SectionHeading": `# SectionHeading

Section opener for dashboard and marketing-style pages: a small label row on a hairline, an editorial serif title, optional supporting copy, and a bottom-aligned action on the right.

Ported from the product's \`src/components/dashboard/SectionHeading.vue\` (same structure, spacing, and 720px breakpoint behaviour). Its scoped CSS variables were mapped to this system's tokens (\`text-ink\`, \`text-ink-soft\`, \`text-muted\`, \`bg-line\`, \`text-cream\`, \`font-display\`), and the title uses \`font-display\` since \`Fraunces\` is not part of this system.

## When to use

- Above a services grid, order list, table, or stat band inside a \`PageShell\`.
- Use \`PageHeader\` instead for the page-level title; \`SectionHeading\` is for the sections beneath it.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| \`index\` | \`string\` | \`''\` | Small tabular number (e.g. \`"01"\`). Legacy — see caveat below. |
| \`label\` | \`string\` | \`''\` | Short section label next to the 24px hairline. |
| \`title\` | \`string\` | \`''\` | Section title. \`children\` wins when provided. |
| \`description\` | \`string\` | \`''\` | Supporting copy, max 560px. |
| \`tone\` | \`'light' \\| 'dark'\` | \`'light'\` | \`dark\` inverts ink for dark bands / photography. |
| \`action\` | \`ReactNode\` | — | Trailing link or button, bottom-aligned with the title block. |
| \`as\` | \`'h1' \\| 'h2' \\| 'h3' \\| 'h4'\` | \`'h2'\` | Keeps the document outline correct. |
| \`children\` | \`ReactNode\` | — | Title content, overrides \`title\`. |
| \`className\` | \`string\` | \`''\` | Extra classes on the wrapper. |

## Behaviour

- Desktop: flex row, \`items-end\`, \`justify-between\`, 40/24px gap, 48px bottom margin.
- Below 721px: stacks (title drops to a fixed 28px, description and action gain 12px top margin, bottom margin drops to 24px).
- Title balances with \`text-wrap: balance\` and uses \`-0.045em\` tracking at weight 400 — never bold.

## Usage

\`\`\`tsx
<SectionHeading
  label="照护方案"
  title="按毛孩子的节奏，选择合适的照护方式"
  description="寄养、洗护、上门陪伴与行为训练由同一套照护标准贯穿。"
  action={<Button variant="ghost">查看全部方案</Button>}
/>
\`\`\`

Dark band:

\`\`\`tsx
<div className="bg-ink p-8">
  <SectionHeading tone="dark" label="门店实拍" title="照护现场，随时可见" />
</div>
\`\`\`

## Caveat: \`index\`

The \`index\` prop is preserved for parity with the Vue source, but \`rules/editorial-warm-guidelines.md\` forbids decorative section numbering (\`01 服务介绍\`). Leave \`index\` unset on new screens and lead with \`label\` only. Business numbers (order ids, prices, counts) are unaffected.
`,
  "ServiceGrid": `# ServiceGrid

Responsive catalog grid of pet-service cards. Ported from \`src/components/dashboard/ServiceGrid.vue\`, with the Editorial Warm appearance that \`src/views/user/Services.vue\` (514-613) previously forced on via \`:deep()\` overrides now baked into the component — so Dashboard and Services render identically without page-level overrides.

Handles all three data states itself: loading skeletons, populated grid, empty state.

## Anatomy

- **Card** — \`rounded-card\` (18px) hairline \`border-line\` on \`bg-surface\`, no shadow at rest; hover lifts \`-4px\` with \`shadow-lift\` and a darker border.
- **Media** — 4/3 button on \`bg-sand\`, image scales to \`1.06\` on hover, \`ImageIcon\` + service name fallback on missing/broken images.
- **Body** — 20px padding; 11px/700 uppercase brand category, 20px \`font-display\` heading, merchant line with a 12%-brand distance chip, right-aligned 22px \`font-display\` price with unit.
- **Action bar** — hairline top border, text "查看详情" link with nudging arrow, optional favorite toggle, and an 11px-radius brand primary "立即预约" button. Stacks full-width below \`sm\`.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| \`services\` | \`ServiceGridItem[]\` | \`[]\` | Empty → empty state. |
| \`loading\` | \`boolean\` | \`false\` | Renders 6 skeleton cards. |
| \`isLoggedIn\` | \`boolean\` | \`false\` | Shows the favorite toggle (source: \`authStore.isLoggedIn\`). |
| \`favoriteIds\` | \`(string \\| number)[]\` | \`[]\` | Ids rendered as favorited. |
| \`onSelect\` | \`(id) => void\` | – | Media button + 查看详情 (source pushed \`/services/:id\`). |
| \`onBook\` | \`(service) => void\` | – | 立即预约 (source pushed \`/services/:id?book=1\`). |
| \`onToggleFavorite\` | \`(id) => void\` | – | Favorite toggle. |
| \`emptyTitle\` / \`emptyDescription\` | \`string\` | 没有找到匹配的服务 / 调整关键词或服务分类后再试试。 | Empty-state copy. |
| \`className\` | \`string\` | \`''\` | Extra classes on the root. |

\`ServiceGridItem\`: \`id_wsh\`, \`name_wsh\`, \`firstImage?\`, \`category_name_wsh?\`, \`merchant_name_wsh?\`, \`distance_m_wsh?\`, \`price_wsh?\`, \`unit_wsh?\`, \`description_wsh?\`.

Also exports the source helpers \`formatServiceDistance\`, \`formatMoney\`, and \`unitLabel\`.

## Usage

\`\`\`tsx
<ServiceGrid
  services={filteredServices}
  loading={loading}
  isLoggedIn={auth.isLoggedIn}
  favoriteIds={favorites}
  onSelect={id => navigate(\`/services/\${id}\`)}
  onBook={service => navigate(\`/services/\${service.id_wsh}?book=1\`)}
  onToggleFavorite={toggleFavorite}
/>
\`\`\`

## Notes

- Columns: 1 → 2 (\`sm\`) → 3 (\`lg\`), 18px gap on mobile and 24px (\`gap-gutter\`) above.
- Prices and distances carry \`data-numeric="true"\` for tabular numerals.
- Long merchant names truncate; descriptions clamp to 3 lines with a 72px min height so action bars stay aligned.
- Motion collapses under \`prefers-reduced-motion\` via the global guard in \`index.css\`.
`,
  "Skeleton": `# Skeleton

The only sanctioned loading placeholder in Editorial Warm. Never ship a bare \`加载中...\` / \`Loading...\` string — render a Skeleton region that matches the shape of the content that will replace it.

Ported from the product's repeated shimmer blocks (\`src/views/user/Services.vue\` \`.s-hero-skel\` / \`.service-skeleton\`, \`src/views/user/ServiceDetail.vue\` \`.d-skel-media\` / \`.d-skel-info\`, \`src/components/dashboard/ServiceGrid.vue\` markup). The fill is the source declaration verbatim:

\`\`\`
linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%)
background-size: 200% 100%
animation: ref-shimmer 1.3s linear infinite
\`\`\`

That declaration now exists **once** as the global \`.ref-shimmer\` utility in \`index.css\`, together with a single \`@keyframes ref-shimmer\` and the \`prefers-reduced-motion: reduce\` guard — do not re-declare it in a view.

## Exports

| Export | Use |
| --- | --- |
| \`Skeleton\` | Primitive shape: text line, block, media, circle |
| \`SkeletonText\` | Stack of ragged shimmer lines |
| \`SkeletonCard\` | Browsable-item placeholder (media + body lines) |
| \`SkeletonList\` | Loading region for a card grid, with \`role="status"\` |

## \`Skeleton\` props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| \`variant\` | \`'line' \\| 'block' \\| 'media' \\| 'circle'\` | \`'line'\` | Shape preset; sets default height/radius |
| \`width\` | \`string \\| number\` | \`100%\` (\`40px\` for circle) | Numbers become px |
| \`height\` | \`string \\| number\` | per variant (\`line\` 14px, \`block\` 120px) | Overrides \`aspect\` |
| \`radius\` | \`'none' \\| 'sm' \\| 'tile' \\| 'control' \\| 'card' \\| 'frame' \\| 'full'\` | per variant | Token radii only |
| \`aspect\` | \`string\` | \`'16 / 10'\` for \`media\` | e.g. \`'4 / 3'\`, \`'1 / 1'\` |
| \`tone\` | \`'shimmer' \\| 'static'\` | \`'shimmer'\` | \`static\` = flat \`bg-sand\` panel (source \`.d-skel-info\`) |
| \`className\` | \`string\` | — | Extra layout classes |

All primitives are \`aria-hidden\`; put the accessible label on the region (\`SkeletonList\` does this for you).

## \`SkeletonText\` props

\`lines\` (3), \`gap\` (14px), \`lastLineWidth\` (\`'76%'\`), \`className\`.

## \`SkeletonCard\` props

\`aspect\` (\`'16 / 10'\`), \`lines\` (3), \`className\`.

## \`SkeletonList\` props

\`count\` (6), \`label\` (\`'加载中'\`), \`gridClassName\`, \`className\`.

## Usage

\`\`\`tsx
import { Skeleton, SkeletonText, SkeletonList } from 'components/Skeleton'

// Region-level: a loading service catalogue
{loading ? <SkeletonList count={6} label="服务加载中" /> : <ServiceGrid items={items} />}

// Detail hero: shimmer media beside a flat secondary panel
<div className="grid grid-cols-1 gap-gutter md:grid-cols-[minmax(0,1.35fr)_minmax(0,1fr)]">
  <Skeleton variant="media" aspect="16 / 10" radius="frame" />
  <Skeleton variant="block" height={320} tone="static" />
</div>

// Inline copy
<SkeletonText lines={2} gap={10} lastLineWidth="42%" />
\`\`\`

## Rules

- Match the real content's shape and rhythm — same aspect ratio, same line count, same radii.
- Use \`tone="static"\` for secondary panels so only one element shimmers per region.
- Skeletons are for first load. Use an inline spinner or optimistic state for refreshes of already-visible data.
- Pair with \`EmptyState\` (no results) and \`ErrorState\` (failure); a data region must handle all three.
`,
  "StatList": `# StatList

Editorial hero statistics: a semantic \`<dl>\` of value/label pairs laid out in a wrapping flex row, separated by hairline dividers. Ported 1:1 from \`src/views/user/Services.vue\` (\`.s-stats\`).

Use it for supporting figures next to a \`PageHeader\`/hero, or as a compact overview band above a table. It is **not** a card grid — no borders, no background, no shadow.

## Anatomy

- Row: \`flex flex-wrap items-end\`, \`0 28px\` gaps (\`0 20px\` below 560px).
- Every item after the first: 1px \`border-line\` left rule + 28px left padding (20px below 560px).
- Value (\`<dd>\`): \`font-display\`, 30px / line-height 1, \`-0.01em\` tracking, \`text-ink\`, tabular numerals (26px below 560px).
- Label (\`<dt>\`): 12px \`text-muted\`, 8px top margin.

## Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| \`items\` | \`StatListItem[]\` | — | Stats to render. Renders nothing when empty. |
| \`loading\` | \`boolean\` | \`false\` | Replaces every value with the placeholder and sets \`aria-busy\`. |
| \`loadingPlaceholder\` | \`string\` | \`'--'\` | Placeholder used while loading. |
| \`label\` | \`string\` | — | Accessible name for the list (\`aria-label\`). |
| \`className\` | \`string\` | \`''\` | Extra classes on the \`<dl>\` (e.g. \`mt-9\`). |

\`StatListItem\`: \`{ id?: string; value: React.ReactNode; label: string }\`

## Usage

\`\`\`tsx
import { StatList } from 'components/StatList'

<StatList
  label="服务概览"
  className="mt-9"
  loading={loading}
  items={[
    { value: services.length, label: '可预约方案' },
    { value: categories.length, label: '服务分类' },
    { value: merchantCount, label: '覆盖门店' },
  ]}
/>
\`\`\`

## Notes

- Values carry \`data-numeric="true"\`, so numbers stay aligned across items.
- Keep labels to 2–5 characters/short words; long labels widen a column and break the hero rhythm.
- Three to four stats is the sweet spot; the row wraps beyond that.
`,
  "StatusBadge": `# StatusBadge

The system's single status pill. Ported from the product's confirmed \`.badge\` / \`.badge-{tone}\`
family (\`src/views/user/KeeperWorkflow.vue:643-655\`, cross-checked with \`KeeperApply.vue:489-503\`
and \`PetDetail.vue:674-692\`).

Use it for any status, category, or priority value: order status, review status, keeper online
status, ticket category, read/unread. It is the only sanctioned way to render a status value —
do not hand-roll a coloured pill.

## Appearance

Every tone is a restrained wash of its colour into the warm surface: \`color-mix()\` fill, matching
deep text, and a low-opacity border. 11px / weight 500 / 4px×10px / 6px radius / 6px gap.

**Warning must stay the amber wash** (\`#b45309\` at 10% fill, \`#b45309\` text, 26% border). The
\`background: var(--ref-brand); color: #fff\` variants in \`Complaints.vue:780\`, \`Payments.vue:378\`,
\`TicketDetail.vue:477\` and \`Tickets.vue:450\` are deviations — migrate them to \`tone="warning"\`.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| \`tone\` | \`'primary' \\| 'secondary' \\| 'success' \\| 'warning' \\| 'danger' \\| 'error' \\| 'info' \\| 'disabled'\` | \`'info'\` | Matches the source \`.badge-{tone}\` classes. \`danger\` and \`error\` are visually identical, as in the source. |
| \`dot\` | \`boolean\` | \`false\` | 5px tone-coloured dot before the label (online/offline/busy style). |
| \`icon\` | \`ReactNode\` | — | Optional leading \`lucide-react\` icon, auto-sized to 12px. |
| \`children\` | \`ReactNode\` | — | The label. Truncates with \`ref-truncate\` so long values never break a grid. |
| \`className\`, \`style\`, \`...rest\` | span attributes | — | Merged onto the root \`<span>\`. |

## Usage

\`\`\`tsx
import { StatusBadge, toneFromBadgeClass } from 'components/StatusBadge'

<StatusBadge tone="warning">待付款</StatusBadge>
<StatusBadge tone="success" dot>在线</StatusBadge>
<StatusBadge tone="danger">已取消</StatusBadge>
\`\`\`

### Migrating from \`statusMaps.js\`

\`toneFromBadgeClass()\` converts the legacy \`getStatusBadge(map, key)\` result (\`'badge-warning'\`,
\`'badge-info'\`, …) into a tone, defaulting to \`info\` exactly like the source helper:

\`\`\`tsx
<StatusBadge tone={toneFromBadgeClass(getStatusBadge(OrderStatus, order.status))}>
  {getStatusLabel(OrderStatus, order.status)}
</StatusBadge>
\`\`\`

## Guidance

- One badge per record row or card. Never stack multiple tones to imply severity.
- Keep labels short (2–5 characters in Chinese); the badge truncates rather than wraps.
- No solid brand fill, no gradients, no shadow — status colour is a wash, not a button.
- Dark mode is inherited: token-driven tones flip with \`html[data-theme="dark"]\`.
`,
  "TextLink": `# TextLink

Inline text link primitive, ported from \`src/components/dashboard/TextLink.vue\`. Used for secondary "go somewhere" actions next to section headings and inside records — never as a primary CTA (use \`Button\` for that).

Treatment: 13px medium body text, \`text-ink\` at rest, \`text-brand\` on hover; the label keeps a 25%-ink hairline underline that shifts to brand, and the trailing \`→\` nudges 4px right. All transitions are 150ms \`ease-editorial\`.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| \`children\` | \`ReactNode\` | — | Link label. |
| \`href\` | \`string\` | — | When set (and not disabled) renders an \`<a>\`; otherwise a \`<button type="button">\`. |
| \`target\` | \`string\` | — | Anchor target; \`rel="noreferrer noopener"\` is applied automatically for \`_blank\`. |
| \`rel\` | \`string\` | — | Overrides the auto \`rel\`. |
| \`showArrow\` | \`boolean\` | \`true\` | Hide the trailing arrow for in-prose links. |
| \`disabled\` | \`boolean\` | \`false\` | Button-only; removes hover treatment and dims to 45%. |
| \`onClick\` | \`(e) => void\` | — | Click handler. |
| \`className\` | \`string\` | \`''\` | Extra classes appended to the root. |

## Usage

\`\`\`tsx
import { TextLink } from 'components/TextLink'

// Section heading action
<TextLink onClick={() => navigate('/services')}>查看全部方案</TextLink>

// External link
<TextLink href="https://example.com" target="_blank">Documentation</TextLink>

// Inline in prose, no arrow
<TextLink showArrow={false} onClick={openOrder}>订单详情</TextLink>
\`\`\`
`,
  "ThemeToggle": `# ThemeToggle

Icon-only light/dark switch that lives in the layout chrome (user, merchant and admin headers). Ported from \`src/components/common/ThemeToggle.vue\` — same transparent square button, ink icon that turns terracotta on hover, and 18px sun/moon glyph.

Clicking it applies the theme the way the product's app store does: sets \`html[data-theme]\`, toggles the \`dark\` class, sets \`color-scheme\`, and persists to \`localStorage\` under \`pet-service-theme\`. That matches this design system's \`darkMode: html[data-theme="dark"]\` selector, so the whole system flips.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| \`theme\` | \`'light' \\| 'dark'\` | — | Controlled mode. Omit to let the toggle read/own the document theme. |
| \`onThemeChange\` | \`(theme: ThemeMode) => void\` | — | Called with the next theme after each toggle. |
| \`size\` | \`'sm' \\| 'md'\` | \`'sm'\` | \`sm\` = 36px (merchant/admin), \`md\` = 44px (user header controls). |
| \`disabled\` | \`boolean\` | \`false\` | Non-interactive state. |
| \`className\` | \`string\` | \`''\` | Extra classes for layout. |

Also exported: \`applyTheme(theme)\`, \`normalizeTheme(value)\`, and the \`ThemeMode\` type.

## Usage

\`\`\`tsx
import { ThemeToggle } from 'components/ThemeToggle'

// Uncontrolled — reads the current document theme on mount
<ThemeToggle />

// User header
<div className="flex items-center gap-2">
  <ThemeToggle size="md" />
</div>

// Controlled
const [theme, setTheme] = useState<ThemeMode>('light')
<ThemeToggle theme={theme} onThemeChange={setTheme} />
\`\`\`

## Notes

- Accessible label and \`title\` follow the source copy: \`切换到夜间主题\` / \`切换到日间主题\`, plus \`aria-pressed\` for the dark state.
- Keeps the source's restraint: no background fill, no shadow, colour-only 150ms transition.
`,
  "Timeline": `# Timeline

Vertical list of care/activity records — each record is a hairline card with a muted
\`type · datetime\` meta line, body copy, and an optional photo grid.

Ported from the product's \`KeeperDailyTab.vue\` timeline, which was previously styled
by the consuming view (\`KeeperWorkflow.vue\` \`:deep(.timeline*)\`). Those measured
values now live inside this component: 10px item gap, 8px top margin, \`--ref-line\`
hairline card on \`--ref-surface\` at 14px radius with 14px padding, 11.5px muted meta,
13px/1.6 \`ink-soft\` body, 88px auto-fill photo grid capped at 420px wide.

Use it for keeper daily records, order activity feeds, and customer-service
workflow histories. It is a record list, not a browsable card grid — no shadow,
no hover lift on the item itself.

## Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| \`items\` | \`TimelineItem[]\` | required | Records, newest first (order is not changed) |
| \`emptyLabel\` | \`string\` | \`'暂无动态'\` | Inline text when \`items\` is empty |
| \`loading\` | \`boolean\` | \`false\` | Renders shimmer skeleton rows instead of the list |
| \`loadingCount\` | \`number\` | \`3\` | Skeleton row count while \`loading\` |
| \`onImageClick\` | \`(url, item) => void\` | – | Makes photos focusable buttons (e.g. open a lightbox) |
| \`className\` | \`string\` | \`''\` | Extra classes on the list container |

### \`TimelineItem\`

| Field | Type | Description |
| --- | --- | --- |
| \`id\` | \`string \\| number\` | React key |
| \`type\` | \`string\` | Raw record type (\`feed\`, \`activity\`, \`medication\`, \`health\`, \`note\`) mapped through \`RECORD_TYPE_LABELS\` |
| \`typeLabel\` | \`string\` | Explicit meta label, overrides \`type\` |
| \`time\` | \`string \\| number \\| Date \\| null\` | Record time, rendered as \`toLocaleString()\`; falls back to \`-\` |
| \`content\` | \`string\` | Body copy |
| \`images\` | \`string \\| string[] \\| null\` | Comma-separated string (API shape) or array of urls |

Also exported: \`RECORD_TYPE_LABELS\`, \`parseImageUrls\`, \`formatDateTime\`.

## Usage

\`\`\`tsx
import { Timeline } from 'components/Timeline'

<Timeline
  items={careRecords.map(record => ({
    id: record.id_wsh,
    type: record.type_wsh,
    time: record.record_time_wsh || record.created_at_wsh,
    content: record.content_wsh,
    images: record.images_wsh,
  }))}
  loading={loadingRecords}
  onImageClick={url => openLightbox(url)}
/>
\`\`\`

Broken photos degrade to a \`sand\` placeholder tile rather than a torn image.
`,
  "TipDialog": `# TipDialog

Small modal for tipping a keeper after an order. Ported from \`src/components/order/TipDialog.vue\`; the logic is unchanged — both fields reset every time the dialog opens, and submission is blocked unless an amount and an order are present. The source used bare bootstrap-ish classes (\`.modal\`, \`.btn\`, \`.form-control\`) that don't exist here, so the visuals are rebased on the Editorial Warm tokens: \`bg-surface\` panel, \`border-line\` hairlines, \`rounded-card\`, \`shadow-lift\`, \`animate-pop\`, brand-filled confirm.

## Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| \`open\` | \`boolean\` | — | Whether the dialog is visible. Returns \`null\` when false. |
| \`order\` | \`TipDialogOrder \\| null\` | \`null\` | Order being tipped. \`id_wsh\` is passed back on confirm; optional \`serviceName\` shows as context. |
| \`onClose\` | \`() => void\` | — | Scrim click, cancel button, or Escape. |
| \`onTip\` | \`(orderId, amount: number, message: string) => void\` | — | Fired on 确认打赏. |
| \`submitting\` | \`boolean\` | \`false\` | Disables confirm and shows 打赏中…. |

## Usage

\`\`\`tsx
const [tipOrder, setTipOrder] = useState<TipDialogOrder | null>(null)

<TipDialog
  open={!!tipOrder}
  order={tipOrder}
  onClose={() => setTipOrder(null)}
  onTip={(id, amount, message) => submitTip(id, amount, message)}
/>
\`\`\`

## Notes

- Amount input carries \`data-numeric="true"\` for tabular numerals; \`min\` 0.01 / \`step\` 0.01 as in the source.
- Docks as a bottom sheet under \`sm\`, centred card above it; caps at \`92vh\` and scrolls.
- Focus moves to the amount field on open; Escape and scrim mousedown both dismiss.
`
};

export const componentRegistry: ComponentPreviewModule[] = [
__AmapAddressPickerPreviews,
__BannerCarouselPreviews,
__BreadcrumbPreviews,
__ButtonPreviews,
__CardPreviews,
__ChipPreviews,
__CreateOrderDialogPreviews,
__DataTablePreviews,
__DetailListPreviews,
__EmptyStatePreviews,
__ErrorStatePreviews,
__EyebrowPreviews,
__FavoriteToggleButtonPreviews,
__FilterToolbarPreviews,
__FormFieldPreviews,
__IconButtonPreviews,
__InputPreviews,
__LoadingSpinnerPreviews,
__LoginPromptDialogPreviews,
__LogoPreviews,
__MediaFramePreviews,
__MediaWithFallbackPreviews,
__MessageIndicatorPreviews,
__ModalPreviews,
__OrderCardPreviews,
__PageHeaderPreviews,
__PageShellPreviews,
__PetFormDialogPreviews,
__PetListPreviews,
__PopupNoticePreviews,
__RevealPreviews,
__ReviewDialogPreviews,
__SectionBandPreviews,
__SectionHeadingPreviews,
__ServiceGridPreviews,
__SkeletonPreviews,
__StatListPreviews,
__StatusBadgePreviews,
__TextLinkPreviews,
__ThemeTogglePreviews,
__TimelinePreviews,
__TipDialogPreviews].

filter((m) => m && m.componentName).
map((m) => ({ ...m, contextMd: __contextMd[m.componentName] ?? m.contextMd })).
sort((a, b) => a.componentName.localeCompare(b.componentName));