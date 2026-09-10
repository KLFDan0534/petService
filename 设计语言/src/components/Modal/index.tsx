import { useCallback, useEffect, useId, useRef, type MouseEvent, type ReactNode } from 'react';
import { createPortal } from 'react-dom';
import { XIcon } from 'lucide-react';

/**
 * Ported from the `.dlg-*` dialog family duplicated across the order / pet
 * dialogs (CreateOrderDialog, ReviewDialog, TipDialog, PetFormDialog) and the
 * user views. Same anatomy — overlay, panel, head, body, foot, close — with the
 * panel width variants consolidated into a `size` prop and body scroll locking
 * handled once here instead of per dialog.
 */

export type ModalSize = 'sm' | 'md' | 'wide' | 'lg' | 'xl';

export interface ModalProps {
  /** Controls visibility. The dialog is unmounted while closed. */
  open: boolean;
  /** Fired by the close button, the Escape key and the overlay click. */
  onClose: () => void;
  /** Dialog title rendered in the head. Also labels the dialog. */
  title?: ReactNode;
  /** Short supporting copy rendered above the body content. */
  description?: ReactNode;
  /** Body content — fields, lists, details. */
  children?: ReactNode;
  /** Footer actions, right aligned. Omit for a dialog without actions. */
  footer?: ReactNode;
  /** Panel max width: sm 400 / md 440 / wide 520 / lg 640 / xl 960. */
  size?: ModalSize;
  /** Hide the ✕ control (e.g. a blocking confirm). */
  hideClose?: boolean;
  /** Close when the overlay itself is pressed. Defaults to true. */
  closeOnOverlayClick?: boolean;
  /** Close on Escape. Defaults to true. */
  closeOnEsc?: boolean;
  /** Accessible name when no visible `title` is provided. */
  ariaLabel?: string;
  /** Extra classes for the panel. */
  className?: string;
}

const SIZE_CLASS: Record<ModalSize, string> = {
  sm: 'max-w-[400px]',
  md: 'max-w-[440px]',
  wide: 'max-w-[520px]',
  lg: 'max-w-[640px]',
  xl: 'max-w-[960px]'
};

export function Modal({
  open,
  onClose,
  title,
  description,
  children,
  footer,
  size = 'md',
  hideClose = false,
  closeOnOverlayClick = true,
  closeOnEsc = true,
  ariaLabel,
  className = ''
}: ModalProps) {
  const panelRef = useRef<HTMLDivElement | null>(null);
  const restoreFocusRef = useRef<HTMLElement | null>(null);
  const titleId = useId();

  // Scroll lock — shared counter so nested / stacked dialogs restore correctly.
  useEffect(() => {
    if (!open) return;
    return lockBodyScroll();
  }, [open]);

  // Escape to dismiss.
  useEffect(() => {
    if (!open || !closeOnEsc) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.stopPropagation();
        onClose();
      }
    };
    document.addEventListener('keydown', onKeyDown);
    return () => document.removeEventListener('keydown', onKeyDown);
  }, [open, closeOnEsc, onClose]);

  // Move focus into the panel, and hand it back on close.
  useEffect(() => {
    if (!open) return;
    restoreFocusRef.current = document.activeElement as HTMLElement | null;
    const focusable = panelRef.current?.querySelector<HTMLElement>(FOCUSABLE);
    (focusable ?? panelRef.current)?.focus({ preventScroll: true });
    return () => restoreFocusRef.current?.focus?.({ preventScroll: true });
  }, [open]);

  const handleOverlayMouseDown = useCallback(
    (event: MouseEvent<HTMLDivElement>) => {
      if (!closeOnOverlayClick) return;
      if (event.target !== event.currentTarget) return;
      onClose();
    },
    [closeOnOverlayClick, onClose]
  );

  if (!open || typeof document === 'undefined') return null;

  const showHead = Boolean(title) || !hideClose;

  return createPortal(
    <div
      className="fixed inset-0 z-[2000] flex items-center justify-center p-5 backdrop-blur-[2px] animate-fade"
      style={{ background: 'rgba(10, 8, 6, 0.5)' }}
      onMouseDown={handleOverlayMouseDown}>
      
      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-label={title ? undefined : ariaLabel}
        aria-labelledby={title ? titleId : undefined}
        tabIndex={-1}
        className={`w-full ${SIZE_CLASS[size]} max-h-[min(90vh,720px)] overflow-y-auto rounded-card border border-line bg-surface outline-none animate-pop ${className}`}
        style={{
          boxShadow: '0 40px 80px -40px color-mix(in srgb, var(--ref-ink) 60%, transparent)'
        }}>
        
        {showHead &&
        <div className="flex items-center justify-between gap-4 px-6 pt-[22px]">
            {title ?
          <h3
            id={titleId}
            className="m-0 min-w-0 font-display text-[20px] font-medium leading-[1.25] tracking-editorial text-ink">
            
                {title}
              </h3> :

          <span aria-hidden="true" />
          }
            {!hideClose &&
          <button
            type="button"
            aria-label="关闭"
            onClick={onClose}
            className="flex h-8 w-8 shrink-0 items-center justify-center rounded-tile border-none bg-transparent text-muted transition-colors duration-fast ease-editorial hover:bg-sand hover:text-ink">
            
                <XIcon className="h-[15px] w-[15px]" aria-hidden="true" />
              </button>
          }
          </div>
        }

        {(description || children) &&
        <div className="px-6 pb-2 pt-[18px]">
            {description &&
          <p className="m-0 text-[13.5px] leading-[1.7] text-ink-soft">{description}</p>
          }
            {children}
          </div>
        }

        {footer &&
        <div className="flex flex-wrap items-center justify-end gap-2.5 px-6 pb-6 pt-5">
            {footer}
          </div>
        }
      </div>
    </div>,
    document.body
  );
}

const FOCUSABLE =
'input:not([disabled]), select:not([disabled]), textarea:not([disabled]), button:not([disabled]), a[href], [tabindex]:not([tabindex="-1"])';

let lockCount = 0;
let previousOverflow = '';

/** Locks `<body>` scrolling for as long as at least one modal is open. */
function lockBodyScroll(): () => void {
  if (lockCount === 0) {
    previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
  }
  lockCount += 1;
  return () => {
    lockCount = Math.max(0, lockCount - 1);
    if (lockCount === 0) document.body.style.overflow = previousOverflow;
  };
}