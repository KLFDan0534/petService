import { StatList } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const previews: ComponentPreviewModule = {
  componentName: 'StatList',
  importPath: 'components/StatList',
  previews: [
  {
    name: 'Hero overview',
    description: 'Three stats with hairline dividers, as used in the services hero.',
    render: () =>
    <StatList
      label="服务概览"
      items={[
      { value: 48, label: '可预约方案' },
      { value: 6, label: '服务分类' },
      { value: 21, label: '覆盖门店' }]
      } />


  },
  {
    name: 'Loading',
    description: 'Values collapse to a placeholder while data loads, keeping the row height stable.',
    render: () =>
    <StatList
      loading
      label="服务概览"
      items={[
      { value: 48, label: '可预约方案' },
      { value: 6, label: '服务分类' },
      { value: 21, label: '覆盖门店' }]
      } />


  },
  {
    name: 'Formatted values',
    description: 'Values may be any short node — currency, ratings or units.',
    render: () =>
    <StatList
      label="经营概览"
      items={[
      { value: '¥12,480', label: '本月成交' },
      { value: '4.9', label: '平均评分' },
      { value: '312', label: '完成订单' },
      { value: '98%', label: '按时到店' }]
      } />


  },
  {
    name: 'Single stat',
    description: 'One item renders without a leading divider.',
    render: () => <StatList items={[{ value: 128, label: '在售方案' }]} />
  }]

};

export default previews;