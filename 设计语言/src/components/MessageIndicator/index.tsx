import React from 'react';
import { MailIcon } from 'lucide-react';

export interface MessageIndicatorProps {
  /** Number of unread notifications. The indicator is shown when greater than 0. */
  unreadCount?: number;
  /** Render the numeric count instead of a plain dot. */
  showCount?: boolean;
  /** Highest number rendered before switching to `{max}+`. */
  max?: number;
  /** Accessible name / native tooltip for the control. */
  label?: string;
  /** Fired on click — wire this to your notifications route. */
  onClick?: () => void;
  disabled?: boolean;
  className?: string;
}

/**
 * Ported from `src/components/common/MessageIndicator.vue`.
 * The original relied on global `--color-*` variables and Element Plus icons,
 * neither of which resolve here, so those styles are expressed as Tailwind
 * utilities on the design system's editorial-warm tokens.
 */
export function MessageIndicator({
  unreadCount = 0,
  showCount = false,
  max = 99,
  label = '消息提醒',
  onClick,
  disabled = false,
  className = ''
}: MessageIndicatorProps) {
  const hasUnread = unreadCount > 0;
  const display = unreadCount > max ? `${max}+` : String(unreadCount);

  return (
    <button
      type="button"
      title={label}
      aria-label={hasUnread ? `${label}（${display} 条未读）` : label}
      onClick={onClick}
      disabled={disabled}
      className={[
      'relative grid h-11 w-11 place-items-center rounded-control border-none bg-transparent',
      'text-ink transition-colors duration-fast ease-editorial',
      'hover:text-brand hover:bg-brand/10',
      'focus-visible:shadow-focus',
      'disabled:cursor-not-allowed disabled:text-muted disabled:hover:bg-transparent disabled:hover:text-muted',
      className].

      filter(Boolean).
      join(' ')}>
      
      <MailIcon className="h-5 w-5" aria-hidden="true" />

      {hasUnread && (
      showCount ?
      <span
        data-numeric="true"
        className="absolute right-0.5 top-0.5 min-w-[16px] rounded-full bg-critical px-1 text-center text-[10px] font-medium leading-4 text-white ring-2 ring-surface">
        
            {display}
          </span> :

      <span className="absolute right-2 top-2 h-2 w-2 rounded-full bg-critical" />)
      }
    </button>);

}