import { PageHeader } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const heroImage =
'https://images.unsplash.com/photo-1520087619250-584c0cbd35e8?auto=format&fit=crop&w=1200&q=80';

const previews: ComponentPreviewModule = {
  componentName: 'PageHeader',
  importPath: 'components/PageHeader',
  previews: [
  {
    name: 'Hero with media',
    description:
    'Eyebrow, display title, subtitle, stat row and framed media — the confirmed Services hero.',
    render: () =>
    <PageHeader
      breadcrumb={
      <>
              <span className="text-muted">首页</span>
              <span className="text-line" aria-hidden="true">
                ›
              </span>
              <span className="text-ink-soft">全部服务</span>
            </>
      }
      eyebrow="Services"
      title={
      <>
              为你的爱宠，
              <br className="hidden min-[900px]:block" />
              找到合适的照护服务
            </>
      }
      subtitle="从日常洗护、遛宠陪伴到寄养与行为训练，按分类浏览门店在售方案，价格与档期实时同步，选定后可直接进入预约。"
      stats={[
      { value: 48, label: '可预约方案' },
      { value: 6, label: '服务分类' },
      { value: 23, label: '覆盖门店' }]
      }
      statsLabel="服务概览"
      media={<img src={heroImage} alt="照护服务" />} />


  },
  {
    name: 'Copy only with actions',
    description:
    'Single-column record header — no media column, with an action cluster.',
    render: () =>
    <PageHeader
      eyebrow="Orders"
      title="订单管理"
      subtitle="查看全部预约订单的状态、金额与门店归属，支持导出与批量处理。"
      actions={
      <>
              <button
          type="button"
          className="rounded-control bg-brand px-4 py-2 text-control text-white transition-colors duration-fast ease-editorial hover:bg-brand-deep">
          
                新建订单
              </button>
              <button
          type="button"
          className="rounded-control border border-line px-4 py-2 text-control text-ink-soft transition-colors duration-fast ease-editorial hover:border-ink-soft hover:text-ink">
          
                导出
              </button>
            </>
      } />


  },
  {
    name: 'Loading',
    description: 'Stats fall back to `--` and the media frame shimmers.',
    render: () =>
    <PageHeader
      eyebrow="Services"
      title="为你的爱宠，找到合适的照护服务"
      subtitle="从日常洗护、遛宠陪伴到寄养与行为训练，按分类浏览门店在售方案。"
      stats={[
      { value: 48, label: '可预约方案' },
      { value: 6, label: '服务分类' }]
      }
      media={<img src={heroImage} alt="照护服务" />}
      loading />


  }]

};

export default previews;