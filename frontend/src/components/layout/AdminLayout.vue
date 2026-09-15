<template>
  <div class="app-layout">
    <aside class="sidebar">
      <div class="sidebar-logo">管理后台</div>
      <div class="sidebar-user">
        <div class="sidebar-avatar" @click="goProfile" title="个人中心">
          <img v-if="avatarUrl" :src="avatarUrl" alt="avatar">
          <span v-else>{{ userInitial }}</span>
        </div>
        <div class="sidebar-user-info">
          <div class="name">{{ authStore.user?.nickname_wsh || authStore.user?.username_wsh }}</div>
          <div class="role">{{ (authStore.user?.roles_wsh || []).join(', ') }}</div>
        </div>
      </div>
      <nav class="sidebar-nav">
        <template v-for="group in navItems" :key="group.label">
          <div v-if="group.items" class="nav-group">
            <div class="nav-group-title" @click="toggleGroup(group.label)">
              <span>{{ group.label }}</span>
              <span class="nav-group-arrow" :class="{ expanded: expandedGroups[group.label] }">▶</span>
            </div>
            <div v-show="expandedGroups[group.label]" class="nav-group-items">
              <router-link
                v-for="item in group.items"
                :key="item.route"
                :to="item.route"
                active-class="active"
                class="nav-item"
              >
                <span>{{ item.label }}</span>
              </router-link>
            </div>
          </div>
          <router-link v-else :key="group.route" :to="group.route" active-class="active" class="nav-item">
            <span>{{ group.label }}</span>
          </router-link>
        </template>
      </nav>
      <div style="padding:12px 20px;border-top:1px solid rgba(255,255,255,.08)">
        <a href="#" @click.prevent="logout" style="color:rgba(255,255,255,0.5);font-size:13px">退出登录</a>
        <router-link to="/dashboard" style="display:block;margin-top:8px;color:rgba(255,255,255,0.5);font-size:13px">返回首页</router-link>
      </div>
    </aside>
    <div class="main-content">
      <div class="top-bar">
        <div class="breadcrumb"><strong>{{ routeName }}</strong></div>
        <div class="top-bar-right">
          <ThemeToggle />
          <MessageIndicator />
          <div class="user-avatar" @click="goProfile" title="个人中心">
            <img v-if="avatarUrl" :src="avatarUrl" alt="avatar">
            <span v-else>{{ userInitial }}</span>
          </div>
        </div>
      </div>
      <div class="content-area">
        <slot />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { resetDynamicRoutes } from '@/router'
import MessageIndicator from '@/components/common/MessageIndicator.vue'
import ThemeToggle from '@/components/common/ThemeToggle.vue'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const avatarUrl = computed(() => authStore.user?.avatar_wsh || '')
const userInitial = computed(() => (authStore.user?.nickname_wsh || authStore.user?.username_wsh || '?')[0])

const routeName = computed(() => {
  const names = {
    'AdminDashboard': '控制台',
    'AdminStatistics': '数据统计',
    'AdminUsers': '用户管理',
    'AdminRoles': '角色管理',
    'AdminRealNameReviews': '实名审核',
    'AdminMerchants': '商家审核',
    'AdminKeepers': '寄养员管理',
    'AdminQualifications': '资质审核',
    'AdminPets': '宠物查看',
    'AdminCategory': '宠物品种管理',
    'AdminServiceCategory': '服务分类管理',
    'AdminOrders': '订单管理',
    'AdminRefunds': '退款审核',
    'AdminCoupons': '优惠券管理',
    'AdminMemberPlans': '会员套餐管理',
    'AdminMembershipUsers': '会员用户管理',
    'AdminMembershipOrders': '会员订单管理',
    'AdminWithdrawals': '提现审核',
    'AdminWallets': '钱包管理',
    'AdminTransactions': '交易记录',
    'AdminTickets': '工单管理',
    'AdminComplaints': '投诉处理',
    'AdminReviews': '内容审核',
    'AdminNotices': '公告管理',
    'AdminBanner': '广告管理',
    'AdminRag': '知识库管理',
    'AdminAiConfig': 'AI 配置',
    'AdminOperationLogs': '操作日志',
    'AdminRecycleBin': '回收站',
    'AdminPayments': '支付记录',
    'AdminTips': '打赏记录',
    'AdminFiles': '文件资源',
    'AdminNotifications': '通知推送',
    'AdminFavorites': '收藏数据',
    'AdminAttendance': '看护出勤',
    'AdminLeaves': '请假审批',
  }
  return names[route.name] || '控制台'
})

const navGroups = [
  {
    label: '总览',
    items: [
      { route: '/admin/dashboard', label: '控制台' },
      { route: '/admin/statistics', label: '数据统计' },
    ]
  },
  {
    label: '用户与权限',
    items: [
      { route: '/admin/users', label: '用户管理' },
      { route: '/admin/roles', label: '角色管理' },
      { route: '/admin/real-name-reviews', label: '实名审核' },
    ]
  },
  {
    label: '商家与宠物',
    items: [
      { route: '/admin/merchants', label: '商家审核' },
      { route: '/admin/keepers', label: '寄养员管理' },
      { route: '/admin/qualifications', label: '资质审核' },
      { route: '/admin/pets', label: '宠物查看' },
      { route: '/admin/categories', label: '宠物品种管理' },
      { route: '/admin/service-categories', label: '服务分类管理' },
    ]
  },
  {
    label: '订单与财务',
    items: [
      { route: '/admin/orders', label: '订单管理' },
      { route: '/admin/refunds', label: '退款审核' },
      { route: '/admin/coupons', label: '优惠券管理' },
      { route: '/admin/member-plans', label: '会员套餐管理' },
      { route: '/admin/membership-users', label: '会员用户管理' },
      { route: '/admin/membership-orders', label: '会员订单管理' },
      { route: '/admin/withdrawals', label: '提现审核' },
      { route: '/admin/wallets', label: '钱包管理' },
      { route: '/admin/transactions', label: '交易记录' },
      { route: '/admin/payments', label: '支付记录' },
      { route: '/admin/tips', label: '打赏记录' },
    ]
  },
  {
    label: '商家与宠物',
    items: [
      { route: '/admin/merchants', label: '商家审核' },
      { route: '/admin/keepers', label: '寄养员管理' },
      { route: '/admin/qualifications', label: '资质审核' },
      { route: '/admin/pets', label: '宠物查看' },
      { route: '/admin/categories', label: '宠物品种管理' },
      { route: '/admin/service-categories', label: '服务分类管理' },
      { route: '/admin/attendance', label: '看护出勤' },
      { route: '/admin/leaves', label: '请假审批' },
    ]
  },
  {
    label: '客服与审核',
    items: [
      { route: '/admin/tickets', label: '工单管理' },
      { route: '/admin/complaints', label: '投诉处理' },
      { route: '/admin/reviews', label: '内容审核' },
    ]
  },
  {
    label: '内容与运营',
    items: [
      { route: '/admin/notices', label: '公告管理' },
      { route: '/admin/banners', label: '广告管理' },
      { route: '/admin/rag', label: '知识库管理' },
      { route: '/admin/ai-config', label: 'AI 配置' },
      { route: '/admin/files', label: '文件资源' },
      { route: '/admin/notifications', label: '通知推送' },
    ]
  },
  {
    label: '数据管理',
    items: [
      { route: '/admin/favorites', label: '收藏数据' },
    ]
  },
  {
    label: '系统',
    items: [
      { route: '/admin/operation-logs', label: '操作日志' },
      { route: '/admin/recycle-bin', label: '回收站' },
    ]
  },
]

const csNavItems = [
  { route: '/admin/tickets', label: '工单管理' },
  { route: '/admin/complaints', label: '投诉处理' },
  { route: '/admin/reviews', label: '内容审核' },
]

const isAdmin = computed(() => authStore.isAdmin)
const isCs = computed(() => authStore.isCs)
const navItems = computed(() => isCs.value && !isAdmin.value ? csNavItems : navGroups)

const expandedGroups = reactive({
  '总览': false,
  '用户与权限': false,
  '商家与宠物': false,
  '订单与财务': false,
  '客服与审核': false,
  '内容与运营': false,
  '数据管理': false,
  '系统': false,
})

function toggleGroup(label) {
  expandedGroups[label] = !expandedGroups[label]
}

onMounted(async () => {
  try {
    const r = await request.get('/users/me')
    if (r.data.code === 200 && r.data.data) {
      const url = r.data.data.avatar_wsh || ''
      if (authStore.user) {
        authStore.user = { ...authStore.user, avatar_wsh: url }
        localStorage.setItem('user', JSON.stringify(authStore.user))
      }
    }
  } catch (e) {}
})

function goProfile() {
  router.push('/profile')
}

function logout() {
  authStore.clearAuth()
  resetDynamicRoutes()
  router.push('/login')
}
</script>

<style scoped>
.nav-group {
  margin-bottom: 4px;
}

.nav-group-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px 6px;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: rgba(255, 255, 255, 0.45);
  cursor: pointer;
  user-select: none;
}

.nav-group-title:hover {
  color: rgba(255, 255, 255, 0.6);
}

.nav-group-arrow {
  font-size: 10px;
  transition: transform 0.2s;
}

.nav-group-arrow.expanded {
  transform: rotate(90deg);
}

.nav-group-items {
  overflow: hidden;
}

.nav-item {
  padding-left: 32px !important;
  font-size: 12px;
}

.top-bar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #fff;
  cursor: pointer;
  overflow: hidden;
  flex-shrink: 0;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.sidebar-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
}
.sidebar-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #fff;
  cursor: pointer;
  overflow: hidden;
  flex-shrink: 0;
}
.sidebar-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.sidebar-user-info {
  overflow: hidden;
}
.sidebar-user-info .name {
  font-size: 14px;
  font-weight: 500;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.sidebar-user-info .role {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
