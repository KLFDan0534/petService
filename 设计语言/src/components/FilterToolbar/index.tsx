import React, { useState } from 'react';
import { SearchIcon, XIcon } from 'lucide-react';

export interface FilterToolbarProps {
  /** Current search value. Omit to let the toolbar manage its own state. */
  value?: string;
  /** Fired on every keystroke and when the clear button is pressed. */
  onValueChange?: (value: string) => void;
  /** Initial value when used uncontrolled. */
  defaultValue?: string;
  placeholder?: string;
  /** Accessible label for the search input. */
  searchLabel?: string;
  /** Accessible label for the toolbar region. */
  ariaLabel?: string;
  /** Show the trailing reset control (source: `v-if="hasFilters"`). */
  showReset?: boolean;
  resetLabel?: string;
  onReset?: () => void;
  /** Extra 44px-tall controls (selects, chips, sort) rendered after the field. */
  children?: React.ReactNode;
  className?: string;
}

const INPUT_CLASSES = [
'h-11 w-full rounded-control border border-line bg-surface px-10 text-control text-ink',
'placeholder:text-muted',
'transition-[border-color,box-shadow] duration-fast ease-editorial',
'hover:border-[color-mix(in_srgb,var(--ref-ink)_25%,transparent)]',
'focus:border-[color-mix(in_srgb,var(--ref-ink)_35%,transparent)]',
'focus:outline-none focus:shadow-[0_0_0_3px_color-mix(in_srgb,var(--ref-brand)_12%,transparent)]',
'disabled:cursor-not-allowed disabled:opacity-50'].
join(' ');

export function FilterToolbar({
  value,
  onValueChange,
  defaultValue = '',
  placeholder = '搜索服务名称或内容',
  searchLabel = '搜索服务',
  ariaLabel = '筛选预约服务',
  showReset = false,
  resetLabel = '清除筛选',
  onReset,
  children,
  className = ''
}: FilterToolbarProps) {
  const [internalValue, setInternalValue] = useState(defaultValue);
  const isControlled = value !== undefined;
  const query = isControlled ? value as string : internalValue;

  const setQuery = (next: string) => {
    if (!isControlled) setInternalValue(next);
    onValueChange?.(next);
  };

  return (
    <div
      role="search"
      aria-label={ariaLabel}
      className={`mt-7 flex flex-col items-stretch gap-3 min-[560px]:flex-row min-[560px]:flex-wrap min-[560px]:items-center ${className}`}>
      
      <div className="relative w-full min-[560px]:min-w-[220px] min-[560px]:max-w-[380px] min-[560px]:flex-1">
        <SearchIcon
          aria-hidden="true"
          className="pointer-events-none absolute left-[14px] top-1/2 h-[15px] w-[15px] -translate-y-1/2 text-muted" />
        
        <input
          type="search"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder={placeholder}
          aria-label={searchLabel}
          autoComplete="off"
          className={INPUT_CLASSES} />
        
        {query ?
        <button
          type="button"
          aria-label="清除搜索"
          onClick={() => setQuery('')}
          className="absolute right-2 top-1/2 flex h-7 w-7 -translate-y-1/2 items-center justify-center rounded-tile bg-transparent text-muted transition-colors duration-fast ease-editorial hover:bg-sand hover:text-ink">
          
            <XIcon className="h-3.5 w-3.5" aria-hidden="true" />
          </button> :
        null}
      </div>

      {children}

      {showReset ?
      <button
        type="button"
        onClick={onReset}
        className="inline-flex h-11 items-center justify-center gap-1.5 rounded-control bg-transparent px-3 text-[13px] text-ink-soft transition-colors duration-fast ease-editorial hover:text-brand">
        
          <XIcon className="h-3.5 w-3.5" aria-hidden="true" />
          {resetLabel}
        </button> :
      null}
    </div>);

}