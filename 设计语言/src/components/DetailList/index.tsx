import React from 'react';

/**
 * DetailList — the key/value `dl` ported from the product's order detail view
 * (`src/views/order/OrderDetailView.vue`: `.od-facts` / `.od-fact` and
 * `.od-money` / `.od-money-row`).
 *
 * The source styling lives in a scoped `<style>` block that cannot load here,
 * so the exact measured values are reproduced with Tailwind utilities backed by
 * the same `--ref-*` tokens.
 */

export type DetailListVariant = 'facts' | 'money';

export type DetailListTone = 'default' | 'discount' | 'muted' | 'ink';

export interface DetailListItem {
  /** dt content — uppercase micro-label in `facts`, sentence label in `money`. */
  label: React.ReactNode;
  /** dd content. */
  value: React.ReactNode;
  /** Renders the serif amount treatment (21px in `facts`, 24px on a money total). */
  amount?: boolean;
  /** Prefixes a small muted `¥` glyph, as the source does for amounts. */
  currency?: boolean;
  /** Tabular numerals for prices, ids, dates and counts. */
  numeric?: boolean;
  /** `money` only — the closing total row (hairline + medium weight ink label). */
  total?: boolean;
  /** Value colour accent. `discount` is the brand-deep reduction row. */
  tone?: DetailListTone;
  /** Optional stable key; falls back to the index. */
  key?: string;
}

export interface DetailListProps {
  items: DetailListItem[];
  /** `facts` = responsive grid of micro-labelled facts. `money` = stacked amount rows. */
  variant?: DetailListVariant;
  /** `facts` only — desktop column count. Collapses to 2 (or 1) below `lg`. */
  columns?: 1 | 2 | 3 | 4;
  /** `facts` only — the `.od-facts-inner` treatment: top hairline inside a panel. */
  divided?: boolean;
  className?: string;
  'aria-label'?: string;
}

const cx = (...parts: Array<string | false | undefined>): string =>
parts.filter(Boolean).join(' ');

const COLUMN_CLASS: Record<NonNullable<DetailListProps['columns']>, string> = {
  1: 'grid-cols-1',
  2: 'grid-cols-1 sm:grid-cols-2',
  3: 'grid-cols-2 lg:grid-cols-3',
  4: 'grid-cols-2 lg:grid-cols-4'
};

const TONE_CLASS: Record<DetailListTone, string> = {
  default: '',
  discount: 'text-brand-deep',
  muted: 'text-muted',
  ink: 'text-ink'
};

function Yen(): JSX.Element {
  return (
    <span className="text-[0.6em] text-muted" aria-hidden="true">
      ¥
    </span>);

}

export function DetailList({
  items,
  variant = 'facts',
  columns = 4,
  divided = false,
  className,
  'aria-label': ariaLabel
}: DetailListProps): JSX.Element {
  if (variant === 'money') {
    return (
      <dl className={cx('m-0', className)} aria-label={ariaLabel}>
        {items.map((item, index) =>
        <div
          key={item.key ?? index}
          className={cx(
            'flex items-baseline justify-between gap-5 py-[9px]',
            item.total && 'mt-2 border-t border-line pt-4'
          )}>
          
            <dt
            className={cx(
              'text-[13.5px] text-ink-soft',
              item.total && 'font-medium text-ink'
            )}>
            
              {item.label}
            </dt>
            <dd
            className={cx(
              'm-0 text-right text-[14px] text-ink',
              item.amount &&
              'font-display text-[24px] leading-none tracking-[-0.02em] text-ink',
              TONE_CLASS[item.tone ?? 'default']
            )}
            data-numeric={item.numeric ?? true ? 'true' : undefined}>
            
              {item.currency && <Yen />}
              {item.value}
            </dd>
          </div>
        )}
      </dl>);

  }

  return (
    <dl
      className={cx(
        'grid gap-x-5 py-1',
        COLUMN_CLASS[columns],
        divided ?
        'mt-5 gap-y-[14px] border-t border-line pt-5' :
        'mt-[18px] gap-y-5',
        className
      )}
      aria-label={ariaLabel}>
      
      {items.map((item, index) =>
      <div key={item.key ?? index} className="min-w-0">
          <dt className="text-[11px] uppercase tracking-[0.14em] text-muted">
            {item.label}
          </dt>
          <dd
          className={cx(
            'mt-[7px] break-words text-[13.5px] leading-[1.5] text-ink-soft',
            item.amount &&
            'font-display text-[21px] tracking-[-0.01em] text-ink',
            TONE_CLASS[item.tone ?? 'default']
          )}
          data-numeric={item.numeric ? 'true' : undefined}>
          
            {item.currency && <Yen />}
            {item.value}
          </dd>
        </div>
      )}
    </dl>);

}