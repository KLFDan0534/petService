import { useState } from 'react';
import { PawPrintIcon } from 'lucide-react';

export type LogoSize = 'sm' | 'md' | 'lg';

export interface LogoProps {
  /** Raster brand mark. Defaults to the product's `/logo.png`, used as-is. */
  src?: string;
  /** Accessible name for the mark. Ignored when the wordmark is visible (mark becomes decorative). */
  alt?: string;
  /** Brand wordmark rendered next to the mark. */
  wordmark?: string;
  /** Hide the wordmark and show the mark alone (compact headers, avatars, favicons). */
  showWordmark?: boolean;
  /** 32 / 48 / 56px mark. `md` (48px) matches the product header and footer. */
  size?: LogoSize;
  /** Renders the lockup as a link. */
  href?: string;
  onClick?: () => void;
  className?: string;
}

const MARK_SIZE: Record<LogoSize, string> = {
  sm: 'w-8 h-8 rounded-tile',
  md: 'w-12 h-12 rounded-[12px]',
  lg: 'w-14 h-14 rounded-[14px]'
};

const MARK_PX: Record<LogoSize, number> = {
  sm: 32,
  md: 48,
  lg: 56
};

const WORDMARK_SIZE: Record<LogoSize, string> = {
  sm: 'text-[15px]',
  md: 'text-[19px]',
  lg: 'text-[22px]'
};

export function Logo({
  src = '/logo.png',
  alt = '宠物寄养平台',
  wordmark = '宠物寄养平台',
  showWordmark = true,
  size = 'md',
  href,
  onClick,
  className = ''
}: LogoProps) {
  const [failed, setFailed] = useState(false);

  const mark = failed ?
  <span
    className={`${MARK_SIZE[size]} shrink-0 grid place-items-center bg-sand text-brand`}
    role={showWordmark ? undefined : 'img'}
    aria-label={showWordmark ? undefined : alt}
    aria-hidden={showWordmark ? true : undefined}>
    
      <PawPrintIcon size={Math.round(MARK_PX[size] * 0.5)} strokeWidth={1.75} aria-hidden="true" />
    </span> :

  <img
    src={src}
    alt={showWordmark ? '' : alt}
    aria-hidden={showWordmark ? true : undefined}
    width={MARK_PX[size]}
    height={MARK_PX[size]}
    onError={() => setFailed(true)}
    className={`${MARK_SIZE[size]} shrink-0 object-cover bg-surface`} />;



  const content =
  <>
      {mark}
      {showWordmark &&
    <span className={`${WORDMARK_SIZE[size]} font-semibold whitespace-nowrap ref-truncate`}>
          {wordmark}
        </span>
    }
    </>;


  const base = `inline-flex items-center gap-[9px] font-body text-ink transition-colors duration-fast ease-editorial ${className}`;

  if (href) {
    return (
      <a href={href} onClick={onClick} className={`${base} hover:text-brand`}>
        {content}
      </a>);

  }

  if (onClick) {
    return (
      <button type="button" onClick={onClick} className={`${base} hover:text-brand`}>
        {content}
      </button>);

  }

  return <span className={base}>{content}</span>;
}