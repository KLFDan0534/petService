<template>
  <div class="app-layout">
    <aside class="sidebar">
      <div class="sidebar-logo">管理后台</div>
      <div class="sidebar-user">
        <div class="name">{{ authStore.user?.nickname_wsh || authStore.user?.username_wsh }}</div>
        <div class="role">{{ (authStore.user?.roles_wsh || []).join(', ') }}</div>
      </div>
      <nav class="sidebar-nav">
        <router-link v-for="item in navItems" :key="item.route" :to="item.route" active-class="active">
          <span>{{ item.label }}</span>
        </router-link>
      </nav>
      <div style="padding:12px 20px;border-top:1px solid rgba(255,255,255,.08)">
        <a href="#" @click.prevent="logout" style="color:rgba(255,255,255,0.5);font-size:13px">退出登录</a>
        <router-link to="/dashboard" style="display:block;margin-top:8px;color:rgba(255,255,255,0.5);font-size:13px">返回首页</router-link>
      </div>
    </aside>
    <div class="main-content">
      <div class="top-bar">
        <div class="breadcrumb"><strong>{{ routeName }}</strong></div>
      </div>
      <div class="content-area">
        <slot />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { resetDynamicRoutes } from '@/router'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const isAdmin = computed(() => authStore.isAdmin)
const isCs = computed(() => authStore.isCs)

const routeName = computed(() => {
  const names = {
    'AdminDashboard': '控制台', 'AdminUsers': '用户管理', 'AdminMerchants': '商家审核',
    'AdminOrders': '订单管理', 'AdminComplaints': '投诉处理', 'AdminKeepers': '寄养员管理',
    'AdminPets': '宠物查看', 'AdminRefunds': '退款审核', 'AdminNotices': '公告管理',
    'AdminStatistics': '数据统计', 'AdminWithdrawals': '提现审核', 'AdminTickets': '工单管理',
    'AdminRoles': '角色管理', 'AdminReviews': '内容审核',
    'AdminCategory': '分类管理', 'AdminBanner': '广告管理',
    'AdminAdoptions': '领养审核', 'AdminRag': '知识库管理',
  }
  return names[route.name] || '控制台'
})

const fullNavItems = [
  { route: '/admin/dashboard', label: '控制台' },
  { route: '/admin/users', label: '用户管理' },
  { route: '/admin/merchants', label: '商家审核' },
  { route: '/admin/orders', label: '订单管理' },
  { route: '/admin/complaints', label: '投诉处理' },
  { route: '/admin/keepers', label: '寄养员管理' },
  { route: '/admin/pets', label: '宠物查看' },
  { route: '/admin/refunds', label: '退款审核' },
  { route: '/admin/notices', label: '公告管理' },
  { route: '/admin/statistics', label: '数据统计' },
  { route: '/admin/withdrawals', label: '提现审核' },
  { route: '/admin/tickets', label: '工单管理' },
  { route: '/admin/categories', label: '分类管理' },
  { route: '/admin/roles', label: '角色管理' },
  { route: '/admin/reviews', label: '内容审核' },
  { route: '/admin/banners', label: '广告管理' },
  { route: '/admin/rag', label: '知识库管理' },
]

const csNavItems = [
  { route: '/admin/tickets', label: '工单管理' },
  { route: '/admin/complaints', label: '投诉处理' },
  { route: '/admin/reviews', label: '内容审核' },
]

const navItems = computed(() => isCs.value && !isAdmin.value ? csNavItems : fullNavItems)

function logout() {
  authStore.clearAuth()
  resetDynamicRoutes()
  router.push('/login')
}
</script>
