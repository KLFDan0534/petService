import { useMemo, type ReactNode } from 'react';
import { AlertTriangleIcon, RotateCcwIcon } from 'lucide-react';

/* -------------------------------------------------------------------------
 * Ported from the product's `src/components/common/DataTable.vue`.
 * The Vue original relied on global `app.css` rules (`.table-wrap`, `table`,
 * `th`, `td`, `.badge-*`) that do not exist inside this artifact, so those
 * rules are reproduced 1:1 with Tailwind utilities bound to the `--ref-*`
 * tokens. The public contract — `columns` / `data`, the badge-cell shape and
 * the trailing "操作" action slot — is unchanged.
 * ---------------------------------------------------------------------- */

export type DataTableRow = Record<string, unknown>;

export type DataTableColumnObject = {
  /** Row key to read. Falls back to `label` when omitted. */
  key: string;
  /** Header text. Falls back to `key` when omitted. */
  label?: string;
  align?: 'left' | 'center' | 'right';
  /** Optional fixed column width, e.g. `'120px'`. */
  width?: string;
};

/** A column may be a bare row key (as in the Vue source) or a descriptor. */
export type DataTableColumn = string | DataTableColumnObject;

/** The badge variants the table is allowed to render (source contract). */
export const ALLOWED_BADGES = [
'badge-primary',
'badge-secondary',
'badge-success',
'badge-warning',
'badge-danger',
'badge-error',
'badge-info',
'badge-disabled'] as
const;

export type AllowedBadge = (typeof ALLOWED_BADGES)[number];

export type DataTableBadgeCell = {
  badge: AllowedBadge;
  label: string;
};

export type DataTableProps<T extends DataTableRow = DataTableRow> = {
  columns?: DataTableColumn[];
  data?: T[];
  /** Trailing action cell — the React equivalent of the Vue default slot. */
  actions?: (row: T, index: number) => ReactNode;
  /** Header for the action column. */
  actionsLabel?: string;
  loading?: boolean;
  /** Number of shimmer rows rendered while loading. */
  loadingRows?: number;
  /** Error message. When set, the table body is replaced by a retry region. */
  error?: string | null;
  onRetry?: () => void;
  retryLabel?: string;
  emptyText?: string;
  /** Accessible name / visible caption for the table. */
  caption?: string;
  /** Hide the caption visually while keeping it for screen readers. */
  captionHidden?: boolean;
  className?: string;
};

const BADGE_CLASS: Record<AllowedBadge, string> = {
  'badge-primary':
  'bg-[color-mix(in_srgb,var(--ref-brand)_14%,transparent)] text-[color-mix(in_srgb,var(--ref-brand-deep)_82%,var(--ref-ink))]',
  'badge-secondary': 'bg-sand text-ink-soft',
  'badge-success':
  'bg-[color-mix(in_srgb,var(--ref-positive)_16%,transparent)] text-[color-mix(in_srgb,var(--ref-positive)_70%,var(--ref-ink))]',
  'badge-warning':
  'bg-[color-mix(in_srgb,var(--ref-caution)_18%,transparent)] text-[color-mix(in_srgb,var(--ref-caution)_65%,var(--ref-ink))]',
  'badge-danger':
  'bg-[color-mix(in_srgb,var(--ref-critical)_14%,transparent)] text-[color-mix(in_srgb,var(--ref-critical)_72%,var(--ref-ink))]',
  'badge-error':
  'bg-[color-mix(in_srgb,var(--ref-critical)_14%,transparent)] text-[color-mix(in_srgb,var(--ref-critical)_72%,var(--ref-ink))]',
  'badge-info':
  'bg-[color-mix(in_srgb,var(--ref-informative)_16%,transparent)] text-[color-mix(in_srgb,var(--ref-informative)_70%,var(--ref-ink))]',
  'badge-disabled': 'bg-cream text-muted'
};

const ALIGN_CLASS = {
  left: 'text-left',
  center: 'text-center',
  right: 'text-right'
} as const;

type NormalizedColumn = Required<Omit<DataTableColumnObject, 'width'>> & {
  width?: string;
};

export function DataTable<T extends DataTableRow = DataTableRow>({
  columns = [],
  data = [],
  actions,
  actionsLabel = '操作',
  loading = false,
  loadingRows = 4,
  error = null,
  onRetry,
  retryLabel = '重试',
  emptyText = '暂无数据',
  caption,
  captionHidden = true,
  className = ''
}: DataTableProps<T>) {
  const cols = useMemo<NormalizedColumn[]>(() => normalizeColumns(columns), [columns]);
  const hasActions = typeof actions === 'function';
  const emptyColspan = cols.length + (hasActions ? 1 : 0);

  const showError = Boolean(error) && !loading;
  const showEmpty = !loading && !showError && data.length === 0;

  return (
    <div
      className={[
      'max-w-full min-w-0 overflow-hidden rounded-card border border-line bg-surface',
      className].

      filter(Boolean).
      join(' ')}
      aria-busy={loading || undefined}>
      
      {/* ---- md+ : the ported table, horizontally scrollable ---- */}
      <div className="ref-scroll-x hidden md:block [-webkit-overflow-scrolling:touch]">
        <table className="w-full min-w-[720px] border-collapse text-[14px]">
          {caption ?
          <caption
            className={
            captionHidden ?
            'sr-only' :
            'px-4 pt-4 pb-3 text-left text-meta text-muted'
            }>
            
              {caption}
            </caption> :
          null}
          <thead>
            <tr>
              {cols.map((col) =>
              <th
                key={col.key}
                scope="col"
                style={col.width ? { width: col.width } : undefined}
                className={[
                'border-b-2 border-line bg-sand px-4 py-3 text-meta font-semibold text-muted',
                ALIGN_CLASS[col.align]].
                join(' ')}>
                
                  {col.label}
                </th>
              )}
              {hasActions ?
              <th
                scope="col"
                className="border-b-2 border-line bg-sand px-4 py-3 text-right text-meta font-semibold text-muted">
                
                  {actionsLabel}
                </th> :
              null}
            </tr>
          </thead>
          <tbody className="[&>tr:last-child>td]:border-b-0">
            {loading ?
            Array.from({ length: Math.max(1, loadingRows) }).map((_, rowIndex) =>
            <tr key={`skeleton-${rowIndex}`}>
                    {Array.from({ length: Math.max(1, emptyColspan) }).map((__, cellIndex) =>
              <td key={cellIndex} className="border-b border-line px-4 py-3">
                        <span
                  className="ref-shimmer block h-3 rounded-full"
                  style={{ width: cellIndex % 3 === 0 ? '52%' : '78%' }} />
                
                        <span className="sr-only">加载中</span>
                      </td>
              )}
                  </tr>
            ) :
            null}

            {showError ?
            <tr>
                <td colSpan={Math.max(1, emptyColspan)} className="px-gutter py-12">
                  <ErrorRegion error={error as string} onRetry={onRetry} retryLabel={retryLabel} />
                </td>
              </tr> :
            null}

            {!loading && !showError ?
            data.map((row, i) =>
            <tr
              key={i}
              className="transition-colors duration-fast ease-editorial hover:bg-[color-mix(in_srgb,var(--ref-brand)_5%,transparent)]">
              
                    {cols.map((col, j) => {
                const value = cellValue(row, col);
                const badge = isBadgeCell(value) ? value : null;
                return (
                  <td
                    key={j}
                    data-numeric={typeof value === 'number' ? 'true' : undefined}
                    className={[
                    'border-b border-line px-4 py-3 text-ink [overflow-wrap:anywhere]',
                    ALIGN_CLASS[col.align]].
                    join(' ')}>
                    
                          {badge ?
                    <span
                      className={[
                      'inline-flex items-center rounded-full px-2.5 py-0.5 text-[12px] font-semibold',
                      BADGE_CLASS[badge.badge]].
                      join(' ')}>
                      
                              {badge.label}
                            </span> :

                    <span className="[overflow-wrap:anywhere] [word-break:break-word]">
                              {formatCell(value)}
                            </span>
                    }
                        </td>);

              })}
                    {hasActions ?
              <td className="border-b border-line px-4 py-3 text-right align-middle">
                        <div className="flex flex-wrap items-center justify-end gap-2">
                          {actions?.(row, i)}
                        </div>
                      </td> :
              null}
                  </tr>
            ) :
            null}

            {showEmpty ?
            <tr>
                <td
                colSpan={Math.max(1, emptyColspan)}
                className="px-gutter py-8 text-center text-meta text-muted">
                
                  {emptyText}
                </td>
              </tr> :
            null}
          </tbody>
        </table>
      </div>

      {/* ---- below md : the same rows restructured as label / value lists ---- */}
      <div className="md:hidden">
        {caption && !captionHidden ?
        <p className="border-b border-line px-4 py-3 text-meta text-muted">{caption}</p> :
        null}

        {loading ?
        <ul className="divide-y divide-line">
            {Array.from({ length: Math.max(1, loadingRows) }).map((_, rowIndex) =>
          <li key={`m-skeleton-${rowIndex}`} className="space-y-3 px-4 py-4">
                {cols.slice(0, 3).map((col) =>
            <div key={col.key} className="flex items-center justify-between gap-4">
                    <span className="ref-shimmer h-2.5 w-20 rounded-full" />
                    <span className="ref-shimmer h-2.5 w-28 rounded-full" />
                  </div>
            )}
                <span className="sr-only">加载中</span>
              </li>
          )}
          </ul> :
        null}

        {showError ?
        <div className="px-4 py-10">
            <ErrorRegion error={error as string} onRetry={onRetry} retryLabel={retryLabel} />
          </div> :
        null}

        {!loading && !showError && data.length > 0 ?
        <ul className="divide-y divide-line">
            {data.map((row, i) =>
          <li key={i} className="px-4 py-4">
                <dl className="space-y-2.5">
                  {cols.map((col, j) => {
                const value = cellValue(row, col);
                const badge = isBadgeCell(value) ? value : null;
                return (
                  <div key={j} className="flex items-start justify-between gap-4">
                        <dt className="shrink-0 pt-0.5 text-label uppercase text-muted">
                          {col.label}
                        </dt>
                        <dd
                      data-numeric={typeof value === 'number' ? 'true' : undefined}
                      className="min-w-0 text-right text-meta text-ink [overflow-wrap:anywhere]">
                      
                          {badge ?
                      <span
                        className={[
                        'inline-flex items-center rounded-full px-2.5 py-0.5 text-[12px] font-semibold',
                        BADGE_CLASS[badge.badge]].
                        join(' ')}>
                        
                              {badge.label}
                            </span> :

                      formatCell(value)
                      }
                        </dd>
                      </div>);

              })}
                </dl>
                {hasActions ?
            <div className="mt-3.5 flex flex-wrap items-center gap-2 border-t border-line pt-3.5">
                    {actions?.(row, i)}
                  </div> :
            null}
              </li>
          )}
          </ul> :
        null}

        {showEmpty ?
        <p className="px-4 py-8 text-center text-meta text-muted">{emptyText}</p> :
        null}
      </div>
    </div>);

}

function ErrorRegion({
  error,
  onRetry,
  retryLabel




}: {error: string;onRetry?: () => void;retryLabel: string;}) {
  return (
    <div role="alert" className="flex flex-col items-center gap-3 text-center">
      <AlertTriangleIcon
        className="h-5 w-5 text-critical"
        strokeWidth={1.75}
        aria-hidden="true" />
      
      <p className="max-w-prose text-meta text-ink-soft">{error}</p>
      {onRetry ?
      <button
        type="button"
        onClick={onRetry}
        className="inline-flex items-center gap-1.5 rounded-control border border-line bg-surface px-3.5 py-2 text-control font-medium text-ink transition-colors duration-fast ease-editorial hover:border-brand hover:text-brand active:translate-y-px disabled:cursor-not-allowed disabled:opacity-50">
        
          <RotateCcwIcon className="h-3.5 w-3.5" strokeWidth={1.75} aria-hidden="true" />
          {retryLabel}
        </button> :
      null}
    </div>);

}

function normalizeColumns(columns: DataTableColumn[]): NormalizedColumn[] {
  return columns.map((col) => {
    if (typeof col === 'string') {
      return { key: col, label: col, align: 'left' };
    }
    return {
      key: col.key || col.label || '',
      label: col.label || col.key,
      align: col.align ?? 'left',
      width: col.width
    };
  });
}

function cellValue(row: DataTableRow | undefined, col: NormalizedColumn): unknown {
  return row?.[col.key];
}

export function isBadgeCell(value: unknown): value is DataTableBadgeCell {
  if (!value || typeof value !== 'object') return false;
  const badge = (value as {badge?: unknown;}).badge;
  return typeof badge === 'string' && (ALLOWED_BADGES as readonly string[]).includes(badge);
}

export function formatCell(value: unknown): ReactNode {
  if (value == null || value === '') return '-';
  if (typeof value === 'object') {
    const label = (value as {label?: unknown;}).label;
    return label != null ? String(label) : JSON.stringify(value);
  }
  if (typeof value === 'boolean') return value ? '是' : '否';
  return value as ReactNode;
}