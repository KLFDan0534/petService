import React from 'react';
import { twMerge } from 'tailwind-merge';

/**
 * PageShell — the confirmed page container of the pet-services product.
 *
 * Ported from `src/views/user/Services.vue` (`.svc-page` / `.svc-shell`), which is
 * duplicated as a per-view `*-shell` class in nearly every refined view:
 *
 *   .svc-page  { width: 100%; padding: 6px 0 72px; }
 *   .svc-shell { max-width: 1180px; margin: 0 auto; padding: 0 24px; }
 *   @media (max-width: 560px) { .svc-shell { padding: 0 16px; } }
 *
 * Scoped Vue stylesheets don't resolve inside this artifact, so the exact same
 * values are expressed as Tailwind utilities (`max-w-shell`, `px-gutter`, and the
 * `min-[560px]` gutter switch) which map 1:1 to the source declarations.
 */
export type PageShellWidth = 'narrow' | 'default' | 'wide';

export interface PageShellProps extends React.HTMLAttributes<HTMLElement> {
  /**
   * Content max-width. `default` is the product's 1180px shell, `narrow` matches
   * the 1080px record shells (order detail), `wide` matches the 1440px admin
   * data workspace.
   */
  width?: PageShellWidth;
  /** Render a different element for the outer page region. Defaults to `main`. */
  as?: 'main' | 'div' | 'section' | 'article';
  /** Vertical page padding (`6px 0 72px`). Set false when nesting inside another shell. */
  padded?: boolean;
  /** Extra classes applied to the inner, max-width-constrained shell. */
  shellClassName?: string;
  children?: React.ReactNode;
}

const WIDTHS: Record<PageShellWidth, string> = {
  narrow: 'max-w-[1080px]',
  default: 'max-w-shell',
  wide: 'max-w-[1440px]'
};

export function PageShell({
  width = 'default',
  as: Component = 'main',
  padded = true,
  shellClassName,
  className,
  children,
  ...rest
}: PageShellProps) {
  return (
    <Component
      className={twMerge('w-full', padded && 'pt-[6px] pb-[72px]', className)}
      {...rest}>
      
      <div
        className={twMerge(
          'mx-auto w-full px-4 min-[560px]:px-gutter',
          WIDTHS[width],
          shellClassName
        )}>
        
        {children}
      </div>
    </Component>);

}