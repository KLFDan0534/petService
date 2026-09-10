import { useState } from 'react';
import { FilterToolbar } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

function WithFilters() {
  const [query, setQuery] = useState('洗护');
  return (
    <FilterToolbar
      value={query}
      onValueChange={setQuery}
      showReset={query.length > 0}
      onReset={() => setQuery('')} />);


}

const previews: ComponentPreviewModule = {
  componentName: 'FilterToolbar',
  importPath: 'components/FilterToolbar',
  previews: [
  {
    name: 'Default',
    description: 'Search field only — flexible 220–380px, 44px tall.',
    render: () =>
    <div className="w-full max-w-shell">
          <FilterToolbar />
        </div>

  },
  {
    name: 'With reset',
    description:
    'Active query shows the inline clear button and the trailing reset control.',
    render: () =>
    <div className="w-full max-w-shell">
          <WithFilters />
        </div>

  },
  {
    name: 'Extra controls',
    description: 'Additional 44px controls wrap in the same 12px-gap row.',
    render: () =>
    <div className="w-full max-w-shell">
          <FilterToolbar
        defaultValue="疫苗"
        showReset
        onReset={() => undefined}>
        
            <select
          aria-label="排序方式"
          className="h-11 rounded-control border border-line bg-surface px-3 text-control text-ink-soft"
          defaultValue="price">
          
              <option value="price">价格从低到高</option>
              <option value="rating">评分优先</option>
            </select>
          </FilterToolbar>
        </div>

  }]

};

export default previews;