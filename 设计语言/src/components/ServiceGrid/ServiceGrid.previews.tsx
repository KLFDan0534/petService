import { ServiceGrid } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const services = [
{
  id_wsh: 1,
  name_wsh: '上门喂养 · 单猫日常照护',
  firstImage:
  'https://images.unsplash.com/photo-1592194996308-7b43878e84a6?auto=format&fit=crop&w=1200&q=80',
  category_name_wsh: '上门喂养',
  merchant_name_wsh: '暖爪宠物照护',
  distance_m_wsh: 850,
  price_wsh: 128,
  unit_wsh: 'session',
  description_wsh:
  '铲屎、换水、添粮与陪玩，全程拍照记录并同步反馈。适合出差或短途旅行的单猫家庭。'
},
{
  id_wsh: 2,
  name_wsh: '宠物寄养 · 家庭式独立房间',
  firstImage:
  'https://images.unsplash.com/photo-1583337130417-3346a1be7dee?auto=format&fit=crop&w=1200&q=80',
  category_name_wsh: '寄养',
  merchant_name_wsh: '小院寄养工作室',
  distance_m_wsh: 3200,
  price_wsh: 198,
  unit_wsh: 'day',
  description_wsh:
  '独立房间、每日两次遛放、24 小时监控回看，接送可选。入住前需提供疫苗与驱虫记录。'
},
{
  id_wsh: 3,
  name_wsh: '专业美容 · 短毛犬基础造型',
  category_name_wsh: '美容',
  merchant_name_wsh: '毛毛造型沙龙',
  distance_m_wsh: null,
  price_wsh: 268,
  unit_wsh: 'hour',
  description_wsh: ''
}];


const previews: ComponentPreviewModule = {
  componentName: 'ServiceGrid',
  importPath: 'components/ServiceGrid',
  previews: [
  {
    name: 'Default',
    description:
    'Three services — the third falls back to the media placeholder and default copy.',
    render: () =>
    <div className="w-full bg-canvas p-6">
          <ServiceGrid services={services} />
        </div>

  },
  {
    name: 'Logged in',
    description:
    'Favorite toggle shown next to the primary booking action, with one service favorited.',
    render: () =>
    <div className="w-full bg-canvas p-6">
          <ServiceGrid services={services.slice(0, 2)} isLoggedIn favoriteIds={[1]} />
        </div>

  },
  {
    name: 'Loading',
    description: 'Six shimmering skeleton cards while the catalog resolves.',
    render: () =>
    <div className="w-full bg-canvas p-6">
          <ServiceGrid loading />
        </div>

  },
  {
    name: 'Empty',
    description: 'Dashed hairline empty state when filters return nothing.',
    render: () =>
    <div className="w-full bg-canvas p-6">
          <ServiceGrid services={[]} />
        </div>

  }]

};

export default previews;