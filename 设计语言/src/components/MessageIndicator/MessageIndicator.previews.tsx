import { MessageIndicator } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const previews: ComponentPreviewModule = {
  componentName: 'MessageIndicator',
  importPath: 'components/MessageIndicator',
  previews: [
  {
    name: 'No unread',
    description: 'Resting state — icon only, no indicator.',
    render: () => <MessageIndicator unreadCount={0} />
  },
  {
    name: 'Unread dot',
    description: 'Default unread state: a small critical dot, as in the source.',
    render: () => <MessageIndicator unreadCount={3} />
  },
  {
    name: 'Unread count',
    description: 'Numeric variant, capped at max (99+).',
    render: () =>
    <div className="flex items-center gap-2">
          <MessageIndicator unreadCount={7} showCount />
          <MessageIndicator unreadCount={128} showCount />
        </div>

  },
  {
    name: 'Disabled',
    description: 'Non-interactive state, e.g. while notifications are loading.',
    render: () => <MessageIndicator unreadCount={5} disabled />
  }]

};

export default previews;