import { ErrorState } from './index';
import { Button } from '../Button';
import type { ComponentPreviewModule } from '../previewTypes';

const previews: ComponentPreviewModule = {
  componentName: 'ErrorState',
  importPath: 'components/ErrorState',
  previews: [
  {
    name: 'Inline · 区域加载失败',
    description:
    'Replaces a failed list/table/panel. Use this instead of a toast-only failure.',
    render: () =>
    <div className="w-full bg-canvas p-6">
          <ErrorState
        description="网络请求失败，请检查连接后重试。"
        onRetry={() => {}} />
      
        </div>

  },
  {
    name: 'Inline · 含服务端信息与重试中',
    description:
    'Server detail is shown small and clamped to two lines; the retry button is busy.',
    render: () =>
    <div className="w-full bg-canvas p-6">
          <ErrorState
        title="订单加载失败"
        description="稍后重试，或联系客服协助处理。"
        detail="502 Bad Gateway · /api/orders/merchant?page=1"
        retrying
        onRetry={() => {}}>
        
            <Button variant="outline" size="sm">
              联系客服
            </Button>
          </ErrorState>
        </div>

  },
  {
    name: 'Page · 404',
    description:
    'Route surface. The numeral is decorative (aria-hidden); the h1 carries the meaning.',
    render: () =>
    <div className="w-full bg-canvas">
          <ErrorState
        variant="page"
        code={404}
        title="页面不存在"
        description="链接可能已失效，或地址输入有误。">
        
            <Button variant="primary" size="sm" href="#">
              返回首页
            </Button>
            <Button variant="outline" size="sm">
              返回上一页
            </Button>
          </ErrorState>
        </div>

  },
  {
    name: 'Page · 500 含重试',
    description:
    '500 / 503 need the one action they were missing: reload. Actions wrap at narrow widths.',
    render: () =>
    <div className="w-full bg-canvas">
          <ErrorState
        variant="page"
        code={500}
        title="服务出现异常"
        description="我们已记录该问题，请稍后重试。"
        retryLabel="重新加载"
        onRetry={() => {}}>
        
            <Button variant="outline" size="sm" href="#">
              返回首页
            </Button>
          </ErrorState>
        </div>

  },
  {
    name: 'Page · 无状态码',
    description: 'Without a code the warning mark leads — for non-HTTP failures.',
    render: () =>
    <div className="w-full bg-canvas">
          <ErrorState
        variant="page"
        title="无法连接服务器"
        description="请检查网络后重试。"
        onRetry={() => {}} />
      
        </div>

  }]

};

export default previews;