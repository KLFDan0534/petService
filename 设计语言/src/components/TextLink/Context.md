# TextLink

Inline text link primitive, ported from `src/components/dashboard/TextLink.vue`. Used for secondary "go somewhere" actions next to section headings and inside records — never as a primary CTA (use `Button` for that).

Treatment: 13px medium body text, `text-ink` at rest, `text-brand` on hover; the label keeps a 25%-ink hairline underline that shifts to brand, and the trailing `→` nudges 4px right. All transitions are 150ms `ease-editorial`.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| `children` | `ReactNode` | — | Link label. |
| `href` | `string` | — | When set (and not disabled) renders an `<a>`; otherwise a `<button type="button">`. |
| `target` | `string` | — | Anchor target; `rel="noreferrer noopener"` is applied automatically for `_blank`. |
| `rel` | `string` | — | Overrides the auto `rel`. |
| `showArrow` | `boolean` | `true` | Hide the trailing arrow for in-prose links. |
| `disabled` | `boolean` | `false` | Button-only; removes hover treatment and dims to 45%. |
| `onClick` | `(e) => void` | — | Click handler. |
| `className` | `string` | `''` | Extra classes appended to the root. |

## Usage

```tsx
import { TextLink } from 'components/TextLink'

// Section heading action
<TextLink onClick={() => navigate('/services')}>查看全部方案</TextLink>

// External link
<TextLink href="https://example.com" target="_blank">Documentation</TextLink>

// Inline in prose, no arrow
<TextLink showArrow={false} onClick={openOrder}>订单详情</TextLink>
```
