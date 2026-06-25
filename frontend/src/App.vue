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
      <div v-for="toast in appStore.toasts" :key="toast.id"
        :class="['toast', `toast-${toast.type}`]">
        {{ toast.message }}
      </div>
    </div>

    <div v-if="showModal" class="modal-overlay" @mousedown.self="closeModal">
      <div class="modal">
        <h2>{{ modalTitle }}</h2>
        <div v-html="modalContent"></div>
        <div class="modal-actions">
          <button class="btn btn-secondary btn-sm" @click="closeModal">取消</button>
          <button v-if="modalConfirm" class="btn btn-primary btn-sm" @click="onModalConfirm">确认</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useNotificationStore } from '@/stores/notification'
import { useAuthStore } from '@/stores/auth'
import UserLayout from '@/components/layout/UserLayout.vue'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import MerchantLayout from '@/components/layout/MerchantLayout.vue'

const route = useRoute()
const appStore = useAppStore()
const authStore = useAuthStore()
const notificationStore = useNotificationStore()

onMounted(() => {
  if (authStore.isLoggedIn) {
    notificationStore.startPolling(30000)
  }
})

onUnmounted(() => {
  notificationStore.stopPolling()
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
</script>
