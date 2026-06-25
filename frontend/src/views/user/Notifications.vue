<template>
  <div>
    <PageHero title="消息通知" subtitle="查看系统通知" />

    <div style="margin-bottom:16px;display:flex;justify-content:flex-end">
      <button class="btn btn-secondary btn-sm" @click="notificationStore.markAllAsRead()" v-if="unreadCount > 0">全部标为已读</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="notifications.length === 0" class="empty-state">
      <div class="icon">🔔</div>
      <h3>暂无通知</h3>
    </div>
    <div v-else>
      <div v-for="n in notifications" :key="n.id_wsh"
        :class="['card', { 'unread': !n.is_read_wsh }]"
        style="margin-bottom:8px;padding:16px;cursor:pointer"
        @click="handleClick(n)">
        <div style="display:flex;justify-content:space-between;align-items:center;gap:8px">
          <div style="display:flex;align-items:center;gap:8px">
            <span v-if="!n.is_read_wsh" style="width:8px;height:8px;border-radius:50%;background:var(--color-primary);display:inline-block"></span>
            <strong :style="{ color: n.is_read_wsh ? 'var(--color-muted-foreground)' : 'inherit' }">{{ n.title_wsh }}</strong>
          </div>
          <span style="font-size:12px;color:var(--color-muted-foreground);white-space:nowrap">{{ typeLabel(n.type_wsh) }}</span>
        </div>
        <p style="margin-top:8px;font-size:14px;color:var(--color-muted-foreground)">{{ n.content_wsh }}</p>
        <div style="font-size:12px;color:var(--color-muted-foreground);margin-top:4px">{{ new Date(n.created_at_wsh).toLocaleString() }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import PageHero from '@/components/common/PageHero.vue'

const authStore = useAuthStore()
const notificationStore = useNotificationStore()
const loading = ref(true)

const notifications = computed(() => notificationStore.notifications)
const unreadCount = computed(() => notificationStore.unreadCount)

function typeLabel(type) {
  return { system: '系统', order: '订单', adoption: '领养' }[type] || type
}

onMounted(async () => {
  await notificationStore.fetchNotifications()
  loading.value = false
})

onUnmounted(() => {
  notificationStore.stopPolling()
})

function handleClick(n) {
  if (!n.is_read_wsh) {
    notificationStore.markAsRead(n.id_wsh)
  }
}
</script>

<style scoped>
.unread {
  border-left: 3px solid var(--color-primary);
}
</style>
