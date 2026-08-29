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

<style scoped>
.message-indicator {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  color: var(--color-foreground);
  background: transparent;
  border: none;
  border-radius: 11px;
  cursor: pointer;
  transition: color 180ms ease, background 180ms ease;
  position: relative;
}

.message-indicator:hover {
  color: var(--color-primary);
  background: color-mix(in srgb, var(--color-primary) 10%, transparent);
}

.message-indicator .el-icon {
  font-size: 20px;
}

.message-indicator-dot {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-danger, #ef4444);
}
</style>