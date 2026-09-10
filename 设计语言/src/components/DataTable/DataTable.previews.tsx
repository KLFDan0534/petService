import { DataTable } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const columns = [
{ key: 'id_wsh', label: '订单号', width: '140px' },
{ key: 'user_wsh', label: '用户' },
{ key: 'service_wsh', label: '服务' },
{ key: 'amount_wsh', label: '金额', align: 'right' as const },
{ key: 'status_label_wsh', label: '状态' }];


const rows = [
{
  id_wsh: 'PS-20260812-0041',
  user_wsh: '陈晓雯',
  service_wsh: '上门喂猫 · 30 分钟',
  amount_wsh: 128,
  status_label_wsh: { badge: 'badge-success' as const, label: '已完成' }
},
{
  id_wsh: 'PS-20260812-0042',
  user_wsh: '李工',
  service_wsh: '宠物寄养 · 3 晚',
  amount_wsh: 960,
  status_label_wsh: { badge: 'badge-warning' as const, label: '待确认' }
},
{
  id_wsh: 'PS-20260811-0038',
  user_wsh: '王一个非常长的用户名字用来测试截断行为',
  service_wsh: '洗澡美容',
  amount_wsh: 218,
  status_label_wsh: { badge: 'badge-danger' as const, label: '已退款' }
},
{
  id_wsh: 'PS-20260811-0037',
  user_wsh: '周牧',
  service_wsh: '',
  amount_wsh: 0,
  status_label_wsh: { badge: 'badge-disabled' as const, label: '已关闭' }
}];


const actionButton = (label: string) =>
<button
  type="button"
  className="rounded-control border border-line bg-surface px-3 py-1.5 text-control text-ink-soft transition-colors duration-fast ease-editorial hover:border-brand hover:text-brand">
  
    {label}
  </button>;


const previews: ComponentPreviewModule = {
  componentName: 'DataTable',
  importPath: 'components/DataTable',
  previews: [
  {
    name: 'Default',
    description: 'Admin order table with badge cells, tabular amounts and an action column.',
    render: () =>
    <DataTable
      columns={columns}
      data={rows}
      caption="订单列表"
      actions={() =>
      <>
              {actionButton('详情')}
              {actionButton('退款')}
            </>
      } />


  },
  {
    name: 'Loading',
    description: 'Shimmer rows keep the table structure while data is fetched.',
    render: () => <DataTable columns={columns} data={[]} loading loadingRows={4} />
  },
  {
    name: 'Empty',
    description: 'Quiet inline empty message — the ported 暂无数据 state.',
    render: () => <DataTable columns={columns} data={[]} />
  },
  {
    name: 'Error with retry',
    description: 'Failed request replaces the body with a retry affordance.',
    render: () =>
    <DataTable
      columns={columns}
      data={[]}
      error="订单数据加载失败，请检查网络后重试。"
      onRetry={() => undefined} />


  },
  {
    name: 'Bare string columns',
    description: 'Columns can still be plain row keys, as in the original Vue usage.',
    render: () =>
    <DataTable
      columns={['name_wsh', 'type_wsh', 'breed_wsh']}
      data={[
      { name_wsh: '豆豆', type_wsh: '猫', breed_wsh: '英短' },
      { name_wsh: '阿黄', type_wsh: '狗', breed_wsh: '柯基' }]
      } />


  }]

};

export default previews;