import { StatusBadge, toneFromBadgeClass } from './index';
import { ClockIcon, CheckIcon } from 'lucide-react';
import type { ComponentPreviewModule } from '../previewTypes';

const previews: ComponentPreviewModule = {
  componentName: 'StatusBadge',
  importPath: 'components/StatusBadge',
  previews: [
  {
    name: 'All tones',
    description:
    'The full badge family. Warning is the restrained amber wash — never a solid brand fill.',
    render: () =>
    <div className="flex flex-wrap items-center gap-2">
          <StatusBadge tone="primary">已送达</StatusBadge>
          <StatusBadge tone="secondary">已关闭</StatusBadge>
          <StatusBadge tone="success">已完成</StatusBadge>
          <StatusBadge tone="warning">待付款</StatusBadge>
          <StatusBadge tone="danger">已取消</StatusBadge>
          <StatusBadge tone="error">投诉</StatusBadge>
          <StatusBadge tone="info">服务中</StatusBadge>
          <StatusBadge tone="disabled">不可用</StatusBadge>
        </div>

  },
  {
    name: 'With dot and icon',
    description:
    'Optional leading dot or lucide icon sits in the badge’s 6px gap.',
    render: () =>
    <div className="flex flex-wrap items-center gap-2">
          <StatusBadge tone="success" dot>
            在线
          </StatusBadge>
          <StatusBadge tone="warning" dot>
            忙碌
          </StatusBadge>
          <StatusBadge tone="secondary" dot>
            离线
          </StatusBadge>
          <StatusBadge tone="warning" icon={<ClockIcon />}>
            待审核
          </StatusBadge>
          <StatusBadge tone="success" icon={<CheckIcon />}>
            已通过
          </StatusBadge>
        </div>

  },
  {
    name: 'From legacy status map',
    description:
    'toneFromBadgeClass() converts the product’s getStatusBadge() output into a tone.',
    render: () =>
    <div className="flex flex-wrap items-center gap-2">
          {[
      { label: '待付款', badge: 'badge-warning' },
      { label: '已支付', badge: 'badge-info' },
      { label: '已送达', badge: 'badge-primary' },
      { label: '已完成', badge: 'badge-success' },
      { label: '已取消', badge: 'badge-danger' }].
      map((s) =>
      <StatusBadge key={s.badge} tone={toneFromBadgeClass(s.badge)}>
              {s.label}
            </StatusBadge>
      )}
        </div>

  },
  {
    name: 'In context',
    description: 'Inline beside a record title, with a truncating long label.',
    render: () =>
    <div className="w-full max-w-sm rounded-card border border-line bg-surface p-5">
          <div className="flex items-center justify-between gap-3">
            <h3 className="font-display text-display-xs text-ink">订单 #10482</h3>
            <StatusBadge tone="warning">退款中</StatusBadge>
          </div>
          <p className="mt-2 text-meta text-muted">2026-08-27 14:20 · 上门喂养</p>
          <div className="mt-4 max-w-[140px]">
            <StatusBadge tone="secondary">
              一个非常非常长的状态标签不会破坏布局
            </StatusBadge>
          </div>
        </div>

  }]

};

export default previews;