import { Eyebrow } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const previews: ComponentPreviewModule = {
  componentName: 'Eyebrow',
  importPath: 'components/Eyebrow',
  previews: [
  {
    name: 'Default',
    description: 'Hairline + uppercase label, as used above hero titles.',
    render: () => <Eyebrow>Services</Eyebrow>
  },
  {
    name: 'Section header',
    description: 'Paired with a section title, decorative for screen readers.',
    render: () =>
    <div>
          <Eyebrow as="p" decorative>
            Catalogue
          </Eyebrow>
          <h2 className="mt-3 font-display text-display-md text-ink">按分类浏览</h2>
          <p className="mt-2 text-meta text-muted">选择分类，下方方案将同步更新</p>
        </div>

  },
  {
    name: 'Without line',
    description: 'Label only — for tight spots such as card meta rows.',
    render: () => <Eyebrow line={false}>Membership</Eyebrow>
  },
  {
    name: 'Brand tone',
    description: 'Terracotta label used on browsable card eyebrows.',
    render: () => <Eyebrow tone="brand">Featured</Eyebrow>
  }]

};

export default previews;