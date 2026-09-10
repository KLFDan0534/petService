import React from 'react';

export interface ChipProps extends
  Omit<React.ButtonHTMLAttributes<HTMLButtonElement>, 'children'> {
  /** Chip label, e.g. a category name. */
  label: React.ReactNode;
  /** Optional trailing count. Rendered with tabular numerals at 11px / 0.6 opacity. */
  count?: number | string;
  /** Selected state — inverts to solid ink with cream text. */
  active?: boolean;
  /** Optional leading icon (lucide-react). */
  icon?: React.ReactNode;
}

/**
 * Chip — the 44px filter control from the services catalogue rail.
 * Ported from `src/views/user/Services.vue` (`.s-chip`).
 */
export function Chip({
  label,
  count,
  active = false,
  icon,
  className = '',
  disabled,
  type = 'button',
  ...rest
}: ChipProps) {
  const base =
  'inline-flex flex-none items-center gap-1.5 h-11 px-4 rounded-control border text-control font-medium transition-[background-color,border-color,color] duration-fast ease-editorial focus-visible:shadow-focus';

  const tone = active ?
  'bg-ink border-ink text-cream' :
  'bg-surface border-line text-ink-soft hover:border-[color-mix(in_srgb,var(--ref-ink)_30%,transparent)] hover:bg-sand active:translate-y-px';

  const state = disabled ?
  'opacity-50 cursor-not-allowed pointer-events-none' :
  'cursor-pointer';

  return (
    <button
      type={type}
      aria-pressed={active}
      disabled={disabled}
      className={`${base} ${tone} ${state} ${className}`}
      {...rest}>
      
      {icon ?
      <span className="flex items-center [&>svg]:h-4 [&>svg]:w-4" aria-hidden="true">
          {icon}
        </span> :
      null}
      <span className="ref-truncate">{label}</span>
      {count !== undefined && count !== null ?
      <span className="text-[11px] opacity-60" data-numeric="true">
          {count}
        </span> :
      null}
    </button>);

}

export interface ChipRailProps extends React.HTMLAttributes<HTMLDivElement> {
  /** Accessible name for the group of chips. */
  label: string;
  children: React.ReactNode;
}

/**
 * ChipRail — the horizontal, scrollbar-less rail the chips live in (`.s-rail`),
 * 8px gaps with 4px bottom padding for the scroll edge.
 */
export function ChipRail({
  label,
  children,
  className = '',
  ...rest
}: ChipRailProps) {
  return (
    <div
      role="group"
      aria-label={label}
      style={{ scrollbarWidth: 'none' }}
      className={`flex gap-2 overflow-x-auto pb-1 [&::-webkit-scrollbar]:hidden ${className}`}
      {...rest}>
      
      {children}
    </div>);

}