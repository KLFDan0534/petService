<template>
  <div>
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
        @click="openDetail(n)">
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

    <!-- 通知详情弹窗 -->
    <div v-if="detailNotice" class="detail-modal-overlay" @mousedown.self="closeDetail">
      <div class="detail-modal" role="dialog" aria-modal="true" aria-labelledby="detail-title">
        <div class="detail-header">
          <h3 id="detail-title">{{ detailNotice.title_wsh }}</h3>
          <button class="detail-close" type="button" aria-label="关闭" @click="closeDetail">&times;</button>
        </div>
        <div class="detail-body">
          <div class="detail-meta">
            <span class="detail-type">{{ typeLabel(detailNotice.type_wsh) }}</span>
            <span class="detail-time">{{ new Date(detailNotice.created_at_wsh).toLocaleString() }}</span>
          </div>
          <div class="detail-content" v-html="detailNotice.content_wsh"></div>
        </div>
        <div class="detail-footer">
          <button class="btn btn-primary" @click="closeDetail">我知道了</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'

const authStore = useAuthStore()
const notificationStore = useNotificationStore()
const loading = ref(true)
const detailNotice = ref(null)

const notifications = computed(() => notificationStore.notifications)
const unreadCount = computed(() => notificationStore.unreadCount)

function typeLabel(type) {
  return { system: '系统', order: '订单' }[type] || type
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

function openDetail(n) {
  handleClick(n)
  detailNotice.value = n
}

function closeDetail() {
  detailNotice.value = null
}
</script>

<style scoped>
.unread {
  border-left: 3px solid var(--color-primary);
}

.loading {
  padding: 24px;
  text-align: center;
  color: var(--color-muted-foreground);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  color: var(--color-muted-foreground);
}

.empty-state .icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-state h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

/* 详情弹窗样式 */
.detail-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 24px;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.detail-modal {
  background: var(--color-card, #fff);
  border-radius: 12px;
  max-width: 520px;
  width: 100%;
  max-height: 90vh;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  animation: slideUp 0.2s ease;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
}

.detail-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--color-foreground, #111827);
}

.detail-close {
  background: none;
  border: none;
  font-size: 24px;
  color: var(--color-muted-foreground, #9ca3af);
  cursor: pointer;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background 0.2s ease, color 0.2s ease;
}

.detail-close:hover {
  background: var(--color-bg-tertiary, #f3f4f6);
  color: var(--color-foreground, #111827);
}

.detail-body {
  padding: 24px;
  max-height: 60vh;
  overflow-y: auto;
}

.detail-meta {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.detail-type {
  font-size: 12px;
  color: var(--color-primary, #2563eb);
  background: color-mix(in srgb, var(--color-primary) 12%, transparent);
  padding: 4px 10px;
  border-radius: 20px;
  font-weight: 500;
}

.detail-time {
  font-size: 13px;
  color: var(--color-muted-foreground, #9ca3af);
}

.detail-content {
  font-size: 15px;
  line-height: 1.7;
  color: var(--color-foreground, #111827);
  white-space: pre-wrap;
  word-wrap: break-word;
}

.detail-footer {
  padding: 16px 24px;
  border-top: 1px solid var(--color-border, #e5e7eb);
  display: flex;
  justify-content: flex-end;
}

.detail-footer .btn {
  min-width: 100px;
}
</style>