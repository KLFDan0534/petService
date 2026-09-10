import { ReviewDialog } from './index';
import type { ReviewDialogOrder } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const order: ReviewDialogOrder = {
  id_wsh: 20481,
  merchant_id_wsh: 12,
  merchant_name_wsh: '暖阳宠物生活馆',
  keeper_id_wsh: 34,
  keeper_name_wsh: '陈静',
  service_id_wsh: 7,
  service_name_wsh: '上门喂养 · 60 分钟'
};

const partialOrder: ReviewDialogOrder = {
  ...order,
  keeper_id_wsh: null,
  keeper_name_wsh: null
};

const previews: ComponentPreviewModule = {
  componentName: 'ReviewDialog',
  importPath: 'components/ReviewDialog',
  previews: [
  {
    name: 'Default',
    description: 'Open dialog with all three dimensions available for review.',
    render: () =>
    <ReviewDialog
      visible
      order={order}
      onClose={() => {}}
      onReviewed={() => {}} />


  },
  {
    name: 'Dimension already reviewed',
    description:
    'The merchant dimension is done — the tab is flagged and submitting is blocked with an explanatory note.',
    render: () =>
    <ReviewDialog
      visible
      order={order}
      doneTypes={['merchant']}
      onClose={() => {}}
      onReviewed={() => {}} />


  },
  {
    name: 'Missing target',
    description:
    'The order has no keeper, so that dimension cannot be reviewed. Only merchant and service remain actionable.',
    render: () =>
    <ReviewDialog
      visible
      order={partialOrder}
      doneTypes={['merchant', 'service']}
      onClose={() => {}}
      onReviewed={() => {}} />


  },
  {
    name: 'Submitting',
    description:
    'While `onReviewed` is pending the primary action shows 提交中... and stays disabled.',
    render: () =>
    <ReviewDialog
      visible
      order={order}
      onClose={() => {}}
      onReviewed={() => new Promise(() => {})} />


  }]

};

export default previews;