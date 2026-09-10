import type { ReactNode } from 'react';
import { AlertTriangleIcon, RotateCcwIcon } from 'lucide-react';
import { Button } from '../Button';

/**
 * ErrorState — the missing third data-region state.
 *
 * `rules/motion-states-and-resilience.md` requires every data region to have
 * loading / empty / error. The system already had `Skeleton` (loading) and
 * `EmptyState` (empty); failures were left to toasts. The module audit found
 * ~20 views where a failed request is toast-only or silently swallowed, and
 * four error pages (`src/views/error/*.vue`) that say "请稍后重试" without
 * offering a retry. This component closes both gaps.
 *
 * Two variants, one contract:
 *  - `inline` — replaces the contents of a failed region (list, table, panel).
 *  - `page`   — the 403 / 404 / 500 / 503 route surfaces.
 */

export type ErrorStateVariant = 'inline' | 'page';

export interface ErrorStateProps {
  /** `inline` for a failed region, `page` for a full route. Defaults to `inline`. */
  variant?: ErrorStateVariant;
  /** HTTP status shown on the `page` variant, e.g. `404`. Decorative — the heading carries the meaning. */
  code?: string | number;
  /** Headline. Defaults to 加载失败. */
  title?: string;
  /** What the user should do next. */
  description?: string;
  /** Optional technical detail (server message). Rendered small and muted. */
  detail?: string;
  /** Retry handler. Omit it and no retry button renders. */
  onRetry?: () => void;
  /** Retry label. Defaults to 重试. */
  retryLabel?: string;
  /** Puts the retry button in its busy state. */
  retrying?: boolean;
  /** Overrides the default warning mark. */
  icon?: ReactNode;
  /** Secondary actions (返回首页, 联系管理员, …). */
  children?: ReactNode;
  className?: string;
}

export function ErrorState({
  variant = 'inline',
  code,
  title = '加载失败',
  description,
  detail,
  onRetry,
  retryLabel = '重试',
  retrying = false,
  icon,
  children,
  className = ''
}: ErrorStateProps) {
  const mark = icon ??
  <AlertTriangleIcon strokeWidth={1.25} aria-hidden="true" />;


  const actions =
  onRetry || children ? (
  /* flex-wrap — the audited error pages could not wrap two CJK buttons at 320px */
  <div
    className={`flex flex-wrap items-center gap-2.5 ${
    variant === 'page' ? 'justify-center' : ''}`
    }>
    
        {onRetry ?
    <Button
      variant="primary"
      size="sm"
      loading={retrying}
      icon={<RotateCcwIcon className="h-[15px] w-[15px]" aria-hidden="true" />}
      onClick={onRetry}>
      
            {retryLabel}
          </Button> :
    null}
        {children}
      </div>) :
  null;

  if (variant === 'page') {
    return (
      <section
        className={`flex min-h-[60vh] w-full flex-col items-center justify-center px-4 py-16 text-center min-[560px]:px-gutter ${className}`}>
        
        {code != null ? (
        /* Semantically decorative: the <h1> below is what gets announced */
        <p
          aria-hidden="true"
          data-numeric="true"
          className="m-0 font-display font-medium leading-none tracking-editorial text-[clamp(56px,13vw,104px)] text-line">
          
            {code}
          </p>) :

        <span className="mb-5 flex h-12 w-12 items-center justify-center text-critical [&>svg]:h-12 [&>svg]:w-12">
            {mark}
          </span>
        }

        <h1
          className={`font-display text-display-md text-ink ${code != null ? 'mt-6' : ''}`}>
          
          {title}
        </h1>

        {description ?
        <p className="mt-3 max-w-prose text-[14px] leading-[1.7] text-ink-soft">
            {description}
          </p> :
        null}

        {detail ?
        <p className="ref-clamp-2 mt-2 max-w-prose text-meta text-muted">{detail}</p> :
        null}

        {actions ? <div className="mt-7">{actions}</div> : null}
      </section>);

  }

  return (
    <div
      role="alert"
      className={`flex flex-col items-center rounded-card border border-critical/25 bg-critical/[0.04] px-6 py-12 text-center ${className}`}>
      
      <span className="mb-4 flex h-9 w-9 items-center justify-center text-critical [&>svg]:h-9 [&>svg]:w-9">
        {mark}
      </span>

      <h3 className="font-display text-display-xs text-ink">{title}</h3>

      {description ?
      <p className="mt-2 max-w-prose text-[14px] leading-[1.7] text-ink-soft">
          {description}
        </p> :
      null}

      {detail ?
      <p className="ref-clamp-2 mt-1.5 max-w-prose text-meta text-muted">{detail}</p> :
      null}

      {actions ? <div className="mt-5">{actions}</div> : null}
    </div>);

}