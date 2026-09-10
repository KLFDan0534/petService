# Eyebrow

The small uppercase kicker that sits above page and section titles in Editorial Warm: a 32×1px `--ref-line` hairline, a 10px gap, then a 9.5px uppercase label at 0.28em tracking in `--ref-muted`.

Ported 1:1 from `.s-eyebrow` in `src/views/user/Services.vue` (lines 256–272), where it appears above the hero title and the catalogue section heading.

## Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `children` | `ReactNode` | — | The label text. Rendered uppercase. |
| `line` | `boolean` | `true` | Show the leading 32px hairline. |
| `tone` | `'muted' \| 'brand'` | `'muted'` | Label colour. `brand` for card eyebrows. |
| `as` | `'div' \| 'p' \| 'span'` | `'div'` | Rendered element. Use `p` inside section headers. |
| `decorative` | `boolean` | `false` | Sets `aria-hidden` on the whole eyebrow — use when the label duplicates the adjacent heading. |
| `className` | `string` | `''` | Extra classes for layout only. |

## Usage

```tsx
<Eyebrow decorative>Services</Eyebrow>
<h1 className="mt-5 font-display text-display-xl text-ink">
  为你的爱宠，找到合适的照护服务
</h1>
```

Section header, label-only, and brand variants:

```tsx
<Eyebrow as="p">Catalogue</Eyebrow>
<Eyebrow line={false}>Membership</Eyebrow>
<Eyebrow tone="brand">Featured</Eyebrow>
```

## Notes

- Keep labels short (one or two words) — long labels truncate rather than wrap.
- Never use it as a decorative numbering device (`01 / 02 / 03`); that pattern is banned by the brand rules.
- Pair with `SectionHeading` / `PageHeader` for the standard 12–20px gap to the title.
