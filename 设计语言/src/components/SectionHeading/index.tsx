import React from 'react';

export type SectionHeadingTone = 'light' | 'dark';

export interface SectionHeadingProps {
  /** Small tabular index shown before the label (e.g. "01"). Optional — omit for editorial-warm screens. */
  index?: string;
  /** Short section label rendered next to the hairline. */
  label?: string;
  /** Section title. `children` takes precedence when provided. */
  title?: string;
  /** Supporting copy under the title. */
  description?: string;
  /** `dark` inverts ink colours for use on dark photography / dark bands. */
  tone?: SectionHeadingTone;
  /** Trailing action slot (link or button), bottom-aligned with the title block. */
  action?: React.ReactNode;
  /** Heading element, so the section keeps a correct document outline. */
  as?: 'h1' | 'h2' | 'h3' | 'h4';
  /** Title content — overrides `title`. */
  children?: React.ReactNode;
  className?: string;
}

export function SectionHeading({
  index = '',
  label = '',
  title = '',
  description = '',
  tone = 'light',
  action,
  as: Heading = 'h2',
  children,
  className = ''
}: SectionHeadingProps) {
  const isDark = tone === 'dark';
  const hasMeta = Boolean(index || label);

  return (
    <div
      className={[
      'block mb-6',
      'min-[721px]:flex min-[721px]:flex-wrap min-[721px]:items-end min-[721px]:justify-between',
      'min-[721px]:gap-x-6 min-[721px]:gap-y-10 min-[721px]:mb-12',
      className].

      filter(Boolean).
      join(' ')}>
      
      <div className="max-w-[640px]">
        {hasMeta &&
        <div className="flex items-center gap-3">
            {index &&
          <span
            data-numeric="true"
            className={`text-[12px] tabular-nums ${isDark ? 'text-white/50' : 'text-muted'}`}>
            
                {index}
              </span>
          }
            <span
            aria-hidden="true"
            className={`h-px w-6 ${isDark ? 'bg-white/25' : 'bg-line'}`} />
          
            {label &&
          <span
            className={`text-[13px] font-medium ${isDark ? 'text-white/50' : 'text-ink-soft'}`}>
            
                {label}
              </span>
          }
          </div>
        }

        <Heading
          className={[
          'font-display font-normal leading-[1.15] tracking-[-0.045em] [text-wrap:balance]',
          'text-[28px] min-[721px]:text-[clamp(28px,3.4vw,42px)]',
          hasMeta ? 'mt-4' : 'mt-0',
          isDark ? 'text-cream' : 'text-ink'].
          join(' ')}>
          
          {children ?? title}
        </Heading>

        {description &&
        <p
          className={[
          'mt-3 min-[721px]:mt-4 max-w-[560px] text-[15px] leading-[1.65]',
          isDark ? 'text-white/55' : 'text-ink-soft opacity-80'].
          join(' ')}>
          
            {description}
          </p>
        }
      </div>

      {action &&
      <div className="mt-3 shrink-0 min-[721px]:mt-0 min-[721px]:pb-1">{action}</div>
      }
    </div>);

}