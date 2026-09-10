# Reveal

Scroll-reveal wrapper ported 1:1 from the product's `src/components/common/Reveal.vue`. It fades its children in with an 18px rise and a light blur when they enter the viewport, using an `IntersectionObserver` with `rootMargin: '-10% 0px -12% 0px'`.

This is the ONLY sanctioned scroll-reveal in Editorial Warm. Do not hand-roll another one.

## Behaviour

- Hidden at rest: `opacity: 0`, `translateY(y)`, `blur(6px)`.
- Revealed: `opacity: 1`, `translateY(0)`, `blur(0)` over 300ms on `ease-editorial` (`cubic-bezier(0.23, 1, 0.32, 1)`).
- `prefers-reduced-motion: reduce` → content renders visible immediately, no observer is created.
- No `IntersectionObserver` support → content renders visible immediately.
- Observer is disconnected on unmount.

## Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `tag` | `keyof JSX.IntrinsicElements` | `'div'` | Element to render (`div`, `span`, `li`, `section`…). |
| `delay` | `number` | `0` | Transition delay in **seconds**. Use `index * 0.05`–`0.07` for staggered groups. |
| `y` | `number` | `18` | Vertical offset (px) before entering. |
| `blur` | `boolean` | `true` | Apply a 6px micro-blur before entering. |
| `once` | `boolean` | `true` | Play only once; when `false` it re-hides on exit and replays. |
| `className` | `string` | — | Classes on the rendered element. |
| `style` | `CSSProperties` | — | Merged after the reveal styles. |
| `children` | `ReactNode` | — | Content to reveal. |

## Usage

```tsx
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
```

## Notes

- The wrapper is `display: block` even as a `span` (matching the source's `.rvl` rule) — wrap inline text in an inner element if you need real inline flow.
- Keep total page entrance under ~500ms: header → primary → secondary → actions, roughly 70ms apart.
