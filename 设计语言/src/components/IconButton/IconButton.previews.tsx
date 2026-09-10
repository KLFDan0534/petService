import { XIcon, SearchIcon, MoreHorizontalIcon, Trash2Icon, StarIcon } from 'lucide-react';
import { IconButton } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const previews: ComponentPreviewModule = {
  componentName: 'IconButton',
  importPath: 'components/IconButton',
  previews: [
  {
    name: 'Default',
    description: '28px square, transparent at rest, sand wash on hover.',
    render: () =>
    <IconButton label="清除搜索">
          <XIcon />
        </IconButton>

  },
  {
    name: 'Sizes',
    description: '24 / 28 / 32px squares.',
    render: () =>
    <div className="flex items-center gap-2">
          <IconButton label="Clear" size="sm">
            <XIcon />
          </IconButton>
          <IconButton label="Search" size="md">
            <SearchIcon />
          </IconButton>
          <IconButton label="More actions" size="lg">
            <MoreHorizontalIcon />
          </IconButton>
        </div>

  },
  {
    name: 'Tones and states',
    description: 'Brand and critical hover tones, plus active and disabled.',
    render: () =>
    <div className="flex items-center gap-2">
          <IconButton label="Favourite" tone="brand">
            <StarIcon />
          </IconButton>
          <IconButton label="Delete" tone="critical">
            <Trash2Icon />
          </IconButton>
          <IconButton label="Favourite" tone="brand" active>
            <StarIcon />
          </IconButton>
          <IconButton label="Delete" tone="critical" disabled>
            <Trash2Icon />
          </IconButton>
        </div>

  },
  {
    name: 'In a search field',
    description: 'The source affordance: a clear button docked inside the 44px search input.',
    render: () =>
    <div className="relative w-full max-w-prose">
          <SearchIcon
        className="pointer-events-none absolute left-3 top-1/2 h-[15px] w-[15px] -translate-y-1/2 text-muted"
        aria-hidden="true" />
      
          <input
        defaultValue="上门喂猫"
        aria-label="搜索服务"
        className="h-11 w-full rounded-control border border-line bg-surface pl-10 pr-10 text-control text-ink placeholder:text-muted transition-colors duration-fast ease-editorial hover:border-ink/25 focus:border-ink/35 focus:outline-none" />
      
          <span className="absolute right-2 top-1/2 -translate-y-1/2">
            <IconButton label="清除搜索">
              <XIcon />
            </IconButton>
          </span>
        </div>

  }]

};

export default previews;