import { useState } from 'react';
import { PopupNotice, type PopupNoticeItem } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const single: PopupNoticeItem[] = [
{
  id_wsh: 1,
  title_wsh: '春节期间上门服务安排',
  content_wsh:
  '2 月 8 日至 2 月 14 日期间，上门喂养与遛狗服务照常接单，部分区域配送时段有所调整。\n如需寄养，请提前 3 天预约以便安排看护人员。',
  type_wsh: 'notice',
  delivery_type_wsh: 'popup'
}];


const queue: PopupNoticeItem[] = [
...single,
{
  id_wsh: 2,
  title_wsh: '新增 5 位认证宠物看护师',
  content_wsh: '海淀、朝阳两区新增 5 位通过背景审核的看护师，可在服务详情页查看资质与评价。',
  type_wsh: 'notice',
  delivery_type_wsh: 'popup'
},
{
  id_wsh: 3,
  title_wsh: '订单退款规则更新',
  content_wsh: '服务开始前 24 小时取消可全额退款，24 小时内取消将收取 20% 服务费。',
  type_wsh: 'notice',
  delivery_type_wsh: 'popup'
}];


const longNotice: PopupNoticeItem[] = [
{
  id_wsh: 9,
  title_wsh: '平台服务协议与隐私政策修订说明（2026 年 3 月版）',
  content_wsh: Array.from(
    { length: 10 },
    (_, i) =>
    `${i + 1}. 本次修订明确了看护人员在服务过程中对宠物健康状况的记录义务，以及平台在纠纷处理中的介入流程与举证责任划分。`
  ).join('\n'),
  type_wsh: 'notice',
  delivery_type_wsh: 'popup'
}];


function PopupNoticeDemo({ notices }: {notices: PopupNoticeItem[];}) {
  const [open, setOpen] = useState(true);

  return (
    <div className="flex min-h-[320px] w-full items-center justify-center bg-canvas">
      {open ?
      <PopupNotice notices={notices} onDismissAll={() => setOpen(false)} /> :

      <button
        type="button"
        onClick={() => setOpen(true)}
        className="rounded-control border border-line bg-surface px-4 py-2 text-control text-ink-soft transition-colors duration-fast ease-editorial hover:border-brand hover:text-brand">
        
          重新显示公告
        </button>
      }
    </div>);

}

const previews: ComponentPreviewModule = {
  componentName: 'PopupNotice',
  importPath: 'components/PopupNotice',
  previews: [
  {
    name: 'Single notice',
    description: 'One announcement — the primary action reads 我知道了 and no counter is shown.',
    render: () => <PopupNoticeDemo notices={single} />
  },
  {
    name: 'Queue of notices',
    description: 'Multiple popups shown one at a time, with a 1 / n counter and a 下一条 action.',
    render: () => <PopupNoticeDemo notices={queue} />
  },
  {
    name: 'Long content',
    description: 'Body scrolls at 300px so a long announcement never breaks the dialog.',
    render: () => <PopupNoticeDemo notices={longNotice} />
  }]

};

export default previews;