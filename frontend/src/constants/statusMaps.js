// 订单状态：从下单付款一路走到完成或取消
export const OrderStatus = {
  pending: { label: '待付款', badge: 'badge-warning' },
  paid: { label: '已支付', badge: 'badge-info' },
  confirmed: { label: '待送达', badge: 'badge-info' },
  delivered: { label: '已送达', badge: 'badge-primary' },
  received: { label: '已接收', badge: 'badge-primary' },
  in_progress: { label: '服务中', badge: 'badge-info' },
  completed: { label: '已完成', badge: 'badge-success' },
  cancelled: { label: '已取消', badge: 'badge-danger' },
  refunding: { label: '退款中', badge: 'badge-warning' },
  refunded: { label: '已退款', badge: 'badge-success' },
}

// 退款单走到哪一步了
export const RefundStatus = {
  pending: { label: '待审核', badge: 'badge-info' },
  approved: { label: '已通过(待退款)', badge: 'badge-primary' },
  rejected: { label: '已拒绝', badge: 'badge-danger' },
  completed: { label: '已完成', badge: 'badge-success' },
}

// 商家入驻的审核结果；下面两组分别是开关店模式和当前营业状态
export const MerchantStatus = {
  0: { label: '待审核', badge: 'badge-warning' },
  1: { label: '已通过', badge: 'badge-success' },
  2: { label: '已拒绝', badge: 'badge-danger' },
}

export const MerchantStoreMode = {
  0: { label: '自动', badge: 'badge-info' },
  1: { label: '开店', badge: 'badge-success' },
  2: { label: '关店', badge: 'badge-secondary' },
}

export const MerchantStoreStatus = {
  0: { label: '休息中', badge: 'badge-secondary' },
  1: { label: '营业中', badge: 'badge-success' },
}

// 看护者提交资料后的审核结果；下面一组是他此刻能不能接单
export const KeeperReviewStatus = {
  0: { label: '待审核', badge: 'badge-warning' },
  1: { label: '已通过', badge: 'badge-success' },
  2: { label: '已拒绝', badge: 'badge-danger' },
}

export const KeeperOnlineStatus = {
  1: { label: '在线', badge: 'badge-success' },
  3: { label: '离线', badge: 'badge-secondary' },
  4: { label: '忙碌', badge: 'badge-warning' },
}

// 用户投诉的处理进度；后面三组是工单的进度、分类和紧急程度
export const ComplaintStatus = {
  pending: { label: '待处理', badge: 'badge-warning' },
  processing: { label: '受理中', badge: 'badge-info' },
  resolved: { label: '已处理', badge: 'badge-success' },
  rejected: { label: '已驳回', badge: 'badge-danger' },
}

export const TicketStatus = {
  pending: { label: '待处理', badge: 'badge-warning' },
  processing: { label: '处理中', badge: 'badge-info' },
  resolved: { label: '已解决', badge: 'badge-success' },
  closed: { label: '已关闭', badge: 'badge-secondary' },
}

export const TicketCategory = {
  complaint: { label: '投诉', badge: 'badge-danger' },
  question: { label: '咨询', badge: 'badge-info' },
  suggestion: { label: '建议', badge: 'badge-warning' },
  other: { label: '其他', badge: 'badge-secondary' },
}

export const TicketPriority = {
  low: { label: '低', badge: 'badge-secondary' },
  medium: { label: '中', badge: 'badge-info' },
  high: { label: '高', badge: 'badge-warning' },
  urgent: { label: '紧急', badge: 'badge-danger' },
}

// 用户写的评价要不要过审；下面一组是追评的审核结果
export const ReviewStatus = {
  pending: { label: '待审核', badge: 'badge-warning' },
  approved: { label: '已通过', badge: 'badge-success' },
  rejected: { label: '已拒绝', badge: 'badge-danger' },
}

export const ReviewSubStatus = {
  pending: { label: '待审核', badge: 'badge-warning' },
  approved: { label: '已通过', badge: 'badge-success' },
  rejected: { label: '已驳回', badge: 'badge-error' },
}

// 轮播图挂不挂；下面两组是实名认证进度和账号有没有被封
export const BannerStatus = {
  0: { label: '下架', badge: 'badge-secondary' },
  1: { label: '上架', badge: 'badge-success' },
}

export const RealNameStatus = {
  0: { label: '未实名', badge: 'badge-secondary' },
  1: { label: '审核中', badge: 'badge-warning' },
  2: { label: '已通过', badge: 'badge-success' },
  3: { label: '未通过', badge: 'badge-danger' },
}

export const UserStatus = {
  0: { label: '封禁', badge: 'badge-danger' },
  1: { label: '正常', badge: 'badge-success' },
}

// 一笔交易的结果；下面两组是提现进度和支付结果
export const TransactionStatus = {
  pending: { label: '待处理', badge: 'badge-warning' },
  success: { label: '成功', badge: 'badge-success' },
  completed: { label: '已完成', badge: 'badge-success' },
  failed: { label: '失败', badge: 'badge-danger' },
}

export const WithdrawalStatus = {
  pending: { label: '待审核', badge: 'badge-warning' },
  approved: { label: '已通过', badge: 'badge-success' },
  rejected: { label: '已拒绝', badge: 'badge-danger' },
  completed: { label: '已完成', badge: 'badge-success' },
}

// 服务项目有没有在售
export const ServiceStatus = {
  0: { label: '已下架', badge: 'badge-secondary' },
  1: { label: '上架中', badge: 'badge-success' },
}

export const PaymentStatus = {
  pending: { label: '待支付', badge: 'badge-warning' },
  success: { label: '支付成功', badge: 'badge-success' },
  failed: { label: '支付失败', badge: 'badge-danger' },
}

// 消息是哪一类，前端按这个选标签颜色；下面一组是收藏对象的种类
export const NotificationTypeMap = {
  system: { label: '系统', badge: 'badge-info' },
  order: { label: '订单', badge: 'badge-primary' },
  notice: { label: '公告', badge: 'badge-warning' },
  complaint: { label: '投诉', badge: 'badge-danger' },
  ticket: { label: '工单', badge: 'badge-info' },
  order_fulfillment: { label: '服务进度', badge: 'badge-primary' },
  order_feedback: { label: '订单评价', badge: 'badge-success' },
}

export const FavoriteTargetTypeMap = {
  merchant: { label: '商家', badge: 'badge-info' },
  keeper: { label: '寄养员', badge: 'badge-primary' },
  service: { label: '服务', badge: 'badge-success' },
}

// 请假单审批到哪一步；下面两组是消息读没读、后台工单分类
export const LeaveApprovalStatus = {
  pending: { label: '待审批', badge: 'badge-warning' },
  approved: { label: '已通过', badge: 'badge-success' },
  rejected: { label: '已拒绝', badge: 'badge-danger' },
}

export const ReadStatus = {
  0: { label: '未读', badge: 'badge-warning' },
  1: { label: '已读', badge: 'badge-success' },
}

export const TicketCategoryMap = {
  appeal: { label: '申诉', badge: 'badge-warning' },
  complaint: { label: '投诉', badge: 'badge-error' },
  other: { label: '其他', badge: 'badge-info' },
}

// 查中文文案；查不到就把原始值原样吐出来，实在没有就显示 -
export function getStatusLabel(map, key) {
  return map[key]?.label ?? (key ?? '-')
}

export function getStatusBadge(map, key) {
  return map[key]?.badge ?? 'badge-info'
}

// 文案和颜色一次拿齐，省得调用方写两遍
export function makeStatusBadge(map, key) {
  return {
    label: getStatusLabel(map, key),
    badge: getStatusBadge(map, key),
  }
}

// 直接在列表项上挂一个 label 对象，模板里就不用反复算
export function enrichWithStatus(item, statusField, statusMap, labelField = 'status_label_wsh') {
  if (!item) return item
  item[labelField] = makeStatusBadge(statusMap, item[statusField])
  return item
}
