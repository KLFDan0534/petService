import type { ReactNode } from 'react';
import { OrderCard, type OrderRecord } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const COVER = "/7272b30d-a8d8-49a4-b59d-030484adc77b.jpg";

const PET_AVATAR = "/da4a850d-b853-4008-94f9-279749fcdfba.jpg";


const NOW = new Date('2026-08-29T10:00:00').getTime();

const baseOrder: OrderRecord = {
  id_wsh: 20871,
  order_no_wsh: 'PS20260829-000871',
  service_name_wsh: '家庭式宠物寄养 · 单只专属房',
  service_images_wsh: [COVER],
  pet_id_wsh: 44,
  pet_name_wsh: '豆豆',
  pet_avatar_wsh: PET_AVATAR,
  pet_breed_wsh: '柯基',
  pet_age_wsh: 3,
  pet_weight_wsh: 11.5,
  merchant_id_wsh: 12,
  merchant_name_wsh: '暖屋宠物之家（静安寺店）',
  merchant_phone_wsh: '02165880012',
  keeper_name_wsh: '林一诺',
  start_date_wsh: '2026-09-02',
  end_date_wsh: '2026-09-06',
  created_at_wsh: '2026-08-29 09:52:00',
  final_amount_wsh: 1280,
  discount_wsh: 120,
  billing_unit_wsh: 'day',
  quantity_wsh: 4
};

const Frame = ({ children }: {children: ReactNode;}) =>
<div className="w-full bg-canvas p-6">
    <div className="mx-auto max-w-shell">{children}</div>
  </div>;


const previews: ComponentPreviewModule = {
  componentName: 'OrderCard',
  importPath: 'components/OrderCard',
  previews: [
  {
    name: '待付款（含支付倒计时）',
    description:
    'pending 状态：品牌色状态徽章、支付倒计时条，操作区提供取消订单与余额支付。',
    render: () =>
    <Frame>
          <OrderCard
        order={{ ...baseOrder, status_wsh: 'pending' }}
        nowMs={NOW} />
      
        </Frame>

  },
  {
    name: '服务中',
    description:
    'in_progress 状态：进度条推进到「寄养中」，操作区提供联系门店与查看服务动态。',
    render: () =>
    <Frame>
          <OrderCard
        order={{
          ...baseOrder,
          status_wsh: 'in_progress',
          delivery_time_wsh: '2026-08-29 10:05:00',
          delivered_at_wsh: '2026-09-02 09:12:00'
        }}
        nowMs={NOW} />
      
        </Frame>

  },
  {
    name: '已完成 · 待反馈',
    description:
    'completed 且未评价：显示「待反馈」标签，操作区提供再次预约、打赏照护师与评价服务。',
    render: () =>
    <Frame>
          <OrderCard
        order={{
          ...baseOrder,
          status_wsh: 'completed',
          has_feedback_wsh: false,
          delivery_time_wsh: '2026-08-20 10:05:00',
          delivered_at_wsh: '2026-08-22 09:12:00',
          received_at_wsh: '2026-08-26 18:40:00'
        }}
        nowMs={NOW} />
      
        </Frame>

  },
  {
    name: '已取消',
    description:
    '关闭类状态：进度条替换为流程中止说明，操作区仅保留订单详情与再次预约。',
    render: () =>
    <Frame>
          <OrderCard
        order={{
          ...baseOrder,
          status_wsh: 'cancelled',
          discount_wsh: 0
        }}
        nowMs={NOW} />
      
        </Frame>

  },
  {
    name: '缺图 + 请求中',
    description:
    '无封面与宠物头像时回退到占位图与首字母；processing 时禁用变更类操作。',
    render: () =>
    <Frame>
          <OrderCard
        order={{
          ...baseOrder,
          status_wsh: 'confirmed',
          service_images_wsh: null,
          pet_avatar_wsh: undefined,
          keeper_name_wsh: undefined,
          order_no_wsh: undefined
        }}
        nowMs={NOW}
        processing />
      
        </Frame>

  }]

};

export default previews;