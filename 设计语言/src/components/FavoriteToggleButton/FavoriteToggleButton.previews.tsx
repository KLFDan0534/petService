import { FavoriteToggleButton } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

const previews: ComponentPreviewModule = {
  componentName: 'FavoriteToggleButton',
  importPath: 'components/FavoriteToggleButton',
  previews: [
  {
    name: 'Default',
    description: 'Icon + label, unfavourited. Toggles optimistically.',
    render: () =>
    <FavoriteToggleButton
      targetId={101}
      targetType="service"
      onToggle={() => wait(600)} />


  },
  {
    name: 'Favorited',
    description: 'Persisted favourite state — filled star in the caution tone.',
    render: () =>
    <FavoriteToggleButton
      targetId={102}
      targetType="merchant"
      defaultFavorited
      onToggle={() => wait(600)} />


  },
  {
    name: 'Icon only',
    description: '40px circular geometry for card and media overlays.',
    render: () =>
    <FavoriteToggleButton
      targetId={103}
      targetType="keeper"
      showText={false}
      onToggle={() => wait(600)} />


  },
  {
    name: 'Loading state',
    description: 'Initial favourite check in flight — control is disabled.',
    render: () =>
    <FavoriteToggleButton
      targetId={104}
      targetType="service"
      onCheckFavorite={async () => {
        await wait(1000000);
        return false;
      }} />


  },
  {
    name: 'Request failure',
    description: 'Optimistic flip reverts and an error toast is emitted.',
    render: () =>
    <FavoriteToggleButton
      targetId={105}
      targetType="service"
      onToggle={async () => {
        await wait(600);
        throw new Error('network');
      }}
      onNotify={(message, tone) => console.log(tone, message)} />


  },
  {
    name: 'Unsupported target',
    description: 'Invalid target type keeps the control disabled.',
    render: () =>
    <FavoriteToggleButton targetId={0} targetType="unknown" />

  }]

};

export default previews;