import { MediaFrame } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const HERO =
'https://images.unsplash.com/photo-1552053831-71594a27632d?auto=format&fit=crop&w=1200&q=80';

const previews: ComponentPreviewModule = {
  componentName: 'MediaFrame',
  importPath: 'components/MediaFrame',
  previews: [
  {
    name: 'Hero (4/3)',
    description:
    'Confirmed hero frame: 26px radius, 4/3 ratio, hairline border. Hover to see the image scale to 1.06.',
    render: () =>
    <div className="w-full max-w-[520px]">
          <MediaFrame src={HERO} alt="照护服务" imgLoading="eager" />
        </div>

  },
  {
    name: 'Loading',
    description: 'Shimmer skeleton while the media is being fetched.',
    render: () =>
    <div className="w-full max-w-[520px]">
          <MediaFrame loading alt="照护服务" />
        </div>

  },
  {
    name: 'Fallback',
    description: 'Missing or broken image falls back to a sand placeholder with a label.',
    render: () =>
    <div className="w-full max-w-[520px]">
          <MediaFrame src="" alt="服务图片" fallbackLabel="暂无图片" />
        </div>

  },
  {
    name: 'Card media',
    description: 'Card radius, 4/3 ratio — used as the media area of a browsable card.',
    render: () =>
    <div className="w-full max-w-[320px]">
          <MediaFrame src={HERO} alt="宠物寄养" radius="card" imgLoading="eager" />
        </div>

  },
  {
    name: 'Square avatar',
    description: 'Square ratio with control radius and hover zoom disabled.',
    render: () =>
    <div className="w-full max-w-[160px]">
          <MediaFrame
        src={HERO}
        alt="家长头像"
        ratio="1/1"
        radius="control"
        zoomOnHover={false}
        imgLoading="eager" />
      
        </div>

  }]

};

export default previews;