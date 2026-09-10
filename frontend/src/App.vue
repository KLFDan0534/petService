<template>
  <div id="app-root">
    <UserLayout v-if="route.meta?.layout === 'user'">
      <router-view v-slot="{ Component }">
        <keep-alive :include="keepAliveNames">
          <component :is="Component" />
        </keep-alive>
      </router-view>
    </UserLayout>
    <AdminLayout v-else-if="route.meta?.layout === 'admin'">
      <router-view />
    </AdminLayout>
    <MerchantLayout v-else-if="route.meta?.layout === 'merchant'">
      <router-view />
    </MerchantLayout>
    <router-view v-else />

    <div class="toast-container">
      <div
        v-for="toast in appStore.toasts"
        :key="toast.id"
        :class="['toast', `toast-${toast.type}`]"
      >
        {{ toast.message }}
      </div>v
    </div>

    <AppDialog :visible="showModal" :width="500" :title="modalTitle" @close="closeModal">
      <div class="modal-content">{{ modalContent }}</div>
      <template #footer>
        <button class="btn btn-secondary btn-sm" type="button" @click="closeModal">取消</button>
        <button v-if="modalConfirm" class="btn btn-primary btn-sm" type="button" @click="onModalConfirm">确认</button>
      </template>
    </AppDialog>

    <PopupNotice />
    <LoginPromptDialog :visible="appStore.showLoginPrompt" @close="appStore.closeLoginPrompt()" />

    <!-- 客服新消息右上角提醒 -->
    <transition name="cs-notice-fade">
      <button
        v-if="csChatStore.incomingNotice"
        class="cs-incoming-notice"
        type="button"
        @click="onCsNoticeClick"
      >
        <span class="cs-incoming-notice__title">📬 新消息</span>
        <span class="cs-incoming-notice__name"><strong>{{ csChatStore.incomingNotice.name }}</strong></span>
        <span class="cs-incoming-notice__preview">{{ csChatStore.incomingNotice.preview }}</span>
      </button>
    </transition>
  </div>
</template>

<script setup>
import { computed, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useNotificationStore } from '@/stores/notification'
import { useAuthStore } from '@/stores/auth'
import { useCsChatStore } from '@/stores/csChat'
import { useRouter } from 'vue-router'
import UserLayout from '@/components/layout/UserLayout.vue'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import MerchantLayout from '@/components/layout/MerchantLayout.vue'
import PopupNotice from '@/components/common/PopupNotice.vue'
import LoginPromptDialog from '@/components/common/LoginPromptDialog.vue'
import AppDialog from '@/components/common/AppDialog.vue'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const authStore = useAuthStore()
const notificationStore = useNotificationStore()
const csChatStore = useCsChatStore()

watch(
  () => authStore.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) {
      notificationStore.startPolling(30000)
      // 客服上线即监听全局用户消息（任意页面都能提醒/发声）
      if (authStore.isCs) csChatStore.connect()
      return
    }
    notificationStore.stopPolling()
    csChatStore.disconnect()
  },
  { immediate: true },
)

onUnmounted(() => {
  notificationStore.stopPolling()
  csChatStore.disconnect()
})

const keepAliveNames = computed(() => {
  if (route.meta?.keepAlive && route.name) {
    return [route.name]
  }
  return []
})

const showModal = ref(false)
const modalTitle = ref('')
const modalContent = ref('')
const modalConfirm = ref(null)

function openModal(title, content, onConfirm) {
  modalTitle.value = title
  modalContent.value = content
  modalConfirm.value = onConfirm || null
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  modalTitle.value = ''
  modalContent.value = ''
  modalConfirm.value = null
}

function onModalConfirm() {
  if (modalConfirm.value) modalConfirm.value()
  closeModal()
}

window.$modal = { open: openModal, close: closeModal }
window.$toast = (msg, type) => appStore.addToast(msg, type)

// 点击客服新消息浮窗：跳转到聊天界面并定位到对应会话
function onCsNoticeClick() {
  const notice = csChatStore.incomingNotice
  if (!notice) return
  csChatStore.dismissNotice()
  if (notice.type === 'thread') {
    router.push({
      path: '/merchant/support/chat',
      query: notice.threadKey
        ? { bizType: notice.threadKey.split('-')[0], bizId: notice.threadKey.split('-')[1], name: notice.name }
        : undefined,
    })
  } else {
    router.push({ path: '/merchant/support/chat', query: { userId: notice.userId, name: notice.name } })
  }
}
</script>

<style>
/* 客服新消息右上角浮窗（全局） */
.cs-incoming-notice {
  position: fixed;
  top: 16px;
  right: 16px;
  z-index: 9999;
  display: grid;
  gap: 2px;
  min-width: 240px;
  max-width: min(360px, calc(100vw - 32px));
  padding: 12px 16px;
  text-align: left;
  color: #fff;
  background: #1F2937;
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-left: 4px solid var(--color-primary);
  border-radius: var(--radius-md);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.32);
  cursor: pointer;
  font-family: inherit;
}
.cs-incoming-notice:hover {
  background: #111827;
  transform: translateY(-2px);
}
.cs-incoming-notice__title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 700;
  color: #FBBF24;
}
.cs-incoming-notice__name {
  font-size: 14px;
  color: #fff;
}
.cs-incoming-notice__preview {
  font-size: 13px;
  color: #d1d5db;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cs-notice-fade-enter-active,
.cs-notice-fade-leave-active {
  transition: opacity 200ms ease, transform 200ms ease;
}
.cs-notice-fade-enter-from,
.cs-notice-fade-leave-to {
  opacity: 0;
  transform: translateX(120px);
}
</style>
