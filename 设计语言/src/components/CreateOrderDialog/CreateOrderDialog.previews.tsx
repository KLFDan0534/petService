import { useState } from 'react';
import {
  CreateOrderDialog,
  toDateOnly,
  type AvailabilityDay,
  type Coupon,
  type CreateOrderPayload,
  type CreateOrdersBatchPayload,
  type Keeper,
  type Merchant,
  type Pet,
  type ServiceSummary } from
'./index';
import type { ComponentPreviewModule } from '../previewTypes';

/* --------------------------------- fixtures ------------------------------- */

const pets: Pet[] = [
{ id_wsh: 11, name_wsh: '汤圆', breed_wsh: '英短蓝猫' },
{ id_wsh: 12, name_wsh: '豆豆', breed_wsh: '柯基' },
{ id_wsh: 13, name_wsh: '奶昔', breed_wsh: '布偶' }];


const merchants: Merchant[] = [
{
  id_wsh: 501,
  name_wsh: '暖屋宠物寄养（望京店）',
  address_wsh: '北京市朝阳区望京东路 6 号院 3 号楼',
  latitude_wsh: 39.9962,
  longitude_wsh: 116.4735
},
{
  id_wsh: 502,
  name_wsh: '毛茸茸生活馆',
  address_wsh: '北京市海淀区中关村南大街 12 号',
  latitude_wsh: 39.9756,
  longitude_wsh: 116.3163
}];


const keepers: Keeper[] = [
{ id_wsh: 901, name_wsh: '李静', current_pets_wsh: 2, max_pets_wsh: 5 },
{ id_wsh: 902, name_wsh: '张伟', current_pets_wsh: 4, max_pets_wsh: 6 }];


const coupons: Coupon[] = [
{
  id_wsh: 3001,
  name_wsh: '新客立减',
  type_wsh: 'amount',
  threshold_amount_wsh: 200,
  discount_amount_template_wsh: 30
},
{
  id_wsh: 3002,
  name_wsh: '寄养 8 折',
  type_wsh: 'percent',
  discount_rate_wsh: 0.8,
  max_discount_amount_wsh: 80
}];


function buildAvailability(slots: string[]): AvailabilityDay[] {
  return Array.from({ length: 12 }, (_, index) => {
    const date = toDateOnly(new Date(Date.now() + index * 86400000));
    return {
      date_wsh: date,
      // 每 5 天一个休息日，用来展示「仅可预约日期」的约束
      bookable_wsh: index % 5 !== 4,
      windows_wsh: [{ slots_wsh: slots.map((time) => `${date}T${time}:00`) }]
    };
  });
}

const dayService: ServiceSummary = {
  id_wsh: 77,
  name_wsh: '猫咪上门喂养 · 全日寄养',
  price_wsh: 168,
  unit_wsh: 'day',
  booking_mode_wsh: 'date_range',
  service_version_wsh: 'v3',
  merchant_id_wsh: 501,
  merchant_name_wsh: '暖屋宠物寄养（望京店）'
};

const sessionService: ServiceSummary = {
  id_wsh: 88,
  name_wsh: '专业宠物洗护 · 单次',
  price_wsh: 128,
  unit_wsh: 'session',
  booking_mode_wsh: 'slot',
  duration_minutes_wsh: 90,
  service_version_wsh: 'v1',
  merchant_id_wsh: 502,
  merchant_name_wsh: '毛茸茸生活馆'
};

const hourService: ServiceSummary = {
  id_wsh: 99,
  name_wsh: '上门陪玩 · 按小时',
  price_wsh: 60,
  unit_wsh: 'hour',
  booking_mode_wsh: 'slot',
  duration_minutes_wsh: 60,
  service_version_wsh: 'v2',
  merchant_id_wsh: 502,
  merchant_name_wsh: '毛茸茸生活馆'
};

/* --------------------------------- harness -------------------------------- */

interface HarnessProps {
  service?: ServiceSummary | null;
  availabilityDays?: AvailabilityDay[];
  merchantLoading?: boolean;
  keeperLoading?: boolean;
  availabilityLoading?: boolean;
  availabilityError?: boolean;
  couponLoading?: boolean;
  keepers?: Keeper[];
  availableCoupons?: Coupon[];
  deliveryDistanceMeters?: number | null;
  distanceError?: string;
}

function Harness({
  service = null,
  availabilityDays = [],
  keepers: keeperList = keepers,
  availableCoupons = coupons,
  ...rest
}: HarnessProps) {
  const [visible, setVisible] = useState(true);
  const [message, setMessage] = useState('');
  const [payload, setPayload] = useState<CreateOrderPayload | CreateOrdersBatchPayload | null>(null);

  return (
    <div className="min-h-[520px] w-full bg-canvas p-4">
      {!visible ?
      <button
        type="button"
        className="rounded-control bg-brand px-4 py-2 text-control text-white transition-colors duration-fast hover:bg-brand-deep"
        onClick={() => {
          setVisible(true);
          setPayload(null);
          setMessage('');
        }}>
        
          立即预约
        </button> :
      null}
      {message ? <p className="mt-3 text-meta text-critical">{message}</p> : null}
      {payload ?
      <pre className="mt-3 max-h-64 overflow-auto rounded-card border border-line bg-surface p-3 font-mono text-[11px] text-ink-soft">
          {JSON.stringify(payload, null, 2)}
        </pre> :
      null}
      <CreateOrderDialog
        visible={visible}
        service={service}
        pets={pets}
        merchants={merchants}
        keepers={keeperList}
        availabilityDays={availabilityDays}
        availableCoupons={availableCoupons}
        maxBookingDays={availabilityDays.length}
        deliveryDistanceMeters={rest.deliveryDistanceMeters ?? null}
        {...rest}
        onClose={() => setVisible(false)}
        onNotify={(text) => setMessage(text)}
        onSubmit={(next) => {
          setPayload(next);
          setMessage('');
          setVisible(false);
        }}
        onRetryDistance={() => setMessage('正在重新定位...')} />
      
    </div>);

}

/* --------------------------------- previews ------------------------------- */

const previews: ComponentPreviewModule = {
  componentName: 'CreateOrderDialog',
  importPath: 'components/CreateOrderDialog',
  previews: [
  {
    name: '服务驱动 · 按天批量',
    description:
    '按天计费的服务会进入批量模式：先选看护人解锁可预约日期，再为每只宠物排送达/接回时间。',
    render: () =>
    <Harness
      service={dayService}
      availabilityDays={buildAvailability(['09:00', '11:00', '14:00', '17:00'])} />


  },
  {
    name: '单次时段服务',
    description: '按次计费走单笔时段预约：只需开始时间，服务数量固定为 1。',
    render: () =>
    <Harness
      service={sessionService}
      availabilityDays={buildAvailability(['10:00', '13:00', '15:30'])} />


  },
  {
    name: '按小时 · 含数量与优惠券',
    description: '按小时计费时出现服务数量输入，估价随数量与自动选中的最优券变化。',
    render: () =>
    <Harness
      service={hourService}
      availabilityDays={buildAvailability(['08:00', '12:00', '16:00', '19:00'])}
      deliveryDistanceMeters={2400} />


  },
  {
    name: '非服务模式 · 自选商家',
    description: '没有服务上下文时商家可选，时间退回 datetime-local 输入，无可预约日期约束。',
    render: () => <Harness />
  },
  {
    name: '加载与空状态',
    description: '商家/看护人/优惠券加载中，且该商家暂无可接单看护人时的提示文案。',
    render: () =>
    <Harness
      service={dayService}
      keepers={[]}
      availableCoupons={[]}
      keeperLoading
      couponLoading
      availabilityError />


  }]

};

export default previews;