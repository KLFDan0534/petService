import { createRouter, createWebHistory } from 'vue-router'
import { ROLES } from '@/utils/permission'
import { useAppStore } from '@/stores/app'
import { isTokenExpired } from '@/utils/jwt'

// ═══════════════ 角色分区常量（供 meta.roles 权限校验使用）═══════════════
// 用户区角色：宠物主 / 照护师 / 商家 / 管理员
const USER_AREA_ROLES = [ROLES.OWNER, ROLES.KEEPER, ROLES.MERCHANT, ROLES.ADMIN]
// 账户区角色：用户区角色 + 客服（个人中心等账户设置页使用）
const ACCOUNT_AREA_ROLES = [...USER_AREA_ROLES, ROLES.CUSTOMER_SERVICE]
// 支持区角色：商家 / 管理员 / 客服（工单、客诉等工作台使用）
const SUPPORT_AREA_ROLES = [ROLES.MERCHANT, ROLES.ADMIN, ROLES.CUSTOMER_SERVICE]

// ═══════════════ 静态公共路由（constantRoutes）═══════════════
// 说明：以下路由始终注册；是否需要登录由 meta.requiresAuth 声明，
//      拦截与跳转逻辑统一交给文件底部的路由守卫处理。
export const constantRoutes = [
  // 根路径 → 重定向到仪表盘
  { path: '/', redirect: '/dashboard' },

  // ── 公开浏览页（无需登录）──
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/user/Dashboard.vue'),
    meta: { layout: 'user', requiresAuth: false },
  },
  {
    path: '/services',
    name: 'Services',
    component: () => import('@/views/user/Services.vue'),
    meta: { layout: 'user', requiresAuth: false },
  },

  // ── 登录 / 注册 / 找回密码 ──
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/user/Login.vue'),
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/user/Register.vue'),
  },
  {
    path: '/forget-password',
    name: 'ForgetPassword',
    component: () => import('@/views/user/ForgetPassword.vue'),
  },

  // ── 错误页 ──
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/Forbidden.vue'),
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/error/NotFound.vue'),
  },
  {
    path: '/500',
    name: 'ServerError',
    component: () => import('@/views/error/ServerError.vue'),
  },
  {
    path: '/503',
    name: 'ServiceUnavailable',
    component: () => import('@/views/error/ServiceUnavailable.vue'),
  },

  // ── 宠物档案 ──
  {
    path: '/pets',
    name: 'Pets',
    component: () => import('@/views/user/Pets.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES, keepAlive: true },
  },
  {
    path: '/pets/:id',
    name: 'PetDetail',
    component: () => import('@/views/user/PetDetail.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES },
  },

  // ── 商家与照护师浏览 ──
  {
    path: '/merchants',
    name: 'Merchants',
    component: () => import('@/views/user/Merchants.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES, keepAlive: true },
  },
  {
    path: '/merchants/:id',
    name: 'MerchantDetail',
    component: () => import('@/views/user/MerchantDetail.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES },
  },
  {
    path: '/keepers',
    name: 'Keepers',
    component: () => import('@/views/user/Keepers.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES },
  },
  {
    path: '/keepers/:id',
    name: 'KeeperDetail',
    component: () => import('@/views/user/KeeperDetail.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES },
  },
  {
    path: '/keeper/profile',
    name: 'KeeperProfile',
    component: () => import('@/views/user/KeeperProfile.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: [ROLES.KEEPER, ROLES.MERCHANT, ROLES.ADMIN] },
  },
  {
    path: '/services/:id',
    name: 'ServiceDetail',
    component: () => import('@/views/user/ServiceDetail.vue'),
    meta: { layout: 'user', requiresAuth: false },
  },

  // ── 订单 ──
  {
    path: '/orders',
    name: 'Orders',
    component: () => import('@/views/user/Orders.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES, keepAlive: true },
  },
  {
    path: '/orders/:id',
    name: 'OrderDetail',
    component: () => import('@/views/order/OrderDetailView.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES },
  },

  // ── 优惠券 / 会员 ──
  {
    path: '/coupons',
    name: 'Coupons',
    component: () => import('@/views/user/Coupons.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES },
  },
  {
    path: '/membership',
    name: 'Membership',
    component: () => import('@/views/user/Membership.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES },
  },

  // ── 入驻与申请 ──
  {
    path: '/keeper-apply',
    name: 'KeeperApply',
    component: () => import('@/views/user/KeeperApply.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES },
  },
  {
    path: '/customer-service/apply',
    name: 'CustomerServiceApply',
    component: () => import('@/views/user/CustomerServiceApply.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES },
  },
  {
    path: '/keeper-workflow',
    name: 'KeeperWorkflow',
    component: () => import('@/views/user/KeeperWorkflow.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: [ROLES.KEEPER, ROLES.ADMIN] },
  },
  // ── 支付与钱包 ──
  {
    path: '/payments',
    name: 'Payments',
    component: () => import('@/views/user/Payments.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES },
  },
  {
    path: '/recharge',
    name: 'Recharge',
    component: () => import('@/views/user/Recharge.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES },
  },
  {
    path: '/wallet',
    name: 'Wallet',
    component: () => import('@/views/user/Wallet.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES },
  },

  // ── 个人中心（账户信息与安全设置）──
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/user/Profile.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: ACCOUNT_AREA_ROLES },
  },
  {
    path: '/profile/phone',
    name: 'ProfilePhoneEdit',
    component: () => import('@/views/user/ProfileAccountEdit.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: ACCOUNT_AREA_ROLES, accountType: 'phone' },
  },
  {
    path: '/profile/email',
    name: 'ProfileEmailEdit',
    component: () => import('@/views/user/ProfileAccountEdit.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: ACCOUNT_AREA_ROLES, accountType: 'email' },
  },
  {
    path: '/profile/real-name',
    name: 'ProfileRealNameEdit',
    component: () => import('@/views/user/ProfileAccountEdit.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: ACCOUNT_AREA_ROLES, accountType: 'realName' },
  },
  {
    path: '/profile/payment-password',
    name: 'ProfilePaymentPasswordEdit',
    component: () => import('@/views/user/ProfileAccountEdit.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: ACCOUNT_AREA_ROLES, accountType: 'paymentPassword' },
  },
  // ── 文件 / 售后 / 互动 ──
  {
    path: '/files',
    name: 'Files',
    component: () => import('@/views/user/Files.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES },
  },
  {
    path: '/refunds',
    name: 'Refunds',
    component: () => import('@/views/user/Refunds.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES },
  },
  {
    path: '/addresses',
    name: 'Addresses',
    component: () => import('@/views/user/Addresses.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES },
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('@/views/user/Chat.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES },
  },

  // ── 消息通知与工单 ──
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('@/views/user/Notifications.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: ACCOUNT_AREA_ROLES },
  },
  {
    path: '/notices/:id',
    name: 'NoticeDetail',
    component: () => import('@/views/user/NoticeDetail.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: ACCOUNT_AREA_ROLES },
  },
  {
    path: '/favorites',
    name: 'Favorites',
    component: () => import('@/views/user/Favorites.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES },
  },
  {
    path: '/tickets',
    name: 'Tickets',
    component: () => import('@/views/user/Tickets.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: ACCOUNT_AREA_ROLES },
  },
  {
    path: '/tickets/:id',
    name: 'TicketDetail',
    component: () => import('@/views/user/TicketDetail.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: ACCOUNT_AREA_ROLES },
  },
  {
    path: '/complaints',
    name: 'Complaints',
    component: () => import('@/views/user/Complaints.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES },
  },
  {
    path: '/ratings',
    name: 'Ratings',
    component: () => import('@/views/user/Ratings.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES },
  },

  // ── 信用与营收 ──
  {
    path: '/credit-reputation',
    name: 'CreditReputation',
    component: () => import('@/views/user/CreditReputation.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES },
  },
  {
    path: '/anomaly',
    name: 'Anomaly',
    component: () => import('@/views/user/Anomaly.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES },
  },
  {
    path: '/revenue',
    name: 'RevenueCenter',
    component: () => import('@/views/user/RevenueCenter.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES },
  },

  // ── AI 能力 ──
  {
    path: '/ai',
    name: 'AI',
    component: () => import('@/views/user/AI.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES },
  },
  {
    path: '/ai/chat',
    name: 'AiChat',
    component: () => import('@/views/user/AiChat.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: ACCOUNT_AREA_ROLES },
  },
  {
    path: '/rag',
    name: 'RAG',
    component: () => import('@/views/user/RAG.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES },
  },
  {
    path: '/agent',
    name: 'Agent',
    component: () => import('@/views/user/Agent.vue'),
    meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES },
  },
]

// ═══════════════ 动态路由分组（登录成功后按角色注入）═══════════════

// 用户区路由：已全部并入上方 constantRoutes，保留导出仅为兼容旧引用
export const userRoutes = []

// ── 商家端路由（商家 / 管理员可见）──
export const merchantRoutes = [
  // 进入商家端时重定向到商家仪表盘
  { path: '/merchant', redirect: '/merchant/dashboard' },
  {
    path: '/merchant/dashboard',
    name: 'MerchantDashboard',
    component: () => import('@/views/merchant/MerchantDashboard.vue'),
    meta: { layout: 'merchant', requiresAuth: true, roles: [ROLES.MERCHANT, ROLES.ADMIN] },
  },
  {
    path: '/merchant/profile',
    name: 'MerchantProfile',
    component: () => import('@/views/merchant/MerchantProfile.vue'),
    meta: { layout: 'merchant', requiresAuth: true, roles: [ROLES.MERCHANT, ROLES.ADMIN] },
  },
  {
    path: '/merchant/services',
    name: 'MerchantServices',
    component: () => import('@/views/merchant/MerchantServices.vue'),
    meta: { layout: 'merchant', requiresAuth: true, roles: [ROLES.MERCHANT, ROLES.ADMIN] },
  },
  {
    path: '/merchant/business-hours',
    name: 'MerchantBusinessHours',
    component: () => import('@/views/merchant/MerchantBusinessHours.vue'),
    meta: { layout: 'merchant', requiresAuth: true, roles: [ROLES.MERCHANT, ROLES.ADMIN] },
  },
  {
    path: '/merchant/pets',
    name: 'MerchantPets',
    component: () => import('@/views/merchant/MerchantPets.vue'),
    meta: { layout: 'merchant', requiresAuth: true, roles: [ROLES.MERCHANT, ROLES.ADMIN] },
  },
  {
    path: '/merchant/orders',
    name: 'MerchantOrders',
    component: () => import('@/views/merchant/MerchantOrders.vue'),
    meta: { layout: 'merchant', requiresAuth: true, roles: [ROLES.MERCHANT, ROLES.ADMIN] },
  },
  {
    path: '/merchant/keepers',
    name: 'MerchantKeepers',
    component: () => import('@/views/merchant/MerchantKeepers.vue'),
    meta: { layout: 'merchant', requiresAuth: true, roles: [ROLES.MERCHANT, ROLES.ADMIN] },
  },
  {
    path: '/merchant/customer-service',
    name: 'MerchantCustomerService',
    component: () => import('@/views/merchant/MerchantCustomerService.vue'),
    meta: { layout: 'merchant', requiresAuth: true, roles: [ROLES.MERCHANT, ROLES.ADMIN] },
  },
  {
    path: '/merchant/statistics',
    name: 'MerchantStatistics',
    component: () => import('@/views/merchant/MerchantStatistics.vue'),
    meta: { layout: 'merchant', requiresAuth: true, roles: [ROLES.MERCHANT, ROLES.ADMIN] },
  },
]

// ── 商家客服支持路由（商家 / 管理员 / 客服可见，复用工单与客诉页面）──
export const merchantSupportRoutes = [
  {
    path: '/merchant/support/dashboard',
    name: 'MerchantSupportDashboard',
    component: () => import('@/views/cs/CsWorkbench.vue'),
    meta: { layout: 'merchant', requiresAuth: true, roles: SUPPORT_AREA_ROLES },
  },
  {
    path: '/merchant/support/tickets',
    name: 'MerchantSupportTickets',
    component: () => import('@/views/admin/AdminTickets.vue'),
    meta: { layout: 'merchant', requiresAuth: true, roles: SUPPORT_AREA_ROLES },
  },
  {
    path: '/merchant/support/complaints',
    name: 'MerchantSupportComplaints',
    component: () => import('@/views/admin/AdminComplaints.vue'),
    meta: { layout: 'merchant', requiresAuth: true, roles: SUPPORT_AREA_ROLES },
  },
  {
    path: '/merchant/support/chat',
    name: 'MerchantSupportChat',
    component: () => import('@/views/cs/CsChat.vue'),
    meta: { layout: 'merchant', requiresAuth: true, roles: SUPPORT_AREA_ROLES },
  },
]

// ── 管理后台路由（仅管理员可见）──
export const adminRoutes = [
  // 进入管理端时重定向到管理仪表盘
  { path: '/admin', redirect: '/admin/dashboard' },
  {
    path: '/admin/dashboard',
    name: 'AdminDashboard',
    component: () => import('@/views/admin/AdminDashboard.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/statistics',
    name: 'AdminStatistics',
    component: () => import('@/views/admin/AdminStatistics.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/users',
    name: 'AdminUsers',
    component: () => import('@/views/admin/AdminUsers.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/merchants',
    name: 'AdminMerchants',
    component: () => import('@/views/admin/AdminMerchants.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/real-name-reviews',
    name: 'AdminRealNameReviews',
    component: () => import('@/views/admin/AdminRealNameReviews.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/orders',
    name: 'AdminOrders',
    component: () => import('@/views/admin/AdminOrders.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/complaints',
    name: 'AdminComplaints',
    component: () => import('@/views/admin/AdminComplaints.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/keepers',
    name: 'AdminKeepers',
    component: () => import('@/views/admin/AdminKeepers.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/pets',
    name: 'AdminPets',
    component: () => import('@/views/admin/AdminPets.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/refunds',
    name: 'AdminRefunds',
    component: () => import('@/views/admin/AdminRefunds.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/coupons',
    name: 'AdminCoupons',
    component: () => import('@/views/admin/AdminCoupons.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/member-plans',
    name: 'AdminMemberPlans',
    component: () => import('@/views/admin/AdminMemberPlans.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/membership-users',
    name: 'AdminMembershipUsers',
    component: () => import('@/views/admin/AdminMembershipUsers.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/membership-orders',
    name: 'AdminMembershipOrders',
    component: () => import('@/views/admin/AdminMembershipOrders.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/notices',
    name: 'AdminNotices',
    component: () => import('@/views/admin/AdminNotices.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/withdrawals',
    name: 'AdminWithdrawals',
    component: () => import('@/views/admin/AdminWithdrawals.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/tickets',
    name: 'AdminTickets',
    component: () => import('@/views/admin/AdminTickets.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/roles',
    name: 'AdminRoles',
    component: () => import('@/views/admin/AdminRoles.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/reviews',
    name: 'AdminReviews',
    component: () => import('@/views/admin/AdminReviews.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/categories',
    name: 'AdminCategory',
    component: () => import('@/views/admin/AdminCategory.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/service-categories',
    name: 'AdminServiceCategory',
    component: () => import('@/views/admin/AdminServiceCategory.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/banners',
    name: 'AdminBanner',
    component: () => import('@/views/admin/AdminBanner.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/operation-logs',
    name: 'AdminOperationLogs',
    component: () => import('@/views/admin/AdminOperationLogs.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/recycle-bin',
    name: 'AdminRecycleBin',
    component: () => import('@/views/admin/AdminRecycleBin.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/transactions',
    name: 'AdminTransactions',
    component: () => import('@/views/admin/AdminTransactions.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/wallets',
    name: 'AdminWallets',
    component: () => import('@/views/admin/AdminWallets.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/rag',
    name: 'AdminRag',
    component: () => import('@/views/admin/AdminRag.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/ai-config',
    name: 'AdminAiConfig',
    component: () => import('@/views/admin/AdminAiConfig.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/qualifications',
    name: 'AdminQualifications',
    component: () => import('@/views/admin/AdminQualifications.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/payments',
    name: 'AdminPayments',
    component: () => import('@/views/admin/AdminPayments.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/tips',
    name: 'AdminTips',
    component: () => import('@/views/admin/AdminTips.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/files',
    name: 'AdminFiles',
    component: () => import('@/views/admin/AdminFiles.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/notifications',
    name: 'AdminNotifications',
    component: () => import('@/views/admin/AdminNotifications.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/favorites',
    name: 'AdminFavorites',
    component: () => import('@/views/admin/AdminFavorites.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/attendance',
    name: 'AdminAttendance',
    component: () => import('@/views/admin/AdminAttendance.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
  {
    path: '/admin/leaves',
    name: 'AdminLeaves',
    component: () => import('@/views/admin/AdminLeaves.vue'),
    meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] },
  },
]

// ═══════════════ 路由实例（初始只注册静态路由，动态路由登录后再注入）═══════════════
const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
})

// 标记动态路由是否已注入，避免重复添加
let dynamicRoutesLoaded = false

// ═══════════════ 按用户角色注入动态路由 ═══════════════
export function addDynamicRoutes(roles) {
  // 已注入过则直接返回，保证幂等
  if (dynamicRoutesLoaded) return
  // 去掉后端角色名中的 'ROLE_' 前缀，统一成前端 ROLES 常量的形式
  const normalizedRoles = (roles || []).map(r => r.replace('ROLE_', ''))
  const allRoutes = []
  // 商家 / 管理员：注入商家端路由
  if (normalizedRoles.includes(ROLES.MERCHANT) || normalizedRoles.includes(ROLES.ADMIN)) {
    allRoutes.push(...merchantRoutes)
  }
  // 商家 / 管理员 / 客服：注入客服支持路由
  if (normalizedRoles.includes(ROLES.MERCHANT) || normalizedRoles.includes(ROLES.ADMIN) || normalizedRoles.includes(ROLES.CUSTOMER_SERVICE)) {
    allRoutes.push(...merchantSupportRoutes)
  }
  // 管理员：注入管理后台路由
  if (normalizedRoles.includes(ROLES.ADMIN)) {
    allRoutes.push(...adminRoutes)
  }
  // 逐条注册，跳过已存在的同名路由，防止重复注册报错
  allRoutes.forEach(route => {
    if (route.name && !router.hasRoute(route.name)) {
      router.addRoute(route)
    }
  })
  dynamicRoutesLoaded = true
}

// ═══════════════ 退出登录时移除所有动态路由 ═══════════════
export function resetDynamicRoutes() {
  const allDynamic = [...merchantRoutes, ...merchantSupportRoutes, ...adminRoutes]
  // 收集所有动态路由的 name（排除重定向等无名路由），逐个移除
  const names = new Set(allDynamic.map(r => r.name).filter(Boolean))
  names.forEach(name => {
    if (router.hasRoute(name)) {
      router.removeRoute(name)
    }
  })
  // 复位标记，下次登录可重新注入
  dynamicRoutesLoaded = false
}

// ═══════════════ 统一路由守卫 ═══════════════
router.beforeEach((to, from, next) => {
  // 1. 令牌已过期：清除本地登录态（token / refreshToken / 用户信息）
  const rawToken = localStorage.getItem('token')
  if (rawToken && isTokenExpired(rawToken)) {
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
  }
  const token = localStorage.getItem('token')
  const userStr = localStorage.getItem('user')
  let user = null
  try { user = userStr ? JSON.parse(userStr) : null } catch (e) {}

  // 2. 目标路由不存在（未匹配任何规则，通常是动态路由尚未注入）
  if (!to.matched.length) {
    // 商家端 / 管理端路径：尝试按本地用户角色注入动态路由后重试
    if (to.path.startsWith('/merchant') || to.path.startsWith('/admin')) {
      if (rawToken && user?.roles_wsh?.length) {
        addDynamicRoutes(user.roles_wsh)
        if (router.resolve(to.fullPath).matched.length) {
          // 注入后能匹配到了：用 replace 方式重新导航一次
          return next({ path: to.path, query: to.query, hash: to.hash, replace: true })
        }
      }
      // 注入后仍无法匹配：跳转登录页并携带回跳地址
      return next({ path: '/login', query: { redirect: to.fullPath } })
    }
    // 其他未知路径 → 404
    return next({ path: '/404', replace: true })
  }

  const requiresAuth = to.meta?.requiresAuth === true

  // 3. 公开路由直接放行；但已登录用户访问登录 / 注册 / 忘记密码页时重定向到仪表盘
  if (!requiresAuth) {
    if (token && user && (to.path === '/login' || to.path === '/register' || to.path === '/forget-password')) {
      return next({ path: '/dashboard' })
    }
    return next()
  }

  // 4. 需要登录但本地没有登录态
  if (!token || !user) {
    // 用户端页面：停留在当前页，弹出登录提示弹窗
    if (to.meta?.layout === 'user') {
      const appStore = useAppStore()
      appStore.loginRedirectPath = to.fullPath
      appStore.showLoginPrompt = true
      // 由页面内跳转触发时：中断本次导航，留在当前页
      if (from.name) return next(false)
      // 首次直接打开受保护路由时：落到登录页
    }
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }

  // 5. 路由要求特定角色：逐项比对，无权限则跳 403
  const requiredRoles = to.meta?.roles
  if (requiredRoles && requiredRoles.length > 0) {
    const userRoles = (user.roles_wsh || []).map(r => r.replace('ROLE_', ''))
    const hasAccess = requiredRoles.some(r => userRoles.includes(r))
    if (!hasAccess) {
      return next({ path: '/403' })
    }
  }

  // 6. 全部校验通过，放行
  next()
})

export default router
