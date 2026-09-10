import type { ReactNode } from 'react';

export interface CardProps {
  /** Media slot rendered flush at the top of the card (image, MediaFrame, video). */
  media?: ReactNode;
  /** Makes the media region a button. Provide `mediaLabel` for its accessible name. */
  onMediaClick?: () => void;
  mediaLabel?: string;
  /** Small uppercase brand eyebrow above the title. */
  eyebrow?: string;
  title?: ReactNode;
  /** Secondary line under the title (merchant, location, date). */
  meta?: ReactNode;
  /** Right-aligned header content — price, rating, badge. */
  trailing?: ReactNode;
  /** Fully custom header. Replaces eyebrow / title / meta / trailing when provided. */
  header?: ReactNode;
  /** Footer slot, divided from the body by a hairline top border. */
  footer?: ReactNode;
  /** Hover lift + border shift + shadow. Disable for static containers. */
  interactive?: boolean;
  className?: string;
  children?: ReactNode;
}

export function Card({
  media,
  onMediaClick,
  mediaLabel,
  eyebrow,
  title,
  meta,
  trailing,
  header,
  footer,
  interactive = true,
  className = '',
  children
}: CardProps) {
  const hasHeader = Boolean(header || eyebrow || title || meta || trailing);

  return (
    <article
      className={[
      'group flex min-w-0 flex-col overflow-hidden rounded-card border border-line bg-surface shadow-none',
      'transition-[transform,border-color,box-shadow] duration-normal ease-editorial',
      interactive ?
      'hover:-translate-y-1 hover:border-[color-mix(in_srgb,var(--ref-ink)_16%,transparent)] hover:shadow-lift' :
      '',
      className].

      filter(Boolean).
      join(' ')}>
      
      {media ?
      onMediaClick ?
      <button
        type="button"
        onClick={onMediaClick}
        aria-label={mediaLabel}
        className="block w-full overflow-hidden bg-sand p-0 aspect-[4/3] [&>*]:h-full [&>*]:w-full [&_img]:h-full [&_img]:w-full [&_img]:object-cover [&_img]:transition-transform [&_img]:duration-slow [&_img]:ease-editorial hover:[&_img]:scale-[1.06]">
        
            {media}
          </button> :

      <div className="w-full overflow-hidden bg-sand aspect-[4/3] [&>*]:h-full [&>*]:w-full [&_img]:h-full [&_img]:w-full [&_img]:object-cover [&_img]:transition-transform [&_img]:duration-slow [&_img]:ease-editorial group-hover:[&_img]:scale-[1.06]">
            {media}
          </div> :

      null}

      <div className="flex flex-1 flex-col p-5">
        {hasHeader ?
        header ??
        <div className="flex items-start justify-between gap-4">
              <div className="min-w-0">
                {eyebrow ?
            <span className="text-label font-bold uppercase tracking-label text-brand">
                    {eyebrow}
                  </span> :
            null}
                {title ?
            <h3 className="mt-1.5 font-display text-[20px] font-medium leading-[1.3] tracking-editorial text-ink [text-wrap:balance]">
                    {title}
                  </h3> :
            null}
                {meta ?
            <span className="mt-0.5 inline-flex items-center gap-2 text-meta text-muted ref-truncate">
                    {meta}
                  </span> :
            null}
              </div>
              {trailing ?
          <div
            className="shrink-0 text-right font-display text-[22px] font-semibold leading-[1.2] text-ink"
            data-numeric="true">
            
                  {trailing}
                </div> :
          null}
            </div> :

        null}

        {children ?
        <div
          className={[
          'text-[13.5px] leading-[1.7] text-ink-soft',
          hasHeader ? 'mt-4' : '',
          footer ? 'mb-5' : ''].

          filter(Boolean).
          join(' ')}>
          
            {children}
          </div> :
        null}

        {footer ?
        <div className="mt-auto flex flex-col items-stretch gap-3 border-t border-line pt-4 sm:flex-row sm:items-center sm:justify-between">
            {footer}
          </div> :
        null}
      </div>
    </article>);

}