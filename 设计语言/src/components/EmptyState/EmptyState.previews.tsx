import { SearchXIcon } from 'lucide-react';
import { EmptyState } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const previews: ComponentPreviewModule = {
  componentName: 'EmptyState',
  importPath: 'components/EmptyState',
  previews: [
  {
    name: 'Default',
    description: 'Title and description with the default line-art mark.',
    render: () =>
    <EmptyState title="暂无服务" description="换个筛选条件或稍后再看看。" />

  },
  {
    name: 'With action',
    description: 'Default slot used for a primary action.',
    render: () =>
    <EmptyState title="还没有订单" description="下单后，订单会显示在这里。">
          <button
        type="button"
        className="rounded-control bg-brand px-4 py-2 text-control text-white transition-colors duration-fast ease-editorial hover:bg-brand-deep">
        
            浏览服务
          </button>
        </EmptyState>

  },
  {
    name: 'Custom icon',
    description: 'A custom lucide mark passed through the icon prop.',
    render: () =>
    <EmptyState
      icon={<SearchXIcon strokeWidth={1.25} aria-hidden="true" />}
      title="没有匹配结果"
      description="试试更短的关键词。" />


  },
  {
    name: 'Title only',
    description: 'Description omitted for compact regions.',
    render: () => <EmptyState title="暂无评价" />
  }]

};

export default previews;