<template>
  <div class="app-layout">
    <aside class="sidebar">
      <div class="sidebar-logo">{{ isSupportOnly ? '客服中心' : '商户中心' }}</div>
      <div class="sidebar-user">
        <div class="name">{{ authStore.user?.nickname_wsh || authStore.user?.username_wsh }}</div>
        <div class="role">{{ isSupportOnly ? '商家客服' : '商户' }}</div>
      </div>

      <nav class="sidebar-nav">
        <router-link v-for="item in navItems" :key="item.route" :to="item.route" active-class="active">
          <span class="nav-icon" aria-hidden="true">
            <el-icon><component :is="item.icon" /></el-icon>
          </span>
          <span>{{ item.label }}</span>
        </router-link>
      </nav>

      <div style="padding:12px 20px;border-top:1px solid rgba(255,255,255,.08)">
        <a href="#" style="color:rgba(255,255,255,0.5);font-size:13px" @click.prevent="logout">退出登录</a>
        <router-link to="/dashboard" style="display:block;margin-top:8px;color:rgba(255,255,255,0.5);font-size:13px">
          返回首页
        </router-link>
      </div>
    </aside>

    <div class="main-content">
      <div class="top-bar">
        <div class="breadcrumb"><strong>{{ isSupportOnly ? '客服中心' : '商户中心' }}</strong></div>
        <div class="top-bar-right">
          <ThemeToggle />
          <MessageIndicator />
        </div>
      </div>
      <div class="content-area">
        <slot />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  Avatar,
  ChatDotRound,
  Clock,
  DataBoard,
  Goods,
  Headset,
  List,
  Odometer,
  Shop,
  Tickets,
  TrendCharts,
  User,
  Warning,
} from '@element-plus/icons-vue'
import MessageIndicator from '@/components/common/MessageIndicator.vue'
import ThemeToggle from '@/components/common/ThemeToggle.vue'
import { resetDynamicRoutes } from '@/router'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()

const merchantNavItems = [
  { route: '/merchant/dashboard', label: '控制台', icon: Odometer },
  { route: '/merchant/profile', label: '商家资料', icon: Shop },
  { route: '/merchant/services', label: '服务管理', icon: Goods },
  { route: '/merchant/business-hours', label: '营业时间', icon: Clock },
  { route: '/merchant/pets', label: '宠物管理', icon: Avatar },
  { route: '/merchant/orders', label: '订单管理', icon: List },
  { route: '/merchant/keepers', label: '寄养员管理', icon: User },
  { route: '/merchant/customer-service', label: '客服管理', icon: Headset },
  { route: '/merchant/support/dashboard', label: '客服工作台', icon: DataBoard },
  { route: '/merchant/support/tickets', label: '支持工单', icon: Tickets },
  { route: '/merchant/support/complaints', label: '投诉处理', icon: Warning },
  { route: '/merchant/statistics', label: '数据统计', icon: TrendCharts },
]

const supportNavItems = [
  { route: '/merchant/support/dashboard', label: '工作台', icon: DataBoard },
  { route: '/merchant/support/tickets', label: '支持工单', icon: Tickets },
  { route: '/merchant/support/complaints', label: '投诉处理', icon: Warning },
  { route: '/merchant/support/chat', label: '实时聊天', icon: ChatDotRound },
]

const isSupportOnly = computed(() =>
  authStore.isCs
  && !authStore.isAdmin
  && !authStore.isMerchant
)
const navItems = computed(() => (isSupportOnly.value ? supportNavItems : merchantNavItems))

function logout() {
  authStore.clearAuth()
  resetDynamicRoutes()
  router.push('/login')
}
</script>
