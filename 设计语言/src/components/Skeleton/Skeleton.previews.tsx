import { Skeleton, SkeletonText, SkeletonCard, SkeletonList } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const previews: ComponentPreviewModule = {
  componentName: 'Skeleton',
  importPath: 'components/Skeleton',
  previews: [
  {
    name: 'Text lines',
    description: 'Three shimmer lines with the source ragged widths (58% / 100% / 76%).',
    render: () =>
    <div className="w-full max-w-prose">
          <SkeletonText />
        </div>

  },
  {
    name: 'Shapes',
    description: 'Line, circle avatar, media frame, and the flat sand panel.',
    render: () =>
    <div className="flex w-full max-w-shell flex-col gap-6">
          <div className="flex items-center gap-4">
            <Skeleton variant="circle" width={48} />
            <div className="flex-1">
              <SkeletonText lines={2} gap={10} lastLineWidth="42%" />
            </div>
          </div>
          <div className="grid grid-cols-1 gap-gutter md:grid-cols-[minmax(0,1.35fr)_minmax(0,1fr)]">
            <Skeleton variant="media" aspect="16 / 10" radius="frame" />
            <Skeleton variant="block" height={220} tone="static" />
          </div>
        </div>

  },
  {
    name: 'Card',
    description: 'Single browsable-item placeholder: shimmer media plus body lines.',
    render: () =>
    <div className="w-full max-w-[340px]">
          <SkeletonCard />
        </div>

  },
  {
    name: 'Loading grid',
    description: 'Region-level loading state for a service catalogue — replaces bare loading text.',
    render: () =>
    <div className="w-full max-w-shell">
          <SkeletonList count={3} label="服务加载中" />
        </div>

  }]

};

export default previews;