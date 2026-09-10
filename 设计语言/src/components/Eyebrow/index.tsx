import React from 'react';

export type EyebrowTone = 'muted' | 'brand';

export interface EyebrowProps {
  /** The label text. Rendered uppercase with 0.28em tracking. */
  children: React.ReactNode;
  /** Show the 32px hairline before the label. Defaults to true. */
  line?: boolean;
  /** Label colour. `muted` (default) matches the product; `brand` is for card eyebrows. */
  tone?: EyebrowTone;
  /** Element to render as. Defaults to `div`. Use `p` inside section headers. */
  as?: 'div' | 'p' | 'span';
  /**
   * Hide the whole eyebrow from assistive tech. Use when the label duplicates
   * the adjacent heading (as the source hero does).
   */
  decorative?: boolean;
  className?: string;
}

const toneClass: Record<EyebrowTone, string> = {
  muted: 'text-muted',
  brand: 'text-brand'
};

/**
 * Editorial Warm eyebrow: a 32×1px hairline, a 10px gap, then a 9.5px
 * uppercase label at 0.28em tracking. Ported from `.s-eyebrow` in
 * `src/views/user/Services.vue`.
 */
export function Eyebrow({
  children,
  line = true,
  tone = 'muted',
  as: Tag = 'div',
  decorative = false,
  className = ''
}: EyebrowProps) {
  return (
    <Tag
      aria-hidden={decorative ? true : undefined}
      className={`flex items-center gap-[10px] m-0 ${className}`}>
      
      {line ?
      <span aria-hidden="true" className="w-8 h-px shrink-0 bg-line" /> :
      null}
      <span className={`text-eyebrow uppercase ref-truncate ${toneClass[tone]}`}>
        {children}
      </span>
    </Tag>);

}