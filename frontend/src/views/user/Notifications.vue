<template>
  <div class="nt-page">
    <div class="nt-shell">
      <!-- ═══ Breadcrumb ═══ -->
      <nav class="nt-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="nt-crumb-link">首页</router-link>
        <span class="nt-crumb-sep" aria-hidden="true">›</span>
        <router-link to="/profile" class="nt-crumb-link">个人中心</router-link>
        <span class="nt-crumb-sep" aria-hidden="true">›</span>
        <span class="nt-crumb-here">站内通知</span>
      </nav>

      <!-- ═══ Header ═══ -->
      <header class="nt-head">
        <div class="nt-head-copy">
          <div class="nt-eyebrow" aria-hidden="true">
            <span class="nt-eyebrow-line" />
            <span>Notifications</span>
          </div>
          <h1 class="nt-title">站内通知</h1>
          <p class="nt-sub">照护日报、订单进度与平台公告都会推送到这里。未读消息排在前面标注。</p>
          <dl class="nt-stats" aria-label="通知概览">
            <div>
              <dd>{{ loading ? '--' : notifications.length }}</dd>
              <dt>全部</dt>
            </div>
            <div class="nt-stat-bordered">
              <dd>{{ loading ? '--' : unreadCount }}</dd>
              <dt>未读</dt>
            </div>
          </dl>
        </div>
        <button
          v-if="unreadCount > 0"
          type="button"
          class="cta cta-primary nt-markall"
          @click="notificationStore.markAllAsRead()"
        >
          全部标为已读
        </button>
      </header>

      <!-- ═══════════════════════════════════════════
           01 · Inbox
           ═══════════════════════════════════════════ -->
      <section class="nt-section" aria-labelledby="nt-inbox-title">
        <header class="nt-sec-head">
          <p class="nt-sec-eyebrow" aria-hidden="true">
            <span class="nt-idx">01</span>
            <span class="nt-sec-line" />
            <span>Inbox</span>
          </p>
          <h2 id="nt-inbox-title" class="nt-sec-title">消息列表</h2>
          <p class="nt-sec-desc">点开任意一条查看完整内容。</p>
        </header>

        <!-- loading -->
        <div v-if="loading" class="nt-skeleton-list" aria-hidden="true">
          <div v-for="i in 4" :key="i" class="nt-skeleton" />
        </div>

        <!-- empty -->
        <div v-else-if="notifications.length === 0" class="nt-empty">
          <svg class="nt-empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
            <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
          </svg>
          <h3>暂无通知</h3>
          <p>下单后，照护日报与订单进度提醒会出现在这里。</p>
          <router-link to="/services" class="cta cta-outline">浏览照护服务</router-link>
        </div>

        <!-- list -->
        <ul v-else class="nt-list">
          <li v-for="n in notifications" :key="n.id_wsh">
            <button
              type="button"
              class="nt-row"
              :class="{ unread: !n.is_read_wsh }"
              @click="openDetail(n)"
            >
              <span class="nt-dot" :class="{ 'is-unread': !n.is_read_wsh }" aria-hidden="true" />
              <span class="nt-main">
                <span class="nt-row-top">
                  <span class="nt-ntitle" :class="{ unread: !n.is_read_wsh }">{{ n.title_wsh || '通知' }}</span>
                  <span class="nt-tag">{{ typeLabel(n.type_wsh) }}</span>
                  <span v-if="!n.is_read_wsh" class="nt-unread-label">未读</span>
                </span>
                <span v-if="n.content_wsh" class="nt-summary">{{ n.content_wsh }}</span>
              </span>
              <span class="nt-time">{{ new Date(n.created_at_wsh).toLocaleString() }}</span>
            </button>
          </li>
        </ul>
      </section>
    </div>

    <!-- ═══════════════════════════════════════════
         通知详情弹窗
         ═══════════════════════════════════════════ -->
    <div v-if="detailNotice" class="dlg-overlay" @mousedown.self="closeDetail">
      <div class="dlg-panel" role="dialog" aria-modal="true" aria-labelledby="nt-detail-title">
        <div class="dlg-head">
          <h3 id="nt-detail-title" class="dlg-title">{{ detailNotice.title_wsh || '通知详情' }}</h3>
          <button type="button" class="dlg-close" aria-label="关闭" @click="closeDetail">✕</button>
        </div>
        <div class="dlg-body">
          <div class="dlg-meta">
            <span class="dlg-tag">{{ typeLabel(detailNotice.type_wsh) }}</span>
            <span class="dlg-time">{{ new Date(detailNotice.created_at_wsh).toLocaleString() }}</span>
          </div>
          <p class="dlg-content">{{ detailNotice.content_wsh || '暂无内容' }}</p>
        </div>
        <div class="dlg-foot">
          <button type="button" class="cta cta-primary" @click="closeDetail">我知道了</button>
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
/* ═══════════════════════════════════════════════════════
   Uses shared --ref-* tokens from assets/css/design-tokens.css.
   Dark mode is handled globally via html[data-theme="dark"].
   ═══════════════════════════════════════════════════════ */
.nt-page {
  width: 100%;
  padding: 6px 0 72px;
  min-height: 60vh;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}

.nt-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ═══ Breadcrumb ═══ */
.nt-crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 12.5px;
  color: var(--ref-muted);
}
.nt-crumb-link { color: var(--ref-muted); text-decoration: none; }
.nt-crumb-link:hover { color: var(--ref-ink); }
.nt-crumb-sep { color: var(--ref-line); }
.nt-crumb-here { color: var(--ref-ink-soft); }

/* ═══ Eyebrow ═══ */
.nt-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
}
.nt-eyebrow-line {
  width: 32px;
  height: 1px;
  background: var(--ref-line);
}
.nt-eyebrow > span:last-child {
  font-size: 9.5px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: var(--ref-muted);
}

/* ═══ Header ═══ */
.nt-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding: 40px 0 8px;
}
.nt-head-copy { min-width: 0; }
.nt-title {
  margin: 18px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(34px, 4.4vw, 52px);
  line-height: 1.12;
  letter-spacing: -0.01em;
  font-weight: 500;
  color: var(--ref-ink);
  text-wrap: balance;
}
.nt-sub {
  margin: 14px 0 0;
  max-width: 600px;
  font-size: 14px;
  line-height: 1.75;
  color: color-mix(in srgb, var(--ref-ink-soft) 82%, transparent);
}
.nt-stats {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 0 28px;
  margin: 28px 0 0;
}
.nt-stats div { display: flex; flex-direction: column; }
.nt-stat-bordered { border-left: 1px solid var(--ref-line); padding-left: 28px; }
.nt-stats dd {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 28px;
  line-height: 1;
  letter-spacing: -0.01em;
  color: var(--ref-ink);
  font-variant-numeric: tabular-nums;
}
.nt-stats dt { margin-top: 8px; font-size: 12px; color: var(--ref-muted); }
.nt-markall { flex-shrink: 0; margin-bottom: 6px; }

/* ═══ CTA ═══ */
.cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 42px;
  padding: 0 18px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid transparent;
  text-decoration: none;
  transition: background 0.15s, border-color 0.15s, color 0.15s, transform 0.15s;
}
.cta:hover { transform: translateY(-1px); }
.cta:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
.cta-primary { background: var(--ref-brand); color: #fff; }
.cta-primary:hover:not(:disabled) { background: var(--ref-brand-deep); }
.cta-outline { background: var(--ref-surface); color: var(--ref-ink); border-color: var(--ref-line); }
.cta-outline:hover { border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent); }

/* ═══ Section ═══ */
.nt-section { margin-top: 36px; }
.nt-sec-head { padding-bottom: 24px; }
.nt-sec-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  font-size: 10px;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.nt-idx { font-variant-numeric: tabular-nums; }
.nt-sec-line { width: 24px; height: 1px; background: var(--ref-line); }
.nt-sec-title {
  margin: 10px 0 0;
  font-family: var(--ref-font-display);
  font-size: 24px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: var(--ref-ink);
}
.nt-sec-desc { margin: 8px 0 0; font-size: 13px; color: var(--ref-muted); }

/* ═══ List ═══ */
.nt-list {
  margin: 0;
  padding: 0;
  list-style: none;
  overflow: hidden;
  border: 1px solid var(--ref-line);
  border-radius: 14px;
  background: var(--ref-surface);
}
.nt-row {
  display: flex;
  width: 100%;
  align-items: flex-start;
  gap: 16px;
  padding: 18px 20px;
  border: 0;
  border-bottom: 1px solid var(--ref-line);
  background: transparent;
  text-align: left;
  cursor: pointer;
  font: inherit;
  color: inherit;
  transition: background 0.15s;
}
.nt-row:last-child { border-bottom: 0; }
.nt-row:hover { background: color-mix(in srgb, var(--ref-cream) 50%, transparent); }
.nt-row.unread { background: color-mix(in srgb, var(--ref-sand) 35%, transparent); }
.nt-row.unread:hover { background: color-mix(in srgb, var(--ref-sand) 55%, transparent); }
.nt-dot {
  width: 8px;
  height: 8px;
  margin-top: 7px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--ref-line);
}
.nt-dot.is-unread { background: var(--ref-brand); }
.nt-main { min-width: 0; flex: 1; }
.nt-row-top {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 12px;
}
.nt-ntitle {
  max-width: 100%;
  font-size: 14px;
  letter-spacing: -0.01em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.nt-ntitle.unread { font-weight: 500; color: var(--ref-ink); }
.nt-ntitle:not(.unread) { color: var(--ref-ink-soft); }
.nt-tag {
  display: inline-flex;
  align-items: center;
  border-radius: 6px;
  border: 1px solid var(--ref-line);
  background: var(--ref-surface);
  padding: 3px 8px;
  font-size: 11px;
  font-weight: 500;
  line-height: 1;
  color: var(--ref-ink-soft);
}
.nt-unread-label { font-size: 11px; font-weight: 500; color: var(--ref-brand); }
.nt-summary {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-top: 6px;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--ref-muted);
}
.nt-time {
  flex-shrink: 0;
  padding-top: 3px;
  font-size: 12px;
  color: var(--ref-muted);
  font-variant-numeric: tabular-nums;
}

/* ═══ Skeleton ═══ */
.nt-skeleton-list { display: grid; gap: 12px; }
.nt-skeleton {
  height: 96px;
  border-radius: 14px;
  border: 1px solid var(--ref-line);
  background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%);
  background-size: 200% 100%;
  animation: nt-shimmer 1.3s linear infinite;
}

/* ═══ Empty ═══ */
.nt-empty {
  padding: 72px 24px;
  border: 1px dashed var(--ref-line);
  border-radius: 14px;
  background: var(--ref-surface);
  text-align: center;
}
.nt-empty-icon {
  width: 40px;
  height: 40px;
  margin: 0 auto 14px;
  color: var(--ref-brand);
}
.nt-empty h3 {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 22px;
  font-weight: 500;
  color: var(--ref-ink);
}
.nt-empty p { margin: 10px 0 0; font-size: 14px; color: var(--ref-muted); }
.nt-empty .cta { margin-top: 22px; }

/* ═══ Dialog ═══ */
.dlg-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(10, 8, 6, 0.5);
  backdrop-filter: blur(2px);
  animation: nt-fade 0.15s ease;
}
.dlg-panel {
  width: 100%;
  max-width: 520px;
  max-height: min(90vh, 720px);
  overflow-y: auto;
  border-radius: 20px;
  background: var(--ref-surface);
  border: 1px solid var(--ref-line);
  box-shadow: 0 40px 80px -40px color-mix(in srgb, var(--ref-ink) 60%, transparent);
  animation: nt-pop 0.18s cubic-bezier(0.23, 1, 0.32, 1);
}
.dlg-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 24px 0;
}
.dlg-title {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 20px;
  font-weight: 500;
  line-height: 1.3;
  color: var(--ref-ink);
}
.dlg-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border-radius: 8px;
  background: transparent;
  border: none;
  color: var(--ref-muted);
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.dlg-close:hover { background: var(--ref-sand); color: var(--ref-ink); }
.dlg-body { padding: 18px 24px 6px; }
.dlg-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}
.dlg-tag {
  display: inline-flex;
  align-items: center;
  border-radius: 6px;
  border: 1px solid var(--ref-line);
  background: var(--ref-sand);
  padding: 3px 8px;
  font-size: 11px;
  font-weight: 500;
  line-height: 1;
  color: var(--ref-ink-soft);
}
.dlg-time { font-size: 12px; color: var(--ref-muted); font-variant-numeric: tabular-nums; }
.dlg-content {
  margin: 0;
  font-size: 14px;
  line-height: 1.8;
  color: var(--ref-ink-soft);
  white-space: pre-line;
  word-break: break-word;
}
.dlg-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
  padding: 16px 24px 24px;
  border-top: 1px solid var(--ref-line);
}

/* ═══ Animations ═══ */
@keyframes nt-shimmer { to { background-position: -200% 0; } }
@keyframes nt-fade { from { opacity: 0; } to { opacity: 1; } }
@keyframes nt-pop {
  from { opacity: 0; transform: translateY(10px) scale(0.98); }
  to { opacity: 1; transform: none; }
}

/* ═══ Responsive ═══ */
@media (max-width: 760px) {
  .nt-time { display: none; }
}
@media (max-width: 520px) {
  .nt-shell { padding: 0 16px; }
  .nt-head { padding: 30px 0 4px; }
  .nt-sub { font-size: 13.5px; }
  .nt-stats { gap: 0 20px; margin-top: 24px; }
  .nt-stat-bordered { padding-left: 20px; }
  .nt-stats dd { font-size: 24px; }
  .nt-section { margin-top: 28px; }
  .nt-row { padding: 15px 16px; gap: 12px; }
  .nt-empty { padding: 52px 20px; }
  .dlg-panel { border-radius: 16px; }
  .dlg-head { padding: 18px 18px 0; }
  .dlg-body { padding: 16px 18px 4px; }
  .dlg-foot { padding: 14px 18px 18px; }
}
@media (prefers-reduced-motion: reduce) {
  .nt-skeleton { animation: none; }
}
</style>
