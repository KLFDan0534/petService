import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';

/** Order shape consumed by the review dialog (mirrors the API's `*_wsh` fields). */
export interface ReviewDialogOrder {
  id_wsh?: number | string;
  merchant_id_wsh?: number | string | null;
  merchant_name_wsh?: string | null;
  keeper_id_wsh?: number | string | null;
  keeper_name_wsh?: string | null;
  service_id_wsh?: number | string | null;
  service_name_wsh?: string | null;
}

export type ReviewDimensionType = 'merchant' | 'keeper' | 'service';

export interface ReviewDimension {
  type: ReviewDimensionType;
  label: string;
  targetId?: number | string | null;
  targetName: string;
  done: boolean;
}

export interface ReviewSubmitPayload {
  orderId?: number | string;
  targetType: ReviewDimensionType;
  targetId: number | string;
  score: number;
  content: string;
}

export interface ReviewDialogProps {
  /** Controls dialog visibility. Nothing renders when false. */
  visible: boolean;
  /** The order being reviewed. */
  order?: ReviewDialogOrder | null;
  /** Dimension types that have already been reviewed for this order. */
  doneTypes?: ReviewDimensionType[];
  /** Fired when the scrim, cancel button, or Escape closes the dialog. */
  onClose?: () => void;
  /** Fired with the review payload. May return a promise — the submit button stays busy until it settles. */
  onReviewed?: (payload: ReviewSubmitPayload) => void | Promise<unknown>;
}

const SCORES = [5, 4, 3, 2, 1];

export function ReviewDialog({
  visible,
  order,
  doneTypes = [],
  onClose,
  onReviewed
}: ReviewDialogProps) {
  const [score, setScore] = useState(5);
  const [content, setContent] = useState('');
  const [activeDimension, setActiveDimension] =
  useState<ReviewDimensionType>('merchant');
  const [submitting, setSubmitting] = useState(false);
  const panelRef = useRef<HTMLDivElement>(null);

  const dimensions: ReviewDimension[] = useMemo(() => {
    const o = order || {};
    return (
    [
    {
      type: 'merchant' as const,
      label: '商家',
      targetId: o.merchant_id_wsh,
      targetName: o.merchant_name_wsh || `商家 #${o.merchant_id_wsh}`
    },
    {
      type: 'keeper' as const,
      label: '看护人',
      targetId: o.keeper_id_wsh,
      targetName: o.keeper_name_wsh || `看护人 #${o.keeper_id_wsh}`
    },
    {
      type: 'service' as const,
      label: '服务',
      targetId: o.service_id_wsh,
      targetName: o.service_name_wsh || `服务 #${o.service_id_wsh}`
    }] as
    const).
    map((dim) => ({ ...dim, done: doneTypes.includes(dim.type) }));
  }, [order, doneTypes]);

  const currentDimension =
  dimensions.find((dim) => dim.type === activeDimension) || dimensions[0];

  // Reset the form each time the dialog opens, landing on the first un-reviewed dimension.
  useEffect(() => {
    if (!visible) return;
    setScore(5);
    setContent('');
    setSubmitting(false);
    const firstUndone = dimensions.find((dim) => !dim.done && dim.targetId);
    setActiveDimension(firstUndone ? firstUndone.type : 'merchant');
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [visible]);

  useEffect(() => {
    if (!visible) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose?.();
    };
    window.addEventListener('keydown', onKeyDown);
    panelRef.current?.focus();
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [visible, onClose]);

  const submitReview = useCallback(async () => {
    const dim = currentDimension;
    if (!order || !dim?.targetId || dim.done || submitting) return;
    setSubmitting(true);
    try {
      await onReviewed?.({
        orderId: order.id_wsh,
        targetType: dim.type,
        targetId: dim.targetId,
        score,
        content
      });
    } finally {
      setSubmitting(false);
    }
  }, [currentDimension, order, submitting, onReviewed, score, content]);

  if (!visible) return null;

  const disabled = submitting || !!currentDimension?.done || !currentDimension?.targetId;
  const controlClass =
  'w-full rounded-control border border-line bg-surface px-3 py-2 text-control text-ink placeholder:text-muted transition-colors duration-fast ease-editorial hover:border-brand/40 focus:border-brand focus:outline-none disabled:opacity-60';

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-ink/40 p-0 animate-fade sm:items-center sm:p-gutter"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose?.();
      }}>
      
      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="review-dialog-title"
        tabIndex={-1}
        className="grid max-h-[92vh] w-full max-w-[440px] gap-3 overflow-y-auto rounded-t-frame border border-line bg-surface p-6 shadow-lift animate-pop focus:outline-none sm:rounded-card">
        
        <h2
          id="review-dialog-title"
          className="font-display text-display-xs text-ink">
          
          评价本次服务
        </h2>

        <div
          className="flex flex-wrap gap-2"
          role="tablist"
          aria-label="评价维度">
          
          {dimensions.map((dim) => {
            const active = activeDimension === dim.type;
            return (
              <button
                key={dim.type}
                type="button"
                role="tab"
                aria-selected={active}
                onClick={() => setActiveDimension(dim.type)}
                className={[
                'rounded-full border px-3 py-1.5 text-meta transition-colors duration-fast ease-editorial',
                active ?
                'border-brand bg-brand text-white' :
                'border-line bg-surface text-muted hover:border-brand/40 hover:text-ink-soft'].
                join(' ')}>
                
                {dim.label}
                {dim.done &&
                <span className="ml-1.5 text-[11px] opacity-85">已评价</span>
                }
              </button>);

          })}
        </div>

        {order &&
        <>
            <div className="text-meta font-semibold text-ink ref-truncate">
              {currentDimension.targetName}
            </div>

            <select
            value={score}
            onChange={(event) => setScore(Number(event.target.value))}
            aria-label="评分"
            className={controlClass}
            data-numeric="true">
            
              {SCORES.map((value) =>
            <option key={value} value={value}>
                  {value} 分
                </option>
            )}
            </select>

            <textarea
            value={content}
            onChange={(event) => setContent(event.target.value)}
            rows={4}
            placeholder="说说这次服务体验"
            aria-label="评价内容"
            className={`${controlClass} resize-y leading-relaxed`} />
          

            <div className="mt-2 flex justify-end gap-2">
              <button
              type="button"
              onClick={() => onClose?.()}
              className="rounded-control border border-line bg-surface px-3 py-1.5 text-control text-ink-soft transition-colors duration-fast ease-editorial hover:border-brand/40 hover:text-ink active:translate-y-px">
              
                取消
              </button>
              <button
              type="button"
              disabled={disabled}
              onClick={submitReview}
              className="rounded-control border border-brand bg-brand px-3 py-1.5 text-control text-white transition-colors duration-fast ease-editorial hover:border-brand-deep hover:bg-brand-deep active:translate-y-px disabled:cursor-not-allowed disabled:opacity-55 disabled:hover:bg-brand">
              
                {submitting ?
              '提交中...' :
              currentDimension.done ?
              '该维度已评价' :
              '提交评价'}
              </button>
            </div>

            {currentDimension.done ?
          <p className="m-0 text-[12px] leading-relaxed text-muted">
                该维度已评价，感谢你的反馈。
              </p> :
          !currentDimension.targetId ?
          <p className="m-0 text-[12px] leading-relaxed text-muted">
                该订单缺少{currentDimension.label}信息，无法评价。
              </p> :
          null}
          </>
        }
      </div>
    </div>);

}