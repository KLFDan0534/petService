import { FormField, FormInput, FormTextarea } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const previews: ComponentPreviewModule = {
  componentName: 'FormField',
  importPath: 'components/FormField',
  previews: [
  {
    name: 'Default',
    description: 'Label, 44px control and a quiet hint line.',
    render: () =>
    <div className="w-full max-w-prose">
          <FormField
        label="手机号"
        htmlFor="preview-phone"
        required
        hint="用于接收订单与照护提醒">
        
            <FormInput
          id="preview-phone"
          type="tel"
          inputMode="numeric"
          maxLength={11}
          placeholder="11 位手机号" />
        
          </FormField>
        </div>

  },
  {
    name: 'Error',
    description: 'Validation message replaces the hint and paints the border.',
    render: () =>
    <div className="w-full max-w-prose">
          <FormField
        label="设置密码"
        htmlFor="preview-password"
        required
        hint="至少 6 位"
        error="密码长度不足 6 位">
        
            <FormInput
          id="preview-password"
          type="password"
          invalid
          defaultValue="123"
          placeholder="至少 6 位" />
        
          </FormField>
        </div>

  },
  {
    name: 'With trailing action',
    description: 'Captcha row — control flexes, action stays at 44px.',
    render: () =>
    <div className="w-full max-w-prose">
          <FormField
        label="短信验证码"
        htmlFor="preview-captcha"
        required
        hint="验证码已发送，10 分钟内有效"
        trailing={
        <button
          type="button"
          className="h-11 shrink-0 whitespace-nowrap rounded-[10px] border border-line bg-surface px-4 text-[13px] font-medium text-ink-soft transition-colors duration-fast ease-editorial hover:border-[color-mix(in_srgb,var(--ref-ink)_30%,transparent)] hover:text-ink disabled:cursor-not-allowed disabled:opacity-50">
          
                获取验证码
              </button>
        }>
        
            <FormInput
          id="preview-captcha"
          inputMode="numeric"
          maxLength={6}
          placeholder="6 位数字" />
        
          </FormField>
        </div>

  },
  {
    name: 'Textarea, wide and disabled',
    description: 'Two-column form grid with a full-width textarea and a read-only field.',
    render: () =>
    <div className="grid w-full grid-cols-2 gap-5">
          <FormField label="真实姓名" htmlFor="preview-name" required>
            <FormInput id="preview-name" placeholder="与证件一致" />
          </FormField>
          <FormField label="联系手机" htmlFor="preview-contact" muted hint="来自账户资料，不可修改">
            <FormInput id="preview-contact" value="138****6021" disabled readOnly />
          </FormField>
          <FormField
        label="自我介绍"
        htmlFor="preview-bio"
        wide
        hint="介绍你的照护经验，将展示在照护师主页">
        
            <FormTextarea
          id="preview-bio"
          rows={4}
          placeholder="例如：从业 3 年，擅长猫咖与老年犬照护…" />
        
          </FormField>
        </div>

  }]

};

export default previews;