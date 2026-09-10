import { LoadingSpinner } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const previews: ComponentPreviewModule = {
  componentName: 'LoadingSpinner',
  importPath: 'components/LoadingSpinner',
  previews: [
  {
    name: 'Default',
    description: 'Default busy indicator with the fallback 加载中... label',
    render: () => <LoadingSpinner />
  },
  {
    name: 'With text',
    description: 'Custom loading message for a specific region',
    render: () => <LoadingSpinner text="加载商家资料..." />
  },
  {
    name: 'In a surface',
    description: 'Placed inside a card surface while its data loads',
    render: () =>
    <div className="w-full max-w-prose rounded-card border border-line bg-surface">
          <LoadingSpinner text="加载订单列表..." />
        </div>

  },
  {
    name: 'Compact',
    description: 'Tighter padding via className for inline panels',
    render: () => <LoadingSpinner text="提交中..." className="py-6" />
  }]

};

export default previews;