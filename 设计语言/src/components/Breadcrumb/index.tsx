import React from 'react';

export type BreadcrumbItem = {
  /** Visible label for the crumb. */
  label: string;
  /** Destination. Omit (or omit on the last item) to render as the current page. */
  href?: string;
};

export type BreadcrumbProps = {
  /** Ordered trail, root first. The last item always renders as the current page. */
  items: BreadcrumbItem[];
  /** Separator glyph between crumbs. */
  separator?: React.ReactNode;
  /** Accessible name for the nav landmark. */
  ariaLabel?: string;
  /** Optional link renderer, e.g. to plug in a router `Link`. */
  renderLink?: (item: BreadcrumbItem, className: string) => React.ReactNode;
  className?: string;
};

const LINK_CLASS =
'text-muted transition-colors duration-fast ease-editorial hover:text-ink focus-visible:text-ink rounded-[4px] ref-truncate';

/**
 * Editorial Warm breadcrumb trail.
 *
 * Ported 1:1 from `src/views/user/Services.vue` (`.s-crumb`): 12.5px muted row,
 * 8px gap, 6px vertical padding, `›` separators in `--ref-line`, current page in
 * `--ref-ink-soft`.
 */
export function Breadcrumb({
  items,
  separator = '›',
  ariaLabel = '面包屑',
  renderLink,
  className = ''
}: BreadcrumbProps) {
  if (items.length === 0) return null;

  return (
    <nav
      aria-label={ariaLabel}
      className={`flex min-w-0 items-center gap-2 py-1.5 text-[12.5px] leading-[1.6] text-muted ${className}`}>
      
      {items.map((item, index) => {
        const isLast = index === items.length - 1;
        const isLink = Boolean(item.href) && !isLast;

        return (
          <React.Fragment key={`${item.label}-${index}`}>
            {index > 0 &&
            <span className="text-line select-none" aria-hidden="true">
                {separator}
              </span>
            }
            {isLink ?
            renderLink ?
            renderLink(item, LINK_CLASS) :

            <a href={item.href} className={LINK_CLASS}>
                  {item.label}
                </a> :


            <span
              className="text-ink-soft ref-truncate"
              aria-current={isLast ? 'page' : undefined}>
              
                {item.label}
              </span>
            }
          </React.Fragment>);

      })}
    </nav>);

}