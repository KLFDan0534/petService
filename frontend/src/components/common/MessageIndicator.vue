<template>
  <div class="msg-dropdown-wrapper" @mouseenter="openDropdown" @mouseleave="scheduleClose">
    <button class="message-indicator" type="button" title="消息提醒" @click="goNotifications">
      <AppIcon><Message /></AppIcon>
      <span v-if="unreadCount > 0" class="message-indicator-dot"></span>
    </button>
    <div v-if="dropdownOpen" class="msg-dropdown" @mouseenter="cancelClose" @mouseleave="scheduleClose">
      <div class="msg-dropdown-header">
        <span>最新消息</span>
        <button v-if="unreadCount > 0" type="button" class="msg-dropdown-all" @click="notificationStore.markAllAsRead()">全部已读</button>
      </div>
      <div v-if="loading" class="msg-dropdown-loading">加载中...</div>
      <div v-else-if="notifications.length" class="msg-dropdown-list">
        <router-link
          v-for="n in notifications.slice(0, 8)"
          :key="n.id_wsh"
          to="/notifications"
          class="msg-dropdown-item"
          @click="handleItemClick(n)"
        >
          <span class="msg-dropdown-item-top">
            <span class="msg-dropdown-item-title" :class="{ unread: !n.is_read_wsh }">{{ n.title_wsh || '通知' }}</span>
            <span v-if="!n.is_read_wsh" class="msg-dropdown-item-unread">未读</span>
          </span>
          <span v-if="n.content_wsh" class="msg-dropdown-item-content">{{ n.content_wsh }}</span>
          <span class="msg-dropdown-item-time">{{ formatTime(n.created_at_wsh) }}</span>
        </router-link>
      </div>
      <div v-else class="msg-dropdown-empty">暂无新消息</div>
      <router-link to="/notifications" class="msg-dropdown-footer" @click="dropdownOpen = false">查看全部 →</router-link>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Message } from '@element-plus/icons-vue'
import { useNotificationStore } from '@/stores/notification'

const router = useRouter()
const notificationStore = useNotificationStore()
const unreadCount = computed(() => notificationStore.unreadCount)
const notifications = computed(() => notificationStore.notifications)

// --- 悬浮预览下拉 ---
const emit = defineEmits(['open'])
const dropdownOpen = ref(false)
const loading = ref(false)
let closeTimer = null

async function loadNotifications() {
  loading.value = true
  await notificationStore.fetchNotifications()
  loading.value = false
}

function openDropdown() {
  if (closeTimer) { clearTimeout(closeTimer); closeTimer = null }
  dropdownOpen.value = true
  emit('open')
  loadNotifications()
}

function scheduleClose() {
  closeTimer = setTimeout(() => { dropdownOpen.value = false }, 100)
}

function cancelClose() {
  if (closeTimer) { clearTimeout(closeTimer); closeTimer = null }
}

function closeDropdown() {
  if (closeTimer) { clearTimeout(closeTimer); closeTimer = null }
  dropdownOpen.value = false
}

defineExpose({ closeDropdown })

function handleItemClick(n) {
  dropdownOpen.value = false
  if (!n.is_read_wsh) notificationStore.markAsRead(n.id_wsh)
}

function formatTime(value) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return ''
  const diff = Date.now() - d.getTime()
  const MIN = 60 * 1000
  const HOUR = 60 * MIN
  const DAY = 24 * HOUR
  if (diff < MIN) return '刚刚'
  if (diff < HOUR) return `${Math.floor(diff / MIN)} 分钟前`
  if (diff < DAY) return `${Math.floor(diff / HOUR)} 小时前`
  if (diff < 7 * DAY) return `${Math.floor(diff / DAY)} 天前`
  const pad = (v) => String(v).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function goNotifications() {
  dropdownOpen.value = false
  router.push('/notifications')
}

onBeforeUnmount(() => {
  if (closeTimer) { clearTimeout(closeTimer); closeTimer = null }
})
</script>

<style scoped>
/* --- 悬浮预览下拉 --- */
.msg-dropdown-wrapper {
  position: relative;
}
.msg-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 300px;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg, 0 8px 24px rgba(0,0,0,0.12));
  z-index: 60;
  overflow: hidden;
}
.msg-dropdown-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid var(--color-border);
  font-size: 13px;
  font-weight: 700;
  color: var(--color-foreground);
}
.msg-dropdown-all {
  padding: 0;
  background: none;
  border: none;
  font-size: 12px;
  font-weight: 600;
  font-family: inherit;
  color: var(--color-primary);
  cursor: pointer;
}
.msg-dropdown-all:hover {
  text-decoration: underline;
}
.msg-dropdown-loading,
.msg-dropdown-empty {
  padding: 16px;
  text-align: center;
  color: var(--color-muted-foreground);
  font-size: 13px;
}
.msg-dropdown-list {
  max-height: 320px;
  overflow-y: auto;
}
.msg-dropdown-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 10px 14px;
  text-decoration: none;
  color: var(--color-foreground);
  border-bottom: 1px solid var(--color-border);
  transition: background 0.15s;
}
.msg-dropdown-item:last-child {
  border-bottom: none;
}
.msg-dropdown-item:hover {
  background: var(--color-muted);
}
.msg-dropdown-item-top {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}
.msg-dropdown-item-title {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
}
.msg-dropdown-item-title.unread {
  font-weight: 700;
}
.msg-dropdown-item-unread {
  flex-shrink: 0;
  padding: 1px 6px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--color-danger, #ef4444) 12%, transparent);
  color: var(--color-danger, #ef4444);
  font-size: 10px;
  font-weight: 600;
  line-height: 1.4;
}
.msg-dropdown-item-content {
  font-size: 12px;
  color: var(--color-muted-foreground);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.5;
}
.msg-dropdown-item-time {
  font-size: 11px;
  color: var(--color-muted-foreground);
}
.msg-dropdown-footer {
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
.msg-dropdown-footer:hover {
  background: var(--color-border);
}

.message-indicator {
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