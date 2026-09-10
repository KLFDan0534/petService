import React from 'react';

export type SectionBandHeadingLevel = 'h1' | 'h2' | 'h3' | 'h4';

export interface SectionBandProps {
  /** Display title, e.g. the active category or "全部照护方案". */
  title: React.ReactNode;
  /** 13px muted note under the title, e.g. a result count. */
  note?: React.ReactNode;
  /** Optional uppercase eyebrow with a 32px hairline, rendered above the title. */
  eyebrow?: string;
  /** Announce note changes to assistive tech (use for live result counts). */
  liveNote?: boolean;
  /** Optional trailing controls (sort, view switch) aligned to the right. */
  action?: React.ReactNode;
  /** Heading element rendered for the title. Defaults to `h2`. */
  headingLevel?: SectionBandHeadingLevel;
  /** Id applied to the heading, for `aria-labelledby` on the parent section. */
  titleId?: string;
  className?: string;
}

/**
 * SectionBand — the editorial section separator + heading block.
 *
 * Ported from `src/views/user/Services.vue` (`.s-results-head` / `.s-cat-title`
 * / `.s-results-note`): 44px top margin, 28px top padding, a 1px `--ref-line`
 * hairline on top, a display title at `clamp(20px, 2.6vw, 26px)` and a 13px
 * muted note. The source's scoped CSS is expressed here with the design
 * system's Tailwind tokens, which resolve to the same `--ref-*` variables.
 */
export function SectionBand({
  title,
  note,
  eyebrow,
  liveNote = false,
  action,
  headingLevel = 'h2',
  titleId,
  className
}: SectionBandProps) {
  const Heading = headingLevel as React.ElementType;

  return (
    <div
      className={[
      'mt-[44px] mb-[24px] pt-[28px] border-t border-line',
      'flex flex-wrap items-end justify-between gap-x-gutter gap-y-4',
      className ?? ''].

      join(' ').
      trim()}>
      
      <div className="min-w-0">
        {eyebrow ?
        <div className="flex items-center gap-[10px]" aria-hidden="true">
            <span className="w-[32px] h-px bg-line" />
            <span className="text-eyebrow uppercase text-muted">{eyebrow}</span>
          </div> :
        null}

        <Heading
          id={titleId}
          className="mt-[14px] font-display text-display-sm text-ink ref-clamp-2">
          
          {title}
        </Heading>

        {note ?
        <p
          className="mt-[8px] text-meta text-muted"
          {...liveNote ? { 'aria-live': 'polite' as const } : {}}>
          
            {note}
          </p> :
        null}
      </div>

      {action ? <div className="flex items-center gap-3">{action}</div> : null}
    </div>);

}