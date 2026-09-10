import React from 'react';

export type PageHeaderStat = {
  /** Display value — numbers stay tabular via `data-numeric`. */
  value: React.ReactNode;
  /** Short label under the value. */
  label: string;
};

export type PageHeaderProps = {
  /** Uppercase kicker rendered after a 32px hairline, e.g. "Services". */
  eyebrow?: string;
  /** Display title. Pass a fragment with `<br className="hidden min-[900px]:block" />` to control line breaks. */
  title: React.ReactNode;
  /** Supporting copy, capped at 560px. */
  subtitle?: React.ReactNode;
  /** Optional stat row shown under the copy. */
  stats?: PageHeaderStat[];
  /** Accessible name for the stat row. */
  statsLabel?: string;
  /** Action cluster (buttons / links) shown under the stats. */
  actions?: React.ReactNode;
  /** Right-hand media column, framed at 4/3 with a hairline border. */
  media?: React.ReactNode;
  /** Breadcrumb or back link rendered above the hero. */
  breadcrumb?: React.ReactNode;
  /** Stats render as `--` and the media frame shimmers. */
  loading?: boolean;
  className?: string;
};

export function PageHeader({
  eyebrow,
  title,
  subtitle,
  stats,
  statsLabel = '概览',
  actions,
  media,
  breadcrumb,
  loading = false,
  className = ''
}: PageHeaderProps) {
  return (
    <header className={className}>
      {breadcrumb ?
      <nav
        className="flex items-center gap-2 pt-6 text-[12.5px] text-muted"
        aria-label="面包屑">
        
          {breadcrumb}
        </nav> :
      null}

      <section
        className={[
        'grid grid-cols-1 items-center gap-8 pt-[34px] pb-[28px]',
        media ?
        'min-[900px]:grid-cols-[minmax(0,1.2fr)_minmax(0,1fr)]' :
        'min-[900px]:grid-cols-1',
        'min-[900px]:gap-[48px] min-[900px]:pt-[48px] min-[900px]:pb-[40px]'].
        join(' ')}>
        
        <div className="min-w-0">
          {eyebrow ?
          <div className="flex items-center gap-[10px]" aria-hidden="true">
              <span className="h-px w-8 bg-line" />
              <span className="text-eyebrow uppercase text-muted">{eyebrow}</span>
            </div> :
          null}

          <h1
            className="mt-5 font-display text-display-xl text-ink"
            style={{ textWrap: 'balance' } as React.CSSProperties}>
            
            {title}
          </h1>

          {subtitle ?
          <p
            className="mt-5 max-w-prose text-[14px] leading-[1.8] min-[560px]:text-lede"
            style={{
              color: 'color-mix(in srgb, var(--ref-ink-soft) 82%, transparent)'
            }}>
            
              {subtitle}
            </p> :
          null}

          {stats && stats.length > 0 ?
          <dl
            className="mt-9 flex flex-wrap items-end gap-x-5 gap-y-0 min-[560px]:gap-x-7"
            aria-label={statsLabel}>
            
              {stats.map((stat, index) =>
            <div
              key={stat.label}
              className={
              index === 0 ?
              'flex flex-col' :
              'flex flex-col border-l border-line pl-5 min-[560px]:pl-7'
              }>
              
                  <dd
                className="m-0 font-display text-[26px] leading-none tracking-editorial text-ink min-[560px]:text-[30px]"
                data-numeric="true">
                
                    {loading ? '--' : stat.value}
                  </dd>
                  <dt className="mt-2 text-[12px] text-muted">{stat.label}</dt>
                </div>
            )}
            </dl> :
          null}

          {actions ?
          <div className="mt-8 flex flex-wrap items-center gap-3">{actions}</div> :
          null}
        </div>

        {media ?
        <div className="min-w-0">
            <div className="relative aspect-[4/3] overflow-hidden rounded-frame border border-line bg-surface [&_img]:h-full [&_img]:w-full [&_img]:object-cover">
              {loading ?
            <div className="ref-shimmer h-full w-full" aria-hidden="true" /> :

            media
            }
            </div>
          </div> :
        null}
      </section>
    </header>);

}