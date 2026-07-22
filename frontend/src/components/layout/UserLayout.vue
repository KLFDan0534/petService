<template>
  <div class="user-layout" :class="{ 'is-dashboard': route.name === 'Dashboard' }">
    <header class="user-header">
      <div class="user-header-shell">
        <router-link to="/dashboard" class="user-logo" @click="closeMobileMenu">
          <span class="logo-mark" aria-hidden="true"><el-icon><House /></el-icon></span>
          <span>宠物寄养平台</span>
        </router-link>

        <nav
          id="user-navigation"
          class="user-nav"
          :class="{ 'is-open': mobileMenuOpen }"
          aria-label="用户导航"
          @click="closeMobileMenu"
        >
          <router-link to="/dashboard">首页</router-link>
          <router-link to="/services">预约服务</router-link>
          <router-link v-if="!isSupportOnly" to="/pets">我的宠物</router-link>
          <router-link v-if="!isSupportOnly" to="/orders">订单</router-link>
          <router-link v-if="!isSupportOnly" to="/favorites">收藏</router-link>
          <router-link v-if="!isSupportOnly" to="/chat">消息</router-link>
          <router-link v-if="!isSupportOnly" to="/ai">AI助手</router-link>
          <router-link
            v-if="(authStore.hasRole('KEEPER') || authStore.isAdmin) && !isSupportOnly"
            to="/keeper-workflow"
            class="nav-role-action"
          >
            看护工作台
          </router-link>
          <router-link
            v-if="authStore.isMerchant && !isSupportOnly"
            to="/merchant/keepers"
            class="nav-role-action"
          >
            寄养员审核
          </router-link>
          <router-link v-if="!isSupportOnly" to="/merchants" class="nav-role-action">附近商户</router-link>
          <router-link v-if="authStore.isAdmin" to="/admin/dashboard" class="nav-role-action">管理后台</router-link>
          <router-link
            v-if="authStore.isCs && !authStore.isAdmin"
            to="/merchant/support/tickets"
            class="nav-role-action"
          >
            客服中心
          </router-link>
        </nav>

        <div class="header-controls">
          <ThemeToggle />
          <button
            class="header-menu-toggle"
            type="button"
            aria-controls="user-navigation"
            :aria-expanded="mobileMenuOpen"
            :aria-label="mobileMenuOpen ? '关闭导航菜单' : '打开导航菜单'"
            @click="mobileMenuOpen = !mobileMenuOpen"
          >
            <el-icon aria-hidden="true"><Close v-if="mobileMenuOpen" /><Menu v-else /></el-icon>
          </button>
        </div>

        <div class="user-header-end" :class="{ 'is-open': mobileMenuOpen }">
          <router-link
            to="/services"
            class="btn btn-primary header-booking"
            @click="closeMobileMenu"
          >
            <el-icon aria-hidden="true"><Calendar /></el-icon>
            预约服务
          </router-link>
          <template v-if="authStore.isLoggedIn">
            <MessageIndicator />
            <button class="user-avatar" type="button" aria-label="打开个人中心" @click="goProfile">
              <img v-if="avatarUrl" :src="avatarUrl" alt="个人头像">
              <span v-else aria-hidden="true">{{ userInitial }}</span>
            </button>
            <button class="btn btn-sm btn-outline header-logout" type="button" @click="logout">
              <el-icon aria-hidden="true"><SwitchButton /></el-icon>
              退出
            </button>
          </template>
          <template v-else>
            <router-link to="/login" class="btn btn-sm btn-outline" @click="closeMobileMenu">登录</router-link>
            <router-link to="/register" class="btn btn-sm btn-primary" @click="closeMobileMenu">注册</router-link>
          </template>
        </div>
      </div>
    </header>

    <main class="user-content">
      <slot />
    </main>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Calendar, Close, House, Menu, SwitchButton } from '@element-plus/icons-vue'
import MessageIndicator from '@/components/common/MessageIndicator.vue'
import ThemeToggle from '@/components/common/ThemeToggle.vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()
const route = computed(() => router.currentRoute?.value || {})
const mobileMenuOpen = ref(false)

const avatarUrl = computed(() => authStore.user?.avatar_wsh || '')
const userInitial = computed(() => (authStore.user?.nickname_wsh || authStore.user?.username_wsh || '?')[0])
const isSupportOnly = computed(() =>
  authStore.isCs
  && !authStore.isAdmin
  && !authStore.isMerchant
  && !authStore.isOwner
  && !authStore.hasRole('KEEPER')
)

function closeMobileMenu() {
  mobileMenuOpen.value = false
}

function goProfile() {
  closeMobileMenu()
  router.push('/profile')
}

function logout() {
  closeMobileMenu()
  authStore.clearAuth()
  router.push('/login')
}

watch(() => route.value.fullPath, closeMobileMenu)
</script>

<style scoped>
:global(header.user-header) {
  display: block;
  position: fixed;
  top: 0;
  right: 0;
  left: 0;
  z-index: 50;
  width: 100%;
  min-width: 0;
  padding: 0;
  background: var(--color-card);
  border-bottom: 1px solid var(--color-border);
  box-shadow: 0 3px 0 rgba(36, 52, 58, 0.04);
}

.user-layout {
  padding-top: 72px;
}

:global(html),
:global(body),
:global(#app) {
  overflow-x: clip;
  overflow-y: visible;
}

.user-header-shell {
  display: grid;
  width: min(1440px, 100%);
  min-height: 72px;
  grid-template-columns: auto minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 20px;
  margin: 0 auto;
  padding: 0 24px;
}

.user-logo {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  color: var(--color-foreground);
  font-family: Fredoka, 'Nunito', 'Microsoft YaHei', sans-serif;
  font-size: 19px;
  font-weight: 700;
  white-space: nowrap;
}

.user-logo:hover {
  color: var(--color-primary);
}

.logo-mark {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  color: var(--color-on-primary);
  background: var(--color-primary);
  border: 2px solid color-mix(in srgb, var(--color-primary) 75%, #fff);
  border-radius: 12px;
  box-shadow: 3px 4px 0 color-mix(in srgb, var(--color-primary) 35%, var(--color-card));
}

.logo-mark .el-icon {
  font-size: 20px;
}

.user-nav {
  grid-column: 2;
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: center;
  gap: 3px;
  overflow-x: auto;
  scrollbar-width: none;
}

.user-nav::-webkit-scrollbar {
  display: none;
}

.user-nav a {
  display: inline-flex;
  min-height: 44px;
  flex: 0 0 auto;
  align-items: center;
  padding: 9px 12px;
  color: var(--color-foreground);
  border-radius: 10px;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.user-nav a:hover,
.user-nav a.router-link-active:not(.nav-role-action) {
  color: var(--color-primary);
  background: var(--color-muted);
}

.user-nav a.nav-role-action {
  color: var(--color-primary);
  background: transparent;
  border: 1px solid var(--color-border);
}

.user-nav a.nav-role-action:hover,
.user-nav a.nav-role-action.router-link-active {
  color: var(--color-on-primary);
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.header-controls,
.user-header-end {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-controls {
  grid-column: 3;
  order: 3;
}

.header-controls :deep(.theme-toggle) {
  width: 44px;
  height: 44px;
}

.header-menu-toggle {
  display: none;
  width: 44px;
  height: 44px;
  place-items: center;
  color: var(--color-foreground);
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 11px;
}

.header-menu-toggle:hover {
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.header-menu-toggle .el-icon {
  font-size: 20px;
}

.user-header-end {
  grid-column: 4;
  order: 4;
  justify-content: end;
  min-width: max-content;
}

.header-booking {
  min-height: 44px;
  padding: 9px 14px;
  background: var(--color-primary);
  color: var(--color-on-primary);
  border: 2px solid color-mix(in srgb, var(--color-primary) 70%, #fff);
  box-shadow: 3px 4px 0 color-mix(in srgb, var(--color-primary) 35%, var(--color-card));
}

.header-booking:hover {
  color: var(--color-on-primary);
  background: var(--color-secondary);
  transform: translateY(-1px);
}

.user-header-end :deep(.message-indicator) {
  width: 44px;
  height: 44px;
}

.user-avatar {
  display: grid;
  width: 44px;
  height: 44px;
  flex: 0 0 44px;
  place-items: center;
  overflow: hidden;
  color: var(--color-on-primary);
  background: var(--color-primary);
  border: 2px solid color-mix(in srgb, var(--color-primary) 70%, #fff);
  border-radius: 13px;
  font-size: 14px;
  font-weight: 900;
}

.user-avatar:hover {
  transform: translateY(-1px);
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.header-logout {
  min-height: 44px;
}

:global(.user-layout.is-dashboard .user-content) {
  max-width: 1440px;
  padding-top: 0;
}

@media (max-width: 1100px) {
  .user-header-shell {
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 0 12px;
    padding: 12px 16px;
  }

  .header-controls {
    grid-column: 2;
    justify-content: end;
  }

  .header-menu-toggle {
    display: grid;
  }

  .user-nav,
  .user-header-end {
    grid-column: 1 / -1;
    display: none;
  }

  .user-nav.is-open,
  .user-header-end.is-open {
    display: flex;
  }

  .user-nav {
    align-items: stretch;
    flex-direction: column;
    gap: 3px;
    width: 100%;
    padding: 10px 0 4px;
    border-top: 1px solid var(--color-border);
  }

  .user-nav a {
    width: 100%;
    justify-content: space-between;
  }

  .user-header-end {
    align-items: stretch;
    flex-wrap: wrap;
    justify-content: flex-start;
    width: 100%;
    min-width: 0;
    padding: 8px 0 2px;
    border-top: 1px solid var(--color-border);
  }

  .header-booking {
    width: 100%;
    margin-bottom: 4px;
  }
}

@media (max-width: 520px) {
  .user-logo {
    font-size: 17px;
  }

  .logo-mark {
    width: 34px;
    height: 34px;
  }

  .user-header-end > .btn:not(.header-booking) {
    flex: 1;
  }
}
</style>
