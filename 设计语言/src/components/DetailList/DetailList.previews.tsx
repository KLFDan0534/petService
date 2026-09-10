import { DetailList } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const previews: ComponentPreviewModule = {
  componentName: 'DetailList',
  importPath: 'components/DetailList',
  previews: [
  {
    name: 'Facts',
    description:
    'Four-column fact grid from the order header — micro-labels with a serif amount lead.',
    render: () =>
    <div className="w-full max-w-shell">
          <DetailList
        aria-label="订单概要"
        items={[
        { label: '实付金额', value: '386.00', amount: true, currency: true, numeric: true },
        { label: '宠物', value: '柯基 · 团子' },
        { label: '寄养师', value: '待分配', tone: 'muted' },
        { label: '服务周期', value: '2026-03-04 → 2026-03-09', numeric: true }]
        } />
      
        </div>

  },
  {
    name: 'Facts inside a panel',
    description:
    'The `divided` treatment (`.od-facts-inner`): a hairline separates the facts from the content above inside a surface panel.',
    render: () =>
    <div className="w-full max-w-shell rounded-card border border-line bg-surface px-[22px] py-5">
          <p className="text-[13px] leading-[1.7] text-muted">
            服务进行中，寄养师每日提交照护记录。
          </p>
          <DetailList
        divided
        aria-label="时间信息"
        items={[
        { label: '下单时间', value: '03-01 14:26', numeric: true },
        { label: '计划送达 / 实际', value: '03-04 09:00 / 待送达', numeric: true },
        { label: '计划接回 / 实际', value: '03-09 18:00 / 待接回', numeric: true },
        { label: '计费', value: '5 天', numeric: true }]
        } />
      
        </div>

  },
  {
    name: 'Money',
    description:
    'Amount breakdown with a brand-deep discount row and the serif total below a hairline.',
    render: () =>
    <div className="w-full max-w-prose rounded-card border border-line bg-surface px-[22px] py-5">
          <DetailList
        variant="money"
        aria-label="费用明细"
        items={[
        { label: '服务金额', value: '¥430.00' },
        { label: '优惠减免', value: '−¥44.00', tone: 'discount' },
        { label: '实付金额', value: '386.00', total: true, amount: true, currency: true }]
        } />
      
        </div>

  },
  {
    name: 'Two columns',
    description: 'Narrower contexts — cards and sidebars — use one or two columns.',
    render: () =>
    <div className="w-full max-w-prose">
          <DetailList
        columns={2}
        items={[
        { label: '联系人', value: '林岚' },
        { label: '联系电话', value: '138 0000 1234', numeric: true },
        {
          label: '服务地址',
          value: '上海市徐汇区某某路 888 号某某公寓 12 楼 1203 室'
        },
        { label: '备注', value: '—', tone: 'muted' }]
        } />
      
        </div>

  }]

};

export default previews;