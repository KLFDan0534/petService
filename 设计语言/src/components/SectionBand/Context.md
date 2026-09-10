# SectionBand

The editorial section separator + heading block. Ported from
`src/views/user/Services.vue` (`.s-results-head` / `.s-cat-title` / `.s-results-note`):
44px top margin, 28px top padding, a 1px `var(--ref-line)` hairline on top, a
display title at `clamp(20px, 2.6vw, 26px)` (`text-display-sm`) and a 13px muted note.

Use it to break a long page into labelled regions — results headings, "按分类浏览",
sub-sections inside a catalogue — where a full `PageHeader` would be too loud and a
`Card` would be wrong. Structure comes from the hairline + spacing + type scale, per the
Editorial Warm guidelines; there is no background, border box or shadow.

## Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `title` | `ReactNode` | — | Display title (required). Clamps to 2 lines. |
| `note` | `ReactNode` | — | 13px muted note under the title, e.g. a result count. |
| `eyebrow` | `string` | — | Optional uppercase eyebrow with a 32px hairline above the title. |
| `liveNote` | `boolean` | `false` | Adds `aria-live="polite"` to the note — use for counts that change with filters. |
| `action` | `ReactNode` | — | Trailing controls (sort, view switch), right-aligned, wraps on narrow viewports. |
| `headingLevel` | `'h1' \| 'h2' \| 'h3' \| 'h4'` | `'h2'` | Heading element for the title; keep the page's heading order correct. |
| `titleId` | `string` | — | Id on the heading, for `aria-labelledby` on the parent `<section>`. |
| `className` | `string` | — | Extra classes on the band wrapper (e.g. spacing overrides). |

## Usage

```tsx
import { SectionBand } from 'components/SectionBand'

<section aria-labelledby="service-results-title">
  <SectionBand
    titleId="service-results-title"
    title={selectedCategory || '全部照护方案'}
    note={loading ? '正在加载方案' : `共 ${filtered.length} 套方案 · 价格与档期实时同步门店`}
    liveNote
  />
  <ServiceGrid services={filtered} loading={loading} />
</section>
```

With an eyebrow and a trailing control:

```tsx
<SectionBand
  eyebrow="Catalogue"
  title="按分类浏览"
  note="选择分类，下方方案将同步更新"
  action={<Button variant="ghost">按价格排序</Button>}
/>
```

## Notes

- Numbers inside `note` that need aligned digits should carry `data-numeric="true"`.
- Do not add decorative section numbering (`01 / 02`) to the title — business numbers are fine.
- The first section on a page should use `PageHeader`; `SectionBand` is for the breaks after it.
