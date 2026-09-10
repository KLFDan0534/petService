import { Breadcrumb } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const previews: ComponentPreviewModule = {
  componentName: 'Breadcrumb',
  importPath: 'components/Breadcrumb',
  previews: [
  {
    name: 'Default',
    description: 'Two-level trail as used on the services listing page.',
    render: () =>
    <Breadcrumb
      items={[
      { label: '首页', href: '/dashboard' },
      { label: '全部服务' }]
      } />


  },
  {
    name: 'Three levels',
    description: 'Deeper trail — every crumb except the last is a link.',
    render: () =>
    <Breadcrumb
      items={[
      { label: '首页', href: '/dashboard' },
      { label: '个人中心', href: '/profile' },
      { label: '会员权益' }]
      } />


  },
  {
    name: 'Long label truncation',
    description:
    'Long service names truncate instead of breaking the layout.',
    render: () =>
    <div style={{ maxWidth: 320 }}>
          <Breadcrumb
        items={[
        { label: '首页', href: '/dashboard' },
        { label: '门店', href: '/merchants' },
        { label: '毛孩子的家 · 上门喂养与遛狗综合服务门店' }]
        } />
      
        </div>

  }]

};

export default previews;