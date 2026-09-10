import { useState } from 'react';
import { SlidersHorizontalIcon } from 'lucide-react';
import { Chip, ChipRail } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const CATEGORIES: {name: string;count: number;}[] = [
{ name: '上门喂养', count: 12 },
{ name: '宠物寄养', count: 8 },
{ name: '洗护美容', count: 21 },
{ name: '医疗陪护', count: 5 },
{ name: '遛狗散步', count: 17 }];


function CategoryRail() {
  const [selected, setSelected] = useState('');
  const total = CATEGORIES.reduce((sum, c) => sum + c.count, 0);

  return (
    <ChipRail label="服务分类" className="w-full">
      <Chip
        label="全部"
        count={total}
        active={selected === ''}
        onClick={() => setSelected('')} />
      
      {CATEGORIES.map((category) =>
      <Chip
        key={category.name}
        label={category.name}
        count={category.count}
        active={selected === category.name}
        onClick={() => setSelected(category.name)} />

      )}
    </ChipRail>);

}

const previews: ComponentPreviewModule = {
  componentName: 'Chip',
  importPath: 'components/Chip',
  previews: [
  {
    name: 'Default & active',
    description:
    'Hairline chip on surface, and the selected state inverted to solid ink with cream text.',
    render: () =>
    <div className="flex items-center gap-2">
          <Chip label="洗护美容" count={21} />
          <Chip label="宠物寄养" count={8} active />
        </div>

  },
  {
    name: 'Category rail',
    description:
    'The catalogue rail: 8px gaps, horizontally scrollable with no visible scrollbar, single selection.',
    render: () => <CategoryRail />
  },
  {
    name: 'Icon, no count, disabled',
    description:
    'Optional leading icon, a chip without a count, and the disabled state.',
    render: () =>
    <div className="flex items-center gap-2">
          <Chip label="筛选" icon={<SlidersHorizontalIcon />} />
          <Chip label="医疗陪护" />
          <Chip label="暂未开放" count={0} disabled />
        </div>

  }]

};

export default previews;