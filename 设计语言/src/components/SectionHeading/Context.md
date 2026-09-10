# SectionHeading

Section opener for dashboard and marketing-style pages: a small label row on a hairline, an editorial serif title, optional supporting copy, and a bottom-aligned action on the right.

Ported from the product's `src/components/dashboard/SectionHeading.vue` (same structure, spacing, and 720px breakpoint behaviour). Its scoped CSS variables were mapped to this system's tokens (`text-ink`, `text-ink-soft`, `text-muted`, `bg-line`, `text-cream`, `font-display`), and the title uses `font-display` since `Fraunces` is not part of this system.

## When to use

- Above a services grid, order list, table, or stat band inside a `PageShell`.
- Use `PageHeader` instead for the page-level title; `SectionHeading` is for the sections beneath it.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| `index` | `string` | `''` | Small tabular number (e.g. `"01"`). Legacy — see caveat below. |
| `label` | `string` | `''` | Short section label next to the 24px hairline. |
| `title` | `string` | `''` | Section title. `children` wins when provided. |
| `description` | `string` | `''` | Supporting copy, max 560px. |
| `tone` | `'light' \| 'dark'` | `'light'` | `dark` inverts ink for dark bands / photography. |
| `action` | `ReactNode` | — | Trailing link or button, bottom-aligned with the title block. |
| `as` | `'h1' \| 'h2' \| 'h3' \| 'h4'` | `'h2'` | Keeps the document outline correct. |
| `children` | `ReactNode` | — | Title content, overrides `title`. |
| `className` | `string` | `''` | Extra classes on the wrapper. |

## Behaviour

- Desktop: flex row, `items-end`, `justify-between`, 40/24px gap, 48px bottom margin.
- Below 721px: stacks (title drops to a fixed 28px, description and action gain 12px top margin, bottom margin drops to 24px).
- Title balances with `text-wrap: balance` and uses `-0.045em` tracking at weight 400 — never bold.

## Usage

```tsx
<SectionHeading
  label="照护方案"
  title="按毛孩子的节奏，选择合适的照护方式"
  description="寄养、洗护、上门陪伴与行为训练由同一套照护标准贯穿。"
  action={<Button variant="ghost">查看全部方案</Button>}
/>
```

Dark band:

```tsx
<div className="bg-ink p-8">
  <SectionHeading tone="dark" label="门店实拍" title="照护现场，随时可见" />
</div>
```

## Caveat: `index`

The `index` prop is preserved for parity with the Vue source, but `rules/editorial-warm-guidelines.md` forbids decorative section numbering (`01 服务介绍`). Leave `index` unset on new screens and lead with `label` only. Business numbers (order ids, prices, counts) are unaffected.
