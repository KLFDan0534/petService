<template>
  <div class="user-layout" :class="{ 'is-dashboard': route.name === 'Dashboard', 'is-dark': appStore.isDarkTheme }">
    <header class="user-header" :class="{ 'user-header-scrolled': navScrolled, 'is-dashboard-header': route.name === 'Dashboard' }">
      <div class="user-header-shell">
        <router-link to="/dashboard" class="user-logo" @click="closeMobileMenu">
          <span class="logo-mark" aria-hidden="true">栖</span>
          <span>栖屿宠护</span>
        </router-link>

        <nav
          id="user-navigation"
          class="user-nav"
          :class="{ 'is-open': mobileMenuOpen }"
          aria-label="用户导航"
          @click="closeMobileMenu"
        >
          <span ref="indicatorRef" class="nav-indicator" :style="indicatorStyle" />
          <router-link to="/dashboard">首页</router-link>
          <router-link to="/services">预约服务</router-link>
          <router-link v-if="!isSupportOnly" to="/pets">我的宠物</router-link>
          <router-link v-if="!isSupportOnly" to="/orders">订单</router-link>
          <router-link v-if="!isSupportOnly" to="/favorites">收藏</router-link>
          <router-link v-if="!isSupportOnly" to="/chat">消息</router-link>
          <router-link v-if="!isSupportOnly" to="/ai">AI助手</router-link>
          <router-link
            v-if="authStore.isMerchant && !isSupportOnly"
            to="/merchant/keepers"
            class="nav-role-action"
          >
            寄养员审核
          </router-link>
          <router-link v-if="authStore.isAdmin" to="/admin/dashboard" class="nav-role-action">管理后台</router-link>
          <router-link
            v-if="authStore.isCs && !authStore.isAdmin"
            to="/merchant/support/dashboard"
            class="nav-role-action"
          >
            客服中心
          </router-link>
        </nav>

        <div class="user-header-right">
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
              <AppIcon aria-hidden="true"><Close v-if="mobileMenuOpen" /><Menu v-else /></AppIcon>
            </button>
          </div>

          <div class="user-header-end" :class="{ 'is-open': mobileMenuOpen }">
            <template v-if="authStore.isLoggedIn">
              <div class="ai-dropdown-wrapper" @mouseenter="openAiDropdown" @mouseleave="scheduleCloseAiDropdown">
                <router-link to="/ai/chat" class="cs-ai-entry" aria-label="智能客服" title="智能客服">
                  <svg class="cs-ai-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <path d="M12 3v2" />
                    <circle cx="12" cy="2.5" r="1.2" />
                    <rect width="16" height="12" x="4" y="7" rx="2.5" />
                    <circle cx="9.5" cy="13" r="1" />
                    <circle cx="14.5" cy="13" r="1" />
                    <path d="M9 17h6" />
                  </svg>
                </router-link>
                <div v-if="aiDropdownOpen" class="ai-dropdown" @mouseenter="cancelCloseAiDropdown" @mouseleave="scheduleCloseAiDropdown">
                  <div class="ai-dropdown-header">
                    <span>历史对话</span>
                    <router-link to="/ai/chat" class="ai-dropdown-new" @click="aiDropdownOpen = false">新对话</router-link>
                  </div>
                  <div v-if="aiSessionsLoading" class="ai-dropdown-loading">加载中...</div>
                  <div v-else-if="aiSessions.length" class="ai-dropdown-list">
                    <router-link
                      v-for="s in aiSessions.slice(0, 8)"
                      :key="s.session_id_wsh"
                      :to="{ path: '/ai/chat', query: { session: s.session_id_wsh } }"
                      class="ai-dropdown-item"
                      @click="aiDropdownOpen = false"
                    >
                      <span class="ai-dropdown-item-title">{{ s.first_message_wsh || '新对话' }}</span>
                      <span class="ai-dropdown-item-meta">{{ s.message_count_wsh }} 条消息</span>
                    </router-link>
                  </div>
                  <div v-else class="ai-dropdown-empty">暂无历史对话</div>
                  <router-link to="/ai/chat" class="ai-dropdown-footer" @click="aiDropdownOpen = false">查看全部 →</router-link>
                </div>
              </div>
              <MessageIndicator ref="messageIndicatorRef" @open="aiDropdownOpen = false" />
              <button class="user-avatar" type="button" aria-label="打开个人中心" @click="goProfile">
                <img v-if="avatarUrl" :src="avatarUrl" alt="个人头像">
                <span v-else aria-hidden="true">{{ userInitial }}</span>
              </button>
              <button
                class="header-logout"
                type="button"
                aria-label="退出登录"
                title="退出登录"
                @click="logout"
              >
                <AppIcon aria-hidden="true"><SwitchButton /></AppIcon>
              </button>
            </template>
            <template v-else>
              <router-link to="/login" class="btn btn-sm btn-outline" @click="closeMobileMenu">登录</router-link>
              <router-link to="/register" class="btn btn-sm btn-primary" @click="closeMobileMenu">注册</router-link>
            </template>
          </div>
        </div>
      </div>
    </header>

    <main class="user-content">
      <slot />
    </main>

    <footer class="user-footer" aria-label="页脚">
      <div class="user-footer-shell">
        <div class="footer-col footer-brand">
          <div class="footer-logo">
            <span class="logo-mark" aria-hidden="true">栖</span>
            <span>栖屿宠护</span>
          </div>
          <p class="footer-tagline">为您的毛孩子提供一个安全、贴心的寄养之家。</p>
        </div>

        <div class="footer-col">
          <h4 class="footer-heading">联系我们</h4>
          <ul class="footer-list">
            <li><AppIcon aria-hidden="true"><Message /></AppIcon> support@petboarding.com</li>
            <li><AppIcon aria-hidden="true"><Phone /></AppIcon> 400-000-0000</li>
            <li><AppIcon aria-hidden="true"><Clock /></AppIcon> 每日 9:00 – 21:00</li>
            <li><AppIcon aria-hidden="true"><Location /></AppIcon> 上海市浦东新区宠物大道 88 号</li>
          </ul>
        </div>

        <div class="footer-col">
          <h4 class="footer-heading">快捷入口</h4>
          <ul class="footer-list footer-links">
            <li><router-link to="/dashboard" @click="closeMobileMenu">首页</router-link></li>
            <li><router-link to="/services" @click="closeMobileMenu">预约服务</router-link></li>
            <li><router-link to="/merchants" @click="closeMobileMenu">附近商户</router-link></li>
            <li><router-link to="/orders" @click="closeMobileMenu">我的订单</router-link></li>
            <li><router-link to="/tickets" @click="closeMobileMenu">我的工单</router-link></li>
            <li><router-link to="/complaints" @click="closeMobileMenu">投诉</router-link></li>
          </ul>
        </div>

        <div class="footer-col">
          <h4 class="footer-heading">服务数据</h4>
          <ul class="footer-list">
            <li>入驻商家 {{ merchantCount }}+</li>
            <li>注册用户 {{ userCount }}+</li>
            <li>累计完成订单 {{ orderCount }}+</li>
          </ul>
        </div>
      </div>
      <div class="user-footer-bottom">
        <span>© {{ currentYear }} 栖屿宠护 版权所有</span>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Close, Clock, House, Location, Menu, Message, Phone, SwitchButton } from '@element-plus/icons-vue'
import MessageIndicator from '@/components/common/MessageIndicator.vue'
import ThemeToggle from '@/components/common/ThemeToggle.vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'

import { getAiChatSessions } from '@/api/ai'

const authStore = useAuthStore()
const appStore = useAppStore()
const router = useRouter()
const route = computed(() => router.currentRoute?.value || {})
const mobileMenuOpen = ref(false)
const navScrolled = ref(false)
const indicatorRef = ref(null)
const indicatorStyle = ref({ opacity: 0 })

// --- AI 历史对话下拉 ---
const aiDropdownOpen = ref(false)
const aiSessions = ref([])
const aiSessionsLoading = ref(false)
let aiCloseTimer = null
const messageIndicatorRef = ref(null)

async function loadAiSessions() {
  if (aiSessions.value.length > 0) return
  aiSessionsLoading.value = true
  try {
    const r = await getAiChatSessions()
    if (r.code === 200) aiSessions.value = r.data || []
  } catch { /* ignore */ }
  aiSessionsLoading.value = false
}

function openAiDropdown() {
  if (aiCloseTimer) { clearTimeout(aiCloseTimer); aiCloseTimer = null }
  aiDropdownOpen.value = true
  messageIndicatorRef.value?.closeDropdown()
  loadAiSessions()
}

function scheduleCloseAiDropdown() {
  aiCloseTimer = setTimeout(() => { aiDropdownOpen.value = false }, 100)
}

function cancelCloseAiDropdown() {
  if (aiCloseTimer) { clearTimeout(aiCloseTimer); aiCloseTimer = null }
}

function updateIndicator() {
  nextTick(() => {
    const nav = document.getElementById('user-navigation')
    if (!nav) return
    const activeLink = nav.querySelector('a.router-link-active:not(.nav-role-action)')
    if (!activeLink || !indicatorRef.value) {
      indicatorStyle.value = { opacity: 0 }
      return
    }
    const navRect = nav.getBoundingClientRect()
    const linkRect = activeLink.getBoundingClientRect()
    indicatorStyle.value = {
      opacity: 1,
      left: (linkRect.left - navRect.left + nav.scrollLeft) + 'px',
      width: linkRect.width + 'px',
    }
  })
}

function handleScroll() {
  navScrolled.value = window.scrollY > 80
}

onMounted(() => {
  handleScroll()
  window.addEventListener('scroll', handleScroll, { passive: true })
  window.addEventListener('resize', updateIndicator)
  updateIndicator()
  const nav = document.getElementById('user-navigation')
  if (nav) {
    const observer = new MutationObserver(updateIndicator)
    observer.observe(nav, { childList: true, subtree: true, attributes: true, attributeFilter: ['class'] })
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('resize', updateIndicator)
})

const currentYear = new Date().getFullYear()
// 底部「服务数据」展示用静态统计值
const merchantCount = 120
const userCount = 10000
const orderCount = 50000

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
  window.$modal.open('提示', '确定要退出登录吗？', () => {
    closeMobileMenu()
    authStore.clearAuth()
    router.push('/login')
  })
}

watch(() => route.value.fullPath, () => {
  closeMobileMenu()
  updateIndicator()
})
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
  background: transparent;
  border-bottom: 1px solid transparent;
  box-shadow: none;
  transition:
    top 620ms ease,
    background-color 620ms ease,
    border-color 620ms ease,
    box-shadow 620ms ease,
    backdrop-filter 620ms ease,
    -webkit-backdrop-filter 620ms ease;
}

:global(header.user-header.user-header-scrolled) {
  top: 28px;
  background: transparent;
  border-bottom: 1px solid transparent;
  box-shadow: none;
}

.user-layout {
  display: flex;
  min-height: 100vh;
  flex-direction: column;
  padding-top: 80px;
}

.user-content {
  flex: 1;
}

:global(html),
:global(body),
:global(#app) {
  overflow-x: clip;
  overflow-y: visible;
}

.user-header-shell {
  display: grid;
  width: 100%;
  min-height: 80px;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 20px;
  margin: 0 auto;
  padding: 0 24px;
  border: 1px solid transparent;
  border-radius: var(--radius-pill);
  background: transparent;
  box-shadow: none;
  transition:
    width 620ms ease,
    min-height 620ms ease,
    padding 620ms ease,
    border-color 620ms ease,
    background-color 620ms ease,
    box-shadow 620ms ease,
    backdrop-filter 620ms ease,
    -webkit-backdrop-filter 620ms ease;
}

:global(header.user-header.user-header-scrolled .user-header-shell) {
  width: min(1180px, calc(100% - 64px));
  min-height: 68px;
  padding: 0 20px;
  border-radius: 999px;
  border-color: color-mix(in srgb, var(--color-border) 68%, transparent);
  background: color-mix(in srgb, var(--color-card) 72%, transparent);
  box-shadow:
    0 14px 48px rgba(36, 52, 58, 0.12),
    0 1px 0 rgba(255, 255, 255, 0.32) inset;
  backdrop-filter: blur(20px) saturate(1.35);
  -webkit-backdrop-filter: blur(20px) saturate(1.35);
}

.user-logo {
  grid-column: 1;
  justify-self: start;
  display: inline-flex;
  align-items: center;
  gap: 9px;
  color: var(--color-foreground);
  font-family: var(--ref-font-sans);
  font-size: 19px;
  font-weight: 700;
  white-space: nowrap;
}

.user-logo:hover {
  color: var(--color-primary);
}

.logo-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 9px;
  border: 1px solid color-mix(in srgb, var(--ref-ink) 15%, transparent);
  background: var(--ref-surface);
  font-family: var(--ref-font-display);
  font-size: 15px;
  color: var(--ref-ink);
  transition: border-color 0.15s, color 0.15s;
}

.user-logo:hover .logo-mark {
  border-color: color-mix(in srgb, var(--ref-brand) 60%, transparent);
  color: var(--ref-brand);
}

.user-nav {
  grid-column: 2;
  display: flex;
  position: relative;
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

.nav-indicator {
  position: absolute;
  bottom: 2px;
  left: 0;
  height: 3px;
  border-radius: 2px;
  background: var(--color-primary);
  transition: left 300ms cubic-bezier(0.4, 0, 0.2, 1), width 300ms cubic-bezier(0.4, 0, 0.2, 1);
  pointer-events: none;
  z-index: 2;
}

.user-nav a {
  display: inline-flex;
  position: relative;
  z-index: 1;
  min-height: 40px;
  flex: 0 0 auto;
  align-items: center;
  padding: 8px 14px;
  color: var(--color-foreground);
  border-radius: var(--radius-pill);
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  transition: color 250ms ease;
}

.user-nav a:hover {
  color: var(--color-primary);
}

.user-nav a.router-link-active:not(.nav-role-action) {
  color: var(--color-primary);
}

.user-nav a.nav-role-action {
  color: var(--color-primary);
  background: transparent;
  border: 1px solid color-mix(in srgb, var(--color-border) 80%, transparent);
}

.user-nav a.nav-role-action:hover,
.user-nav a.nav-role-action.router-link-active {
  color: var(--color-on-primary);
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.user-header-right {
  grid-column: 3;
  display: flex;
  align-items: center;
  gap: 8px;
  justify-self: end;
  min-width: 0;
}

.header-controls,
.user-header-end {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-controls :deep(.theme-toggle) {
  width: 44px;
  height: var(--control-height);
}

.header-menu-toggle {
  display: none;
  width: 44px;
  height: var(--control-height);
  place-items: center;
  color: var(--color-foreground);
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-control);
}

.header-menu-toggle:hover {
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.header-menu-toggle .el-icon {
  font-size: 20px;
}

.user-header-end {
  justify-content: end;
  min-width: max-content;
}

.user-header-end :deep(.message-indicator) {
  width: 44px;
  height: var(--control-height);
}

.user-avatar {
  display: grid;
  width: 44px;
  height: var(--control-height);
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
  display: grid;
  width: 44px;
  height: var(--control-height);
  place-items: center;
  color: var(--color-foreground);
  background: transparent;
  border: none;
  border-radius: var(--radius-control);
  cursor: pointer;
  transition: color 180ms ease, background 180ms ease;
}

.header-logout:hover {
  color: var(--color-primary);
  background: color-mix(in srgb, var(--color-primary) 10%, transparent);
}

.header-logout .el-icon {
  font-size: 20px;
}

.cs-ai-entry {
  display: grid;
  width: 44px;
  height: var(--control-height);
  flex: 0 0 44px;
  place-items: center;
  color: var(--color-foreground);
}

.cs-ai-entry:hover {
  color: var(--color-primary);
}

.cs-ai-entry .cs-ai-icon {
  width: 20px;
  height: 20px;
}

/* --- AI 历史对话下拉 --- */
.ai-dropdown-wrapper {
  position: relative;
}
.ai-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 280px;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg, 0 8px 24px rgba(0,0,0,0.12));
  z-index: 60;
  overflow: hidden;
}
.ai-dropdown-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid var(--color-border);
  font-size: 13px;
  font-weight: 700;
  color: var(--color-foreground);
}
.ai-dropdown-new {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-primary);
  text-decoration: none;
}
.ai-dropdown-new:hover {
  text-decoration: underline;
}
.ai-dropdown-loading,
.ai-dropdown-empty {
  padding: 16px;
  text-align: center;
  color: var(--color-muted-foreground);
  font-size: 13px;
}
.ai-dropdown-list {
  max-height: 280px;
  overflow-y: auto;
}
.ai-dropdown-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 10px 14px;
  text-decoration: none;
  color: var(--color-foreground);
  border-bottom: 1px solid var(--color-border);
  transition: background 0.15s;
}
.ai-dropdown-item:last-child {
  border-bottom: none;
}
.ai-dropdown-item:hover {
  background: var(--color-muted);
}
.ai-dropdown-item-title {
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
}
.ai-dropdown-item-meta {
  font-size: 11px;
  color: var(--color-muted-foreground);
}
.ai-dropdown-footer {
  display: block;
  padding: 10px 14px;
  text-align: center;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-primary);
  text-decoration: none;
  border-top: 1px solid var(--color-border);
  background: var(--color-muted);
}
.ai-dropdown-footer:hover {
  background: var(--color-border);
}

:global(.user-layout.is-dashboard .user-content) {
  max-width: 1440px;
  padding-top: 0;
  background: transparent;
}

:global(.user-layout.is-dashboard .user-footer) {
  display: none;
}

/* 顶部时所有页面导航栏均透明(与页面 canvas 背景一致);
   首页(Dashboard)滚动后 shell 使用暖色调玻璃胶囊,其余页面使用 --color-card 基调 */
:global(header.user-header.is-dashboard-header.user-header-scrolled .user-header-shell) {
  background: rgba(251, 248, 244, 0.88);
  border-color: rgba(231, 222, 210, 0.6);
  backdrop-filter: blur(20px) saturate(1.35);
  -webkit-backdrop-filter: blur(20px) saturate(1.35);
  box-shadow: 0 14px 48px rgba(36, 52, 58, 0.08);
}

.user-layout.is-dark header.user-header.user-header-scrolled .user-header-shell {
  background: rgba(16, 17, 20, 0.88);
  border-color: rgba(52, 57, 70, 0.6);
}

.user-layout.is-dark {
  background: var(--ref-canvas);
}

.user-layout.is-dark .user-content {
  background: transparent;
}

.user-layout.is-dark header.user-header {
  background: transparent;
  border-bottom-color: transparent;
}

@media (max-width: 1100px) {
  .user-header-shell {
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 0 12px;
    padding: 12px 16px;
    border-radius: var(--radius-md);
  }

  .user-logo {
    grid-column: 1;
  }

  .user-header-right {
    grid-column: 2;
    justify-self: end;
  }

  .header-menu-toggle {
    display: grid;
  }

  :global(header.user-header.user-header-scrolled .user-header-shell) {
    border-radius: var(--radius-md);
  }

  .user-nav {
    grid-column: 1 / -1;
    display: none;
  }

  .user-nav.is-open {
    display: flex;
  }

  .user-nav .nav-indicator {
    display: none;
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

  .user-nav a.router-link-active:not(.nav-role-action) {
    color: var(--color-primary);
    background: color-mix(in srgb, var(--color-primary) 12%, transparent);
  }

  .user-nav a.nav-bottom {
    margin-top: 4px;
    padding-top: 12px;
    border-top: 1px solid var(--color-border);
  }

  .user-header-end {
    display: none;
    align-items: stretch;
    flex-wrap: wrap;
    justify-content: flex-start;
    width: 100%;
    min-width: 0;
    padding: 8px 0 2px;
    border-top: 1px solid var(--color-border);
  }

  .user-header-right {
    align-items: stretch;
    flex-wrap: wrap;
  }

  .user-header-end.is-open {
    display: flex;
  }
}

@media (max-width: 520px) {
  .user-logo {
    font-size: 17px;
  }

  .logo-mark {
    width: 32px;
    height: 32px;
    font-size: 14px;
  }

  .user-header-end > .btn {
    flex: 1;
  }
}

/* ====== 底部导航/联系我们 ====== */
.user-footer {
  width: 100%;
  margin-top: 48px;
  background: var(--ref-canvas);
  border-top: 1px solid var(--color-border);
}

.user-footer-shell {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 32px;
  width: min(1180px, calc(100% - 48px));
  margin: 0 auto;
  padding: 40px 0 28px;
}

.footer-logo {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  color: var(--color-foreground);
  font-family: var(--ref-font-sans);
  font-size: 18px;
  font-weight: 700;
}

.footer-tagline {
  margin-top: 12px;
  color: var(--color-muted-foreground);
  font-size: 13px;
  line-height: 1.7;
}

.footer-heading {
  margin-bottom: 12px;
  color: var(--color-foreground);
  font-size: 14px;
  font-weight: 700;
}

.footer-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
  color: var(--color-muted-foreground);
  font-size: 13px;
}

.footer-list li {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.footer-list .el-icon {
  flex: 0 0 auto;
  font-size: 14px;
  color: var(--color-primary);
}

.footer-links a {
  color: var(--color-muted-foreground);
}

.footer-links a:hover {
  color: var(--color-primary);
}

.user-footer-bottom {
  padding: 14px 24px;
  border-top: 1px solid var(--color-border);
  color: var(--color-muted-foreground);
  font-size: 12px;
  text-align: center;
}

@media (max-width: 900px) {
  .user-footer-shell {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 24px;
    width: calc(100% - 32px);
    padding: 32px 0 20px;
  }
}

@media (max-width: 520px) {
  .user-footer-shell {
    grid-template-columns: 1fr;
    gap: 20px;
  }
}
</style>
