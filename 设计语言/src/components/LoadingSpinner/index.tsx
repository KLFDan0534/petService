import React from 'react';

export interface LoadingSpinnerProps {
  /** Message shown under the spinner. Defaults to 加载中... */
  text?: string;
  /** Optional extra classes on the wrapper. */
  className?: string;
}

/**
 * LoadingSpinner — ported 1:1 from `src/components/common/LoadingSpinner.vue`.
 *
 * The source relied on the global `.loading` / `.spinner` rules and the
 * `--color-*` variables, neither of which resolve in this artifact, so those
 * styles are reproduced with the warm Editorial tokens (`border-line`,
 * `border-t-brand`, `text-muted`). Structure and behaviour are unchanged.
 */
export function LoadingSpinner({ text, className }: LoadingSpinnerProps) {
  const label = text || '加载中...';

  return (
    <div
      role="status"
      aria-live="polite"
      aria-label={label}
      className={['px-6 py-12 text-center text-muted', className].
      filter(Boolean).
      join(' ')}>
      
      <div className="mx-auto mb-3 h-8 w-8 rounded-full border-[3px] border-line border-t-brand animate-spin" />
      <p className="text-meta">{label}</p>
    </div>);

}