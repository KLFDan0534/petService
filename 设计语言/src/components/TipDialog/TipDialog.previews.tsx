import React, { useState } from 'react';
import { TipDialog } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const order = { id_wsh: 'WSH-20260812-0031', serviceName: '上门喂猫 · 60 分钟' };

function Playground({ submitting = false }: {submitting?: boolean;}) {
  const [open, setOpen] = useState(true);

  return (
    <div className="flex min-h-[280px] w-full items-center justify-center">
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="rounded-control border border-line bg-surface px-4 py-2 text-control text-ink-soft transition-colors duration-fast ease-editorial hover:bg-sand">
        
        打开打赏弹窗
      </button>
      <TipDialog
        open={open}
        order={order}
        submitting={submitting}
        onClose={() => setOpen(false)}
        onTip={(id, amount, message) => {
          // eslint-disable-next-line no-console
          console.log('tipped', id, amount, message);
          setOpen(false);
        }} />
      
    </div>);

}

const previews: ComponentPreviewModule = {
  componentName: 'TipDialog',
  importPath: 'components/TipDialog',
  previews: [
  {
    name: 'Open',
    description: 'Amount + message fields, confirm disabled until an amount is entered.',
    render: () => <Playground />
  },
  {
    name: 'Submitting',
    description: 'Confirm action pending while the tip is posted.',
    render: () => <Playground submitting />
  },
  {
    name: 'Closed',
    description: 'Renders nothing when closed — the trigger remains available.',
    render: () =>
    <div className="flex min-h-[160px] w-full items-center justify-center">
          <TipDialog open={false} order={order} onClose={() => {}} onTip={() => {}} />
          <p className="text-meta text-muted">弹窗已关闭</p>
        </div>

  }]

};

export default previews;