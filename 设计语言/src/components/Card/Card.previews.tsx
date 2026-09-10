import { ArrowRightIcon, CalendarIcon, HeartIcon } from 'lucide-react';
import { Card } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const IMAGE =
'https://images.unsplash.com/photo-1508672019048-805c876b67e2?auto=format&fit=crop&w=800&q=70';

const previews: ComponentPreviewModule = {
  componentName: 'Card',
  importPath: 'components/Card',
  previews: [
  {
    name: 'Service card',
    description:
    'The confirmed catalog pattern: media, brand eyebrow, serif title, price, description and a hairline-divided footer.',
    render: () =>
    <div className="w-full max-w-[360px] p-4">
          <Card
        media={<img src={IMAGE} alt="上门喂养服务图片" />}
        mediaLabel="查看上门喂养详情"
        onMediaClick={() => {}}
        eyebrow="日常照护"
        title="上门喂养 · 每日陪伴"
        meta="暖爪宠物照护 · 2.1km"
        trailing={
        <>
                <strong className="block">¥128</strong>
                <span className="block text-meta font-body font-normal text-muted">/ 次</span>
              </>
        }
        footer={
        <>
                <button
            type="button"
            className="inline-flex min-h-[44px] items-center gap-1.5 text-control font-semibold text-ink transition-colors duration-fast ease-editorial hover:text-brand">
            
                  查看详情
                  <ArrowRightIcon className="h-4 w-4" aria-hidden="true" />
                </button>
                <div className="flex items-center gap-2">
                  <button
              type="button"
              aria-label="收藏服务"
              className="inline-flex h-9 w-9 items-center justify-center rounded-control border border-line text-muted transition-colors duration-fast ease-editorial hover:border-brand hover:text-brand">
              
                    <HeartIcon className="h-4 w-4" aria-hidden="true" />
                  </button>
                  <button
              type="button"
              className="inline-flex h-9 items-center gap-1.5 rounded-control bg-brand px-3 text-control font-medium text-white transition-colors duration-fast ease-editorial hover:bg-brand-deep">
              
                    <CalendarIcon className="h-4 w-4" aria-hidden="true" />
                    立即预约
                  </button>
                </div>
              </>
        }>
        
            <p className="ref-clamp-3">
              专业照护师上门喂食、换水、清理猫砂，并附带一份图文照护日报。
            </p>
          </Card>
        </div>

  },
  {
    name: 'Content only',
    description: 'No media. Header, body and footer slots on the surface hairline frame.',
    render: () =>
    <div className="w-full max-w-[360px] p-4">
          <Card
        eyebrow="订单"
        title="订单 #WSH-20481"
        meta="2026-08-24 · 已完成"
        footer={
        <span className="text-meta text-muted">照护师：林晚</span>
        }>
        
            <p>宠物寄养 3 晚，含每日两次遛狗与图文日报。</p>
          </Card>
        </div>

  },
  {
    name: 'Static container',
    description:
    'interactive={false} removes the hover lift — use for non-clickable panels and summaries.',
    render: () =>
    <div className="w-full max-w-[360px] p-4">
          <Card interactive={false} title="本月概览">
            <dl className="divide-y divide-line">
              <div className="flex items-center justify-between py-2">
                <dt className="text-meta text-muted">完成订单</dt>
                <dd className="font-display text-[18px] text-ink" data-numeric="true">
                  24
                </dd>
              </div>
              <div className="flex items-center justify-between py-2">
                <dt className="text-meta text-muted">好评率</dt>
                <dd className="font-display text-[18px] text-ink" data-numeric="true">
                  98%
                </dd>
              </div>
            </dl>
          </Card>
        </div>

  }]

};

export default previews;