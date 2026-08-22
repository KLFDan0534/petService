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

export const RefundStatus = {
  pending: { label: '待审核', badge: 'badge-info' },
  approved: { label: '已通过(待退款)', badge: 'badge-primary' },
  rejected: { label: '已拒绝', badge: 'badge-danger' },
  completed: { label: '已完成', badge: 'badge-success' },
}

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

export const ServiceStatus = {
  0: { label: '已下架', badge: 'badge-secondary' },
  1: { label: '上架中', badge: 'badge-success' },
}

export const TicketCategoryMap = {
  appeal: { label: '申诉', badge: 'badge-warning' },
  complaint: { label: '投诉', badge: 'badge-error' },
  other: { label: '其他', badge: 'badge-info' },
}

export function getStatusLabel(map, key) {
  return map[key]?.label ?? (key ?? '-')
}

export function getStatusBadge(map, key) {
  return map[key]?.badge ?? 'badge-info'
}

export function makeStatusBadge(map, key) {
  return {
    label: getStatusLabel(map, key),
    badge: getStatusBadge(map, key),
  }
}

export function enrichWithStatus(item, statusField, statusMap, labelField = 'status_label_wsh') {
  if (!item) return item
  item[labelField] = makeStatusBadge(statusMap, item[statusField])
  return item
}
