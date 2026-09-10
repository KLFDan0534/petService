import React from 'react';

export interface TextLinkProps {
  /** Renders an anchor when provided (maps the source's `to` / `href` props). */
  href?: string;
  /** Anchor target, only applied when `href` is set. */
  target?: string;
  rel?: string;
  /** Hides the trailing arrow when false. */
  showArrow?: boolean;
  disabled?: boolean;
  className?: string;
  onClick?: (event: React.MouseEvent<HTMLElement>) => void;
  children: React.ReactNode;
}

const ROOT_CLASSES =
'group inline-flex items-center gap-[6px] text-[13px] font-medium font-body text-ink bg-transparent border-none p-0 cursor-pointer no-underline transition-colors duration-[150ms] ease-editorial hover:text-brand disabled:cursor-not-allowed disabled:opacity-45 disabled:hover:text-ink';

export function TextLink({
  href,
  target,
  rel,
  showArrow = true,
  disabled = false,
  className = '',
  onClick,
  children
}: TextLinkProps) {
  const content =
  <>
      <span className="border-b border-[color-mix(in_srgb,var(--ref-ink)_25%,transparent)] pb-[2px] transition-colors duration-[150ms] ease-editorial group-hover:border-brand group-disabled:group-hover:border-[color-mix(in_srgb,var(--ref-ink)_25%,transparent)]">
        {children}
      </span>
      {showArrow ?
    <span
      aria-hidden="true"
      className="text-[14px] leading-none transition-transform duration-[150ms] ease-editorial group-hover:translate-x-[4px]">
      
          →
        </span> :
    null}
    </>;


  if (href && !disabled) {
    return (
      <a
        href={href}
        target={target}
        rel={rel ?? (target === '_blank' ? 'noreferrer noopener' : undefined)}
        className={`${ROOT_CLASSES} ${className}`.trim()}
        onClick={onClick}>
        
        {content}
      </a>);

  }

  return (
    <button
      type="button"
      disabled={disabled}
      className={`${ROOT_CLASSES} ${className}`.trim()}
      onClick={onClick}>
      
      {content}
    </button>);

}