import { Logo } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const previews: ComponentPreviewModule = {
  componentName: 'Logo',
  importPath: 'components/Logo',
  previews: [
  {
    name: 'Header lockup',
    description: '48px mark + wordmark, linked to the dashboard — the product header lockup.',
    render: () => <Logo href="#" />
  },
  {
    name: 'Mark only',
    description: 'Compact mark with an accessible name, for tight headers and mobile bars.',
    render: () => <Logo showWordmark={false} />
  },
  {
    name: 'Sizes',
    description: 'sm (32px), md (48px, default) and lg (56px).',
    render: () =>
    <div className="flex flex-col gap-4">
          <Logo size="sm" />
          <Logo size="md" />
          <Logo size="lg" />
        </div>

  },
  {
    name: 'Image failure fallback',
    description: 'If the raster mark cannot load, a quiet paw mark on sand takes its place.',
    render: () => <Logo src="/missing-logo.png" />
  }]

};

export default previews;