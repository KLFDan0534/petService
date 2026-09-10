# ThemeToggle

Icon-only light/dark switch that lives in the layout chrome (user, merchant and admin headers). Ported from `src/components/common/ThemeToggle.vue` — same transparent square button, ink icon that turns terracotta on hover, and 18px sun/moon glyph.

Clicking it applies the theme the way the product's app store does: sets `html[data-theme]`, toggles the `dark` class, sets `color-scheme`, and persists to `localStorage` under `pet-service-theme`. That matches this design system's `darkMode: html[data-theme="dark"]` selector, so the whole system flips.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| `theme` | `'light' \| 'dark'` | — | Controlled mode. Omit to let the toggle read/own the document theme. |
| `onThemeChange` | `(theme: ThemeMode) => void` | — | Called with the next theme after each toggle. |
| `size` | `'sm' \| 'md'` | `'sm'` | `sm` = 36px (merchant/admin), `md` = 44px (user header controls). |
| `disabled` | `boolean` | `false` | Non-interactive state. |
| `className` | `string` | `''` | Extra classes for layout. |

Also exported: `applyTheme(theme)`, `normalizeTheme(value)`, and the `ThemeMode` type.

## Usage

```tsx
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
```

## Notes

- Accessible label and `title` follow the source copy: `切换到夜间主题` / `切换到日间主题`, plus `aria-pressed` for the dark state.
- Keeps the source's restraint: no background fill, no shadow, colour-only 150ms transition.
