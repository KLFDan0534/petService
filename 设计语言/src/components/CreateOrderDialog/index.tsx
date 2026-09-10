import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { MapPinIcon } from 'lucide-react';

/* ============================================================================
 * Ported from src/components/order/CreateOrderDialog.vue
 * The multi-step booking flow, validation order, estimate math and API payload
 * shapes are reproduced 1:1. Only the data plumbing changes: the Vue component
 * called `@/api/*` directly, this one receives lists/quotes as props and emits
 * the finished payload through `onSubmit`.
 * ========================================================================= */

/* ---------------------------------- domain -------------------------------- */

export type BookingUnit = 'day' | 'session' | 'hour';

const NORMALIZE_ALIASES: Record<string, BookingUnit> = {
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

/** src/domain/BookingUnit.js */
export function normalizeUnit(value: unknown): BookingUnit | null {
  const key = String(value ?? '').
  trim().
  toLowerCase();
  return NORMALIZE_ALIASES[key] || null;
}

/** src/domain/BookingWindow.js */
export const AVAILABILITY_REQUEST_WINDOW_DAYS = 365;
export const BATCH_MAX_PETS = 10;

/* ---------------------------------- types --------------------------------- */

export interface Pet {
  id_wsh: number | string;
  name_wsh: string;
  breed_wsh?: string;
}

export interface Merchant {
  id_wsh: number | string;
  name_wsh: string;
  address_wsh?: string;
  latitude_wsh?: number | null;
  longitude_wsh?: number | null;
}

export interface Keeper {
  id_wsh: number | string;
  name_wsh: string;
  price_per_day_wsh?: number | string;
  current_pets_wsh?: number;
  max_pets_wsh?: number;
}

export interface AvailabilityWindow {
  slots_wsh?: string[];
}

export interface AvailabilityDay {
  date_wsh: string;
  bookable_wsh: boolean;
  windows_wsh: AvailabilityWindow[];
}

export interface Coupon {
  id_wsh: number | string;
  name_wsh?: string;
  type_wsh?: 'percent' | 'amount' | string;
  discount_rate_wsh?: number | string;
  max_discount_amount_wsh?: number | string;
  discount_amount_template_wsh?: number | string;
  threshold_amount_wsh?: number | string;
}

export interface CouponQuote {
  total_amount_wsh?: number | string;
  coupon_discount_wsh?: number | string;
  long_stay_discount_wsh?: number | string;
  platform_subsidy_wsh?: number | string;
  final_amount_wsh?: number | string;
}

export interface MembershipQuote {
  base_amount_wsh?: number | string;
  membership_discount_wsh?: number | string;
  final_amount_wsh?: number | string;
}

export interface ServiceSummary {
  id_wsh: number | string;
  name_wsh?: string;
  price_wsh?: number | string;
  unit_wsh?: string;
  booking_mode_wsh?: string;
  duration_minutes_wsh?: number | null;
  service_version_wsh?: string;
  merchant_id_wsh?: number | string;
  merchant_name_wsh?: string;
  merchant_address_wsh?: string;
}

export interface Estimate {
  total: string;
  discount: string;
  couponDiscount: string;
  membershipDiscount: string;
  platformSubsidy: string;
  final: string;
}

export interface BatchOrderItem {
  pet_id_wsh: number;
  start_date_wsh: string;
  end_date_wsh: string;
  delivery_time_wsh: string | null;
  pickup_time_wsh: string | null;
}

export interface CreateOrderPayload {
  pet_id_wsh: number;
  merchant_id_wsh: number;
  keeper_id_wsh: number;
  service_id_wsh: number | null;
  service_version_wsh: string | null;
  user_coupon_id_wsh: number | null;
  billing_unit_wsh: BookingUnit;
  expected_unit_price_wsh: number;
  delivery_address_wsh: string;
  delivery_location_source_wsh: string;
  pickup_address_wsh: string;
  pickup_location_source_wsh: string;
  emergency_contact_name_wsh: string;
  emergency_contact_phone_wsh: string;
  remark_wsh: string;
  quantity_wsh?: number;
  start_date_wsh?: string;
  end_date_wsh?: string;
  start_time_wsh?: string | null;
  delivery_time_wsh?: string | null;
  pickup_time_wsh?: string | null;
}

export interface CreateOrdersBatchPayload {
  keeper_id_wsh: number;
  merchant_id_wsh: number;
  service_id_wsh: number;
  service_version_wsh: string | null;
  billing_unit_wsh: BookingUnit;
  expected_unit_price_wsh: number;
  user_coupon_id_wsh: number | null;
  delivery_address_wsh: string;
  delivery_location_source_wsh: string;
  pickup_address_wsh: string;
  pickup_location_source_wsh: string;
  emergency_contact_name_wsh: string;
  emergency_contact_phone_wsh: string;
  remark_wsh: string;
  items: BatchOrderItem[];
}

export interface OrderForm {
  pet_id_wsh: string;
  merchant_id_wsh: string;
  keeper_id_wsh: string;
  delivery_address_wsh: string;
  delivery_location_source_wsh: string;
  delivery_latitude_wsh: number | null;
  delivery_longitude_wsh: number | null;
  delivery_time_wsh: string;
  pickup_time_wsh: string;
  delivery_date_wsh: string;
  delivery_slot_wsh: string;
  pickup_date_wsh: string;
  pickup_slot_wsh: string;
  emergency_contact_name_wsh: string;
  emergency_contact_phone_wsh: string;
  user_coupon_id_wsh: string;
  remark_wsh: string;
}

export interface PetRow {
  pet_id_wsh: string;
  delivery_date_wsh: string;
  delivery_slot_wsh: string;
  pickup_date_wsh: string;
  pickup_slot_wsh: string;
}

export interface CreateOrderDialogProps {
  visible: boolean;
  /** Present = service-driven booking (availability + slot pickers unlocked). */
  service?: ServiceSummary | null;
  pets?: Pet[];
  merchants?: Merchant[];
  keepers?: Keeper[];
  availabilityDays?: AvailabilityDay[];
  availableCoupons?: Coupon[];
  couponQuote?: CouponQuote | null;
  membershipQuote?: MembershipQuote | null;
  /** Authoritative booking window (inclusive) returned by the availability API. */
  maxBookingDays?: number;
  merchantLoading?: boolean;
  keeperLoading?: boolean;
  availabilityLoading?: boolean;
  availabilityError?: boolean;
  couponLoading?: boolean;
  submitting?: boolean;
  initialMerchantId?: string;
  initialKeeperId?: string;
  deliveryDistanceMeters?: number | null;
  distanceLoading?: boolean;
  distanceError?: string;
  /** Slot for the real AmapAddressPicker; a plain text field is used otherwise. */
  addressPicker?: ReactNode;
  onClose: () => void;
  onSubmit: (payload: CreateOrderPayload | CreateOrdersBatchPayload) => void;
  onNotify?: (message: string, level: 'warning' | 'error') => void;
  onMerchantChange?: (merchantId: string) => void;
  onKeeperChange?: (keeperId: string) => void;
  onCouponChange?: (userCouponId: string) => void;
  onRetryDistance?: () => void;
}

/* --------------------------------- helpers -------------------------------- */

export const EMPTY_FORM: OrderForm = {
  pet_id_wsh: '',
  merchant_id_wsh: '',
  keeper_id_wsh: '',
  delivery_address_wsh: '',
  delivery_location_source_wsh: '',
  delivery_latitude_wsh: null,
  delivery_longitude_wsh: null,
  delivery_time_wsh: '',
  pickup_time_wsh: '',
  delivery_date_wsh: '',
  delivery_slot_wsh: '',
  pickup_date_wsh: '',
  pickup_slot_wsh: '',
  emergency_contact_name_wsh: '',
  emergency_contact_phone_wsh: '',
  user_coupon_id_wsh: '',
  remark_wsh: ''
};

export function createPetRow(): PetRow {
  return {
    pet_id_wsh: '',
    delivery_date_wsh: '',
    delivery_slot_wsh: '',
    pickup_date_wsh: '',
    pickup_slot_wsh: ''
  };
}

const ZERO_ESTIMATE: Estimate = {
  total: '0.00',
  discount: '0.00',
  couponDiscount: '0.00',
  membershipDiscount: '0.00',
  platformSubsidy: '0.00',
  final: '0.00'
};

const pad = (value: number | string) => String(value).padStart(2, '0');

export function toLocalDateTimeInput(date: Date): string {
  return (
    [date.getFullYear(), pad(date.getMonth() + 1), pad(date.getDate())].join('-') +
    `T${pad(date.getHours())}:${pad(date.getMinutes())}`);

}

export function toDateOnly(date: Date): string {
  return toLocalDateTimeInput(date).slice(0, 10);
}

export function diffDays(startDate: string, endDate: string): number {
  return Math.ceil((new Date(endDate).getTime() - new Date(startDate).getTime()) / 86400000);
}

export function addDays(date: Date, days: number): Date {
  const result = new Date(date);
  result.setDate(result.getDate() + days);
  return result;
}

export function isSameDate(first: Date, second: Date): boolean {
  return toDateOnly(first) === toDateOnly(second);
}

export function buildServiceDates(deliveryTime: Date, pickupTime: Date) {
  return {
    startDate: toDateOnly(deliveryTime),
    endDate: toDateOnly(addDays(pickupTime, isSameDate(deliveryTime, pickupTime) ? 1 : 0))
  };
}

export function slotToDateTime(dateValue: string, slotValue: string): string {
  return slotValue ? `${dateValue}T${slotValue.slice(11, 16)}` : '';
}

export function toApiDateTime(value: string): string | null {
  return value ? `${value}:00` : null;
}

export function displaySlot(slot: string): string {
  return slot ? String(slot).slice(11, 16) : '';
}

/** src/utils/geo.js */
export function formatDistanceMeters(meters: number | null | undefined): string {
  if (meters == null) return '';
  const value = Number(meters);
  if (!Number.isFinite(value) || value < 0) return '';
  if (value < 1000) return `${Math.max(1, Math.round(value))} 米`;
  return `${(value / 1000).toFixed(1)} 公里`;
}

function fmtAmount(value: number | string): string {
  return Math.max(0, Number(value || 0)).toFixed(2);
}

function sumAmounts(rows: Estimate[], key: keyof Estimate): number {
  return rows.reduce((sum, item) => sum + Number(item[key] || 0), 0);
}

/** 单只宠物按天计费的估算（含长住折扣），供单笔与批量行共用 */
export function dayEstimate(
deliveryTime: string,
pickupTime: string,
dayPrice: number | string)
: Estimate {
  if (!deliveryTime || !pickupTime) return ZERO_ESTIMATE;
  const start = new Date(deliveryTime);
  const end = new Date(pickupTime);
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime()) || end <= start) {
    return ZERO_ESTIMATE;
  }
  const serviceDates = buildServiceDates(start, end);
  const days = Math.max(0, diffDays(serviceDates.startDate, serviceDates.endDate));
  const total = days * Number(dayPrice || 0);
  let discount = 0;
  if (days >= 30) discount = total * 0.1;else
  if (days >= 7) discount = total * 0.05;
  return {
    total: total.toFixed(2),
    discount: discount.toFixed(2),
    couponDiscount: '0.00',
    membershipDiscount: '0.00',
    platformSubsidy: '0.00',
    final: Math.max(0, total - discount).toFixed(2)
  };
}

export function couponText(coupon: Coupon): string {
  if (coupon.type_wsh === 'percent') {
    const rate = Number(coupon.discount_rate_wsh || 0);
    const max = Number(coupon.max_discount_amount_wsh || 0);
    return `${(rate * 10).toFixed(1)}折${max > 0 ? `，最高减￥${max.toFixed(2)}` : ''}`;
  }
  const amount = Number(coupon.discount_amount_template_wsh || 0);
  return `满￥${Number(coupon.threshold_amount_wsh || 0).toFixed(2)}减￥${amount.toFixed(2)}`;
}

/**
 * 按后端 CouponServiceImpl.calculateCouponDiscount 口径估算单张券的优惠金额：
 * - percent：金额 × (1 - 折扣率)，受最大减额约束（0.80 表示 8 折）
 * - amount：满减券直接取减免金额
 * 仅用于「自动选择最优惠券」的排序，实付以后端 quote 结果为准。
 */
export function couponDiscountEstimate(coupon: Coupon, orderAmount: number): number {
  const amount = Number(orderAmount || 0);
  if (amount <= 0 || !coupon) return 0;
  let discount = 0;
  if (coupon.type_wsh === 'percent') {
    const rate = Number(coupon.discount_rate_wsh || 0);
    discount = amount * (1 - rate);
    const max = Number(coupon.max_discount_amount_wsh || 0);
    if (max > 0) discount = Math.min(discount, max);
  } else if (coupon.type_wsh === 'amount') {
    discount = Number(coupon.discount_amount_template_wsh || 0);
  }
  return Math.max(0, Math.min(discount, amount));
}

export function pickBestCoupon(coupons: Coupon[], orderAmount: number): Coupon | null {
  let best: Coupon | null = null;
  let bestDiscount = -1;
  for (const coupon of coupons) {
    const discount = couponDiscountEstimate(coupon, orderAmount);
    if (discount > bestDiscount) {
      bestDiscount = discount;
      best = coupon;
    }
  }
  return best;
}

function isConfirmedDeliverySource(source: string): boolean {
  const text = String(source || '').trim();
  return text === 'merchant' || text.startsWith('amap');
}

/* ------------------------------ local controls ---------------------------- */

const CONTROL =
'w-full rounded-control border border-line bg-surface px-4 py-3 text-control text-ink transition-colors duration-fast ease-editorial placeholder:text-muted hover:border-brand/60 focus:border-brand disabled:cursor-not-allowed disabled:border-line disabled:bg-sand disabled:text-muted';
const FIELD = 'grid gap-[6px] text-meta text-muted';
const HINT = 'min-h-[18px] text-[12px] leading-[1.5] text-muted';
const BTN_BASE =
'inline-flex items-center justify-center gap-1.5 rounded-control text-[12.5px] font-medium transition-colors duration-fast ease-editorial disabled:cursor-not-allowed disabled:opacity-55';
const BTN_OUTLINE = `${BTN_BASE} border border-line bg-transparent px-3 py-1.5 text-ink-soft hover:border-brand hover:text-brand`;
const BTN_SECONDARY = `${BTN_BASE} border border-line bg-sand px-4 py-2 text-ink-soft hover:border-brand hover:text-brand`;
const BTN_PRIMARY = `${BTN_BASE} border border-transparent bg-brand px-4 py-2 text-white hover:bg-brand-deep active:translate-y-px`;

/* -------------------------------- component ------------------------------- */

export function CreateOrderDialog({
  visible,
  service = null,
  pets = [],
  merchants = [],
  keepers = [],
  availabilityDays = [],
  availableCoupons = [],
  couponQuote = null,
  membershipQuote = null,
  maxBookingDays = 0,
  merchantLoading = false,
  keeperLoading = false,
  availabilityLoading = false,
  availabilityError = false,
  couponLoading = false,
  submitting = false,
  initialMerchantId = '',
  initialKeeperId = '',
  deliveryDistanceMeters = null,
  distanceLoading = false,
  distanceError = '',
  addressPicker,
  onClose,
  onSubmit,
  onNotify,
  onMerchantChange,
  onKeeperChange,
  onCouponChange,
  onRetryDistance
}: CreateOrderDialogProps) {
  const [form, setForm] = useState<OrderForm>(EMPTY_FORM);
  const [petRows, setPetRows] = useState<PetRow[]>([createPetRow()]);
  const [slotQuantity, setSlotQuantity] = useState(1);
  const [couponTouched, setCouponTouched] = useState(false);
  const [nowTick, setNowTick] = useState(() => Date.now());
  const autoFilledMerchantRef = useRef('');

  const notify = useCallback(
    (message: string, level: 'warning' | 'error' = 'warning') => {
      onNotify?.(message, level);
    },
    [onNotify]
  );

  const patch = useCallback((next: Partial<OrderForm>) => {
    setForm((current) => ({ ...current, ...next }));
  }, []);

  const serviceId = service?.id_wsh ? String(service.id_wsh) : '';
  const serviceName = service?.name_wsh || '';
  const serviceVersion = service?.service_version_wsh || '';
  const price = service?.price_wsh ?? '';

  /* --------------------------------- resets -------------------------------- */

  useEffect(() => {
    if (!visible) return;
    setNowTick(Date.now());
    autoFilledMerchantRef.current = '';
    setForm({
      ...EMPTY_FORM,
      merchant_id_wsh: String(initialMerchantId || service?.merchant_id_wsh || ''),
      keeper_id_wsh: String(initialKeeperId || '')
    });
    setPetRows([createPetRow()]);
    setSlotQuantity(1);
    setCouponTouched(false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [visible]);

  /* ------------------------------- computed -------------------------------- */

  const availabilityEnabled = Boolean(serviceId);
  const normalizedUnit = normalizeUnit(service?.unit_wsh) || 'day';
  const slotMode = useMemo(() => {
    if (!availabilityEnabled) return false;
    const mode = service?.booking_mode_wsh;
    if (mode) return mode === 'slot';
    const unit = normalizeUnit(service?.unit_wsh);
    return unit === 'session' || unit === 'hour';
  }, [availabilityEnabled, service?.booking_mode_wsh, service?.unit_wsh]);
  /** 批量模式：服务驱动 + 按天计费（date_range）；session/hour 保持单笔 */
  const isBatchMode = availabilityEnabled && !slotMode;
  const isHourUnit = normalizedUnit === 'hour';

  const unitLabel = normalizedUnit === 'hour' ? '小时' : normalizedUnit === 'session' ? '次' : '天';

  const selectedMerchant = useMemo(
    () => merchants.find((item) => Number(item.id_wsh) === Number(form.merchant_id_wsh)),
    [merchants, form.merchant_id_wsh]
  );
  const merchantDisplayName =
  selectedMerchant?.name_wsh || service?.merchant_name_wsh || '加载中...';

  const minDate = useCallback(() => {
    const d = new Date(nowTick);
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  }, [nowTick]);

  const maxDate = useCallback(() => {
    const d = new Date(nowTick);
    // 以后端 booking_window_days_wsh 为准（含首尾）；未查询前用请求上界兜底
    const offset = maxBookingDays > 0 ? maxBookingDays - 1 : AVAILABILITY_REQUEST_WINDOW_DAYS;
    d.setDate(d.getDate() + offset);
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  }, [nowTick, maxBookingDays]);

  const minDateTime = toLocalDateTimeInput(new Date(nowTick));
  const pickupMinDateTime = form.delivery_time_wsh ?
  toLocalDateTimeInput(new Date(new Date(form.delivery_time_wsh).getTime() + 600000)) :
  minDateTime;

  const bookableDates = useMemo(
    () => availabilityDays.filter((day) => day.bookable_wsh).map((day) => String(day.date_wsh)),
    [availabilityDays]
  );

  const slotsForDate = useCallback(
    (dateValue: string) => {
      if (!dateValue) return [];
      const day = availabilityDays.find((item) => String(item.date_wsh) === dateValue);
      if (!day || !day.bookable_wsh) return [];
      return day.windows_wsh.flatMap((window) => window.slots_wsh || []);
    },
    [availabilityDays]
  );

  const deliverySlotOptions = slotsForDate(form.delivery_date_wsh);
  const pickupSlotOptions = useMemo(() => {
    const slots = slotsForDate(form.pickup_date_wsh);
    // 接回时间必须晚于送达时间；跨天接回不在此过滤（日期本身已保证顺序）
    if (String(form.pickup_date_wsh) === String(form.delivery_date_wsh) && form.delivery_slot_wsh) {
      return slots.filter((slot) => slot > form.delivery_slot_wsh);
    }
    return slots;
  }, [slotsForDate, form.pickup_date_wsh, form.delivery_date_wsh, form.delivery_slot_wsh]);

  const rowPickupSlotOptions = useCallback(
    (row: PetRow) => {
      const slots = slotsForDate(row.pickup_date_wsh);
      if (String(row.pickup_date_wsh) === String(row.delivery_date_wsh) && row.delivery_slot_wsh) {
        return slots.filter((slot) => slot > row.delivery_slot_wsh);
      }
      return slots;
    },
    [slotsForDate]
  );

  const rowSlotPlaceholder = useCallback(
    (dateValue: string) => {
      if (!dateValue) return '请先选择送达日期';
      if (availabilityLoading) return '正在加载时间槽位...';
      if (slotsForDate(dateValue).length === 0) return '该日暂无可约时间';
      return '请选择送达时间';
    },
    [availabilityLoading, slotsForDate]
  );

  /* ------------------------------- estimates ------------------------------- */

  const rowEstimate = useCallback(
    (row: PetRow) =>
    dayEstimate(
      slotToDateTime(row.delivery_date_wsh, row.delivery_slot_wsh),
      slotToDateTime(row.pickup_date_wsh, row.pickup_slot_wsh),
      Number(price || 0)
    ),
    [price]
  );

  const batchBase = useMemo(
    () => petRows.map(rowEstimate).filter((item) => Number(item.total) > 0),
    [petRows, rowEstimate]
  );

  const batchBaseEstimate = useMemo<Estimate>(() => {
    const total = sumAmounts(batchBase, 'total');
    const discount = sumAmounts(batchBase, 'discount');
    return {
      total: fmtAmount(total),
      discount: fmtAmount(discount),
      couponDiscount: '0.00',
      membershipDiscount: '0.00',
      platformSubsidy: '0.00',
      final: fmtAmount(total - discount)
    };
  }, [batchBase]);

  const baseEstimate = useMemo<Estimate>(() => {
    if (isBatchMode) return batchBaseEstimate;
    if (slotMode) {
      const startDt = slotToDateTime(form.delivery_date_wsh, form.delivery_slot_wsh);
      if (!startDt) return ZERO_ESTIMATE;
      const unitPrice = Number(price || 0);
      const quantity = normalizedUnit === 'hour' ? Math.max(1, Number(slotQuantity) || 1) : 1;
      const total = quantity * unitPrice;
      return {
        total: total.toFixed(2),
        discount: '0.00',
        couponDiscount: '0.00',
        membershipDiscount: '0.00',
        platformSubsidy: '0.00',
        final: total.toFixed(2)
      };
    }
    const deliveryTime = availabilityEnabled ?
    slotToDateTime(form.delivery_date_wsh, form.delivery_slot_wsh) :
    form.delivery_time_wsh;
    const pickupTime = availabilityEnabled ?
    slotToDateTime(form.pickup_date_wsh, form.pickup_slot_wsh) :
    form.pickup_time_wsh;
    const keeper = keepers.find((item) => Number(item.id_wsh) === Number(form.keeper_id_wsh));
    const dayPrice = availabilityEnabled ?
    Number(price || 0) :
    Number(keeper?.price_per_day_wsh || price || 0);
    return dayEstimate(deliveryTime, pickupTime, dayPrice);
  }, [
  isBatchMode,
  batchBaseEstimate,
  slotMode,
  availabilityEnabled,
  normalizedUnit,
  slotQuantity,
  price,
  keepers,
  form.delivery_date_wsh,
  form.delivery_slot_wsh,
  form.pickup_date_wsh,
  form.pickup_slot_wsh,
  form.delivery_time_wsh,
  form.pickup_time_wsh,
  form.keeper_id_wsh]
  );

  /** 优惠券报价基准：批量 = 金额最大的单（与后端券归属规则一致）；单笔 = 整单 */
  const couponQuoteBase = useMemo(() => {
    if (isBatchMode) {
      if (batchBase.length === 0) return { total: 0, discount: 0 };
      const largest = batchBase.reduce((a, b) => Number(a.total) >= Number(b.total) ? a : b);
      return { total: Number(largest.total), discount: Number(largest.discount) };
    }
    return { total: Number(baseEstimate.total), discount: Number(baseEstimate.discount) };
  }, [isBatchMode, batchBase, baseEstimate]);

  const estimatedAmount = useMemo<Estimate>(() => {
    if (isBatchMode) {
      const total = Number(batchBaseEstimate.total);
      const discount = Number(batchBaseEstimate.discount);
      let couponDiscount = 0;
      if (
      couponQuote &&
      Number(couponQuote.total_amount_wsh || 0).toFixed(2) === couponQuoteBase.total.toFixed(2))
      {
        couponDiscount = Number(couponQuote.coupon_discount_wsh || 0);
      }
      const afterCoupon = Math.max(0, total - discount - couponDiscount);
      let memberDiscount = 0;
      if (
      membershipQuote &&
      Number(membershipQuote.base_amount_wsh || 0).toFixed(2) === afterCoupon.toFixed(2))
      {
        memberDiscount = Number(membershipQuote.membership_discount_wsh || 0);
      }
      return {
        total: fmtAmount(total),
        discount: fmtAmount(discount),
        couponDiscount: fmtAmount(couponDiscount),
        membershipDiscount: fmtAmount(memberDiscount),
        platformSubsidy: fmtAmount(Number(couponQuote?.platform_subsidy_wsh || 0) + memberDiscount),
        final: fmtAmount(afterCoupon - memberDiscount)
      };
    }
    let current = baseEstimate;
    if (
    couponQuote &&
    Number(couponQuote.total_amount_wsh || 0).toFixed(2) === baseEstimate.total)
    {
      const couponDiscount = Number(couponQuote.coupon_discount_wsh || 0);
      const longStayDiscount = Number(couponQuote.long_stay_discount_wsh || baseEstimate.discount);
      current = {
        total: baseEstimate.total,
        discount: longStayDiscount.toFixed(2),
        couponDiscount: couponDiscount.toFixed(2),
        membershipDiscount: '0.00',
        platformSubsidy: Number(couponQuote.platform_subsidy_wsh || 0).toFixed(2),
        final: Number(couponQuote.final_amount_wsh || 0).toFixed(2)
      };
    }
    if (
    membershipQuote &&
    Number(membershipQuote.base_amount_wsh || 0).toFixed(2) === current.final)
    {
      const memberDiscount = Number(membershipQuote.membership_discount_wsh || 0);
      return {
        ...current,
        membershipDiscount: memberDiscount.toFixed(2),
        platformSubsidy: (Number(current.platformSubsidy || 0) + memberDiscount).toFixed(2),
        final: Number(membershipQuote.final_amount_wsh || 0).toFixed(2)
      };
    }
    return current;
  }, [isBatchMode, batchBaseEstimate, couponQuoteBase, baseEstimate, couponQuote, membershipQuote]);

  /* --------------------------------- hints --------------------------------- */

  const merchantSelectPlaceholder = merchantLoading ?
  '正在筛选可接单商家...' :
  merchants.length === 0 ?
  '暂无可接单商家' :
  '请选择商家';

  const keeperSelectPlaceholder = !form.merchant_id_wsh ?
  '请先选择商家' :
  keeperLoading ?
  '正在加载看护人...' :
  keepers.length === 0 ?
  '暂无可接单看护人' :
  '请选择看护人';

  const merchantHint = merchantLoading ?
  '正在校验商家是否有资质通过的在职看护人' :
  merchants.length === 0 ?
  '当前没有满足接单条件的商家，请稍后再试' :
  '仅展示有合格看护人的商家';

  const keeperHint = !form.merchant_id_wsh ?
  '选择商家后再选择看护人' :
  keeperLoading ?
  '正在同步该商家的可接单看护人' :
  keepers.length === 0 ?
  '该商家暂无可接单看护人' :
  '仅展示资质通过且在职的看护人（休息中也接受未来预约）';

  const availabilityHint = availabilityLoading ?
  '正在查询可预约日期...' :
  availabilityError ?
  '可预约日期查询失败，请刷新重试' :
  availabilityEnabled && !form.keeper_id_wsh ?
  keepers.length === 0 ?
  '该商家暂无可接单看护人，暂无法选择日期' :
  '请先选择看护人，再选择送达/接回日期' :
  bookableDates.length === 0 ?
  maxBookingDays > 0 ?
  `近 ${maxBookingDays} 天暂无可预约日期` :
  '暂无可预约日期' :
  `仅展示可预约日期（${bookableDates.length} 天可约）`;

  const deliverySlotPlaceholder = !form.delivery_date_wsh ?
  '请先选择送达日期' :
  availabilityLoading ?
  '正在加载时间槽位...' :
  deliverySlotOptions.length === 0 ?
  '该日暂无可约时间' :
  '请选择送达时间';

  const pickupSlotPlaceholder = !form.pickup_date_wsh ?
  '请先选择接回日期' :
  availabilityLoading ?
  '正在加载时间槽位...' :
  pickupSlotOptions.length === 0 ?
  '该日暂无可约时间' :
  '请选择接回时间';

  const keeperOptionLabel = (keeper: Keeper) => {
    // 服务驱动下单：展示服务价格口径，避免与看护人个人价混淆
    const unitPrice = availabilityEnabled ?
    Number(price || 0) :
    Number(keeper?.price_per_day_wsh || 0);
    const unitSuffix = availabilityEnabled ? `/${unitLabel}` : '/天';
    const priceText = unitPrice > 0 ? ` ￥${unitPrice}${unitSuffix}` : '';
    const capacity = `当前 ${keeper.current_pets_wsh || 0}/${keeper.max_pets_wsh || '-'}`;
    return `${keeper.name_wsh}${priceText} · ${capacity}`;
  };

  const deliveryDistanceVisible =
  Number.isFinite(Number(form.delivery_latitude_wsh)) &&
  Number.isFinite(Number(form.delivery_longitude_wsh)) &&
  form.delivery_latitude_wsh != null &&
  form.delivery_longitude_wsh != null;

  const deliveryDistanceText = distanceLoading ?
  '正在定位您的位置，计算送达距离...' :
  distanceError ?
  `无法获取您的位置：${distanceError}` :
  deliveryDistanceMeters != null ?
  `距您约 ${formatDistanceMeters(deliveryDistanceMeters)}` :
  '正在获取您的位置...';

  /* -------------------------------- coupons -------------------------------- */

  useEffect(() => {
    if (!visible) return;
    if (
    form.user_coupon_id_wsh &&
    !availableCoupons.some((item) => Number(item.id_wsh) === Number(form.user_coupon_id_wsh)))
    {
      patch({ user_coupon_id_wsh: '' });
      return;
    }
    // 用户未手动选择过时，自动勾选当前订单金额下最优惠的一张券
    if (!couponTouched && !form.user_coupon_id_wsh && availableCoupons.length > 0) {
      const listingAmount = isBatchMode ? couponQuoteBase.total : Number(baseEstimate.final);
      const best = pickBestCoupon(availableCoupons, listingAmount);
      if (best) patch({ user_coupon_id_wsh: String(best.id_wsh) });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [visible, availableCoupons, couponTouched, form.user_coupon_id_wsh]);

  /* ------------------------- delivery from merchant ------------------------ */

  const fillDeliveryFromMerchant = (showWarning = false): OrderForm | null => {
    const merchant = selectedMerchant;
    if (!merchant) {
      if (showWarning) notify('请先选择商家', 'warning');
      return null;
    }
    const address = merchant.address_wsh || '';
    const next: OrderForm = {
      ...form,
      delivery_address_wsh: address,
      delivery_location_source_wsh: address ? 'merchant' : '',
      delivery_latitude_wsh: merchant.latitude_wsh ?? null,
      delivery_longitude_wsh: merchant.longitude_wsh ?? null
    };
    setForm(next);
    if (!(next.delivery_address_wsh && isConfirmedDeliverySource(next.delivery_location_source_wsh))) {
      if (showWarning) notify('商家位置缺少地址，请搜索选择送达地址', 'warning');
      return null;
    }
    return next;
  };

  // 服务驱动下单时商家由服务指定，商家解析出来后自动带出送达地址
  useEffect(() => {
    if (!visible || !selectedMerchant) return;
    const key = String(selectedMerchant.id_wsh);
    if (autoFilledMerchantRef.current === key) return;
    autoFilledMerchantRef.current = key;
    if (form.delivery_address_wsh) return;
    patch({
      delivery_address_wsh: selectedMerchant.address_wsh || '',
      delivery_location_source_wsh: selectedMerchant.address_wsh ? 'merchant' : '',
      delivery_latitude_wsh: selectedMerchant.latitude_wsh ?? null,
      delivery_longitude_wsh: selectedMerchant.longitude_wsh ?? null
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [visible, selectedMerchant]);

  /* ------------------------------- handlers -------------------------------- */

  const handleMerchantSelect = (merchantId: string) => {
    if (serviceId) return;
    autoFilledMerchantRef.current = '';
    setForm((current) => ({
      ...current,
      merchant_id_wsh: merchantId,
      keeper_id_wsh: '',
      ...(merchantId ?
      {} :
      {
        delivery_address_wsh: '',
        delivery_location_source_wsh: '',
        delivery_latitude_wsh: null,
        delivery_longitude_wsh: null
      })
    }));
    onMerchantChange?.(merchantId);
  };

  const handleKeeperSelect = (keeperId: string) => {
    // 换看护人时清空日期槽位（可预约日期需要按看护人重新查询），保留宠物选择与行数
    setForm((current) => ({
      ...current,
      keeper_id_wsh: keeperId,
      delivery_date_wsh: '',
      delivery_slot_wsh: '',
      pickup_date_wsh: '',
      pickup_slot_wsh: ''
    }));
    setPetRows((rows) =>
    rows.map((row) => ({
      ...row,
      delivery_date_wsh: '',
      delivery_slot_wsh: '',
      pickup_date_wsh: '',
      pickup_slot_wsh: ''
    }))
    );
    onKeeperChange?.(keeperId);
  };

  const handleDeliveryDateChange = (value: string) => {
    setForm((current) => {
      const next = { ...current, delivery_date_wsh: value, delivery_slot_wsh: '' };
      if (!value || next.pickup_date_wsh && next.pickup_date_wsh < value) {
        next.pickup_date_wsh = '';
        next.pickup_slot_wsh = '';
      }
      return next;
    });
  };

  const handleDeliverySlotChange = (value: string) => {
    patch({ delivery_slot_wsh: value, pickup_date_wsh: '', pickup_slot_wsh: '' });
  };

  const updateRow = (index: number, next: Partial<PetRow>) => {
    setPetRows((rows) => rows.map((row, i) => i === index ? { ...row, ...next } : row));
  };

  const handleRowDeliveryDateChange = (index: number, value: string) => {
    setPetRows((rows) =>
    rows.map((row, i) => {
      if (i !== index) return row;
      const next = { ...row, delivery_date_wsh: value, delivery_slot_wsh: '' };
      if (next.pickup_date_wsh && next.pickup_date_wsh < value) {
        next.pickup_date_wsh = '';
        next.pickup_slot_wsh = '';
      }
      return next;
    })
    );
  };

  const addPetRow = () => {
    setPetRows((rows) => rows.length >= BATCH_MAX_PETS ? rows : [...rows, createPetRow()]);
  };

  const removePetRow = (index: number) => {
    setPetRows((rows) => rows.length <= 1 ? rows : rows.filter((_, i) => i !== index));
  };

  const handleCouponSelect = (value: string) => {
    setCouponTouched(true);
    patch({ user_coupon_id_wsh: value });
    onCouponChange?.(value);
  };

  /* --------------------------------- submit -------------------------------- */

  const resolveDeliveryLocation = (): OrderForm | null => {
    if (
    form.delivery_address_wsh &&
    isConfirmedDeliverySource(form.delivery_location_source_wsh))
    {
      return form;
    }
    if (!form.delivery_address_wsh) {
      const filled = fillDeliveryFromMerchant();
      if (filled) return filled;
    }
    notify('请使用商家位置，或从地址搜索结果中选择送达地址', 'warning');
    return null;
  };

  const submitOrder = () => {
    if (submitting) return;
    const now = Date.now();
    setNowTick(now);
    if (merchantLoading || keeperLoading || availabilityLoading) {
      notify('接单信息正在加载，请稍后再提交', 'warning');
      return;
    }

    // ============ 批量模式（多宠物连续下单） ============
    if (isBatchMode) {
      const location = resolveDeliveryLocation();
      if (!location) return;
      const items: BatchOrderItem[] = [];
      for (const row of petRows) {
        if (
        !row.pet_id_wsh ||
        !row.delivery_date_wsh ||
        !row.delivery_slot_wsh ||
        !row.pickup_date_wsh ||
        !row.pickup_slot_wsh)
        {
          notify('请为每只宠物补全送达/接回日期与时间', 'warning');
          return;
        }
        if (
        !bookableDates.includes(row.delivery_date_wsh) ||
        !bookableDates.includes(row.pickup_date_wsh))
        {
          notify('所选日期不可预约，请重新选择', 'warning');
          return;
        }
        const deliveryTime = new Date(slotToDateTime(row.delivery_date_wsh, row.delivery_slot_wsh));
        const pickupTime = new Date(slotToDateTime(row.pickup_date_wsh, row.pickup_slot_wsh));
        if (Number.isNaN(deliveryTime.getTime()) || Number.isNaN(pickupTime.getTime())) {
          notify('请选择有效的送达/接回时间', 'warning');
          return;
        }
        if (deliveryTime.getTime() < now) {
          notify('送达时间不能早于当前时间', 'warning');
          return;
        }
        if (pickupTime.getTime() <= deliveryTime.getTime()) {
          notify('接回时间必须晚于送达时间', 'warning');
          return;
        }
        const serviceDates = buildServiceDates(deliveryTime, pickupTime);
        items.push({
          pet_id_wsh: Number(row.pet_id_wsh),
          start_date_wsh: serviceDates.startDate,
          end_date_wsh: serviceDates.endDate,
          delivery_time_wsh: toApiDateTime(
            slotToDateTime(row.delivery_date_wsh, row.delivery_slot_wsh)
          ),
          pickup_time_wsh: toApiDateTime(slotToDateTime(row.pickup_date_wsh, row.pickup_slot_wsh))
        });
      }
      const shared = {
        keeper_id_wsh: Number(form.keeper_id_wsh),
        merchant_id_wsh: Number(form.merchant_id_wsh),
        service_id_wsh: Number(serviceId),
        service_version_wsh: serviceVersion || null,
        billing_unit_wsh: normalizedUnit,
        expected_unit_price_wsh: Number(price || 0),
        user_coupon_id_wsh: form.user_coupon_id_wsh ? Number(form.user_coupon_id_wsh) : null,
        delivery_address_wsh: location.delivery_address_wsh,
        delivery_location_source_wsh: location.delivery_location_source_wsh || 'amap',
        pickup_address_wsh: location.delivery_address_wsh,
        pickup_location_source_wsh: location.delivery_location_source_wsh || 'amap',
        emergency_contact_name_wsh: form.emergency_contact_name_wsh,
        emergency_contact_phone_wsh: form.emergency_contact_phone_wsh,
        remark_wsh: form.remark_wsh
      };
      // 单只宠物：走单笔接口（与旧路径完全一致）；多只：批量接口
      if (items.length === 1) {
        const item = items[0];
        onSubmit({
          ...shared,
          pet_id_wsh: item.pet_id_wsh,
          quantity_wsh: Math.max(0, diffDays(item.start_date_wsh, item.end_date_wsh)),
          start_date_wsh: item.start_date_wsh,
          end_date_wsh: item.end_date_wsh,
          delivery_time_wsh: item.delivery_time_wsh,
          pickup_time_wsh: item.pickup_time_wsh
        } as CreateOrderPayload);
        return;
      }
      onSubmit({ ...shared, items } as CreateOrdersBatchPayload);
      return;
    }

    const isSlot = slotMode;
    const deliveryTime = availabilityEnabled ?
    new Date(slotToDateTime(form.delivery_date_wsh, form.delivery_slot_wsh)) :
    new Date(form.delivery_time_wsh);
    const pickupTime = isSlot ?
    null :
    availabilityEnabled ?
    new Date(slotToDateTime(form.pickup_date_wsh, form.pickup_slot_wsh)) :
    new Date(form.pickup_time_wsh);
    const deliveryValid = !Number.isNaN(deliveryTime.getTime());
    const pickupValid = isSlot || !Number.isNaN((pickupTime as Date).getTime());
    if (
    !form.pet_id_wsh ||
    !form.merchant_id_wsh ||
    !form.keeper_id_wsh ||
    !deliveryValid ||
    !pickupValid)
    {
      notify(
        isSlot ?
        '请补全宠物、商家、看护人和开始时间' :
        '请补全宠物、商家、看护人、送达时间和接回时间',
        'warning'
      );
      return;
    }
    if (availabilityEnabled) {
      const deliveryDateBookable = bookableDates.includes(form.delivery_date_wsh);
      const pickupDateBookable = isSlot || bookableDates.includes(form.pickup_date_wsh);
      if (!deliveryDateBookable || !pickupDateBookable) {
        notify('所选日期不可预约，请重新选择', 'warning');
        return;
      }
    }
    const merchantLoaded = merchants.some(
      (item) => Number(item.id_wsh) === Number(form.merchant_id_wsh)
    );
    const keeperLoaded = keepers.some((item) => Number(item.id_wsh) === Number(form.keeper_id_wsh));
    if (!merchantLoaded || !keeperLoaded) {
      notify('请选择可接单商家和看护人', 'warning');
      return;
    }
    if (!form.emergency_contact_name_wsh || !form.emergency_contact_phone_wsh) {
      notify('请填写紧急联系人和联系电话', 'warning');
      return;
    }
    if (deliveryTime.getTime() < now) {
      notify('送达时间不能早于当前时间', 'warning');
      return;
    }
    if (!isSlot && (pickupTime as Date).getTime() <= deliveryTime.getTime()) {
      notify('接回时间必须晚于送达时间', 'warning');
      return;
    }
    const location = resolveDeliveryLocation();
    if (!location) return;

    const unitPrice = Number(price || 0);
    const payload: CreateOrderPayload = {
      pet_id_wsh: Number(form.pet_id_wsh),
      merchant_id_wsh: Number(form.merchant_id_wsh),
      keeper_id_wsh: Number(form.keeper_id_wsh),
      service_id_wsh: serviceId ? Number(serviceId) : null,
      service_version_wsh: serviceVersion || null,
      user_coupon_id_wsh: form.user_coupon_id_wsh ? Number(form.user_coupon_id_wsh) : null,
      billing_unit_wsh: normalizedUnit,
      expected_unit_price_wsh: unitPrice,
      delivery_address_wsh: location.delivery_address_wsh,
      delivery_location_source_wsh: location.delivery_location_source_wsh || 'amap',
      pickup_address_wsh: location.delivery_address_wsh,
      pickup_location_source_wsh: location.delivery_location_source_wsh || 'amap',
      emergency_contact_name_wsh: form.emergency_contact_name_wsh,
      emergency_contact_phone_wsh: form.emergency_contact_phone_wsh,
      remark_wsh: form.remark_wsh
    };
    if (isSlot) {
      payload.quantity_wsh = normalizedUnit === 'hour' ? Math.max(1, Number(slotQuantity) || 1) : 1;
      payload.start_time_wsh = toApiDateTime(
        slotToDateTime(form.delivery_date_wsh, form.delivery_slot_wsh)
      );
      payload.start_date_wsh = form.delivery_date_wsh;
    } else {
      const serviceDates = buildServiceDates(deliveryTime, pickupTime as Date);
      payload.quantity_wsh = Math.max(0, diffDays(serviceDates.startDate, serviceDates.endDate));
      payload.start_date_wsh = serviceDates.startDate;
      payload.end_date_wsh = serviceDates.endDate;
      payload.delivery_time_wsh = toApiDateTime(
        availabilityEnabled ?
        slotToDateTime(form.delivery_date_wsh, form.delivery_slot_wsh) :
        form.delivery_time_wsh
      );
      payload.pickup_time_wsh = toApiDateTime(
        availabilityEnabled ?
        slotToDateTime(form.pickup_date_wsh, form.pickup_slot_wsh) :
        form.pickup_time_wsh
      );
    }
    onSubmit(payload);
  };

  if (!visible) return null;

  /* ---------------------------------- view --------------------------------- */

  const petOptions = pets.map((pet) =>
  <option key={String(pet.id_wsh)} value={String(pet.id_wsh)}>
      {pet.name_wsh} {pet.breed_wsh ? `(${pet.breed_wsh})` : ''}
    </option>
  );

  const stepTitle = (step: number, label: string) =>
  <div className="col-span-full mt-1 flex items-center gap-2.5 text-[14px] font-semibold text-ink">
      <span
      className="inline-flex h-[22px] w-[22px] shrink-0 items-center justify-center rounded-full bg-brand text-[12px] font-semibold text-white"
      aria-hidden="true"
      data-numeric="true">
      
        {step}
      </span>
      <span>{label}</span>
      <span className="ml-1 h-px flex-1 bg-line" aria-hidden="true" />
    </div>;


  return (
    <div
      className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-ink/50 p-4 animate-fade sm:p-6"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}>
      
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="create-order-dialog-title"
        className="my-auto w-[min(92vw,960px)] max-w-[960px] animate-pop rounded-frame border border-line bg-surface px-6 py-7 shadow-lift sm:px-8">
        
        <header className="text-center">
          <h2
            id="create-order-dialog-title"
            className="mb-1.5 font-display text-display-xs text-ink">
            
            创建订单
          </h2>
          <p className="text-meta text-muted">先选好宠物与看护人，再安排送达、接回时间</p>
        </header>

        {/* 服务信息卡片 */}
        {serviceId ?
        <div className="mt-4 rounded-card border-l-[3px] border-brand bg-sand px-4 py-3">
            <div className="flex items-baseline gap-3 text-[14px] leading-[1.8]">
              <span className="min-w-[40px] text-meta text-muted">服务</span>
              <span className="ref-truncate font-semibold text-ink">
                {serviceName || '加载中...'}
              </span>
            </div>
            <div className="flex items-baseline gap-3 text-[14px] leading-[1.8]">
              <span className="min-w-[40px] text-meta text-muted">计价</span>
              <span className="font-semibold text-ink" data-numeric="true">
                ￥{price || '--'} / {unitLabel}
              </span>
            </div>
          </div> :
        null}

        <div className="mt-5 grid grid-cols-[repeat(auto-fit,minmax(250px,1fr))] gap-x-[18px] gap-y-4">
          {/* ============ 第一步：宠物与看护人 ============ */}
          {stepTitle(1, '宠物与看护人')}

          {/* 批量模式：看护人优先选择（解锁后续日期），宠物在每行内选择 */}
          {isBatchMode ?
          <label className={FIELD}>
              看护人
              <select
              className={CONTROL}
              value={form.keeper_id_wsh}
              disabled={!form.merchant_id_wsh || keeperLoading || keepers.length === 0}
              onChange={(event) => handleKeeperSelect(event.target.value)}>
              
                <option value="">{keeperSelectPlaceholder}</option>
                {keepers.map((keeper) =>
              <option key={String(keeper.id_wsh)} value={String(keeper.id_wsh)}>
                    {keeperOptionLabel(keeper)}
                  </option>
              )}
              </select>
              <span className={HINT}>{keeperHint}</span>
            </label> :

          <label className={FIELD}>
              宠物
              <select
              className={CONTROL}
              value={form.pet_id_wsh}
              onChange={(event) => patch({ pet_id_wsh: event.target.value })}>
              
                <option value="">请选择宠物</option>
                {petOptions}
              </select>
            </label>
          }

          {/* 服务模式：商家只读展示 / 非服务模式：商家可选 */}
          {serviceId ?
          <div className="grid content-start gap-[6px] text-meta text-muted">
              <span>商家</span>
              <div className="flex min-h-[51px] items-center gap-2">
                <span className="ref-truncate text-[14px] font-medium text-ink">
                  {merchantDisplayName}
                </span>
                <span className="whitespace-nowrap rounded-tile bg-brand px-2 py-0.5 text-[11px] text-white">
                  由当前服务指定
                </span>
              </div>
            </div> :

          <label className={FIELD}>
              商家
              <select
              className={CONTROL}
              value={form.merchant_id_wsh}
              disabled={merchantLoading || merchants.length === 0}
              onChange={(event) => handleMerchantSelect(event.target.value)}>
              
                <option value="">{merchantSelectPlaceholder}</option>
                {merchants.map((merchant) =>
              <option key={String(merchant.id_wsh)} value={String(merchant.id_wsh)}>
                    {merchant.name_wsh}
                  </option>
              )}
              </select>
              <span className={HINT}>{merchantHint}</span>
            </label>
          }

          {/* 单宠物模式：看护人 */}
          {!isBatchMode ?
          <label className={FIELD}>
              看护人
              <select
              className={CONTROL}
              value={form.keeper_id_wsh}
              disabled={!form.merchant_id_wsh || keeperLoading || keepers.length === 0}
              onChange={(event) => handleKeeperSelect(event.target.value)}>
              
                <option value="">{keeperSelectPlaceholder}</option>
                {keepers.map((keeper) =>
              <option key={String(keeper.id_wsh)} value={String(keeper.id_wsh)}>
                    {keeperOptionLabel(keeper)}
                  </option>
              )}
              </select>
              <span className={HINT}>{keeperHint}</span>
            </label> :
          null}

          {/* ============ 第二步：预约时间 ============ */}
          {stepTitle(2, '预约时间')}

          {isBatchMode ?
          <>
              {petRows.map((row, index) =>
            <div
              key={index}
              className="col-span-full grid grid-cols-1 gap-3.5 rounded-card border border-line bg-surface p-3.5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
              
                  <div className="col-span-full flex items-center justify-between text-meta font-semibold text-muted">
                    <span>
                      宠物 <span data-numeric="true">{index + 1}</span>
                    </span>
                    {petRows.length > 1 ?
                <button
                  type="button"
                  className={`${BTN_OUTLINE} font-normal`}
                  onClick={() => removePetRow(index)}>
                  
                        移除
                      </button> :
                null}
                  </div>
                  <label className={FIELD}>
                    宠物
                    <select
                  className={CONTROL}
                  value={row.pet_id_wsh}
                  onChange={(event) => updateRow(index, { pet_id_wsh: event.target.value })}>
                  
                      <option value="">请选择宠物</option>
                      {petOptions}
                    </select>
                  </label>
                  <label className={FIELD}>
                    送达日期
                    <input
                  type="date"
                  className={CONTROL}
                  value={row.delivery_date_wsh}
                  min={minDate()}
                  max={maxDate()}
                  disabled={availabilityLoading || bookableDates.length === 0}
                  onChange={(event) => handleRowDeliveryDateChange(index, event.target.value)} />
                
                  </label>
                  <label className={FIELD}>
                    送达时间
                    <select
                  className={CONTROL}
                  value={row.delivery_slot_wsh}
                  disabled={
                  !row.delivery_date_wsh || slotsForDate(row.delivery_date_wsh).length === 0
                  }
                  onChange={(event) =>
                  updateRow(index, {
                    delivery_slot_wsh: event.target.value,
                    pickup_date_wsh: '',
                    pickup_slot_wsh: ''
                  })
                  }>
                  
                      <option value="">{rowSlotPlaceholder(row.delivery_date_wsh)}</option>
                      {slotsForDate(row.delivery_date_wsh).map((slot) =>
                  <option key={slot} value={slot}>
                          {displaySlot(slot)}
                        </option>
                  )}
                    </select>
                  </label>
                  <label className={FIELD}>
                    接回日期
                    <input
                  type="date"
                  className={CONTROL}
                  value={row.pickup_date_wsh}
                  min={row.delivery_date_wsh || minDate()}
                  max={maxDate()}
                  disabled={
                  !row.delivery_date_wsh || availabilityLoading || bookableDates.length === 0
                  }
                  onChange={(event) =>
                  updateRow(index, {
                    pickup_date_wsh: event.target.value,
                    pickup_slot_wsh: ''
                  })
                  } />
                
                  </label>
                  <label className={FIELD}>
                    接回时间
                    <select
                  className={CONTROL}
                  value={row.pickup_slot_wsh}
                  disabled={!row.pickup_date_wsh || rowPickupSlotOptions(row).length === 0}
                  onChange={(event) => updateRow(index, { pickup_slot_wsh: event.target.value })}>
                  
                      <option value="">{rowSlotPlaceholder(row.pickup_date_wsh)}</option>
                      {rowPickupSlotOptions(row).map((slot) =>
                  <option key={slot} value={slot}>
                          {displaySlot(slot)}
                        </option>
                  )}
                    </select>
                  </label>
                </div>
            )}
              <div className="col-span-full flex flex-wrap items-center gap-3">
                <button
                type="button"
                className={BTN_OUTLINE}
                disabled={petRows.length >= BATCH_MAX_PETS}
                onClick={addPetRow}>
                
                  {petRows.length >= BATCH_MAX_PETS ?
                `最多可添加 ${BATCH_MAX_PETS} 只宠物` :
                '+ 添加宠物'}
                </button>
                <span className={HINT}>{availabilityHint}</span>
              </div>
            </> :
          availabilityEnabled ?
          <>
              <label className={FIELD}>
                送达日期
                <input
                type="date"
                className={CONTROL}
                value={form.delivery_date_wsh}
                min={minDate()}
                max={maxDate()}
                disabled={availabilityLoading || bookableDates.length === 0}
                onChange={(event) => handleDeliveryDateChange(event.target.value)} />
              
                <span className={HINT}>{availabilityHint}</span>
              </label>
              <label className={FIELD}>
                {slotMode ? '开始时间' : '送达时间'}
                <select
                className={CONTROL}
                value={form.delivery_slot_wsh}
                disabled={!form.delivery_date_wsh || deliverySlotOptions.length === 0}
                onChange={(event) => handleDeliverySlotChange(event.target.value)}>
                
                  <option value="">{deliverySlotPlaceholder}</option>
                  {deliverySlotOptions.map((slot) =>
                <option key={slot} value={slot}>
                      {displaySlot(slot)}
                    </option>
                )}
                </select>
              </label>
              {isHourUnit ?
            <label className={FIELD}>
                  服务数量（小时）
                  <input
                data-testid="quantity-input"
                type="number"
                className={CONTROL}
                min={1}
                max={24}
                value={slotQuantity}
                data-numeric="true"
                onChange={(event) => setSlotQuantity(Number(event.target.value))} />
              
                  <span className={HINT}>连续服务时长 = 数量 × 60 分钟</span>
                </label> :
            null}
              {slotMode && !isHourUnit ?
            <label className={FIELD}>
                  服务数量
                  <input className={CONTROL} value={1} disabled readOnly data-numeric="true" />
                  <span className={HINT}>每次服务一个连续时段</span>
                </label> :
            null}
              {!slotMode ?
            <label className={FIELD}>
                  接回日期
                  <input
                type="date"
                className={CONTROL}
                value={form.pickup_date_wsh}
                min={form.delivery_date_wsh || minDate()}
                max={maxDate()}
                disabled={
                !form.delivery_date_wsh || availabilityLoading || bookableDates.length === 0
                }
                onChange={(event) =>
                patch({ pickup_date_wsh: event.target.value, pickup_slot_wsh: '' })
                } />
              
                  <span className={HINT}>接回日期不早于送达日期</span>
                </label> :
            null}
              {!slotMode ?
            <label className={FIELD}>
                  接回时间
                  <select
                className={CONTROL}
                value={form.pickup_slot_wsh}
                disabled={!form.pickup_date_wsh || pickupSlotOptions.length === 0}
                onChange={(event) => patch({ pickup_slot_wsh: event.target.value })}>
                
                    <option value="">{pickupSlotPlaceholder}</option>
                    {pickupSlotOptions.map((slot) =>
                <option key={slot} value={slot}>
                        {displaySlot(slot)}
                      </option>
                )}
                  </select>
                </label> :
            null}
            </> :

          <>
              <label className={FIELD}>
                送达时间
                <input
                type="datetime-local"
                className={CONTROL}
                value={form.delivery_time_wsh}
                min={minDateTime}
                onChange={(event) => patch({ delivery_time_wsh: event.target.value })} />
              
              </label>
              <label className={FIELD}>
                接回时间
                <input
                type="datetime-local"
                className={CONTROL}
                value={form.pickup_time_wsh}
                min={pickupMinDateTime}
                onChange={(event) => patch({ pickup_time_wsh: event.target.value })} />
              
              </label>
            </>
          }

          {/* ============ 第三步：宠物送达地址 ============ */}
          {stepTitle(3, '宠物送达地址')}
          <div className={`col-span-full ${FIELD}`}>
            <div className="flex items-center justify-between gap-2">
              <span>宠物送达地址</span>
              <button
                type="button"
                className={BTN_OUTLINE}
                disabled={!form.merchant_id_wsh}
                onClick={() => fillDeliveryFromMerchant(true)}>
                
                商家位置
              </button>
            </div>
            {addressPicker ??
            <input
              className={CONTROL}
              value={form.delivery_address_wsh}
              placeholder="搜索地点，或点击地图选址"
              onChange={(event) =>
              patch({
                delivery_address_wsh: event.target.value,
                delivery_location_source_wsh: '',
                delivery_latitude_wsh: null,
                delivery_longitude_wsh: null
              })
              } />

            }
            {deliveryDistanceVisible ?
            <div
              className={[
              'flex w-fit max-w-full items-center gap-2 px-3 py-1.5 text-meta leading-[1.4]',
              distanceError ?
              'rounded-card border border-critical/35 bg-transparent font-normal text-critical' :
              'rounded-full border border-brand/30 bg-cream',
              distanceError ?
              '' :
              distanceLoading ?
              'font-normal text-muted' :
              'font-semibold text-brand'].
              join(' ')}>
              
                {!distanceError ?
              <MapPinIcon className="h-3.5 w-3.5 shrink-0" aria-hidden="true" /> :
              null}
                <span className="[overflow-wrap:anywhere]">{deliveryDistanceText}</span>
                {distanceError ?
              <button
                type="button"
                className={`${BTN_OUTLINE} shrink-0`}
                onClick={() => onRetryDistance?.()}>
                
                    重试定位
                  </button> :
              null}
              </div> :
            null}
          </div>

          {/* ============ 第四步：联系信息与备注 ============ */}
          {stepTitle(4, '联系信息与备注')}
          <label className={FIELD}>
            紧急联系人
            <input
              className={CONTROL}
              placeholder="联系人姓名"
              value={form.emergency_contact_name_wsh}
              onChange={(event) => patch({ emergency_contact_name_wsh: event.target.value })} />
            
          </label>
          <label className={FIELD}>
            紧急联系电话
            <input
              type="tel"
              className={CONTROL}
              placeholder="联系人手机号"
              value={form.emergency_contact_phone_wsh}
              onChange={(event) => patch({ emergency_contact_phone_wsh: event.target.value })} />
            
          </label>
          <label className={`col-span-full ${FIELD}`}>
            优惠券
            <select
              className={CONTROL}
              value={form.user_coupon_id_wsh}
              disabled={couponLoading || availableCoupons.length === 0}
              onChange={(event) => handleCouponSelect(event.target.value)}>
              
              <option value="">
                {couponLoading ?
                '正在加载优惠券...' :
                availableCoupons.length === 0 ?
                '暂无优惠券' :
                '不使用优惠券'}
              </option>
              {availableCoupons.map((coupon) =>
              <option key={String(coupon.id_wsh)} value={String(coupon.id_wsh)}>
                  {coupon.name_wsh || `优惠券 #${coupon.id_wsh}`} · {couponText(coupon)}
                </option>
              )}
            </select>
          </label>
          <label className={`col-span-full ${FIELD}`}>
            备注
            <textarea
              className={CONTROL}
              rows={3}
              placeholder="饮食、过敏、用药、性格等注意事项"
              value={form.remark_wsh}
              onChange={(event) => patch({ remark_wsh: event.target.value })} />
            
          </label>
        </div>

        {estimatedAmount.total ?
        <div
          className="mt-5 rounded-card border border-brand/30 bg-cream px-[18px] py-3.5 font-semibold text-brand"
          data-numeric="true">
          
            预计金额：￥{estimatedAmount.total}
            {Number(estimatedAmount.discount) > 0 ?
          <span className="ml-2 text-meta font-normal text-critical">
                优惠 ￥{estimatedAmount.discount}
              </span> :
          null}
            {Number(estimatedAmount.couponDiscount) > 0 ?
          <span className="ml-2 text-meta font-normal text-critical">
                券减 ￥{estimatedAmount.couponDiscount}
              </span> :
          null}
            {Number(estimatedAmount.membershipDiscount) > 0 ?
          <span className="ml-2 text-meta font-normal text-critical">
                会员减 ￥{estimatedAmount.membershipDiscount}
              </span> :
          null}
            <div className="mt-1 text-[14px] font-normal">实付：￥{estimatedAmount.final}</div>
            {Number(estimatedAmount.platformSubsidy) > 0 ?
          <div className="mt-1 text-meta font-normal text-muted">
                优惠券由平台补贴，商家结算不受影响
              </div> :
          null}
          </div> :
        null}

        <div className="mt-6 flex items-center justify-end gap-3">
          <button type="button" className={BTN_SECONDARY} onClick={onClose}>
            取消
          </button>
          <button
            type="button"
            className={BTN_PRIMARY}
            disabled={submitting || merchantLoading || keeperLoading}
            onClick={submitOrder}>
            
            {submitting ? '提交中...' : '确认下单'}
          </button>
        </div>
      </div>
    </div>);

}