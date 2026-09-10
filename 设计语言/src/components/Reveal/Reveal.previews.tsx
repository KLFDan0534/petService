import { Reveal } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const previews: ComponentPreviewModule = {
  componentName: 'Reveal',
  importPath: 'components/Reveal',
  previews: [
  {
    name: 'Default',
    description: '18px rise with a light blur, played once on entering the viewport.',
    render: () =>
    <div className="w-full max-w-prose">
          <Reveal>
            <p className="font-display text-display-sm text-ink">每一次托付，都被认真照护。</p>
            <p className="mt-3 text-lede text-ink-soft">
              Reveal 包裹任意内容，进入视口时以 18px 上浮 + 轻微模糊淡入。
            </p>
          </Reveal>
        </div>

  },
  {
    name: 'Staggered group',
    description: 'Delay stepped by index * 0.07s to reveal a list in sequence.',
    render: () =>
    <ul className="w-full max-w-prose divide-y divide-line border-y border-line">
          {['寄养服务', '上门喂养', '专业洗护', '行为训练'].map((item, index) =>
      <Reveal key={item} tag="li" delay={index * 0.07}>
              <div className="flex items-baseline justify-between py-3">
                <span className="text-ink">{item}</span>
                <span className="text-meta text-muted" data-numeric="true">
                  0{index + 1}
                </span>
              </div>
            </Reveal>
      )}
        </ul>

  },
  {
    name: 'Inline, no blur, repeating',
    description: 'A span tag with a larger offset, blur disabled and once={false} so it replays.',
    render: () =>
    <div className="w-full max-w-prose">
          <Reveal tag="span" y={30} blur={false} once={false} delay={0.1}>
            <span className="font-display text-display-xs text-brand">重复播放的入场</span>
          </Reveal>
        </div>

  }]

};

export default previews;