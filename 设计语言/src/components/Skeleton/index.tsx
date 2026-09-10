import React from 'react';

/**
 * Skeleton — the single sanctioned loading placeholder.
 *
 * Ported from the product's shimmer blocks:
 *  - src/views/user/Services.vue (.s-hero-skel, .service-skeleton .skeleton-media/.skeleton-body span)
 *  - src/views/user/ServiceDetail.vue (.d-skel-media, .d-skel-info)
 *  - src/components/dashboard/ServiceGrid.vue (skeleton markup structure)
 *
 * The source repeated
 *   background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%);
 *   background-size: 200% 100%;
 *   animation: ref-shimmer 1.3s linear infinite;
 * in every view. That declaration now lives once, globally, as the `.ref-shimmer`
 * utility in index.css (with the `@keyframes ref-shimmer` block and the
 * `prefers-reduced-motion` guard), so this component simply consumes it.
 */

export type SkeletonVariant = 'line' | 'block' | 'media' | 'circle';

export type SkeletonRadius =
'none' |
'sm' |
'tile' |
'control' |
'card' |
'frame' |
'full';

export type SkeletonTone = 'shimmer' | 'static';

export interface SkeletonProps extends React.HTMLAttributes<HTMLDivElement> {
  /** Shape preset. `line` for text, `block` for panels, `media` for images, `circle` for avatars. */
  variant?: SkeletonVariant;
  /** CSS width, e.g. `'58%'` or `120`. Defaults per variant. */
  width?: string | number;
  /** CSS height, e.g. `'320px'` or `14`. Defaults per variant. */
  height?: string | number;
  /** Corner radius token. Defaults per variant. */
  radius?: SkeletonRadius;
  /** Aspect ratio for `media`, e.g. `'16 / 10'`, `'4 / 3'`. Ignored when `height` is set. */
  aspect?: string;
  /** `shimmer` animates; `static` is the flat sand fill used for secondary panels. */
  tone?: SkeletonTone;
  className?: string;
}

const RADIUS_CLASS: Record<SkeletonRadius, string> = {
  none: 'rounded-none',
  sm: 'rounded-[4px]',
  tile: 'rounded-tile',
  control: 'rounded-control',
  card: 'rounded-card',
  frame: 'rounded-frame',
  full: 'rounded-full'
};

const DEFAULT_RADIUS: Record<SkeletonVariant, SkeletonRadius> = {
  line: 'sm',
  block: 'card',
  media: 'card',
  circle: 'full'
};

function toCssSize(value: string | number | undefined): string | undefined {
  if (value === undefined) return undefined;
  return typeof value === 'number' ? `${value}px` : value;
}

export function Skeleton({
  variant = 'line',
  width,
  height,
  radius,
  aspect,
  tone = 'shimmer',
  className = '',
  style,
  ...rest
}: SkeletonProps) {
  const resolvedRadius = RADIUS_CLASS[radius ?? DEFAULT_RADIUS[variant]];
  const fill = tone === 'shimmer' ? 'ref-shimmer' : 'bg-sand';

  const resolvedStyle: React.CSSProperties = {
    width: toCssSize(width) ?? (variant === 'circle' ? '40px' : '100%'),
    height:
    toCssSize(height) ?? (
    variant === 'line' ?
    '14px' :
    variant === 'circle' ?
    toCssSize(width) ?? '40px' :
    variant === 'block' ?
    '120px' :
    undefined),
    aspectRatio:
    variant === 'media' && height === undefined ? aspect ?? '16 / 10' : undefined,
    ...style
  };

  return (
    <div
      aria-hidden="true"
      className={`${fill} ${resolvedRadius} ${className}`.trim()}
      style={resolvedStyle}
      {...rest} />);


}

export interface SkeletonTextProps {
  /** Number of shimmer lines. */
  lines?: number;
  /** Gap between lines in px. Source used 14px. */
  gap?: number;
  /** Width of the final line — text blocks read better ragged. */
  lastLineWidth?: string;
  className?: string;
}

export function SkeletonText({
  lines = 3,
  gap = 14,
  lastLineWidth = '76%',
  className = ''
}: SkeletonTextProps) {
  return (
    <div className={`grid ${className}`.trim()} style={{ gap: `${gap}px` }}>
      {Array.from({ length: lines }).map((_, index) =>
      <Skeleton
        key={index}
        variant="line"
        width={
        index === 0 ?
        '58%' :
        index === lines - 1 ?
        lastLineWidth :
        '100%'
        } />

      )}
    </div>);

}

export interface SkeletonCardProps {
  /** Media aspect ratio. Source cards used 16 / 10. */
  aspect?: string;
  /** Body text lines. */
  lines?: number;
  className?: string;
}

/** Mirrors the source's `.service-skeleton` card: shimmer media + three body lines. */
export function SkeletonCard({
  aspect = '16 / 10',
  lines = 3,
  className = ''
}: SkeletonCardProps) {
  return (
    <article
      aria-hidden="true"
      className={`overflow-hidden rounded-card border border-line bg-surface ${className}`.trim()}>
      
      <Skeleton variant="media" aspect={aspect} radius="none" />
      <div className="p-5">
        <SkeletonText lines={lines} />
      </div>
    </article>);

}

export interface SkeletonListProps {
  /** How many card placeholders to render. Source rendered 6. */
  count?: number;
  /** Accessible status label announced while the region loads. */
  label?: string;
  /** Tailwind grid classes for the placeholder grid. */
  gridClassName?: string;
  className?: string;
}

/** Loading region for a browsable grid — replaces any bare `加载中...` text. */
export function SkeletonList({
  count = 6,
  label = '加载中',
  gridClassName = 'grid grid-cols-1 gap-gutter sm:grid-cols-2 lg:grid-cols-3',
  className = ''
}: SkeletonListProps) {
  return (
    <div
      role="status"
      aria-busy="true"
      aria-label={label}
      className={`${gridClassName} ${className}`.trim()}>
      
      {Array.from({ length: count }).map((_, index) =>
      <SkeletonCard key={index} />
      )}
    </div>);

}