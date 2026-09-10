import { useState, type ReactNode } from 'react';
import { Modal, type ModalSize } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const CANCEL_CLASS =
'h-[42px] px-[18px] rounded-control border border-line bg-surface text-[13px] font-medium text-ink-soft transition-colors duration-fast hover:bg-sand';
const PRIMARY_CLASS =
'h-[42px] px-[18px] rounded-control border border-transparent bg-brand text-[13px] font-medium text-white transition-colors duration-fast hover:bg-brand-deep';
const DANGER_CLASS =
'h-[42px] px-[18px] rounded-control border border-critical/30 bg-critical/10 text-[13px] font-medium text-critical transition-colors duration-fast hover:bg-critical/20';
const FIELD_CLASS =
'h-[44px] w-full rounded-control border border-line bg-canvas px-3 text-[13.5px] text-ink outline-none transition-colors duration-fast focus:border-brand';
const LABEL_CLASS = 'text-label uppercase tracking-label text-muted';

interface DemoProps {
  trigger: string;
  title: string;
  description?: string;
  size?: ModalSize;
  footer: (close: () => void) => ReactNode;
  children?: ReactNode;
}

function ModalDemo({ trigger, title, description, size, footer, children }: DemoProps) {
  const [open, setOpen] = useState(false);
  const close = () => setOpen(false);

  return (
    <div className="flex w-full items-center justify-center py-6">
      <button type="button" className={PRIMARY_CLASS} onClick={() => setOpen(true)}>
        {trigger}
      </button>
      <Modal
        open={open}
        onClose={close}
        title={title}
        description={description}
        size={size}
        footer={footer(close)}>
        
        {children}
      </Modal>
    </div>);

}

const previews: ComponentPreviewModule = {
  componentName: 'Modal',
  importPath: 'components/Modal',
  previews: [
  {
    name: 'Confirm',
    description: 'Small destructive confirm — title, supporting copy, two actions.',
    render: () =>
    <ModalDemo
      trigger="删除宠物档案"
      title="删除这份宠物档案？"
      description="删除后该宠物的历史订单仍会保留，但无法再用于新的预约。此操作不可撤销。"
      size="sm"
      footer={(close) =>
      <>
              <button type="button" className={CANCEL_CLASS} onClick={close}>
                取消
              </button>
              <button type="button" className={DANGER_CLASS} onClick={close}>
                确认删除
              </button>
            </>
      } />


  },
  {
    name: 'Form',
    description: 'Wide panel used for the pet / address form dialogs.',
    render: () =>
    <ModalDemo
      trigger="添加宠物"
      title="添加宠物"
      description="填写基础信息，之后可随时补充。"
      size="wide"
      footer={(close) =>
      <>
              <button type="button" className={CANCEL_CLASS} onClick={close}>
                取消
              </button>
              <button type="button" className={PRIMARY_CLASS} onClick={close}>
                保存
              </button>
            </>
      }>
      
          <div className="mt-4 grid gap-4">
            <div className="grid grid-cols-2 gap-4">
              <label className="grid gap-1.5">
                <span className={LABEL_CLASS}>名字</span>
                <input className={FIELD_CLASS} defaultValue="豆豆" />
              </label>
              <label className="grid gap-1.5">
                <span className={LABEL_CLASS}>品种</span>
                <input className={FIELD_CLASS} placeholder="柯基" />
              </label>
            </div>
            <label className="grid gap-1.5">
              <span className={LABEL_CLASS}>备注</span>
              <textarea
            className={`${FIELD_CLASS} h-auto py-2.5 leading-[1.7]`}
            rows={3}
            placeholder="饮食、过敏、用药、性格等注意事项" />
          
            </label>
          </div>
        </ModalDemo>

  },
  {
    name: 'Scrolling detail',
    description: 'Large panel with overflowing content — the panel scrolls, the page does not.',
    render: () =>
    <ModalDemo
      trigger="查看订单详情"
      title="订单 SO-20260829-0142"
      size="lg"
      footer={(close) =>
      <button type="button" className={CANCEL_CLASS} onClick={close}>
              关闭
            </button>
      }>
      
          <dl className="mt-4 divide-y divide-line border-t border-line">
            {Array.from({ length: 12 }).map((_, index) =>
        <div key={index} className="flex items-baseline justify-between gap-4 py-3">
                <dt className="text-meta text-muted">服务日 {index + 1}</dt>
                <dd className="text-meta text-ink" data-numeric="true">
                  ￥{(128 + index * 6).toFixed(2)}
                </dd>
              </div>
        )}
          </dl>
        </ModalDemo>

  }]

};

export default previews;