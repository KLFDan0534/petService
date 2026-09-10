import { MediaWithFallback } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const previews: ComponentPreviewModule = {
  componentName: 'MediaWithFallback',
  importPath: 'components/MediaWithFallback',
  previews: [
  {
    name: 'Loaded image',
    description: 'A valid source fills the parent box with object-fit: cover.',
    render: () =>
    <div className="h-48 w-80 overflow-hidden rounded-card border border-line">
          <MediaWithFallback
        src="https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?auto=format&fit=crop&w=800&q=70"
        alt="金毛犬在户外散步" />
      
        </div>

  },
  {
    name: 'Empty source',
    description:
    'No src renders the placeholder with an icon and optional caption.',
    render: () =>
    <div className="h-48 w-80 overflow-hidden rounded-card border border-line">
          <MediaWithFallback src="" alt="宠物服务现场" placeholder="暂无图片" />
        </div>

  },
  {
    name: 'Broken source',
    description:
    'A failing request falls back automatically; changing src retries.',
    render: () =>
    <div className="h-48 w-80 overflow-hidden rounded-card border border-line">
          <MediaWithFallback
        src="https://example.invalid/missing.jpg"
        alt="服务图片"
        placeholder="图片暂不可用"
        loading="eager" />
      
        </div>

  },
  {
    name: 'Avatar tile',
    description: 'Small square usage — the frame inherits the parent radius.',
    render: () =>
    <div className="flex items-center gap-4">
          <div className="h-16 w-16 overflow-hidden rounded-tile border border-line">
            <MediaWithFallback
          src="https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&w=200&q=70"
          alt="猫咪头像" />
        
          </div>
          <div className="h-16 w-16 overflow-hidden rounded-tile border border-line">
            <MediaWithFallback src="" alt="宠物头像" />
          </div>
        </div>

  }]

};

export default previews;