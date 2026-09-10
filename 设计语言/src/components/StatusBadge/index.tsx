import React from 'react';

/**
 * StatusBadge — ported 1:1 from the product's confirmed `.badge` / `.badge-{tone}`
 * family (src/views/user/KeeperWorkflow.vue:643-655, cross-checked against
 * KeeperApply.vue:489-503 and PetDetail.vue:674-692).
 *
 * The source declares these rules in a `<style scoped>` block that does not exist
 * in this artifact, so the tone rules are reproduced verbatim as inline styles
 * (identical `color-mix()` recipes, identical `--ref-*` token references) and the
 * layout half of `.badge` is expressed with the equivalent Tailwind utilities.
 *
 * Warning is intentionally the restrained amber wash into the warm surface —
 * never a solid `--ref-brand` fill (the Complaints / Payments / Tickets /
 * TicketDetail deviations).
 */

export type StatusBadgeTone =
'primary' |
'secondary' |
'success' |
'warning' |
'danger' |
'error' |
'info' |
'disabled';

export interface StatusBadgeProps extends
  Omit<React.HTMLAttributes<HTMLSpanElement>, 'children'> {
  /** Semantic tone. Mirrors the source's `.badge-{tone}` classes. */
  tone?: StatusBadgeTone;
  /** Optional leading icon (lucide-react). Sits in the source's 6px gap. */
  icon?: React.ReactNode;
  /** Renders a 5px tone-coloured dot before the label. */
  dot?: boolean;
  /** Badge label. */
  children?: React.ReactNode;
}

/* Verbatim tone recipes from KeeperWorkflow.vue:649-655 */
const TONE_STYLES: Record<StatusBadgeTone, React.CSSProperties> = {
  success: {
    background: 'color-mix(in srgb, var(--ref-moss, #3f5347) 12%, transparent)',
    color: 'var(--ref-moss, #3f5347)',
    borderColor: 'color-mix(in srgb, var(--ref-moss, #3f5347) 25%, transparent)'
  },
  warning: {
    background: 'color-mix(in srgb, #b45309 10%, transparent)',
    color: '#b45309',
    borderColor: 'color-mix(in srgb, #b45309 26%, transparent)'
  },
  danger: {
    background: 'color-mix(in srgb, #b3402a 8%, transparent)',
    color: 'color-mix(in srgb, #b3402a 92%, transparent)',
    borderColor: 'color-mix(in srgb, #b3402a 28%, transparent)'
  },
  error: {
    background: 'color-mix(in srgb, #b3402a 8%, transparent)',
    color: 'color-mix(in srgb, #b3402a 92%, transparent)',
    borderColor: 'color-mix(in srgb, #b3402a 28%, transparent)'
  },
  info: {
    background: 'color-mix(in srgb, var(--ref-brand) 10%, transparent)',
    color: 'var(--ref-brand-deep)',
    borderColor: 'color-mix(in srgb, var(--ref-brand) 22%, transparent)'
  },
  primary: {
    background: 'color-mix(in srgb, var(--ref-brand) 12%, transparent)',
    color: 'var(--ref-brand-deep)',
    borderColor: 'color-mix(in srgb, var(--ref-brand) 28%, transparent)'
  },
  secondary: {
    background: 'color-mix(in srgb, var(--ref-ink) 6%, transparent)',
    color: 'var(--ref-ink-soft)',
    borderColor: 'color-mix(in srgb, var(--ref-ink) 18%, transparent)'
  },
  disabled: {
    background: 'var(--ref-sand)',
    color: 'var(--ref-muted)',
    borderColor: 'var(--ref-line)'
  }
};

/* .badge layout — border-radius is the page-level `--r-tag: 6px` */
const BASE_CLASS =
'inline-flex items-center gap-1.5 rounded-[6px] border border-solid border-transparent px-2.5 py-1 text-[11px] font-medium leading-none whitespace-nowrap align-middle max-w-full';

export function StatusBadge({
  tone = 'info',
  icon,
  dot = false,
  children,
  className = '',
  style,
  ...rest
}: StatusBadgeProps) {
  return (
    <span
      className={`${BASE_CLASS} ${className}`.trim()}
      style={{ ...TONE_STYLES[tone], ...style }}
      {...rest}>
      
      {dot ?
      <span
        aria-hidden="true"
        className="h-[5px] w-[5px] shrink-0 rounded-full bg-current" /> :

      null}
      {icon ?
      <span aria-hidden="true" className="inline-flex shrink-0 items-center [&>svg]:h-3 [&>svg]:w-3">
          {icon}
        </span> :
      null}
      <span className="ref-truncate">{children}</span>
    </span>);

}

/**
 * Maps a legacy `badge-*` class name (as returned by the product's
 * `getStatusBadge(map, key)` in src/constants/statusMaps.js) to a tone.
 * Falls back to `info`, matching the source's default.
 */
export function toneFromBadgeClass(badgeClass?: string | null): StatusBadgeTone {
  const tone = (badgeClass ?? '').replace(/^badge-/, '') as StatusBadgeTone;
  return tone in TONE_STYLES ? tone : 'info';
}