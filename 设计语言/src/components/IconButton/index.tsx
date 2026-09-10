import React from 'react';

export type IconButtonSize = 'sm' | 'md' | 'lg';
export type IconButtonTone = 'default' | 'brand' | 'critical';

export interface IconButtonProps extends
  Omit<React.ButtonHTMLAttributes<HTMLButtonElement>, 'children'> {
  /** Accessible name — required, since the button renders an icon only. */
  label: string;
  /** The icon element, typically a `lucide-react` icon. */
  children: React.ReactNode;
  /** 24 / 28 / 32px square. `md` (28px) is the system default. */
  size?: IconButtonSize;
  /** Hover ink colour on the resting state — for selected/toggled affordances. */
  active?: boolean;
  /** Hover colour. `default` warms to ink, `brand`/`critical` for destructive or accent affordances. */
  tone?: IconButtonTone;
}

const SIZE_CLASSES: Record<IconButtonSize, string> = {
  sm: 'h-6 w-6 [&>svg]:h-3 [&>svg]:w-3',
  md: 'h-7 w-7 [&>svg]:h-3.5 [&>svg]:w-3.5',
  lg: 'h-8 w-8 [&>svg]:h-4 [&>svg]:w-4'
};

const TONE_HOVER_CLASSES: Record<IconButtonTone, string> = {
  default: 'hover:text-ink hover:bg-sand',
  brand: 'hover:text-brand hover:bg-sand',
  critical: 'hover:text-critical hover:bg-sand'
};

const TONE_ACTIVE_CLASSES: Record<IconButtonTone, string> = {
  default: 'text-ink bg-sand',
  brand: 'text-brand bg-sand',
  critical: 'text-critical bg-sand'
};

export function IconButton({
  label,
  children,
  size = 'md',
  tone = 'default',
  active = false,
  disabled = false,
  className = '',
  type = 'button',
  ...rest
}: IconButtonProps) {
  const restingClasses = active ?
  TONE_ACTIVE_CLASSES[tone] :
  `text-muted bg-transparent ${TONE_HOVER_CLASSES[tone]}`;

  return (
    <button
      {...rest}
      type={type}
      disabled={disabled}
      aria-label={label}
      title={label}
      className={[
      'inline-flex shrink-0 items-center justify-center rounded-tile text-xs',
      'transition-colors duration-fast ease-editorial',
      'disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:bg-transparent disabled:hover:text-muted',
      SIZE_CLASSES[size],
      restingClasses,
      className].

      filter(Boolean).
      join(' ')}>
      
      {children}
    </button>);

}