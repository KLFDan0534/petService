import { createRouter, createWebHistory } from 'vue-router'
import { ROLES } from '@/utils/permission'
import { useAppStore } from '@/stores/app'
import { isTokenExpired } from '@/utils/jwt'

const USER_AREA_ROLES = [ROLES.OWNER, ROLES.KEEPER, ROLES.MERCHANT, ROLES.ADMIN]
const ACCOUNT_AREA_ROLES = [...USER_AREA_ROLES, ROLES.CUSTOMER_SERVICE]
const SUPPORT_AREA_ROLES = [ROLES.MERCHANT, ROLES.ADMIN, ROLES.CUSTOMER_SERVICE]

// ====== Public routes (no auth required) ======
export const constantRoutes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', name: 'Dashboard', component: () => import('@/views/user/Dashboard.vue'), meta: { layout: 'user', requiresAuth: false } },
  { path: '/services', name: 'Services', component: () => import('@/views/user/Services.vue'), meta: { layout: 'user', requiresAuth: false } },
  // Authenticated routes are always registered; the route guard redirects to /login if not logged in
  { path: '/login', name: 'Login', component: () => import('@/views/user/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('@/views/user/Register.vue') },
  { path: '/forget-password', name: 'ForgetPassword', component: () => import('@/views/user/ForgetPassword.vue') },
  { path: '/403', name: 'Forbidden', component: () => import('@/views/error/Forbidden.vue') },
  { path: '/404', name: 'NotFound', component: () => import('@/views/error/NotFound.vue') },
  { path: '/500', name: 'ServerError', component: () => import('@/views/error/ServerError.vue') },
  { path: '/503', name: 'ServiceUnavailable', component: () => import('@/views/error/ServiceUnavailable.vue') },
  { path: '/pets', name: 'Pets', component: () => import('@/views/user/Pets.vue'), meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES, keepAlive: true } },
  { path: '/pets/:id', name: 'PetDetail', component: () => import('@/views/user/PetDetail.vue'), meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES } },
  { path: '/merchants', name: 'Merchants', component: () => import('@/views/user/Merchants.vue'), meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES, keepAlive: true } },
  { path: '/merchants/:id', name: 'MerchantDetail', component: () => import('@/views/user/MerchantDetail.vue'), meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES } },
  { path: '/keepers', name: 'Keepers', component: () => import('@/views/user/Keepers.vue'), meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES } },
  { path: '/keepers/:id', name: 'KeeperDetail', component: () => import('@/views/user/KeeperDetail.vue'), meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES } },
  { path: '/keeper/profile', name: 'KeeperProfile', component: () => import('@/views/user/KeeperProfile.vue'), meta: { layout: 'user', requiresAuth: true, roles: [ROLES.KEEPER, ROLES.MERCHANT, ROLES.ADMIN] } },
  { path: '/services/:id', name: 'ServiceDetail', component: () => import('@/views/user/ServiceDetail.vue'), meta: { layout: 'user', requiresAuth: false } },
  { path: '/orders', name: 'Orders', component: () => import('@/views/user/Orders.vue'), meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES, keepAlive: true } },
  { path: '/orders/:id', name: 'OrderDetail', component: () => import('@/views/order/OrderDetailView.vue'), meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES } },
  { path: '/coupons', name: 'Coupons', component: () => import('@/views/user/Coupons.vue'), meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES } },
  { path: '/membership', name: 'Membership', component: () => import('@/views/user/Membership.vue'), meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES } },
  { path: '/keeper-apply', name: 'KeeperApply', component: () => import('@/views/user/KeeperApply.vue'), meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES } },
  { path: '/customer-service/apply', name: 'CustomerServiceApply', component: () => import('@/views/user/CustomerServiceApply.vue'), meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES } },
  { path: '/keeper-workflow', name: 'KeeperWorkflow', component: () => import('@/views/user/KeeperWorkflow.vue'), meta: { layout: 'user', requiresAuth: true, roles: [ROLES.KEEPER, ROLES.ADMIN] } },
  { path: '/payments', name: 'Payments', component: () => import('@/views/user/Payments.vue'), meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES } },
  { path: '/recharge', name: 'Recharge', component: () => import('@/views/user/Recharge.vue'), meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES } },
  { path: '/profile', name: 'Profile', component: () => import('@/views/user/Profile.vue'), meta: { layout: 'user', requiresAuth: true, roles: ACCOUNT_AREA_ROLES } },
  { path: '/profile/phone', name: 'ProfilePhoneEdit', component: () => import('@/views/user/ProfileAccountEdit.vue'), meta: { layout: 'user', requiresAuth: true, roles: ACCOUNT_AREA_ROLES, accountType: 'phone' } },
  { path: '/profile/email', name: 'ProfileEmailEdit', component: () => import('@/views/user/ProfileAccountEdit.vue'), meta: { layout: 'user', requiresAuth: true, roles: ACCOUNT_AREA_ROLES, accountType: 'email' } },
  { path: '/profile/real-name', name: 'ProfileRealNameEdit', component: () => import('@/views/user/ProfileAccountEdit.vue'), meta: { layout: 'user', requiresAuth: true, roles: ACCOUNT_AREA_ROLES, accountType: 'realName' } },
  { path: '/profile/payment-password', name: 'ProfilePaymentPasswordEdit', component: () => import('@/views/user/ProfileAccountEdit.vue'), meta: { layout: 'user', requiresAuth: true, roles: ACCOUNT_AREA_ROLES, accountType: 'paymentPassword' } },
  { path: '/files', name: 'Files', component: () => import('@/views/user/Files.vue'), meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES } },
  { path: '/refunds', name: 'Refunds', component: () => import('@/views/user/Refunds.vue'), meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES } },
  { path: '/addresses', name: 'Addresses', component: () => import('@/views/user/Addresses.vue'), meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES } },
  { path: '/chat', name: 'Chat', component: () => import('@/views/user/Chat.vue'), meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES } },
  { path: '/notifications', name: 'Notifications', component: () => import('@/views/user/Notifications.vue'), meta: { layout: 'user', requiresAuth: true, roles: ACCOUNT_AREA_ROLES } },
  { path: '/notices/:id', name: 'NoticeDetail', component: () => import('@/views/user/NoticeDetail.vue'), meta: { layout: 'user', requiresAuth: true, roles: ACCOUNT_AREA_ROLES } },
  { path: '/favorites', name: 'Favorites', component: () => import('@/views/user/Favorites.vue'), meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES } },
  { path: '/tickets', name: 'Tickets', component: () => import('@/views/user/Tickets.vue'), meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES } },
  { path: '/complaints', name: 'Complaints', component: () => import('@/views/user/Complaints.vue'), meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES } },
  { path: '/ratings', name: 'Ratings', component: () => import('@/views/user/Ratings.vue'), meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES } },
  { path: '/credit-reputation', name: 'CreditReputation', component: () => import('@/views/user/CreditReputation.vue'), meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES } },
  { path: '/anomaly', name: 'Anomaly', component: () => import('@/views/user/Anomaly.vue'), meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES } },
  { path: '/revenue', name: 'RevenueCenter', component: () => import('@/views/user/RevenueCenter.vue'), meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES } },
  { path: '/ai', name: 'AI', component: () => import('@/views/user/AI.vue'), meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES } },
  { path: '/rag', name: 'RAG', component: () => import('@/views/user/RAG.vue'), meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES } },
  { path: '/agent', name: 'Agent', component: () => import('@/views/user/Agent.vue'), meta: { layout: 'user', requiresAuth: true, roles: USER_AREA_ROLES } },
]

// ====== All user routes are loaded as constant routes; userRoutes kept empty for compatibility ======
export const userRoutes = []

// ====== Merchant routes (MERCHANT / ADMIN) ======
export const merchantRoutes = [
  { path: '/merchant', redirect: '/merchant/dashboard' },
  { path: '/merchant/dashboard', name: 'MerchantDashboard', component: () => import('@/views/merchant/MerchantDashboard.vue'), meta: { layout: 'merchant', requiresAuth: true, roles: [ROLES.MERCHANT, ROLES.ADMIN] } },
  { path: '/merchant/profile', name: 'MerchantProfile', component: () => import('@/views/merchant/MerchantProfile.vue'), meta: { layout: 'merchant', requiresAuth: true, roles: [ROLES.MERCHANT, ROLES.ADMIN] } },
  { path: '/merchant/services', name: 'MerchantServices', component: () => import('@/views/merchant/MerchantServices.vue'), meta: { layout: 'merchant', requiresAuth: true, roles: [ROLES.MERCHANT, ROLES.ADMIN] } },
  { path: '/merchant/business-hours', name: 'MerchantBusinessHours', component: () => import('@/views/merchant/MerchantBusinessHours.vue'), meta: { layout: 'merchant', requiresAuth: true, roles: [ROLES.MERCHANT, ROLES.ADMIN] } },
  { path: '/merchant/pets', name: 'MerchantPets', component: () => import('@/views/merchant/MerchantPets.vue'), meta: { layout: 'merchant', requiresAuth: true, roles: [ROLES.MERCHANT, ROLES.ADMIN] } },
  { path: '/merchant/orders', name: 'MerchantOrders', component: () => import('@/views/merchant/MerchantOrders.vue'), meta: { layout: 'merchant', requiresAuth: true, roles: [ROLES.MERCHANT, ROLES.ADMIN] } },
  { path: '/merchant/keepers', name: 'MerchantKeepers', component: () => import('@/views/merchant/MerchantKeepers.vue'), meta: { layout: 'merchant', requiresAuth: true, roles: [ROLES.MERCHANT, ROLES.ADMIN] } },
  { path: '/merchant/customer-service', name: 'MerchantCustomerService', component: () => import('@/views/merchant/MerchantCustomerService.vue'), meta: { layout: 'merchant', requiresAuth: true, roles: [ROLES.MERCHANT, ROLES.ADMIN] } },
  { path: '/merchant/statistics', name: 'MerchantStatistics', component: () => import('@/views/merchant/MerchantStatistics.vue'), meta: { layout: 'merchant', requiresAuth: true, roles: [ROLES.MERCHANT, ROLES.ADMIN] } },
]

export const merchantSupportRoutes = [
  { path: '/merchant/support/tickets', name: 'MerchantSupportTickets', component: () => import('@/views/admin/AdminTickets.vue'), meta: { layout: 'merchant', requiresAuth: true, roles: SUPPORT_AREA_ROLES } },
  { path: '/merchant/support/complaints', name: 'MerchantSupportComplaints', component: () => import('@/views/admin/AdminComplaints.vue'), meta: { layout: 'merchant', requiresAuth: true, roles: SUPPORT_AREA_ROLES } },
]

// ====== Admin routes (ADMIN only) ======
export const adminRoutes = [
  { path: '/admin', redirect: '/admin/dashboard' },
  { path: '/admin/dashboard', name: 'AdminDashboard', component: () => import('@/views/admin/AdminDashboard.vue'), meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] } },
  { path: '/admin/statistics', name: 'AdminStatistics', component: () => import('@/views/admin/AdminStatistics.vue'), meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] } },
  { path: '/admin/users', name: 'AdminUsers', component: () => import('@/views/admin/AdminUsers.vue'), meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] } },
  { path: '/admin/merchants', name: 'AdminMerchants', component: () => import('@/views/admin/AdminMerchants.vue'), meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] } },
  { path: '/admin/real-name-reviews', name: 'AdminRealNameReviews', component: () => import('@/views/admin/AdminRealNameReviews.vue'), meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] } },
  { path: '/admin/orders', name: 'AdminOrders', component: () => import('@/views/admin/AdminOrders.vue'), meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] } },
  { path: '/admin/complaints', name: 'AdminComplaints', component: () => import('@/views/admin/AdminComplaints.vue'), meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] } },
  { path: '/admin/keepers', name: 'AdminKeepers', component: () => import('@/views/admin/AdminKeepers.vue'), meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] } },
  { path: '/admin/pets', name: 'AdminPets', component: () => import('@/views/admin/AdminPets.vue'), meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] } },
  { path: '/admin/refunds', name: 'AdminRefunds', component: () => import('@/views/admin/AdminRefunds.vue'), meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] } },
  { path: '/admin/coupons', name: 'AdminCoupons', component: () => import('@/views/admin/AdminCoupons.vue'), meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] } },
  { path: '/admin/member-plans', name: 'AdminMemberPlans', component: () => import('@/views/admin/AdminMemberPlans.vue'), meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] } },
  { path: '/admin/membership-users', name: 'AdminMembershipUsers', component: () => import('@/views/admin/AdminMembershipUsers.vue'), meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] } },
  { path: '/admin/membership-orders', name: 'AdminMembershipOrders', component: () => import('@/views/admin/AdminMembershipOrders.vue'), meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] } },
  { path: '/admin/notices', name: 'AdminNotices', component: () => import('@/views/admin/AdminNotices.vue'), meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] } },
  { path: '/admin/withdrawals', name: 'AdminWithdrawals', component: () => import('@/views/admin/AdminWithdrawals.vue'), meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] } },
  { path: '/admin/tickets', name: 'AdminTickets', component: () => import('@/views/admin/AdminTickets.vue'), meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] } },
  { path: '/admin/roles', name: 'AdminRoles', component: () => import('@/views/admin/AdminRoles.vue'), meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] } },
  { path: '/admin/reviews', name: 'AdminReviews', component: () => import('@/views/admin/AdminReviews.vue'), meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] } },
  { path: '/admin/categories', name: 'AdminCategory', component: () => import('@/views/admin/AdminCategory.vue'), meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] } },
  { path: '/admin/service-categories', name: 'AdminServiceCategory', component: () => import('@/views/admin/AdminServiceCategory.vue'), meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] } },
  { path: '/admin/banners', name: 'AdminBanner', component: () => import('@/views/admin/AdminBanner.vue'), meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] } },
  { path: '/admin/operation-logs', name: 'AdminOperationLogs', component: () => import('@/views/admin/AdminOperationLogs.vue'), meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] } },
  { path: '/admin/recycle-bin', name: 'AdminRecycleBin', component: () => import('@/views/admin/AdminRecycleBin.vue'), meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] } },
  { path: '/admin/transactions', name: 'AdminTransactions', component: () => import('@/views/admin/AdminTransactions.vue'), meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] } },
  { path: '/admin/wallets', name: 'AdminWallets', component: () => import('@/views/admin/AdminWallets.vue'), meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] } },
  { path: '/admin/rag', name: 'AdminRag', component: () => import('@/views/admin/AdminRag.vue'), meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] } },
  { path: '/admin/qualifications', name: 'AdminQualifications', component: () => import('@/views/admin/AdminQualifications.vue'), meta: { layout: 'admin', requiresAuth: true, roles: [ROLES.ADMIN] } },
]

// ====== Router instance (initially only constant routes) ======
const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
})

// Track which dynamic route names are loaded
let dynamicRoutesLoaded = false

// ====== Add dynamic routes based on user roles ======
export function addDynamicRoutes(roles) {
  if (dynamicRoutesLoaded) return
  const normalizedRoles = (roles || []).map(r => r.replace('ROLE_', ''))
  const allRoutes = []
  // All authenticated users get user routes (already loaded as constant routes)
  if (normalizedRoles.includes(ROLES.MERCHANT) || normalizedRoles.includes(ROLES.ADMIN)) {
    allRoutes.push(...merchantRoutes)
  }
  if (normalizedRoles.includes(ROLES.MERCHANT) || normalizedRoles.includes(ROLES.ADMIN) || normalizedRoles.includes(ROLES.CUSTOMER_SERVICE)) {
    allRoutes.push(...merchantSupportRoutes)
  }
  if (normalizedRoles.includes(ROLES.ADMIN)) {
    allRoutes.push(...adminRoutes)
  }
  allRoutes.forEach(route => {
    if (route.name && !router.hasRoute(route.name)) {
      router.addRoute(route)
    }
  })
  dynamicRoutesLoaded = true
}

// ====== Remove dynamic routes on logout ======
export function resetDynamicRoutes() {
  const allDynamic = [...merchantRoutes, ...merchantSupportRoutes, ...adminRoutes]
  const names = new Set(allDynamic.map(r => r.name).filter(Boolean))
  names.forEach(name => {
    if (router.hasRoute(name)) {
      router.removeRoute(name)
    }
  })
  dynamicRoutesLoaded = false
}

// ====== Unified route guard ======
router.beforeEach((to, from, next) => {
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

  if (!to.matched.length) {
    if (to.path.startsWith('/merchant') || to.path.startsWith('/admin')) {
      if (rawToken && user?.roles_wsh?.length) {
        addDynamicRoutes(user.roles_wsh)
        if (router.resolve(to.fullPath).matched.length) {
          return next({ path: to.path, query: to.query, hash: to.hash, replace: true })
        }
      }
      return next({ path: '/login', query: { redirect: to.fullPath } })
    }
    return next({ path: '/404', replace: true })
  }

  const requiresAuth = to.meta?.requiresAuth === true

  // Public route -> allow, but redirect to dashboard if already logged in (login/register/etc.)
  if (!requiresAuth) {
    if (token && user && (to.path === '/login' || to.path === '/register' || to.path === '/forget-password')) {
      return next({ path: '/dashboard' })
    }
    return next()
  }

  // Not logged in
  if (!token || !user) {
    // User-facing page -> stay on current page, show login prompt dialog
    if (to.meta?.layout === 'user') {
      const appStore = useAppStore()
      appStore.loginRedirectPath = to.fullPath
      appStore.showLoginPrompt = true
      // User-triggered navigation: abort and stay on current page
      if (from.name) return next(false)
      // Initial page load to a protected route: redirect to login
    }
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }

  // Route requires specific roles -> check
  const requiredRoles = to.meta?.roles
  if (requiredRoles && requiredRoles.length > 0) {
    const userRoles = (user.roles_wsh || []).map(r => r.replace('ROLE_', ''))
    const hasAccess = requiredRoles.some(r => userRoles.includes(r))
    if (!hasAccess) {
      return next({ path: '/403' })
    }
  }

  next()
})

export default router
