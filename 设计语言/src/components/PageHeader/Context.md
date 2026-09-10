# PageHeader

The editorial page hero, ported from the Services view (`src/views/user/Services.vue`). Left-aligned, asymmetric: an eyebrow + display title + capped subtitle + optional stat row and action cluster, with an optional framed media column on the right.

Use exactly one per route, above the page's sections.

## Layout

- Two columns `minmax(0, 1.2fr) / minmax(0, 1fr)` with a 48px gap above 900px; collapses to one column with a 32px gap below it.
- Padding: `48px 0 40px` desktop, `34px 0 28px` mobile.
- Title `clamp(34px, 4.4vw, 52px)`, serif, weight 500, `-0.01em`, `text-wrap: balance`.
- Subtitle 15px / 1.8 (14px under 560px), capped at 560px, `ink-soft` at 82% opacity.
- Stats: serif 30px (26px mobile) tabular numerals, 12px muted labels, hairline divider before every stat after the first.
- Media frame: 4/3, `rounded-frame` (26px), 1px `line` border, images cover.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| `title` | `ReactNode` | — | Required. Pass a fragment with `<br className="hidden min-[900px]:block" />` to control desktop-only line breaks. |
| `eyebrow` | `string` | — | Uppercase kicker after a 32px hairline. |
| `subtitle` | `ReactNode` | — | Supporting copy, capped at 560px. |
| `stats` | `{ value: ReactNode; label: string }[]` | — | Optional stat row. |
| `statsLabel` | `string` | `'概览'` | Accessible name for the `<dl>`. |
| `actions` | `ReactNode` | — | Action cluster under the stats. |
| `media` | `ReactNode` | — | Right column content (usually an `<img>` or `MediaFrame`). Omit for a single-column header. |
| `breadcrumb` | `ReactNode` | — | Rendered in a `<nav aria-label="面包屑">` above the hero. |
| `loading` | `boolean` | `false` | Stats render `--`; the media frame shows the `ref-shimmer` skeleton. |
| `className` | `string` | `''` | Applied to the outer `<header>`. |

## Usage

```tsx
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
```

Copy-only header for records and settings pages:

```tsx
<PageHeader
  eyebrow="Orders"
  title="订单管理"
  subtitle="查看全部预约订单的状态、金额与门店归属。"
  actions={<Button>新建订单</Button>}
/>
```

## Don't

- Don't center the stack or add a background gradient/glow — the hero is left-aligned, canvas-on-canvas.
- Don't set the title to weight 700+, or use a second heading font.
- Don't decorate with `01 / 02` numbering; business numbers belong in `stats`.
