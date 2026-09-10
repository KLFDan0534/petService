import { TextLink } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const previews: ComponentPreviewModule = {
  componentName: 'TextLink',
  importPath: 'components/TextLink',
  previews: [
  {
    name: 'Default',
    description: 'Ink at rest, terracotta on hover, arrow nudges 4px.',
    render: () => <TextLink onClick={() => {}}>查看全部方案</TextLink>
  },
  {
    name: 'As anchor',
    description: 'Renders an <a> when href is provided.',
    render: () =>
    <TextLink href="#services" target="_blank">
          View all services
        </TextLink>

  },
  {
    name: 'Without arrow',
    description: 'Underlined inline link only, for in-prose usage.',
    render: () =>
    <TextLink showArrow={false} onClick={() => {}}>
          订单详情
        </TextLink>

  },
  {
    name: 'Disabled',
    description: 'Non-interactive state, no hover treatment.',
    render: () => <TextLink disabled>查看全部方案</TextLink>
  }]

};

export default previews;