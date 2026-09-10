import { SectionBand } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const previews: ComponentPreviewModule = {
  componentName: 'SectionBand',
  importPath: 'components/SectionBand',
  previews: [
  {
    name: 'Default',
    description: 'Hairline separator, display title and a muted note — the results heading from the services catalogue.',
    render: () =>
    <div className="w-full">
          <SectionBand
        title="全部照护方案"
        note="共 24 套方案 · 价格与档期实时同步门店"
        liveNote />
      
        </div>

  },
  {
    name: 'Title only',
    description: 'Without a note, for quieter section breaks inside a long page.',
    render: () =>
    <div className="w-full">
          <SectionBand title="洗护美容" />
        </div>

  },
  {
    name: 'With eyebrow',
    description: 'Optional uppercase eyebrow with a 32px hairline above the title.',
    render: () =>
    <div className="w-full">
          <SectionBand
        eyebrow="Catalogue"
        title="按分类浏览"
        note="选择分类，下方方案将同步更新" />
      
        </div>

  },
  {
    name: 'With trailing action',
    description: 'Trailing controls sit on the baseline of the title block and wrap on narrow viewports.',
    render: () =>
    <div className="w-full">
          <SectionBand
        title="寄养托管"
        note="正在加载方案"
        action={
        <button
          type="button"
          className="inline-flex items-center h-11 px-4 rounded-control border border-line bg-surface text-control font-medium text-ink-soft transition-colors duration-fast ease-editorial hover:bg-sand">
          
                按价格排序
              </button>
        } />
      
        </div>

  }]

};

export default previews;