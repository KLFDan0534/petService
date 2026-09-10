import { Timeline } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const items = [
{
  id: 3,
  type: 'feed',
  time: '2026-08-29T08:10:00',
  content: '上午 8 点喂食 100g，食欲正常，饮水充足。'
},
{
  id: 2,
  type: 'activity',
  time: '2026-08-28T17:40:00',
  content: '下午散步 40 分钟，精神状态良好，途中与其他犬只互动正常。',
  images:
  'https://images.unsplash.com/photo-1543466835-00a7907e9de1?w=200&q=60,https://images.unsplash.com/photo-1552053831-71594a27632d?w=200&q=60'
},
{
  id: 1,
  type: 'health',
  time: '2026-08-28T09:05:00',
  content: '体温正常，无异常情况。'
}];


const previews: ComponentPreviewModule = {
  componentName: 'Timeline',
  importPath: 'components/Timeline',
  previews: [
  {
    name: 'Default',
    description: 'Care records with type + datetime meta line and a photo grid',
    render: () => <Timeline items={items} />
  },
  {
    name: 'Text only',
    description: 'Records without attached photos',
    render: () =>
    <Timeline items={items.filter((item) => !item.images)} />

  },
  {
    name: 'Loading',
    description: 'Skeleton rows while records are being fetched',
    render: () => <Timeline items={[]} loading loadingCount={3} />
  },
  {
    name: 'Empty',
    description: 'Inline empty label instead of a blank region',
    render: () => <Timeline items={[]} />
  }]

};

export default previews;