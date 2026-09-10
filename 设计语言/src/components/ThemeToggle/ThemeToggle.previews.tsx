import { useState } from 'react';
import { ThemeToggle } from './index';
import type { ThemeMode } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

function ControlledExample() {
  const [theme, setTheme] = useState<ThemeMode>('light');
  return (
    <div className="flex items-center gap-3">
      <ThemeToggle theme={theme} onThemeChange={setTheme} />
      <span className="text-meta text-ink-soft">{theme === 'dark' ? '夜间主题' : '日间主题'}</span>
    </div>);

}

const previews: ComponentPreviewModule = {
  componentName: 'ThemeToggle',
  importPath: 'components/ThemeToggle',
  previews: [
  {
    name: 'Default',
    description: '36px chrome control — toggles the document theme on click.',
    render: () => <ThemeToggle />
  },
  {
    name: 'Header size',
    description: '44px variant used in the user header controls.',
    render: () => <ThemeToggle size="md" />
  },
  {
    name: 'Controlled',
    description: 'Theme owned by the parent via theme / onThemeChange.',
    render: () => <ControlledExample />
  },
  {
    name: 'Disabled',
    description: 'Non-interactive state.',
    render: () => <ThemeToggle disabled />
  }]

};

export default previews;