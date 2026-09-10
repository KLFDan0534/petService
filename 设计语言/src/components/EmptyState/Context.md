# EmptyState

The empty-region placeholder for any list, table, or card grid that has no data. Ported from `src/components/common/EmptyState.vue` — same API (`icon` / `title` / `description` + default slot), so all existing consumers work unchanged. Two presentational fixes: the hardcoded `📭` emoji default is replaced with a line-art `lucide-react` mark, and the measured shell from `Services.vue` (`--ref-surface` fill, 1px dashed `--ref-line`, 18px radius, muted body, display-font heading, brand-tinted icon) is now the component's own styling.

Never leave a data region blank or print a bare `暂无数据` — use this component.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| `icon` | `ReactNode` | line-art inbox icon | Pass a `lucide-react` icon. No emoji. |
| `title` | `string` | — | Serif display heading (`text-display-xs`, ink). Omitted when absent. |
| `description` | `string` | — | 14px muted line, clamped to `max-w-prose`. |
| `children` | `ReactNode` | — | Action slot below the copy (usually one `Button`). |
| `className` | `string` | `''` | Extra classes on the outer shell. |

## Usage

```tsx
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
```

## Notes

- Pair with `Skeleton` (loading) and `ErrorState` (failure) so every data region covers all three states.
- Keep copy short: one headline, one next step.
