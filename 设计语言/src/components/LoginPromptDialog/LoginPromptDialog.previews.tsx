import React, { useState } from 'react';
import { LoginPromptDialog } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

function OpenByDefault() {
  const [visible, setVisible] = useState(true);
  return (
    <div className="relative flex min-h-[320px] w-full items-center justify-center">
      {!visible &&
      <button
        type="button"
        onClick={() => setVisible(true)}
        className="rounded-control bg-brand px-6 py-3 text-control font-semibold text-white">
        
          打开登录提示
        </button>
      }
      <LoginPromptDialog
        visible={visible}
        onClose={() => setVisible(false)}
        loginRedirectPath="/orders/1024"
        onLogin={(path) => window.alert('navigate → ' + path)} />
      
    </div>);

}

function Triggered() {
  const [visible, setVisible] = useState(false);
  return (
    <div className="flex min-h-[160px] w-full items-center justify-center">
      <button
        type="button"
        onClick={() => setVisible(true)}
        className="rounded-control border border-line bg-surface px-6 py-3 text-control font-semibold text-ink transition-colors duration-fast ease-editorial hover:border-brand hover:text-brand">
        
        收藏该服务
      </button>
      <LoginPromptDialog
        visible={visible}
        onClose={() => setVisible(false)}
        loginRedirectPath="/services/pet-grooming" />
      
    </div>);

}

const previews: ComponentPreviewModule = {
  componentName: 'LoginPromptDialog',
  importPath: 'components/LoginPromptDialog',
  previews: [
  {
    name: 'Default',
    description: 'Auth gate shown when a guest triggers a members-only action.',
    render: () => <OpenByDefault />
  },
  {
    name: 'Triggered by action',
    description: 'Closed until a gated control is used; scrim click or Escape dismisses it.',
    render: () => <Triggered />
  },
  {
    name: 'Custom copy',
    description: 'Message and action labels overridden for a checkout gate.',
    render: () =>
    <div className="relative flex min-h-[320px] w-full items-center justify-center">
          <LoginPromptDialog
        visible
        onClose={() => {}}
        message="登录后即可继续下单并管理你的宠物服务预约"
        cancelLabel="继续浏览"
        confirmLabel="登录 / 注册"
        loginRedirectPath="/checkout" />
      
        </div>

  }]

};

export default previews;