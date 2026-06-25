<template>
  <div class="user-layout">
    <header class="user-header">
      <div class="user-header-inner">
        <router-link to="/dashboard" class="user-logo">
          宠物寄养平台
        </router-link>
        <nav class="user-nav">
          <router-link to="/dashboard">首页</router-link>
          <router-link to="/pets">我的宠物</router-link>
          <router-link to="/orders">订单</router-link>
          <router-link to="/adoptions">宠物领养</router-link>
          <router-link v-if="authStore.isLoggedIn" to="/my-adoptions">我的领养</router-link>
          <router-link to="/chat">消息</router-link>
          <router-link to="/ai">AI助手</router-link>
          <router-link v-if="authStore.hasRole('KEEPER') || authStore.isMerchant || authStore.isAdmin" to="/keeper-workflow" class="btn btn-sm btn-outline">看护工作台</router-link>
          <router-link v-if="authStore.isMerchant || authStore.isAdmin" to="/merchant/dashboard" class="btn btn-sm btn-outline">商户中心</router-link>
          <router-link v-if="authStore.isAdmin" to="/admin/dashboard" class="btn btn-sm btn-outline">管理后台</router-link>
          <router-link v-if="authStore.isCs && !authStore.isAdmin" to="/admin/tickets" class="btn btn-sm btn-outline">客服中心</router-link>
          <div class="user-avatar" @click="goProfile" style="cursor:pointer">
            {{ (authStore.user?.nickname_wsh || authStore.user?.username_wsh || '?')[0] }}
          </div>
          <a v-if="authStore.isLoggedIn" href="#" @click.prevent="logout" class="btn btn-sm btn-outline" style="margin-left:8px">退出登录</a>
        </nav>
      </div>
    </header>
    <main class="user-content">
      <slot />
    </main>
  </div>
</template>

<script setup>
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const router = useRouter()
function goProfile() { router.push('/profile') }
function logout() {
  authStore.clearAuth()
  router.push('/login')
}
</script>
