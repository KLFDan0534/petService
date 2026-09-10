import React from 'react';
import { ArrowRightIcon, Loader2Icon } from 'lucide-react';

/* -------------------------------------------------------------------------
 * Button — the consolidated `.cta` family.
 *
 * Ported from the product's per-view `.cta` blocks (canonically
 * src/views/user/CustomerServiceApply.vue:479, KeeperWorkflow.vue:631-639,
 * Login.vue:277-289). The scoped Vue stylesheets can't load here, so the
 * declarations are reproduced 1:1 as Tailwind utilities against the same
 * `--ref-*` tokens. The per-view `--r-btn: 10px` alias is normalised to the
 * canonical 11px (`rounded-control`).
 * ---------------------------------------------------------------------- */

export type ButtonVariant =
'primary' |
'outline' |
'dark' |
'light' |
'ghost' |
'quiet' |
'danger';

export type ButtonSize = 'sm' | 'md' | 'lg';

export interface ButtonProps extends
  Omit<React.ButtonHTMLAttributes<HTMLButtonElement>, 'children'> {
  /** Visual treatment. Defaults to `primary`. */
  variant?: ButtonVariant;
  /** 34 / 42 / 48px control heights. Defaults to `md` (42px). */
  size?: ButtonSize;
  /** Trailing arrow affordance that nudges 3px on hover (the `.cta-arrow`). */
  arrow?: boolean;
  /** Leading element — pass a `lucide-react` icon. */
  icon?: React.ReactNode;
  /** Swaps the trailing slot for a spinner and disables the control. */
  loading?: boolean;
  /** Stretch to the container. Defaults to `true` for `size="lg"`. */
  fullWidth?: boolean;
  /** Renders an `<a>` instead of a `<button>`. */
  href?: string;
  children?: React.ReactNode;
}

/* `.cta` */
const base =
'group inline-flex items-center justify-center gap-2 rounded-control border border-transparent font-body font-medium no-underline align-middle whitespace-nowrap cursor-pointer transition-[background-color,border-color,color,transform,filter] duration-fast ease-editorial focus-visible:outline-none focus-visible:shadow-focus ' +
/* `.cta:hover { transform: translateY(-1px) }` — never while disabled */
'enabled:hover:-translate-y-px aria-disabled:hover:translate-y-0 ' +
/* `.cta:disabled { opacity: .5; cursor: not-allowed; transform: none }` */
'disabled:opacity-50 disabled:cursor-not-allowed disabled:translate-y-0 ' +
'aria-disabled:opacity-50 aria-disabled:cursor-not-allowed';

const sizes: Record<ButtonSize, string> = {
  /* `.cta-sm` */
  sm: 'h-[34px] px-[13px] text-[12.5px]',
  /* `.cta` */
  md: 'h-[42px] px-[18px] text-[13px]',
  /* `.cta-lg` */
  lg: 'h-12 px-5 text-[14px]'
};

const variants: Record<ButtonVariant, string> = {
  /* `.cta-primary` */
  primary: 'bg-brand text-white enabled:hover:bg-brand-deep',
  /* `.cta-outline` */
  outline:
  'bg-surface text-ink border-line enabled:hover:border-[color-mix(in_srgb,var(--ref-ink)_35%,transparent)]',
  /* `.cta-dark` */
  dark: 'bg-ink text-cream enabled:hover:brightness-[1.18]',
  /* `.cta-light` — fixed cream on fixed paper, for use over dark sections */
  light: 'bg-[#f5efe7] text-[#17130f] enabled:hover:bg-white',
  /* `.cta-ghost` */
  ghost: 'bg-transparent text-ink-soft enabled:hover:text-brand',
  /* `.cta-quiet` */
  quiet: 'bg-transparent text-ink-soft border-transparent enabled:hover:text-ink',
  /* `.cta-danger` */
  danger:
  'bg-[color-mix(in_srgb,var(--ref-critical)_9%,transparent)] text-critical border-[color-mix(in_srgb,var(--ref-critical)_28%,transparent)] enabled:hover:bg-[color-mix(in_srgb,var(--ref-critical)_15%,transparent)]'
};

export function Button({
  variant = 'primary',
  size = 'md',
  arrow = false,
  icon,
  loading = false,
  fullWidth,
  href,
  className = '',
  children,
  disabled,
  type = 'button',
  ...rest
}: ButtonProps) {
  const stretched = fullWidth ?? size === 'lg';
  const isDisabled = Boolean(disabled) || loading;

  const classes = [
  base,
  sizes[size],
  variants[variant],
  stretched ? 'w-full' : '',
  className].

  filter(Boolean).
  join(' ');

  const content =
  <>
      {loading ?
    <Loader2Icon
      aria-hidden="true"
      className="h-[15px] w-[15px] shrink-0 animate-spin" /> :


    icon
    }
      {children != null && <span className="ref-truncate">{children}</span>}
      {/* `.cta-arrow` — 3px nudge on hover */}
      {arrow && !loading &&
    <ArrowRightIcon
      aria-hidden="true"
      className="h-[15px] w-[15px] shrink-0 transition-transform duration-fast ease-editorial group-hover:translate-x-[3px]" />

    }
    </>;


  if (href) {
    return (
      <a
        href={isDisabled ? undefined : href}
        className={classes}
        aria-disabled={isDisabled || undefined}
        aria-busy={loading || undefined}
        tabIndex={isDisabled ? -1 : undefined}
        {...rest as React.AnchorHTMLAttributes<HTMLAnchorElement>}>
        
        {content}
      </a>);

  }

  return (
    <button
      type={type}
      className={classes}
      disabled={isDisabled}
      aria-busy={loading || undefined}
      {...rest}>
      
      {content}
    </button>);

}