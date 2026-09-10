import type { ReactNode } from 'react';
import { InboxIcon } from 'lucide-react';

export interface EmptyStateProps {
  /** Optional custom mark. Defaults to a line-art inbox icon. */
  icon?: ReactNode;
  /** Short headline describing the empty region. */
  title?: string;
  /** One-line explanation, usually what to do next. */
  description?: string;
  /** Optional action slot rendered below the copy. */
  children?: ReactNode;
  className?: string;
}

/**
 * EmptyState — ported from `src/components/common/EmptyState.vue` (icon / title /
 * description + default slot). The structure is unchanged; the global `.empty-state`
 * rules from `src/assets/css/app.css` and the `.empty-services` overrides from
 * `src/views/user/Services.vue` are reproduced with utilities because those global
 * stylesheets don't load inside this artifact.
 */
export function EmptyState({
  icon,
  title,
  description,
  children,
  className = ''
}: EmptyStateProps) {
  return (
    <div
      className={`flex flex-col items-center rounded-card border border-dashed border-line bg-surface px-6 py-16 text-center text-muted ${className}`}>
      
      <div className="mb-4 flex h-12 w-12 items-center justify-center text-brand [&>svg]:h-12 [&>svg]:w-12">
        {icon ?? <InboxIcon className="h-12 w-12" strokeWidth={1.25} aria-hidden="true" />}
      </div>
      {title ?
      <h3 className="mb-2 font-display text-display-xs text-ink">{title}</h3> :
      null}
      {description ?
      <p className={`max-w-prose text-[14px] leading-[1.7] ${children ? 'mb-6' : ''}`}>
          {description}
        </p> :
      null}
      {children}
    </div>);

}