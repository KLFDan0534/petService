import { useMemo, useState } from 'react';
import {
  ArrowUpRightIcon,
  BanIcon,
  ClockIcon,
  ImageIcon } from
'lucide-react';

/* ────────────────────────────── Types ────────────────────────────── */

export type OrderStatus =
'pending' |
'paid' |
'confirmed' |
'delivered' |
'received' |
'in_progress' |
'completed' |
'cancelled' |
'refunding' |
'refunded' |
(string & {});

/** Raw order record as returned by the pet-service API (`*_wsh` fields). */
export interface OrderRecord {
  id_wsh?: number | string;
  order_no_wsh?: string;
  status_wsh?: OrderStatus;
  service_id_wsh?: number | string;
  service_name_wsh?: string;
  service_images_wsh?: string | string[] | null;
  start_photo_wsh?: string;
  pet_id_wsh?: number | string;
  pet_name_wsh?: string;
  pet_avatar_wsh?: string;
  pet_breed_wsh?: string;
  pet_age_wsh?: number | string;
  pet_weight_wsh?: number | string;
  merchant_id_wsh?: number | string;
  merchant_name_wsh?: string;
  merchant_phone_wsh?: string;
  contact_phone_wsh?: string;
  keeper_name_wsh?: string;
  start_date_wsh?: string;
  end_date_wsh?: string;
  created_at_wsh?: string;
  delivery_time_wsh?: string;
  delivered_at_wsh?: string;
  received_at_wsh?: string;
  final_amount_wsh?: number | string;
  total_amount_wsh?: number | string;
  settlement_amount_wsh?: number | string;
  discount_wsh?: number | string;
  days_wsh?: number | string;
  billing_unit_wsh?: string;
  quantity_wsh?: number | string;
  duration_minutes_wsh?: number | string;
  has_feedback_wsh?: boolean;
  [key: string]: unknown;
}

export type PaymentMethod = 'balance' | (string & {});

export interface OrderCardProps {
  /** The order record to render. */
  order: OrderRecord;
  /** Current clock, passed down so a list of cards shares one ticking timer. */
  nowMs?: number;
  /** Disables destructive / mutating actions while a request is in flight. */
  processing?: boolean;
  onCancel?: (order: OrderRecord) => void;
  onPay?: (method: PaymentMethod) => void;
  onDeliver?: (order: OrderRecord) => void;
  onReview?: (order: OrderRecord) => void;
  onTip?: (order: OrderRecord) => void;
  onViewDetail?: (order: OrderRecord) => void;
  /** "再次预约" — navigates to the service booking page in the product. */
  onRebook?: (order: OrderRecord) => void;
}

/* ────────────────────── Status / phase vocabulary ────────────────────── */

type StatusTone = 'action' | 'queued' | 'active' | 'done' | 'closed';

const STATUS_META: Record<string, {label: string;tone: StatusTone;}> = {
  pending: { label: '待付款', tone: 'action' },
  paid: { label: '已支付', tone: 'queued' },
  confirmed: { label: '待送达', tone: 'queued' },
  delivered: { label: '已送达', tone: 'active' },
  received: { label: '已接收', tone: 'active' },
  in_progress: { label: '服务中', tone: 'active' },
  completed: { label: '已完成', tone: 'done' },
  cancelled: { label: '已取消', tone: 'closed' },
  refunding: { label: '退款中', tone: 'action' },
  refunded: { label: '已退款', tone: 'closed' }
};

const PHASE_INDEX: Record<string, number> = {
  pending: 0,
  paid: 1,
  confirmed: 1,
  delivered: 2,
  received: 2,
  in_progress: 2,
  completed: 3,
  cancelled: -1,
  refunding: -1,
  refunded: -1
};

export const ORDER_PHASES = [
{ key: 'placed', label: '已下单', timeField: 'created_at_wsh' },
{ key: 'paid', label: '已支付', timeField: 'delivery_time_wsh' },
{ key: 'staying', label: '寄养中', timeField: 'delivered_at_wsh' },
{ key: 'done', label: '已完成', timeField: 'received_at_wsh' }] as
const;

const TONE_CLASS: Record<StatusTone, string> = {
  action: 'bg-brand text-white border-transparent',
  queued:
  'bg-surface text-ink border-[color-mix(in_srgb,var(--ref-ink)_20%,transparent)]',
  active: 'bg-[#3f5347]/[0.12] text-[#3f5347] border-[#3f5347]/25',
  done: 'bg-[color-mix(in_srgb,var(--ref-ink)_5%,transparent)] text-ink-soft border-transparent',
  closed: 'bg-transparent text-muted border-line'
};

/* ─────────────────────────── Button tokens ─────────────────────────── */

const BTN_BASE =
'inline-flex h-[38px] items-center justify-center rounded-[10px] border px-4 text-[13px] font-medium transition-[background-color,border-color,color,transform,opacity] duration-fast disabled:cursor-not-allowed disabled:opacity-45';

const BTN_VARIANT = {
  default:
  'border-line bg-surface text-ink-soft enabled:hover:-translate-y-px enabled:hover:border-[color-mix(in_srgb,var(--ref-ink)_30%,transparent)] enabled:hover:bg-sand enabled:hover:text-ink',
  primary:
  'border-brand bg-brand text-white enabled:hover:-translate-y-px enabled:hover:border-brand-deep enabled:hover:bg-brand-deep',
  danger:
  'border-[#a03422]/35 bg-transparent text-[#a03422] enabled:hover:-translate-y-px enabled:hover:border-[#a03422]/45 enabled:hover:bg-[#a03422]/[0.09]',
  quiet:
  'border-transparent bg-transparent text-ink-soft enabled:hover:-translate-y-px enabled:hover:bg-sand enabled:hover:text-ink'
} as const;

const ANCHOR_QUIET =
'border-transparent bg-transparent text-ink-soft hover:-translate-y-px hover:bg-sand hover:text-ink';

function cx(...parts: Array<string | false | null | undefined>): string {
  return parts.filter(Boolean).join(' ');
}

/* ───────────────────────────── Component ───────────────────────────── */

export function OrderCard({
  order,
  nowMs,
  processing = false,
  onCancel,
  onPay,
  onDeliver,
  onReview,
  onTip,
  onViewDetail,
  onRebook
}: OrderCardProps) {
  const now = nowMs ?? Date.now();

  const status = order.status_wsh || '';
  const meta = STATUS_META[status] || {
    label: status || '未知状态',
    tone: 'closed' as StatusTone
  };
  const phaseIndex = PHASE_INDEX[status] ?? -1;
  const isActive = ['delivered', 'received', 'in_progress'].includes(status);
  const isCompleted = status === 'completed';

  const serviceName = order.service_name_wsh || '宠物寄养服务';
  const petName = order.pet_name_wsh || `宠物 #${order.pet_id_wsh ?? '-'}`;
  const merchantName =
  order.merchant_name_wsh || `商家 #${order.merchant_id_wsh ?? '-'}`;
  const amount = Number(
    order.final_amount_wsh ||
    order.total_amount_wsh ||
    order.settlement_amount_wsh ||
    0
  );
  const discount = Number(order.discount_wsh ?? 0);
  const billing = billingText(order);
  const nights = nightsBetween(order.start_date_wsh, order.end_date_wsh);
  const contactPhone = order.merchant_phone_wsh || order.contact_phone_wsh || '';

  const petSummaryText =
  [
  order.pet_breed_wsh,
  order.pet_age_wsh ? `${order.pet_age_wsh} 岁` : '',
  order.pet_weight_wsh ? `${order.pet_weight_wsh} kg` : ''].

  filter(Boolean).
  join(' · ') || '宠物信息待完善';

  const cover = useMemo(() => {
    const images = parseImages(order.service_images_wsh);
    return images[0] || order.start_photo_wsh || '';
  }, [order.service_images_wsh, order.start_photo_wsh]);

  const paymentRemaining =
  status === 'pending' ? getPaymentTimeoutRemaining(order, now) : null;
  const showPaymentCountdown = paymentRemaining != null;
  const paymentCountdownText = formatPaymentTimeoutRemaining(paymentRemaining);

  const amountBlock = (mobile?: boolean) =>
  <>
      <p className="text-[12px] text-muted">实付金额</p>
      <p
      className={cx(
        'flex items-baseline gap-[3px]',
        mobile ? 'mt-[3px]' : 'mt-1.5'
      )}>
      
        <span className="text-[12px] text-muted">¥</span>
        <span
        className="font-display text-[27px] leading-none tracking-[-0.02em] text-ink"
        data-numeric="true">
        
          {formatMoney(amount)}
        </span>
      </p>
    </>;


  return (
    <article className="group overflow-hidden rounded-card border border-line bg-surface transition-[transform,box-shadow,border-color] duration-normal hover:-translate-y-[3px] hover:border-[color-mix(in_srgb,var(--ref-ink)_15%,transparent)] hover:shadow-[0_28px_60px_-40px_color-mix(in_srgb,var(--ref-ink)_45%,transparent)]">
      <button
        type="button"
        className="block w-full cursor-pointer border-0 bg-transparent p-0 text-left"
        aria-label={`查看订单 ${serviceName} 详情`}
        onClick={() => onViewDetail?.(order)}>
        
        <div className="flex gap-5 p-5">
          {/* Cover + pet avatar badge */}
          <div className="relative shrink-0 self-start">
            <OrderCover
              src={cover}
              alt={serviceName}
              className="h-[92px] w-[92px] overflow-hidden rounded-[14px]" />
            
            <span className="absolute -bottom-[9px] -right-[9px] flex h-10 w-10 items-center justify-center overflow-hidden rounded-full border-2 border-surface bg-sand">
              {order.pet_avatar_wsh ?
              <img
                src={order.pet_avatar_wsh}
                alt={petName}
                loading="lazy"
                decoding="async"
                className="h-full w-full object-cover" /> :


              <span className="font-display text-[13px] text-brand">
                  {petName.charAt(0)}
                </span>
              }
            </span>
          </div>

          {/* Info */}
          <div className="flex min-w-0 flex-1 flex-col">
            <div className="flex items-start justify-between gap-3">
              <p
                className="ref-truncate font-mono text-[12px] text-[color-mix(in_srgb,var(--ref-muted)_82%,transparent)]"
                data-numeric="true">
                
                {order.order_no_wsh || `#${order.id_wsh}`}
              </p>
              <div className="flex shrink-0 items-center gap-2">
                {isCompleted && !order.has_feedback_wsh &&
                <span className="whitespace-nowrap rounded-full border border-[color-mix(in_srgb,var(--ref-brand)_25%,transparent)] bg-[color-mix(in_srgb,var(--ref-brand)_6%,transparent)] px-[9px] py-[5px] text-[11px] leading-none text-brand-deep">
                    待反馈
                  </span>
                }
                <span
                  className={cx(
                    'inline-flex items-center whitespace-nowrap rounded-full border px-[10px] py-[5px] text-[11px] font-semibold leading-none',
                    TONE_CLASS[meta.tone] ?? TONE_CLASS.closed
                  )}>
                  
                  {meta.label}
                </span>
              </div>
            </div>

            <h3 className="ref-truncate mt-2 font-display text-[19px] font-medium leading-[1.3] tracking-editorial text-ink transition-colors duration-fast group-hover:text-brand">
              {serviceName}
            </h3>

            <div className="mt-[9px] flex flex-wrap items-center gap-x-4 gap-y-1.5 text-[12.5px] text-ink-soft">
              <span className="inline-flex min-w-0 items-center">
                {petName}
                {petSummaryText &&
                <span className="text-muted">&nbsp;· {petSummaryText}</span>
                }
              </span>
              <span
                className="inline-flex min-w-0 items-center"
                data-numeric="true">
                
                {formatDayRange(order.start_date_wsh, order.end_date_wsh)}
                {nights > 0 &&
                <span className="text-muted">&nbsp;· {nights} 晚</span>
                }
              </span>
            </div>

            <div className="mt-[9px] flex flex-wrap items-center gap-x-4 gap-y-1.5 text-[12.5px] text-muted">
              <span className="ref-truncate inline-flex min-w-0 items-center">
                {merchantName}
              </span>
              {order.keeper_name_wsh &&
              <span className="ref-truncate inline-flex min-w-0 items-center">
                  照护师 {order.keeper_name_wsh}
                </span>
              }
            </div>

            {/* Mobile amount */}
            <div className="mt-3 hidden max-[900px]:block">
              {amountBlock(true)}
            </div>
          </div>

          {/* Amount column */}
          <div className="flex w-[180px] shrink-0 flex-col items-start border-l border-line pl-5 max-[900px]:hidden">
            {amountBlock()}
            <p className="mt-2 flex flex-wrap items-center gap-x-2.5 gap-y-[3px] text-[12px] text-muted">
              {billing && <span>{billing}</span>}
              {discount > 0 &&
              <span className="text-brand-deep">
                  已优惠 ¥{formatMoney(discount)}
                </span>
              }
            </p>
            <span className="mt-auto inline-flex items-center gap-1.5 pt-3.5 text-[12.5px] text-muted">
              查看详情
              <ArrowUpRightIcon
                className="h-3.5 w-3.5 transition-transform duration-normal group-hover:-translate-y-[2px] group-hover:translate-x-[2px]"
                aria-hidden="true" />
              
            </span>
          </div>
        </div>
      </button>

      {/* Payment countdown */}
      {showPaymentCountdown &&
      <div className="flex flex-wrap items-center gap-x-2.5 gap-y-1 border-t border-[color-mix(in_srgb,var(--ref-brand)_15%,transparent)] bg-[color-mix(in_srgb,var(--ref-brand)_5%,transparent)] px-5 py-3">
          <ClockIcon
          className="h-3.5 w-3.5 shrink-0 text-brand-deep"
          aria-hidden="true" />
        
          {(paymentRemaining ?? 0) <= 0 ?
        <p className="text-[12.5px] text-brand-deep">
              支付已超时，订单即将自动取消。
            </p> :

        <>
              <p className="text-[12.5px] text-brand-deep">
                请在
                <span
              className="mx-[3px] font-mono text-[15px] font-semibold"
              data-numeric="true">
              
                  {paymentCountdownText}
                </span>
                内完成支付
              </p>
              <span className="text-[12px] text-muted">
                超时后订单将自动取消，档期释放给其他家长
              </span>
            </>
        }
        </div>
      }

      {/* Progress */}
      <div className="border-t border-line bg-[color-mix(in_srgb,var(--ref-cream)_45%,var(--ref-surface))] px-5 py-4">
        {phaseIndex >= 0 ?
        <ol
          className="m-0 grid list-none grid-cols-4 gap-2 p-0"
          aria-label="服务进度">
          
            {ORDER_PHASES.map((phase, index) =>
          <li key={phase.key} className="min-w-0">
                <div className="flex items-center gap-1.5">
                  <span
                aria-hidden="true"
                className={cx(
                  'h-2 w-2 shrink-0 rounded-full',
                  index === phaseIndex &&
                  'bg-brand shadow-[0_0_0_3px_color-mix(in_srgb,var(--ref-brand)_18%,transparent)]',
                  index < phaseIndex && 'bg-brand',
                  index > phaseIndex && 'bg-line'
                )} />
              
                  {index < ORDER_PHASES.length - 1 &&
              <span
                aria-hidden="true"
                className="relative h-px flex-1 overflow-hidden bg-line">
                
                      <span
                  className={cx(
                    'absolute inset-0 origin-left bg-brand transition-transform duration-slow ease-editorial',
                    index < phaseIndex ? 'scale-x-100' : 'scale-x-0'
                  )} />
                
                    </span>
              }
                </div>
                <p
              className={cx(
                'ref-truncate mt-2 text-[12.5px] leading-none',
                index === phaseIndex ?
                'font-semibold text-ink' :
                index < phaseIndex ?
                'text-ink-soft' :
                'text-muted'
              )}>
              
                  {phase.label}
                </p>
                <p
              className="ref-truncate mt-1 text-[11px] text-muted"
              data-numeric="true">
              
                  {phaseStamp(order, phase.timeField)}
                </p>
              </li>
          )}
          </ol> :

        <div className="flex items-center gap-2 text-[12.5px] text-muted">
            <BanIcon className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
            <span>
              该订单已
              {meta.label === '退款中' ? '进入退款流程' : '关闭'}
              ，服务流程未继续。
            </span>
          </div>
        }
      </div>

      {/* Actions */}
      <footer className="flex flex-wrap items-center justify-end gap-2 border-t border-line px-5 py-3.5">
        {status === 'pending' &&
        <>
            <button
            type="button"
            className={cx(BTN_BASE, BTN_VARIANT.danger)}
            disabled={processing}
            onClick={() => onCancel?.(order)}>
            
              取消订单
            </button>
            <button
            type="button"
            className={cx(BTN_BASE, BTN_VARIANT.primary)}
            disabled={processing}
            onClick={() => onPay?.('balance')}>
            
              余额支付
            </button>
          </>
        }

        {status === 'paid' &&
        <>
            <span className="mr-auto text-[12.5px] text-muted">
              门店确认中，通常 30 分钟内回复
            </span>
            <button
            type="button"
            className={cx(BTN_BASE, BTN_VARIANT.default)}
            onClick={() => onViewDetail?.(order)}>
            
              订单详情
            </button>
          </>
        }

        {status === 'confirmed' &&
        <>
            <button
            type="button"
            className={cx(BTN_BASE, BTN_VARIANT.default)}
            onClick={() => onViewDetail?.(order)}>
            
              订单详情
            </button>
            <button
            type="button"
            className={cx(BTN_BASE, BTN_VARIANT.primary)}
            disabled={processing}
            onClick={() => onDeliver?.(order)}>
            
              确认已送达
            </button>
          </>
        }

        {isActive &&
        <>
            {contactPhone &&
          <a
            className={cx(BTN_BASE, ANCHOR_QUIET)}
            href={`tel:${contactPhone}`}>
            
                联系门店
              </a>
          }
            <button
            type="button"
            className={cx(BTN_BASE, BTN_VARIANT.primary)}
            onClick={() => onViewDetail?.(order)}>
            
              查看服务动态
            </button>
          </>
        }

        {isCompleted &&
        <>
            <button
            type="button"
            className={cx(BTN_BASE, BTN_VARIANT.quiet)}
            onClick={() => onRebook?.(order)}>
            
              再次预约
            </button>
            <button
            type="button"
            className={cx(BTN_BASE, BTN_VARIANT.default)}
            disabled={processing}
            onClick={() => onTip?.(order)}>
            
              打赏照护师
            </button>
            {!order.has_feedback_wsh ?
          <button
            type="button"
            className={cx(BTN_BASE, BTN_VARIANT.primary)}
            disabled={processing}
            onClick={() => onReview?.(order)}>
            
                评价服务
              </button> :

          <button
            type="button"
            className={cx(BTN_BASE, BTN_VARIANT.default)}
            onClick={() => onViewDetail?.(order)}>
            
                订单详情
              </button>
          }
          </>
        }

        {['cancelled', 'refunded', 'refunding'].includes(status) &&
        <>
            <button
            type="button"
            className={cx(BTN_BASE, BTN_VARIANT.quiet)}
            onClick={() => onViewDetail?.(order)}>
            
              订单详情
            </button>
            <button
            type="button"
            className={cx(BTN_BASE, BTN_VARIANT.default)}
            onClick={() => onRebook?.(order)}>
            
              再次预约
            </button>
          </>
        }
      </footer>
    </article>);

}

/* ─────────────────────────── Local media ─────────────────────────── */

interface OrderCoverProps {
  src?: string;
  alt?: string;
  className?: string;
}

function OrderCover({ src = '', alt = '', className }: OrderCoverProps) {
  const [failed, setFailed] = useState(false);
  const showFallback = !src || failed;

  return (
    <div className={cx('relative min-w-0 overflow-hidden bg-sand', className)}>
      {showFallback ?
      <div
        className="flex h-full w-full items-center justify-center bg-[color-mix(in_srgb,var(--ref-brand)_8%,var(--ref-sand))]"
        role={alt ? 'img' : undefined}
        aria-label={alt || undefined}>
        
          <ImageIcon className="h-6 w-6 text-brand" aria-hidden="true" />
        </div> :

      <img
        key={src}
        className="block h-full w-full object-cover"
        src={src}
        alt={alt}
        loading="lazy"
        decoding="async"
        onError={() => setFailed(true)} />

      }
    </div>);

}

/* ──────────────────────────── Domain utils ──────────────────────────── */

export const ORDER_PAYMENT_TIMEOUT_MS = 15 * 60 * 1000;

function parseOrderTime(value?: string): number | null {
  if (!value) return null;
  const text = String(value).trim();
  const normalized = /^\d{4}-\d{2}-\d{2} \d{2}/.test(text) ?
  text.replace(' ', 'T') :
  text;
  const time = new Date(normalized).getTime();
  return Number.isNaN(time) ? null : time;
}

export function getPaymentTimeoutRemaining(
order: OrderRecord,
now = Date.now())
: number | null {
  const createdAt = parseOrderTime(order?.created_at_wsh);
  if (!createdAt) return null;
  return Math.max(0, createdAt + ORDER_PAYMENT_TIMEOUT_MS - now);
}

export function formatPaymentTimeoutRemaining(ms: number | null): string {
  if (ms == null) return '--:--';
  const totalSeconds = Math.max(0, Math.ceil(ms / 1000));
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
}

const UNIT_ALIASES: Record<string, 'day' | 'session' | 'hour'> = {
  day: 'day',
  days: 'day',
  天: 'day',
  session: 'session',
  sessions: 'session',
  次: 'session',
  hour: 'hour',
  hours: 'hour',
  小时: 'hour'
};

function normalizeUnit(value: unknown): 'day' | 'session' | 'hour' | null {
  const key = String(value || '').
  trim().
  toLowerCase();
  return UNIT_ALIASES[key] || null;
}

function unitLabel(unit: 'day' | 'session' | 'hour'): string {
  if (unit === 'day') return '天';
  if (unit === 'session') return '次';
  return '小时';
}

function durationText(minutes: unknown): string {
  const value = Number(minutes || 0);
  if (value <= 0) return '';
  if (value % 1440 === 0) return `${value / 1440} 天`;
  if (value % 60 === 0) return `${value / 60} 小时`;
  return `${value} 分钟`;
}

/** Human-readable billing description, e.g. "3 天"、"1 次（60 分钟）". */
export function billingText(order?: OrderRecord): string {
  if (!order) return '';
  const unit = normalizeUnit(order.billing_unit_wsh);
  if (unit === null) {
    const days = Number(order.days_wsh || 0);
    return days > 0 ? `${days} 天` : '';
  }
  const quantity = Number(order.quantity_wsh || 0);
  if (quantity <= 0) return unitLabel(unit);
  if (unit === 'day') return `${quantity} 天`;
  if (unit === 'session') {
    const duration = durationText(order.duration_minutes_wsh);
    return duration ? `1 次（${duration}）` : '1 次';
  }
  const duration = durationText(order.duration_minutes_wsh);
  return quantity > 1 ?
  `${quantity} ${unitLabel(unit)}（${duration}）` :
  `1 ${unitLabel(unit)}`;
}

/* ──────────────────────────── Format utils ──────────────────────────── */

function parseImages(raw: unknown): string[] {
  if (!raw) return [];
  if (Array.isArray(raw)) return raw.filter(Boolean) as string[];
  if (typeof raw !== 'string') return [];
  const trimmed = raw.trim();
  if (!trimmed) return [];
  try {
    const parsed = JSON.parse(trimmed);
    if (Array.isArray(parsed)) return parsed.filter(Boolean);
    if (typeof parsed === 'string') return [parsed];
    return [];
  } catch {

    /* fall through */}
  return trimmed.
  split(',').
  map((s) => s.trim()).
  filter(Boolean);
}

function formatMoney(value: unknown): string {
  const n = Number(value || 0);
  return Number.isFinite(n) ?
  n.toLocaleString('zh-CN', { maximumFractionDigits: 2 }) :
  '0.00';
}

function formatDayRange(start?: string, end?: string): string {
  const compact = (value?: string) => {
    const day = value ? String(value).slice(0, 10) : '';
    return day ? day.slice(5).replace('-', '.') : '-';
  };
  const s = compact(start);
  const e = compact(end);
  if (s === '-' && e === '-') return '-';
  return `${s} — ${e}`;
}

function nightsBetween(start?: string, end?: string): number {
  if (!start || !end) return 0;
  const a = new Date(start).getTime();
  const b = new Date(end).getTime();
  if (Number.isNaN(a) || Number.isNaN(b) || b <= a) return 0;
  return Math.round((b - a) / 86400000);
}

function formatDateTime(value?: string): string {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
}

function phaseStamp(order: OrderRecord, timeField: string): string {
  const stamp = order?.[timeField];
  return stamp ? formatDateTime(String(stamp)) : '—';
}