import React, { useEffect, useRef, useState } from 'react';

export interface TipDialogOrder {
  /** Order id used by the tip endpoint (`id_wsh` in the source data model). */
  id_wsh: string | number;
  /** Optional service name, shown as context under the dialog title. */
  serviceName?: string;
}

export interface TipDialogProps {
  /** Whether the dialog is visible. */
  open: boolean;
  /** The order being tipped. Submission is blocked while this is null. */
  order?: TipDialogOrder | null;
  /** Called when the scrim, the cancel button, or Escape dismisses the dialog. */
  onClose: () => void;
  /** Called with (orderId, amount, message) when the tip is confirmed. */
  onTip: (orderId: string | number, amount: number, message: string) => void;
  /** Disables the confirm action and shows a pending label. */
  submitting?: boolean;
}

export function TipDialog({
  open,
  order = null,
  onClose,
  onTip,
  submitting = false
}: TipDialogProps) {
  const [amount, setAmount] = useState('');
  const [message, setMessage] = useState('');
  const amountRef = useRef<HTMLInputElement>(null);

  // Source behaviour: both fields reset every time the dialog opens.
  useEffect(() => {
    if (open) {
      setAmount('');
      setMessage('');
      amountRef.current?.focus();
    }
  }, [open]);

  useEffect(() => {
    if (!open) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', onKeyDown);
    return () => document.removeEventListener('keydown', onKeyDown);
  }, [open, onClose]);

  if (!open) return null;

  const canSubmit = Boolean(amount) && Number(amount) > 0 && Boolean(order) && !submitting;

  const submitTip = () => {
    if (!amount || !order) return;
    onTip(order.id_wsh, Number(amount), message);
  };

  const fieldClass =
  'w-full rounded-control border border-line bg-canvas px-3.5 py-2.5 text-control text-ink ' +
  'placeholder:text-muted transition-colors duration-fast ease-editorial ' +
  'hover:border-brand/40 focus:border-brand focus:outline-none';

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-ink/50 p-0 backdrop-blur-sm animate-fade sm:items-center sm:p-gutter"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}>
      
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="tip-dialog-title"
        className="grid w-full max-w-[420px] gap-3 rounded-t-frame border border-line bg-surface p-6 shadow-lift animate-pop max-h-[92vh] overflow-y-auto sm:rounded-card sm:p-8">
        
        <h2 id="tip-dialog-title" className="font-display text-display-xs text-ink">
          打赏服务
        </h2>

        {order?.serviceName ?
        <p className="ref-truncate text-meta text-muted">{order.serviceName}</p> :
        null}

        <input
          ref={amountRef}
          value={amount}
          onChange={(event) => setAmount(event.target.value)}
          type="number"
          min="0.01"
          step="0.01"
          inputMode="decimal"
          data-numeric="true"
          aria-label="打赏金额"
          placeholder="金额"
          className={fieldClass} />
        

        <textarea
          value={message}
          onChange={(event) => setMessage(event.target.value)}
          rows={3}
          aria-label="留言"
          placeholder="留言"
          className={`${fieldClass} resize-none`} />
        

        <div className="mt-2 flex flex-wrap justify-end gap-2">
          <button
            type="button"
            onClick={onClose}
            className="rounded-control border border-line bg-surface px-4 py-2 text-control text-ink-soft transition-colors duration-fast ease-editorial hover:bg-sand active:translate-y-px">
            
            取消
          </button>
          <button
            type="button"
            onClick={submitTip}
            disabled={!canSubmit}
            className="rounded-control bg-brand px-4 py-2 text-control text-white transition-colors duration-fast ease-editorial hover:bg-brand-deep active:translate-y-px disabled:cursor-not-allowed disabled:opacity-45 disabled:hover:bg-brand">
            
            {submitting ? '打赏中…' : '确认打赏'}
          </button>
        </div>
      </div>
    </div>);

}