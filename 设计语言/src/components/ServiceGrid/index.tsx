import React, { useState } from 'react';
import {
  ArrowRightIcon,
  CalendarIcon,
  HeartIcon,
  ImageIcon,
  PawPrintIcon } from
'lucide-react';

/**
 * ServiceGrid — ported from `src/components/dashboard/ServiceGrid.vue`
 * with the Editorial Warm appearance that `src/views/user/Services.vue`
 * (lines 514-613) used to force onto it via `:deep()` overrides.
 *
 * The overrides now live in the component itself: 18px hairline card,
 * 4/3 media on sand, 20px body padding, 11px/700 uppercase brand
 * category, 20px display heading, 22px display price, hairline action
 * bar, 11px-radius brand primary button, 12% brand distance chip.
 */

export type ServiceUnit = 'day' | 'session' | 'hour' | string;

export interface ServiceGridItem {
  id_wsh: string | number;
  name_wsh: string;
  firstImage?: string;
  category_name_wsh?: string;
  merchant_name_wsh?: string;
  distance_m_wsh?: number | null;
  price_wsh?: number | string | null;
  unit_wsh?: ServiceUnit;
  description_wsh?: string;
}

export interface ServiceGridProps {
  services?: ServiceGridItem[];
  loading?: boolean;
  /** Renders the favorite toggle (source: `authStore.isLoggedIn`). */
  isLoggedIn?: boolean;
  /** Ids of services currently favorited. */
  favoriteIds?: Array<string | number>;
  onSelect?: (serviceId: string | number) => void;
  onBook?: (service: ServiceGridItem) => void;
  onToggleFavorite?: (serviceId: string | number) => void;
  emptyTitle?: string;
  emptyDescription?: string;
  className?: string;
}

const GRID_CLASS =
'grid min-w-0 grid-cols-1 gap-[18px] sm:grid-cols-2 sm:gap-gutter lg:grid-cols-3';

export function ServiceGrid({
  services = [],
  loading = false,
  isLoggedIn = false,
  favoriteIds = [],
  onSelect,
  onBook,
  onToggleFavorite,
  emptyTitle = '没有找到匹配的服务',
  emptyDescription = '调整关键词或服务分类后再试试。',
  className = ''
}: ServiceGridProps) {
  if (loading) {
    return (
      <div
        className={`${GRID_CLASS} ${className}`}
        aria-label="服务加载中"
        aria-busy="true">
        
        {Array.from({ length: 6 }).map((_, index) =>
        <article
          key={index}
          aria-hidden="true"
          className="flex min-h-[430px] min-w-0 flex-col overflow-hidden rounded-card border border-line bg-surface">
          
            <div className="aspect-[4/3] w-full ref-shimmer" />
            <div className="grid gap-[14px] p-5">
              <span className="h-[14px] w-[58%] rounded ref-shimmer" />
              <span className="h-[14px] w-full rounded ref-shimmer" />
              <span className="h-[14px] w-[76%] rounded ref-shimmer" />
            </div>
          </article>
        )}
      </div>);

  }

  if (!services.length) {
    return (
      <div
        className={`grid min-h-[260px] place-items-center content-center gap-[10px] rounded-card border border-dashed border-line bg-surface p-8 text-center text-muted ${className}`}>
        
        <PawPrintIcon className="h-9 w-9 text-brand" aria-hidden="true" />
        <h3 className="font-display text-[18px] font-medium tracking-editorial text-ink">
          {emptyTitle}
        </h3>
        <p className="text-meta text-muted">{emptyDescription}</p>
      </div>);

  }

  return (
    <div className={`${GRID_CLASS} ${className}`}>
      {services.map((service) =>
      <ServiceGridCard
        key={service.id_wsh}
        service={service}
        isLoggedIn={isLoggedIn}
        favorited={favoriteIds.some((id) => String(id) === String(service.id_wsh))}
        onSelect={onSelect}
        onBook={onBook}
        onToggleFavorite={onToggleFavorite} />

      )}
    </div>);

}

interface ServiceGridCardProps {
  service: ServiceGridItem;
  isLoggedIn: boolean;
  favorited: boolean;
  onSelect?: (serviceId: string | number) => void;
  onBook?: (service: ServiceGridItem) => void;
  onToggleFavorite?: (serviceId: string | number) => void;
}

function ServiceGridCard({
  service,
  isLoggedIn,
  favorited,
  onSelect,
  onBook,
  onToggleFavorite
}: ServiceGridCardProps) {
  const [failed, setFailed] = useState(false);
  const showFallback = !service.firstImage || failed;
  const distance = formatServiceDistance(service.distance_m_wsh);

  return (
    <article className="group flex min-w-0 flex-col overflow-hidden rounded-card border border-line bg-surface transition-[transform,border-color,box-shadow] duration-normal ease-editorial hover:-translate-y-1 hover:border-ink/[0.16] hover:shadow-lift">
      <button
        type="button"
        className="block w-full overflow-hidden border-0 bg-sand p-0 aspect-[4/3]"
        aria-label={`查看${service.name_wsh}详情`}
        onClick={() => onSelect?.(service.id_wsh)}>
        
        {showFallback ?
        <div
          className="flex h-full w-full flex-col items-center justify-center gap-2 bg-sand p-6 text-center text-muted"
          role="img"
          aria-label={`${service.name_wsh}服务图片`}>
          
            <ImageIcon className="h-9 w-9 text-brand" aria-hidden="true" />
            <span className="ref-clamp-2 text-meta font-semibold">
              {service.name_wsh}
            </span>
          </div> :

        <img
          src={service.firstImage}
          alt={`${service.name_wsh}服务图片`}
          loading="lazy"
          decoding="async"
          onError={() => setFailed(true)}
          className="block h-full w-full object-cover transition-transform duration-slow ease-editorial group-hover:scale-[1.06]" />

        }
      </button>

      <div className="flex flex-1 flex-col p-5">
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0">
            <span className="text-label font-bold uppercase tracking-label text-brand">
              {service.category_name_wsh || '宠物服务'}
            </span>
            <h3 className="mt-1.5 font-display text-[20px] font-medium leading-[1.3] tracking-editorial text-ink [text-wrap:balance]">
              {service.name_wsh}
            </h3>
            {(service.merchant_name_wsh || distance) &&
            <span className="mt-0.5 inline-flex max-w-full items-center gap-2 text-meta text-muted">
                <span className="ref-truncate">{service.merchant_name_wsh}</span>
                {distance &&
              <em
                className="shrink-0 whitespace-nowrap rounded-full bg-brand/[0.12] px-2 py-px text-[11px] font-bold not-italic text-brand"
                data-numeric="true">
                
                    {distance}
                  </em>
              }
              </span>
            }
          </div>

          <div className="flex-none text-right">
            <strong
              className="block font-display text-[22px] font-semibold leading-[1.2] text-ink"
              data-numeric="true">
              
              ¥{formatMoney(service.price_wsh)}
            </strong>
            <span className="mt-[3px] block text-meta text-muted">
              / {unitLabel(service.unit_wsh) || '次'}
            </span>
          </div>
        </div>

        <p className="ref-clamp-3 mt-4 mb-5 min-h-[72px] text-[13.5px] leading-[1.7] text-ink-soft">
          {service.description_wsh || '查看服务详情并选择适合爱宠的照护方案。'}
        </p>

        <div className="mt-auto flex flex-col items-stretch gap-3 border-t border-line pt-4 sm:flex-row sm:items-center sm:justify-between">
          <button
            type="button"
            onClick={() => onSelect?.(service.id_wsh)}
            className="group/link inline-flex min-h-[44px] items-center gap-1.5 border-0 bg-transparent p-0 text-[14px] font-bold text-ink transition-colors duration-fast ease-editorial hover:text-brand">
            
            查看详情
            <ArrowRightIcon
              className="h-4 w-4 transition-transform duration-normal ease-editorial group-hover/link:translate-x-1"
              aria-hidden="true" />
            
          </button>

          <div className="flex items-center gap-2">
            {isLoggedIn &&
            <button
              type="button"
              onClick={() => onToggleFavorite?.(service.id_wsh)}
              aria-label={`收藏${service.name_wsh}`}
              aria-pressed={favorited}
              className="flex h-11 w-11 flex-none items-center justify-center rounded-control border border-line bg-surface text-muted transition-colors duration-fast ease-editorial hover:border-brand hover:text-brand">
              
                <HeartIcon
                className={`h-4 w-4 ${favorited ? 'fill-brand text-brand' : ''}`}
                aria-hidden="true" />
              
              </button>
            }
            <button
              type="button"
              onClick={() => onBook?.(service)}
              className="inline-flex min-h-11 w-full items-center justify-center gap-1.5 rounded-control bg-brand px-4 text-control font-semibold text-white transition-colors duration-fast ease-editorial hover:bg-brand-deep active:translate-y-px sm:w-auto">
              
              <CalendarIcon className="h-4 w-4" aria-hidden="true" />
              立即预约
            </button>
          </div>
        </div>
      </div>
    </article>);

}

/** 距离文案：<1km 用 m，否则用 km（如 850m、1.2km）。 */
export function formatServiceDistance(meters?: number | string | null): string {
  if (meters == null || meters === '') return '';
  const value = Number(meters);
  if (!Number.isFinite(value) || value < 0) return '';
  if (value < 1000) return `${Math.max(1, Math.round(value))}m`;
  return `${(value / 1000).toFixed(1)}km`;
}

export function formatMoney(value?: number | string | null): string {
  const amount = Number(value);
  if (!Number.isFinite(amount)) return '--';
  return amount.toLocaleString('zh-CN', { maximumFractionDigits: 2 });
}

export function unitLabel(value?: ServiceUnit): string {
  const key = String(value || '').trim().toLowerCase();
  if (['day', 'days', '天'].includes(key)) return '天';
  if (['session', 'sessions', '次'].includes(key)) return '次';
  if (['hour', 'hours', '小时'].includes(key)) return '小时';
  return '';
}