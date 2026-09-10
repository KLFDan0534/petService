import React, { forwardRef, useCallback, useRef, useState } from 'react';
import { SearchIcon, XIcon } from 'lucide-react';

export type InputProps = {
  /** Optional leading icon, rendered at 14px from the left edge. */
  leadingIcon?: React.ReactNode;
  /** Shows a trailing clear button whenever the field has a value. */
  clearable?: boolean;
  /** Called when the clear button is pressed (after the value is cleared). */
  onClear?: () => void;
  /** Marks the field as invalid — border and focus ring turn critical. */
  invalid?: boolean;
  /** Accessible label for the clear button. */
  clearLabel?: string;
  /** Wrapper class, useful for width/flex control. */
  containerClassName?: string;
} & Omit<React.InputHTMLAttributes<HTMLInputElement>, 'size'>;

/** `s-search input` — border-color: color-mix(in srgb, var(--ref-ink) 25%, transparent) */
const HOVER_BORDER =
'hover:border-[color-mix(in_srgb,var(--ref-ink)_25%,transparent)]';
/** focus: 35% ink border + 3px brand @12% ring, outline none */
const FOCUS_BORDER =
'focus:border-[color-mix(in_srgb,var(--ref-ink)_35%,transparent)] focus:shadow-[0_0_0_3px_color-mix(in_srgb,var(--ref-brand)_12%,transparent)]';
const INVALID_FOCUS =
'focus:border-critical focus:shadow-[0_0_0_3px_color-mix(in_srgb,var(--ref-critical)_14%,transparent)]';

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
{
  leadingIcon,
  clearable = false,
  onClear,
  invalid = false,
  clearLabel = 'Clear',
  containerClassName = '',
  className = '',
  type = 'text',
  value,
  defaultValue,
  disabled,
  onChange,
  ...rest
},
forwardedRef)
{
  const innerRef = useRef<HTMLInputElement | null>(null);
  const setRefs = useCallback(
    (node: HTMLInputElement | null) => {
      innerRef.current = node;
      if (typeof forwardedRef === 'function') forwardedRef(node);else
      if (forwardedRef) forwardedRef.current = node;
    },
    [forwardedRef]
  );

  const isControlled = value !== undefined;
  const [uncontrolled, setUncontrolled] = useState(
    defaultValue === undefined ? '' : String(defaultValue)
  );
  const currentValue = isControlled ? String(value ?? '') : uncontrolled;

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    if (!isControlled) setUncontrolled(event.target.value);
    onChange?.(event);
  };

  const handleClear = () => {
    const node = innerRef.current;
    if (node) {
      // Fire a real input event so controlled consumers see the empty value.
      const setter = Object.getOwnPropertyDescriptor(
        window.HTMLInputElement.prototype,
        'value'
      )?.set;
      setter?.call(node, '');
      node.dispatchEvent(new Event('input', { bubbles: true }));
      node.focus();
    }
    if (!isControlled) setUncontrolled('');
    onClear?.();
  };

  const showClear = clearable && !disabled && currentValue.length > 0;

  return (
    <div
      className={`relative flex-1 min-w-[220px] ${containerClassName}`.trim()}>
      
      {leadingIcon ?
      <span
        aria-hidden="true"
        className="pointer-events-none absolute left-[14px] top-1/2 -translate-y-1/2 flex items-center text-[15px] text-muted [&>svg]:h-[15px] [&>svg]:w-[15px]">
        
          {leadingIcon}
        </span> :
      null}

      <input
        ref={setRefs}
        type={type}
        value={isControlled ? currentValue : undefined}
        defaultValue={isControlled ? undefined : defaultValue}
        disabled={disabled}
        aria-invalid={invalid || undefined}
        onChange={handleChange}
        className={[
        'w-full h-11 rounded-control border bg-surface text-ink text-control',
        'font-body transition-[border-color,box-shadow] duration-fast ease-editorial',
        'placeholder:text-muted focus:outline-none',
        invalid ? 'border-critical' : 'border-line',
        disabled ?
        'cursor-not-allowed bg-sand text-muted' :
        `${HOVER_BORDER} ${invalid ? INVALID_FOCUS : FOCUS_BORDER}`,
        leadingIcon ? 'pl-10' : 'pl-[14px]',
        clearable ? 'pr-10' : 'pr-[14px]',
        '[&::-webkit-search-cancel-button]:hidden',
        className].

        filter(Boolean).
        join(' ')}
        {...rest} />
      

      {showClear ?
      <button
        type="button"
        aria-label={clearLabel}
        onClick={handleClear}
        className="absolute right-2 top-1/2 -translate-y-1/2 inline-flex h-7 w-7 items-center justify-center rounded-tile bg-transparent text-[12px] text-muted transition-colors duration-fast ease-editorial hover:bg-sand hover:text-ink">
        
          <XIcon className="h-3.5 w-3.5" aria-hidden="true" />
        </button> :
      null}
    </div>);

});

/** Search-flavoured preset matching the services toolbar 1:1. */
export const SearchInput = forwardRef<HTMLInputElement, InputProps>(
  function SearchInput(props, ref) {
    return (
      <Input
        ref={ref}
        type="search"
        autoComplete="off"
        clearable
        leadingIcon={<SearchIcon />}
        {...props} />);


  }
);