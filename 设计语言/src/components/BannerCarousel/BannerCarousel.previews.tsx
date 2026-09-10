import { BannerCarousel } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const banners = [
{
  id_wsh: 1,
  title_wsh: '春季洗护套餐 · 老客立减 80 元',
  image_url_wsh:
  'https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?auto=format&fit=crop&w=1600&q=80',
  link_url_wsh: 'https://example.com/campaign/spring-grooming',
  status_wsh: 1
},
{
  id_wsh: 2,
  title_wsh: '上门喂养 · 首单免服务费',
  image_url_wsh:
  'https://images.unsplash.com/photo-1548199973-03cce0bbc87b?auto=format&fit=crop&w=1600&q=80',
  link_url_wsh: 'https://example.com/campaign/feeding',
  status_wsh: 1
},
{
  id_wsh: 3,
  title_wsh: '认证宠托师招募中',
  image_url_wsh:
  'https://images.unsplash.com/photo-1552053831-71594a27632d?auto=format&fit=crop&w=1600&q=80',
  status_wsh: 1
}];


const previews: ComponentPreviewModule = {
  componentName: 'BannerCarousel',
  importPath: 'components/BannerCarousel',
  previews: [
  {
    name: 'Auto-rotating',
    description:
    'Three active banners with arrows and dot controls. Rotates every 6s and pauses on hover or focus.',
    render: () => <BannerCarousel banners={banners} />
  },
  {
    name: 'Single banner',
    description:
    'One slide only — arrows and dots are hidden and auto-rotation never starts.',
    render: () => <BannerCarousel banners={[banners[0]]} />
  },
  {
    name: 'No link',
    description:
    'Banner without a safe http(s) link: no call-to-action, cursor stays default.',
    render: () => <BannerCarousel banners={[banners[2]]} />
  },
  {
    name: 'Image failure fallback',
    description:
    'Broken or missing image URL falls back to a warm placeholder with the banner title.',
    render: () =>
    <BannerCarousel
      banners={[
      {
        id_wsh: 9,
        title_wsh: '平台精选活动',
        image_url_wsh: 'https://example.com/missing-banner.jpg',
        link_url_wsh: 'https://example.com/campaign',
        status_wsh: 1
      }]
      } />


  }]

};

export default previews;