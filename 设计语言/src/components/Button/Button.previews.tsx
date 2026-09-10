import { PlusIcon, TrashIcon } from 'lucide-react';
import { Button } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const previews: ComponentPreviewModule = {
  componentName: 'Button',
  importPath: 'components/Button',
  previews: [
  {
    name: 'Variants',
    description:
    'The full .cta family: primary, outline, dark, ghost, quiet and danger.',
    render: () =>
    <div className="flex flex-wrap items-center gap-3">
          <Button variant="primary">立即预约</Button>
          <Button variant="outline">查看详情</Button>
          <Button variant="dark">登录</Button>
          <Button variant="ghost">返回</Button>
          <Button variant="quiet">取消</Button>
          <Button variant="danger" icon={<TrashIcon className="h-[15px] w-[15px]" />}>
            删除
          </Button>
        </div>

  },
  {
    name: 'Sizes',
    description:
    '34px (sm), 42px (md, canonical) and 48px (lg, full-width). Distinct from the 44px input scale.',
    render: () =>
    <div className="flex w-full max-w-xs flex-col items-start gap-3">
          <Button size="sm">小号操作</Button>
          <Button size="md">标准操作</Button>
          <Button size="lg" arrow>
            提交申请
          </Button>
        </div>

  },
  {
    name: 'Arrow and icon',
    description:
    'The cta-arrow affordance nudges 3px on hover; leading icons use lucide-react.',
    render: () =>
    <div className="flex flex-wrap items-center gap-3">
          <Button variant="primary" arrow>
            浏览服务
          </Button>
          <Button variant="outline" icon={<PlusIcon className="h-[15px] w-[15px]" />}>
            添加宠物
          </Button>
          <Button variant="ghost" arrow>
            全部订单
          </Button>
        </div>

  },
  {
    name: 'Loading and disabled',
    description: 'Disabled drops to 0.5 opacity and cancels the hover lift.',
    render: () =>
    <div className="flex flex-wrap items-center gap-3">
          <Button variant="primary" loading>
            登录中…
          </Button>
          <Button variant="primary" disabled>
            不可用
          </Button>
          <Button variant="outline" disabled>
            不可用
          </Button>
        </div>

  },
  {
    name: 'Light on dark',
    description: 'The light variant is fixed cream on paper, for dark sections only.',
    render: () =>
    <div className="flex w-full flex-wrap items-center gap-3 rounded-card bg-[#17130f] p-6">
          <Button variant="light" arrow>
            成为服务者
          </Button>
          <Button variant="light" size="sm">
            了解更多
          </Button>
        </div>

  }]

};

export default previews;