import React from 'react';

export type StatListItem = {
  /** Stable key; falls back to the label when omitted. */
  id?: string;
  /** The number or short value shown in the display face. */
  value: React.ReactNode;
  /** Muted caption under the value. */
  label: string;
};

export type StatListProps = {
  items: StatListItem[];
  /**
   * When true every value renders as `--` so the row keeps its height while
   * data is in flight (matches the source hero behaviour).
   */
  loading?: boolean;
  /** Placeholder shown for each value while `loading`. */
  loadingPlaceholder?: string;
  /** Accessible name for the definition list. */
  label?: string;
  className?: string;
};

/**
 * Editorial hero statistics — a `<dl>` of value/label pairs separated by
 * hairlines. Ported from `src/views/user/Services.vue` (`.s-stats`).
 */
export function StatList({
  items,
  loading = false,
  loadingPlaceholder = '--',
  label,
  className = ''
}: StatListProps) {
  if (items.length === 0) return null;

  return (
    <dl
      aria-label={label}
      aria-busy={loading || undefined}
      className={`m-0 flex flex-wrap items-end gap-y-0 gap-x-7 max-[560px]:gap-x-5 ${className}`}>
      
      {items.map((item, index) =>
      <div
        key={item.id ?? item.label}
        className={
        index === 0 ?
        'flex flex-col' :
        'flex flex-col border-l border-line pl-7 max-[560px]:pl-5'
        }>
        
          <dd
          data-numeric="true"
          className="m-0 font-display text-[30px] leading-none tracking-editorial tabular-nums text-ink max-[560px]:text-[26px]">
          
            {loading ? loadingPlaceholder : item.value}
          </dd>
          <dt className="mt-2 text-[12px] leading-normal text-muted">
            {item.label}
          </dt>
        </div>
      )}
    </dl>);

}