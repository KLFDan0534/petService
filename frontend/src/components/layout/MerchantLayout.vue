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
import MessageIndicator from '@/components/common/MessageIndicator.vue'
import ThemeToggle from '@/components/common/ThemeToggle.vue'
import { resetDynamicRoutes } from '@/router'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()

const merchantNavItems = [
  { route: '/merchant/dashboard', label: '控制台' },
  { route: '/merchant/profile', label: '商家资料' },
  { route: '/merchant/services', label: '服务管理' },
  { route: '/merchant/business-hours', label: '营业时间' },
  { route: '/merchant/pets', label: '宠物管理' },
  { route: '/merchant/orders', label: '订单管理' },
  { route: '/merchant/keepers', label: '寄养员管理' },
  { route: '/merchant/customer-service', label: '客服管理' },
  { route: '/merchant/support/tickets', label: '支持工单' },
  { route: '/merchant/support/complaints', label: '投诉处理' },
  { route: '/merchant/statistics', label: '数据统计' },
]

const supportNavItems = [
  { route: '/merchant/support/tickets', label: '支持工单' },
  { route: '/merchant/support/complaints', label: '投诉处理' },
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
