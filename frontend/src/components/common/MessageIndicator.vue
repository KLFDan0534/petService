<template>
  <button class="message-indicator" type="button" title="消息提醒" @click="goNotifications">
    <el-icon><Message /></el-icon>
    <span v-if="unreadCount > 0" class="message-indicator-dot"></span>
  </button>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Message } from '@element-plus/icons-vue'
import { useNotificationStore } from '@/stores/notification'

const router = useRouter()
const notificationStore = useNotificationStore()
const unreadCount = computed(() => notificationStore.unreadCount)

function goNotifications() {
  router.push('/notifications')
}
</script>
