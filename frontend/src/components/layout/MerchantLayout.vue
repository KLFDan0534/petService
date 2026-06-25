<template>
  <div class="app-layout">
    <aside class="sidebar">
      <div class="sidebar-logo">商户中心</div>
      <div class="sidebar-user">
        <div class="name">{{ authStore.user?.nickname_wsh || authStore.user?.username_wsh }}</div>
        <div class="role">商户</div>
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
      <div class="content-area">
        <slot />
      </div>
    </div>
  </div>
</template>

<script setup>
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { resetDynamicRoutes } from '@/router'

const authStore = useAuthStore()
const router = useRouter()

const navItems = [
  { route: '/merchant/dashboard', label: '控制台' },
  { route: '/merchant/pets', label: '宠物管理' },
  { route: '/merchant/adoptions', label: '领养审核' },
  { route: '/merchant/orders', label: '订单管理' },
  { route: '/keeper-workflow', label: '看护工作台' },
  { route: '/merchant/statistics', label: '数据统计' },
]

function logout() {
  authStore.clearAuth()
  resetDynamicRoutes()
  router.push('/login')
}
</script>
