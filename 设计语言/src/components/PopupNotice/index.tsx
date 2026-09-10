import React, { useCallback, useEffect, useState } from 'react';

/**
 * PopupNotice — ported from src/components/common/PopupNotice.vue.
 *
 * Structure, copy, queue behaviour and dismiss semantics are 1:1 with the Vue
 * source. The source's scoped CSS depended on `--color-*` variables supplied by
 * a global stylesheet that does not exist in this artifact, so those rules are
 * reproduced with the design system's own tokens (`bg-surface`, `text-ink`,
 * `bg-brand`, `shadow-lift`, `rounded-tile`) at the same measurements.
 *
 * Data loading stays with the caller: pass the popup queue in via `notices` and
 * persist the dismissal in `onDismiss`.
 */

export interface PopupNoticeItem {
  id_wsh: string | number;
  title_wsh: string;
  content_wsh: string;
  type_wsh?: string;
  delivery_type_wsh?: string;
}

export interface PopupNoticeProps {
  /** Popup queue, shown one at a time in order. */
  notices: PopupNoticeItem[];
  /** Called when a notice is dismissed (close button, primary action, or scrim). */
  onDismiss?: (notice: PopupNoticeItem, index: number) => void;
  /** Called once the whole queue has been cleared. */
  onDismissAll?: () => void;
  /** Badge label above the title. Defaults to the source's 公告. */
  badgeLabel?: string;
}

/** Faithful port of the source's `isPopupNotice` guard. */
export function isPopupNotice(notice: Partial<PopupNoticeItem> | null | undefined): boolean {
  return (
    String(notice?.type_wsh || '').toLowerCase() === 'notice' &&
    String(notice?.delivery_type_wsh || '').
    toLowerCase().
    includes('popup'));

}

export function PopupNotice({
  notices,
  onDismiss,
  onDismissAll,
  badgeLabel = '公告'
}: PopupNoticeProps) {
  const [popups, setPopups] = useState<PopupNoticeItem[]>(notices);
  const [currentIndex, setCurrentIndex] = useState(0);

  useEffect(() => {
    setPopups(notices);
    setCurrentIndex(0);
  }, [notices]);

  const currentNotice = popups[currentIndex] || null;

  const dismiss = useCallback(
    (idx: number) => {
      const notice = popups[idx];
      if (!notice) return;
      onDismiss?.(notice, idx);
      if (popups.length === 1) {
        setPopups([]);
        setCurrentIndex(0);
        onDismissAll?.();
      } else {
        const next = popups.slice(0, idx).concat(popups.slice(idx + 1));
        setPopups(next);
        setCurrentIndex((prev) => prev >= next.length ? Math.max(0, next.length - 1) : prev);
      }
    },
    [popups, onDismiss, onDismissAll]
  );

  useEffect(() => {
    if (!currentNotice) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') dismiss(currentIndex);
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [currentNotice, currentIndex, dismiss]);

  if (!currentNotice) return null;

  return (
    <div
      className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/45 animate-fade"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) dismiss(currentIndex);
      }}>
      
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="popup-notice-title"
        className="w-[90%] max-w-[480px] overflow-hidden rounded-tile bg-surface text-ink shadow-lift animate-pop">
        
        <div className="flex items-center justify-between px-5 pt-[18px]">
          <div className="min-w-0">
            <span className="mb-2 inline-flex h-6 items-center rounded-full bg-brand px-[9px] text-[12px] font-bold text-white">
              {badgeLabel}
            </span>
            <h3
              id="popup-notice-title"
              className="m-0 font-display text-[17px] font-medium leading-snug tracking-editorial ref-clamp-2">
              
              {currentNotice.title_wsh}
            </h3>
          </div>
          <button
            type="button"
            aria-label="关闭公告"
            onClick={() => dismiss(currentIndex)}
            className="shrink-0 self-start px-1 text-[22px] leading-none text-muted transition-colors duration-fast ease-editorial hover:text-ink">
            
            &times;
          </button>
        </div>

        <div className="max-h-[300px] overflow-y-auto whitespace-pre-wrap px-5 py-[14px] text-[14px] leading-[1.6] text-ink-soft">
          {currentNotice.content_wsh}
        </div>

        <div className="flex items-center justify-between px-5 pb-[18px] pt-3">
          {popups.length > 1 ?
          <span className="text-[12px] text-muted" data-numeric="true">
              {currentIndex + 1} / {popups.length}
            </span> :

          <span />
          }
          <button
            type="button"
            onClick={() => dismiss(currentIndex)}
            className="inline-flex min-h-[40px] items-center justify-center gap-2 whitespace-nowrap rounded-control bg-brand px-4 py-2 text-[13px] font-semibold text-white transition-colors duration-fast ease-editorial hover:bg-brand-deep active:translate-y-[1px]">
            
            {popups.length > 1 ? '下一条' : '我知道了'}
          </button>
        </div>
      </div>
    </div>);

}