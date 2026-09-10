import React, { useState } from 'react';

/**
 * Timeline — ported 1:1 from the product's care-record timeline.
 *
 * Source: `src/components/keeper/KeeperDailyTab.vue` (`.timeline` / `.timeline-item`
 * / `.timeline-item__meta` / `.timeline-photos` / `.empty-inline`) styled by the
 * consuming view `src/views/user/KeeperWorkflow.vue:780-785` via `:deep(...)`.
 *
 * That view-level `:deep` styling is the leak this component consolidates: the
 * measured values (10px item gap, 8px top margin, hairline `--ref-line` card on
 * `--ref-surface` at `--r-card` = 14px radius, 14px padding, 11.5px muted meta
 * line, 13px/1.6 `--ref-ink-soft` body, 88px auto-fill photo grid capped at
 * 420px) now live here as Tailwind utilities on this design system's tokens.
 */

export type TimelineRecordType =
'feed' |
'activity' |
'medication' |
'health' |
'note' |
(string & {});

export interface TimelineItem {
  id: string | number;
  /** Raw record type, e.g. `feed`. Mapped through `RECORD_TYPE_LABELS`. */
  type?: TimelineRecordType;
  /** Explicit meta label; overrides the mapped `type`. */
  typeLabel?: string;
  /** Record time — rendered next to the type in the meta line. */
  time?: string | number | Date | null;
  content?: string;
  /** Comma-separated string (as stored by the API) or an array of urls. */
  images?: string | string[] | null;
}

export interface TimelineProps {
  items: TimelineItem[];
  /** Shown in place of the list when `items` is empty. */
  emptyLabel?: string;
  /** Renders skeleton rows instead of the list. */
  loading?: boolean;
  /** Number of skeleton rows while `loading`. */
  loadingCount?: number;
  onImageClick?: (url: string, item: TimelineItem) => void;
  className?: string;
}

/** Record type → Chinese label map, as used by the keeper daily tab. */
export const RECORD_TYPE_LABELS: Record<string, string> = {
  feed: '喂食',
  activity: '活动',
  medication: '用药',
  health: '健康',
  note: '日志'
};

export function Timeline({
  items,
  emptyLabel = '暂无动态',
  loading = false,
  loadingCount = 3,
  onImageClick,
  className = ''
}: TimelineProps) {
  if (loading) {
    return (
      <div className={`grid gap-2.5 mt-2 ${className}`.trim()}>
        {Array.from({ length: loadingCount }).map((_, index) =>
        <div
          key={index}
          className="rounded-[14px] border border-line bg-surface p-3.5"
          aria-hidden="true">
          
            <div className="ref-shimmer h-[11.5px] w-40 rounded-full" />
            <div className="ref-shimmer mt-2.5 h-[13px] w-full rounded-full" />
            <div className="ref-shimmer mt-1.5 h-[13px] w-3/5 rounded-full" />
          </div>
        )}
        <span className="sr-only">动态加载中…</span>
      </div>);

  }

  if (items.length === 0) {
    return (
      <div className={`grid gap-2.5 mt-2 ${className}`.trim()}>
        <div className="py-3 text-meta text-muted">{emptyLabel}</div>
      </div>);

  }

  return (
    <div className={`grid gap-2.5 mt-2 ${className}`.trim()}>
      {items.map((item) => {
        const urls = parseImageUrls(item.images);
        const label =
        item.typeLabel ?? (
        item.type ? RECORD_TYPE_LABELS[item.type] ?? item.type : '');

        return (
          <article
            key={item.id}
            className="rounded-[14px] border border-line bg-surface p-3.5">
            
            <div
              className="break-all text-[11.5px] leading-[1.6] text-muted"
              data-numeric="true">
              
              {[label, formatDateTime(item.time)].filter(Boolean).join(' · ')}
            </div>

            {item.content ?
            <p className="my-1.5 text-meta leading-[1.6] text-ink-soft">
                {item.content}
              </p> :
            null}

            {urls.length > 0 ?
            <div className="mt-2 grid max-w-[420px] grid-cols-[repeat(auto-fill,minmax(88px,1fr))] gap-2">
                {urls.map((url) =>
              <TimelinePhoto
                key={url}
                url={url}
                onClick={
                onImageClick ? () => onImageClick(url, item) : undefined
                } />

              )}
              </div> :
            null}
          </article>);

      })}
    </div>);

}

function TimelinePhoto({
  url,
  onClick



}: {url: string;onClick?: () => void;}) {
  const [failed, setFailed] = useState(false);

  const frame =
  'w-full aspect-square rounded-[10px] border border-line bg-sand object-cover';

  if (failed) {
    return (
      <div
        className={`${frame} flex items-center justify-center text-[10px] text-muted`}
        role="img"
        aria-label="照片加载失败">
        
        加载失败
      </div>);

  }

  const image =
  <img
    src={url}
    alt="动态照片"
    loading="lazy"
    onError={() => setFailed(true)}
    className={frame} />;



  if (!onClick) return image;

  return (
    <button
      type="button"
      onClick={onClick}
      className="block rounded-[10px] transition-transform duration-normal ease-editorial hover:-translate-y-0.5 focus-visible:shadow-focus"
      aria-label="查看动态照片">
      
      {image}
    </button>);

}

/** `"a.jpg,b.jpg"` (as stored by the API) or an array → clean url list. */
export function parseImageUrls(value: string | string[] | null | undefined) {
  const raw = Array.isArray(value) ? value : String(value ?? '').split(',');
  return raw.map((url) => String(url).trim()).filter(Boolean);
}

export function formatDateTime(value: TimelineItem['time']) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '-';
  return date.toLocaleString();
}