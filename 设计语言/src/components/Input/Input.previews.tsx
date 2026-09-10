import { useState } from 'react';
import { MailIcon, SearchIcon } from 'lucide-react';
import { Input, SearchInput } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

function ControlledSearch() {
  const [query, setQuery] = useState('金毛洗护');
  return (
    <div style={{ width: '100%', maxWidth: 380 }}>
      <SearchInput
        placeholder="搜索服务名称或内容"
        aria-label="搜索服务"
        value={query}
        onChange={(event) => setQuery(event.target.value)} />
      
    </div>);

}

const previews: ComponentPreviewModule = {
  componentName: 'Input',
  importPath: 'components/Input',
  previews: [
  {
    name: 'Default',
    description: '44px field, 11px radius, hairline border on surface.',
    render: () =>
    <div style={{ width: '100%', maxWidth: 380 }}>
          <Input placeholder="请输入宠物昵称" aria-label="宠物昵称" />
        </div>

  },
  {
    name: 'Search with clear',
    description:
    'Leading icon at 14px, trailing clear button — the services toolbar pattern.',
    render: () => <ControlledSearch />
  },
  {
    name: 'Leading icon',
    description: 'Any lucide icon can be used as the leading slot.',
    render: () =>
    <div style={{ width: '100%', maxWidth: 380 }}>
          <Input
        type="email"
        leadingIcon={<MailIcon />}
        placeholder="you@example.com"
        aria-label="邮箱" />
      
        </div>

  },
  {
    name: 'Invalid',
    description: 'Critical border with a matching focus ring.',
    render: () =>
    <div style={{ width: '100%', maxWidth: 380 }}>
          <Input invalid defaultValue="13x-000" aria-label="手机号" />
        </div>

  },
  {
    name: 'Disabled',
    description: 'Sand fill, muted text, no hover or focus treatment.',
    render: () =>
    <div style={{ width: '100%', maxWidth: 380 }}>
          <Input
        disabled
        leadingIcon={<SearchIcon />}
        placeholder="搜索已停用"
        aria-label="搜索已停用" />
      
        </div>

  }]

};

export default previews;