import { ArrowRightIcon } from 'lucide-react';
import { SectionHeading } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

function TextLink({ children }: {children: React.ReactNode;}) {
  return (
    <button
      type="button"
      className="group inline-flex items-center gap-2 text-control font-medium text-brand transition-colors duration-fast ease-editorial hover:text-brand-deep">
      
      {children}
      <ArrowRightIcon
        className="h-4 w-4 transition-transform duration-normal ease-editorial group-hover:translate-x-1"
        aria-hidden="true" />
      
    </button>);

}

const previews: ComponentPreviewModule = {
  componentName: 'SectionHeading',
  importPath: 'components/SectionHeading',
  previews: [
  {
    name: 'Default',
    description: 'Label, title and description — the standard dashboard section opener.',
    render: () =>
    <SectionHeading
      label="照护方案"
      title="按毛孩子的节奏，选择合适的照护方式"
      description="寄养、洗护、上门陪伴与行为训练由同一套照护标准贯穿，价格与档期实时同步门店。" />


  },
  {
    name: 'With action',
    description: 'Trailing action bottom-aligned with the title block; wraps below on mobile.',
    render: () =>
    <SectionHeading
      label="近期订单"
      title="你的照护安排"
      description="包含进行中与已完成的订单，按开始时间排序。"
      action={<TextLink>查看全部方案</TextLink>} />


  },
  {
    name: 'Title only',
    description: 'Minimal variant for tight sections — no label, index or description.',
    render: () => <SectionHeading title="常见问题" as="h3" />
  },
  {
    name: 'Dark tone',
    description: 'Inverted ink for dark bands and photography.',
    render: () =>
    <div className="bg-ink p-8">
          <SectionHeading
        tone="dark"
        label="门店实拍"
        title="照护现场，随时可见"
        description="每一次寄养与洗护都会留下影像记录，家长可在订单里查看。"
        action={
        <button
          type="button"
          className="inline-flex items-center gap-2 rounded-control border border-white/25 px-4 py-2 text-control font-medium text-cream transition-colors duration-fast ease-editorial hover:bg-white/10">
          
                查看相册
              </button>
        } />
      
        </div>

  },
  {
    name: 'With index (legacy)',
    description:
    'The original dashboard numbering. Editorial Warm guidelines remove decorative section numbers — prefer the label-only variants above.',
    render: () =>
    <SectionHeading
      index="01"
      label="照护方案"
      title="按毛孩子的节奏，选择合适的照护方式" />


  }]

};

export default previews;