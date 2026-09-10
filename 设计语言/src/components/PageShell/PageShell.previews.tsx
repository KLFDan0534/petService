import { PageShell } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

function Ruler({ label }: {label: string;}) {
  return (
    <div className="rounded-card border border-line bg-surface p-6">
      <div className="text-eyebrow uppercase text-brand">{label}</div>
      <h1 className="mt-3 font-display text-display-sm text-ink">
        为你的爱宠，找到合适的照护服务
      </h1>
      <p className="mt-3 max-w-prose text-lede text-ink-soft">
        每个视图都用同一个外壳：最大宽度居中，两侧留出统一的栏距，页面底部保留 72px 收尾空间。
      </p>
    </div>);

}

const previews: ComponentPreviewModule = {
  componentName: 'PageShell',
  importPath: 'components/PageShell',
  previews: [
  {
    name: 'Default',
    description: '1180px 内容宽度，24px 栏距（560px 以下收窄到 16px），页面上下留白 6px / 72px。',
    render: () =>
    <div className="w-full bg-canvas">
          <PageShell>
            <Ruler label="Services" />
          </PageShell>
        </div>

  },
  {
    name: 'Narrow record shell',
    description: '1080px — 用于订单详情等单条记录页面。',
    render: () =>
    <div className="w-full bg-canvas">
          <PageShell width="narrow">
            <Ruler label="Order" />
          </PageShell>
        </div>

  },
  {
    name: 'Wide data workspace',
    description: '1440px — 管理后台的数据工作台，配合 DataTable 使用。',
    render: () =>
    <div className="w-full bg-canvas">
          <PageShell width="wide">
            <Ruler label="Admin" />
          </PageShell>
        </div>

  },
  {
    name: 'Multiple sections',
    description: '同一外壳内的分节节奏：56px 间距 + 细分割线。',
    render: () =>
    <div className="w-full bg-canvas">
          <PageShell>
            <nav aria-label="面包屑" className="flex items-center gap-2 py-1.5 text-meta text-muted">
              <span>首页</span>
              <span aria-hidden="true">›</span>
              <span className="text-ink-soft">全部服务</span>
            </nav>
            <Ruler label="Services" />
            <section className="mt-section border-t border-line pt-8">
              <h2 className="font-display text-display-xs text-ink">服务分类</h2>
              <ul className="mt-4 divide-y divide-line border-y border-line">
                {['洗护美容', '遛宠陪伴', '寄养托管'].map((item) =>
            <li key={item} className="flex items-center justify-between py-3">
                    <span className="text-ink-soft">{item}</span>
                    <span className="text-meta text-muted" data-numeric="true">
                      12 个方案
                    </span>
                  </li>
            )}
              </ul>
            </section>
          </PageShell>
        </div>

  }]

};

export default previews;