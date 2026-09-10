# Button

The consolidated `.cta` family that was duplicated across ~30 product views. This is the only button primitive — do not re-declare local `.cta` / `.btn` rules in a screen.

Ported from `src/views/user/CustomerServiceApply.vue:479`, `KeeperWorkflow.vue:631-639` and `Login.vue:277-289`. The per-view `--r-btn: 10px` alias is normalised to the canonical **11px** (`rounded-control`).

## Anatomy

- 42px height, `0 18px` padding, 11px radius, 13px / weight 500 (`md`, canonical)
- `inline-flex` with an 8px gap, 1px transparent border so outline/danger don't shift
- Hover: `translateY(-1px)` plus the variant's colour change, 150ms `ease-editorial`
- Disabled: `opacity: .5`, `cursor: not-allowed`, transform cancelled
- Focus-visible: the system focus ring (`shadow-focus`)
- 42px is deliberately distinct from the 44px chip/input scale

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| `variant` | `'primary' \| 'outline' \| 'dark' \| 'light' \| 'ghost' \| 'quiet' \| 'danger'` | `'primary'` | See table below |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | 34px / 42px / 48px |
| `arrow` | `boolean` | `false` | Trailing arrow that nudges 3px on hover (`.cta-arrow`) |
| `icon` | `ReactNode` | — | Leading slot; pass a `lucide-react` icon at 15px |
| `loading` | `boolean` | `false` | Spinner in the leading slot, control disabled, `aria-busy` |
| `fullWidth` | `boolean` | `true` when `size="lg"` | Stretch to container |
| `href` | `string` | — | Renders an `<a>` instead of a `<button>` |
| `disabled`, `type`, `onClick`, … | native button props | — | Forwarded |

## Variants

| Variant | Use |
| --- | --- |
| `primary` | The one committing action per view — `brand` on white, hover `brand-deep` |
| `outline` | Secondary action — `surface` fill, `line` border, border darkens on hover |
| `dark` | Auth and full-bleed submits — `ink` on `cream`, hover brightens |
| `light` | Only over dark sections — fixed cream on fixed paper, hover white |
| `ghost` | Tertiary/back links — transparent, text goes `brand` on hover |
| `quiet` | Dismissals next to a primary (Cancel) — transparent, text goes `ink` |
| `danger` | Destructive — tinted `critical` fill, `critical` text and border |

## Usage

```tsx
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
```

## Rules

- One `primary` per view. Everything else is `outline`, `ghost` or `quiet`.
- Never scale a button on hover, add a shadow, or use a gradient fill.
- Use `loading` rather than swapping the button for a spinner — the control keeps its footprint.
- Icons come from `lucide-react`; never an emoji glyph.
