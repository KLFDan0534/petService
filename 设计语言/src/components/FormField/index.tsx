import React from 'react';

/* -------------------------------------------------------------------------- */
/* Types                                                                      */
/* -------------------------------------------------------------------------- */

export interface FormFieldProps {
  /** Field label. Rendered as a real <label> bound to `htmlFor`. */
  label: React.ReactNode;
  /** id of the control this label describes. */
  htmlFor?: string;
  /** Appends the terracotta required marker after the label. */
  required?: boolean;
  /** Quiet helper copy. Hidden while `error` is present. */
  hint?: React.ReactNode;
  /** Validation message. Takes precedence over `hint`. */
  error?: React.ReactNode;
  /** Action rendered inline to the right of the control (e.g. 获取验证码). */
  trailing?: React.ReactNode;
  /** Span the full width of a parent form grid. */
  wide?: boolean;
  /** Dim the field — used for derived / read-only values. */
  muted?: boolean;
  className?: string;
  /** The control: `FormInput`, `FormTextarea`, a select, or any custom input. */
  children: React.ReactNode;
}

export interface FormInputProps extends
  React.InputHTMLAttributes<HTMLInputElement> {
  /** Paints the error border. Mirror the field's `error` state here. */
  invalid?: boolean;
}

export interface FormTextareaProps extends
  React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  invalid?: boolean;
}

/* -------------------------------------------------------------------------- */
/* Shared control styling (ported from .auth-input / .ka-input)               */
/* -------------------------------------------------------------------------- */

const controlBase =
'w-full min-w-0 border border-line rounded-[10px] bg-surface text-ink text-[14px] ' +
'placeholder:text-muted transition-[border-color,box-shadow] duration-fast ease-editorial ' +
'focus:outline-none focus:border-brand focus:shadow-focus ' +
'disabled:bg-sand disabled:text-ink-soft disabled:cursor-not-allowed';

const errorBorder = 'border-[color-mix(in_srgb,var(--ref-brand-deep)_60%,transparent)]';

/* -------------------------------------------------------------------------- */
/* Controls                                                                   */
/* -------------------------------------------------------------------------- */

export const FormInput = React.forwardRef<HTMLInputElement, FormInputProps>(
  function FormInput({ invalid = false, className = '', ...rest }, ref) {
    return (
      <input
        ref={ref}
        aria-invalid={invalid || undefined}
        className={[
        controlBase,
        'h-11 px-[14px]',
        invalid ? errorBorder : '',
        className].

        filter(Boolean).
        join(' ')}
        {...rest} />);


  }
);

export const FormTextarea = React.forwardRef<
  HTMLTextAreaElement,
  FormTextareaProps>(
  function FormTextarea({ invalid = false, className = '', ...rest }, ref) {
    return (
      <textarea
        ref={ref}
        aria-invalid={invalid || undefined}
        className={[
        controlBase,
        'min-h-24 py-3 px-[14px] leading-[1.7] resize-y',
        invalid ? errorBorder : '',
        className].

        filter(Boolean).
        join(' ')}
        {...rest} />);


  });

/* -------------------------------------------------------------------------- */
/* FormField                                                                  */
/* -------------------------------------------------------------------------- */

export function FormField({
  label,
  htmlFor,
  required = false,
  hint,
  error,
  trailing,
  wide = false,
  muted = false,
  className = '',
  children
}: FormFieldProps) {
  const labelNode =
  <label
    htmlFor={htmlFor}
    className="text-[12.5px] font-medium text-ink-soft">
    
      {label}
      {required ?
    <span className="ml-[3px] text-brand" aria-hidden="true">
          *
        </span> :
    null}
    </label>;


  return (
    <div
      className={[
      'grid gap-2 min-w-0',
      wide ? 'col-span-full' : '',
      muted ? 'opacity-[0.82]' : '',
      className].

      filter(Boolean).
      join(' ')}>
      
      {trailing ?
      <div className="flex items-end gap-3">
          <div className="grid flex-1 min-w-0 gap-2">
            {labelNode}
            {children}
          </div>
          {trailing}
        </div> :

      <>
          {labelNode}
          {children}
        </>
      }

      {error ?
      <p className="m-0 text-[11px] leading-[1.6] text-brand-deep" role="alert">
          {error}
        </p> :
      hint ?
      <p className="m-0 text-[11px] leading-[1.7] text-muted">{hint}</p> :
      null}
    </div>);

}