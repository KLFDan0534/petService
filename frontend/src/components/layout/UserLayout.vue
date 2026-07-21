<template>
  <div class="user-layout">
    <header class="user-header">
      <div class="user-header-start"></div>

      <div class="user-header-inner">
        <router-link to="/dashboard" class="user-logo">宠物寄养平台</router-link>

        <nav class="user-nav">
          <router-link to="/dashboard">首页</router-link>
          <router-link v-if="!isSupportOnly" to="/pets">我的宠物</router-link>
          <router-link v-if="!isSupportOnly" to="/orders">订单</router-link>
          <router-link v-if="!isSupportOnly" to="/favorites">收藏</router-link>
          <router-link v-if="!isSupportOnly" to="/chat">消息</router-link>
          <router-link v-if="!isSupportOnly" to="/ai">AI助手</router-link>
          <router-link
            v-if="(authStore.hasRole('KEEPER') || authStore.isAdmin) && !isSupportOnly"
            to="/keeper-workflow"
            class="btn btn-sm btn-outline"
          >
            看护工作台
          </router-link>
          <router-link
            v-if="authStore.isMerchant && !isSupportOnly"
            to="/merchant/keepers"
            class="btn btn-sm btn-outline"
          >
            寄养员审核
          </router-link>
          <router-link v-if="!isSupportOnly" to="/merchants" class="btn btn-sm btn-outline">附近商户</router-link>
          <router-link v-if="authStore.isAdmin" to="/admin/dashboard" class="btn btn-sm btn-outline">管理后台</router-link>
          <router-link
            v-if="authStore.isCs && !authStore.isAdmin"
            to="/merchant/support/tickets"
            class="btn btn-sm btn-outline"
          >
            客服中心
          </router-link>
        </nav>
      </div>

      <div class="user-header-end">
        <ThemeToggle />
        <template v-if="authStore.isLoggedIn">
          <MessageIndicator />
          <div class="user-avatar" @click="goProfile" title="个人中心">
            <img v-if="avatarUrl" :src="avatarUrl" alt="avatar">
            <span v-else>{{ userInitial }}</span>
          </div>
          <a href="#" class="btn btn-sm btn-outline" @click.prevent="logout">退出</a>
        </template>
        <template v-else>
          <router-link to="/login" class="btn btn-sm btn-outline">登录</router-link>
          <router-link to="/register" class="btn btn-sm btn-primary">注册</router-link>
        </template>
      </div>
    </header>

    <main class="user-content">
      <slot />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import MessageIndicator from '@/components/common/MessageIndicator.vue'
import ThemeToggle from '@/components/common/ThemeToggle.vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()

const avatarUrl = computed(() => authStore.user?.avatar_wsh || '')
const userInitial = computed(() => (authStore.user?.nickname_wsh || authStore.user?.username_wsh || '?')[0])
const isSupportOnly = computed(() =>
  authStore.isCs
  && !authStore.isAdmin
  && !authStore.isMerchant
  && !authStore.isOwner
  && !authStore.hasRole('KEEPER')
)

function goProfile() {
  router.push('/profile')
}

function logout() {
  authStore.clearAuth()
  router.push('/login')
}
</script>
