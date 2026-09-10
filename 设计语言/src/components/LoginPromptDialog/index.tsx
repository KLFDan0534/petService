import React, { useCallback, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { LockIcon } from 'lucide-react';

export type LoginPromptDialogProps = {
  /** Whether the dialog is mounted and visible. */
  visible: boolean;
  /** Called when the scrim, the "暂不登录" button, or Escape dismisses the dialog. */
  onClose: () => void;
  /**
   * Called with the resolved login path (e.g. `/login?redirect=%2Forders`) after the
   * dialog closes. Wire this to your router's navigate/push.
   */
  onLogin?: (loginPath: string) => void;
  /** Path the user should return to after signing in. Sanitised before use. */
  loginRedirectPath?: string;
  /** Body copy. Defaults to the product's copy. */
  message?: string;
  /** Label for the dismiss action. */
  cancelLabel?: string;
  /** Label for the confirm action. */
  confirmLabel?: string;
};

/**
 * Only allow same-origin, single-slash paths through to the redirect query so a
 * crafted value can't bounce the user to another host (F-NAV-006).
 */
export function safeRedirect(raw: unknown): string {
  if (typeof raw !== 'string' || !raw.startsWith('/') || raw.startsWith('//')) {
    return '/dashboard';
  }
  return raw;
}

export function LoginPromptDialog({
  visible,
  onClose,
  onLogin,
  loginRedirectPath,
  message = '登录后即可使用该功能',
  cancelLabel = '暂不登录',
  confirmLabel = '去登录'
}: LoginPromptDialogProps) {
  const confirmRef = useRef<HTMLButtonElement | null>(null);

  const goLogin = useCallback(() => {
    onClose();
    const redirect = encodeURIComponent(safeRedirect(loginRedirectPath));
    onLogin?.('/login?redirect=' + redirect);
  }, [onClose, onLogin, loginRedirectPath]);

  useEffect(() => {
    if (!visible) return;
    confirmRef.current?.focus();
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', onKeyDown);
    return () => document.removeEventListener('keydown', onKeyDown);
  }, [visible, onClose]);

  if (!visible || typeof document === 'undefined') return null;

  return createPortal(
    <div
      className="fixed inset-0 z-[1000] flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm animate-fade"
      onClick={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}>
      
      <div
        role="dialog"
        aria-modal="true"
        aria-label={message}
        className="w-[min(90vw,400px)] max-w-[calc(100vw-32px)] max-h-[80vh] overflow-y-auto rounded-card bg-surface p-8 text-center shadow-lift animate-pop">
        
        <div
          aria-hidden="true"
          className="mx-auto mb-3 grid h-12 w-12 place-items-center rounded-[14px] bg-sand text-brand">
          
          <LockIcon className="h-[26px] w-[26px]" strokeWidth={1.75} />
        </div>
        <p className="mt-2 text-meta text-muted">{message}</p>
        <div className="mt-6 flex flex-wrap justify-center gap-3">
          <button
            type="button"
            onClick={onClose}
            className="inline-flex min-h-[40px] max-w-full items-center justify-center gap-2 whitespace-nowrap rounded-control border border-line bg-surface px-6 py-3 text-control font-semibold text-ink transition-colors duration-fast ease-editorial hover:border-brand hover:bg-sand hover:text-brand">
            
            {cancelLabel}
          </button>
          <button
            type="button"
            ref={confirmRef}
            onClick={goLogin}
            className="inline-flex min-h-[40px] max-w-full items-center justify-center gap-2 whitespace-nowrap rounded-control bg-brand px-6 py-3 text-control font-semibold text-white transition-colors duration-fast ease-editorial hover:bg-brand-deep active:translate-y-px">
            
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>,
    document.body
  );
}