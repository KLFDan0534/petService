<template>
  <div class="td-page">
    <div class="td-shell">

      <!-- ═══ Breadcrumb ═══ -->
      <nav class="td-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="td-crumb-link">首页</router-link>
        <span class="td-crumb-sep" aria-hidden="true">›</span>
        <router-link to="/tickets" class="td-crumb-link">我的工单</router-link>
        <span class="td-crumb-sep" aria-hidden="true">›</span>
        <span class="td-crumb-here">{{ ticket ? ticket.title_wsh : `工单 #${ticketId}` }}</span>
      </nav>

      <!-- ═══ Loading skeleton ═══ -->
      <div v-if="loading" class="td-loading" aria-hidden="true">
        <div class="td-skel td-skel-eyebrow"></div>
        <div class="td-skel td-skel-title"></div>
        <div class="td-skel td-skel-lede"></div>
        <div class="td-skel td-skel-facts"></div>
      </div>

      <!-- ═════════════ 工单详情 ═════════════ -->
      <template v-else-if="ticket">
        <!-- Hero -->
        <header class="td-head">
          <div class="td-head-copy">
            <p class="td-eyebrow">
              <span class="td-idx">Ticket #{{ ticket.id_wsh ?? '' }}</span>
            </p>
            <h1 class="td-title">{{ ticket.title_wsh || '工单' }}</h1>
            <p v-if="ticket.content_wsh" class="td-desc">{{ ticket.content_wsh }}</p>
            <dl class="td-facts">
              <div>
                <dt>状态</dt>
                <dd><span :class="['badge', statusBadge(ticket.status_wsh)]">{{ statusLabel(ticket.status_wsh) }}</span></dd>
              </div>
              <div>
                <dt>类型</dt>
                <dd>{{ categoryLabel(ticket.category_wsh) }}</dd>
              </div>
              <div>
                <dt>紧急程度</dt>
                <dd>{{ priorityLabel(ticket.priority_wsh) }}</dd>
              </div>
              <div>
                <dt>提交时间</dt>
                <dd class="tabular">{{ formatDT(ticket.created_at_wsh) }}</dd>
              </div>
            </dl>
          </div>
          <div class="td-head-actions">
            <router-link to="/tickets" class="cta cta-outline">返回列表</router-link>
            <button
              v-if="isOpen"
              type="button"
              class="cta cta-danger"
              :disabled="closing"
              @click="handleClose"
            >{{ closing ? '关闭中…' : '关闭工单' }}</button>
          </div>
        </header>

        <!-- 处理结论 -->
        <div v-if="ticket.result_wsh" class="td-result">
          <p class="td-result-label">处理结论</p>
          <p class="td-result-text">{{ ticket.result_wsh }}</p>
        </div>

        <!-- 01 · 沟通记录 -->
        <section class="td-section">
          <header class="td-sec-head">
            <div class="td-sec-copy">
              <p class="td-eyebrow td-sec-eyebrow">
                <span class="td-idx">01</span>
                <span class="td-line" aria-hidden="true"></span>
                <span>对话</span>
              </p>
              <h2 class="td-sec-title">沟通记录</h2>
              <p class="td-sec-desc">
                {{ isOpen ? '客服的回复会出现在这里，你也可以随时补充说明。' : '该工单已结束，记录保留可随时查看。' }}
              </p>
            </div>
          </header>

          <div class="td-thread">
            <p v-if="!messages.length" class="td-thread-empty">
              {{ isOpen ? '客服还没有回复，通常会在 2 小时内响应。' : '暂无消息记录。' }}
            </p>
            <div
              v-for="m in messages"
              :key="m.id_wsh"
              class="td-msg"
              :class="{ 'is-me': m.user_id_wsh === currentUserId }"
            >
              <div class="td-msg-bubble">
                <div class="td-msg-meta">
                  <strong>{{ m.user_id_wsh === currentUserId ? '我' : '客服' }}</strong>
                  <span>{{ formatDT(m.created_at_wsh) }}</span>
                </div>
                <div>{{ m.content_wsh }}</div>
              </div>
            </div>
          </div>

          <form v-if="isOpen" class="td-reply" @submit.prevent="handleReply">
            <textarea
              v-model="draft"
              class="td-reply-input"
              rows="3"
              placeholder="补充说明或上传更多细节…"
              :disabled="sending"
            ></textarea>
            <div class="td-reply-foot">
              <p class="td-reply-note">回复将追加到该工单的沟通记录中。</p>
              <button type="submit" class="cta cta-primary" :disabled="sending || !draft.trim()">
                {{ sending ? '发送中…' : '发送' }}
              </button>
            </div>
          </form>
          <p v-else class="td-locked">该工单已关闭，如需继续沟通请新建一条工单。</p>
        </section>
      </template>

      <!-- ═══ 未找到 ═══ -->
      <div v-else class="td-notfound">
        <p class="td-notfound-title">找不到这条工单</p>
        <p class="td-notfound-desc">它可能已被删除，或不属于当前账号。</p>
        <router-link to="/tickets" class="cta cta-primary">返回工单列表</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getMyTickets, getTicketMessages, sendTicketMessage, closeTicket } from '@/api/ticket'
import { TicketStatus, getStatusBadge, getStatusLabel } from '@/constants/statusMaps'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const authStore = useAuthStore()
const appStore = useAppStore()

const ticketId = String(route.params.id ?? '')
const ticket = ref(null)
const messages = ref([])
const loading = ref(true)
const sending = ref(false)
const closing = ref(false)
const draft = ref('')

const currentUserId = authStore.user?.id_wsh

const isOpen = computed(() => {
  const s = ticket.value?.status_wsh
  return s === 'pending' || s === 'processing'
})

const CATEGORY_MAP = {
  appeal: '订单申诉', complaint: '服务投诉', question: '使用咨询',
  suggestion: '产品建议', other: '其他',
}
const PRIORITY_MAP = { low: '低', medium: '普通', high: '紧急', urgent: '紧急' }

function categoryLabel(cat) { return CATEGORY_MAP[cat] || cat || '其他' }
function priorityLabel(p) { return PRIORITY_MAP[p] || p || '普通' }
function statusLabel(s) { return getStatusLabel(TicketStatus, s) }
function statusBadge(s) { return getStatusBadge(TicketStatus, s) }

function formatDT(value) {
  if (!value) return '-'
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? String(value) : d.toLocaleString('zh-CN')
}

onMounted(async () => {
  loading.value = true
  try {
    const [listRes, msgRes] = await Promise.all([getMyTickets(), getTicketMessages(ticketId)])
    if (listRes.code === 200) {
      const list = Array.isArray(listRes.data) ? listRes.data : []
      ticket.value = list.find((t) => String(t.id_wsh) === ticketId) ?? null
    } else {
      ticket.value = null
    }
    if (msgRes.code === 200) messages.value = Array.isArray(msgRes.data) ? msgRes.data : []
  } catch (e) {
    appStore.addToast(e?.message || '工单详情加载失败', 'error')
  } finally {
    loading.value = false
  }
})

async function handleReply() {
  if (!draft.value.trim() || sending.value) return
  sending.value = true
  try {
    const res = await sendTicketMessage(ticketId, { content_wsh: draft.value.trim() })
    if (res.code === 200) {
      if (res.data) messages.value = [...messages.value, res.data]
      else messages.value = [...messages.value, { id_wsh: Date.now(), content_wsh: draft.value.trim(), created_at_wsh: new Date().toISOString(), user_id_wsh: currentUserId }]
      draft.value = ''
      appStore.addToast('已回复', 'success')
    } else {
      appStore.addToast(res.message || '发送失败', 'error')
    }
  } catch (e) {
    appStore.addToast(e?.message || '发送失败，请稍后重试', 'error')
  } finally {
    sending.value = false
  }
}

async function handleClose() {
  if (!ticket.value || closing.value) return
  closing.value = true
  try {
    const res = await closeTicket(ticketId)
    if (res.code === 200) {
      ticket.value = { ...ticket.value, status_wsh: 'closed' }
      appStore.addToast('工单已关闭', 'success')
    } else {
      appStore.addToast(res.message || '关闭失败', 'error')
    }
  } catch (e) {
    appStore.addToast(e?.message || '关闭失败，请稍后重试', 'error')
  } finally {
    closing.value = false
  }
}
</script>

<style scoped>
/* ═══ Editorial warm — shared --ref-* tokens from design-tokens.css ═══ */
.td-page {
  width: 100%;
  min-height: 60vh;
  padding: 6px 0 72px;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}
.td-shell { max-width: 1180px; margin: 0 auto; padding: 0 24px; }

/* ═══ Breadcrumb ═══ */
.td-crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 12.5px;
  color: var(--ref-muted);
}
.td-crumb-link { color: var(--ref-muted); text-decoration: none; }
.td-crumb-link:hover { color: var(--ref-ink); }
.td-crumb-sep { color: var(--ref-line); }
.td-crumb-here { color: var(--ref-ink-soft); }

/* ═══ Eyebrow ═══ */
.td-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 9.5px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.td-idx { font-variant-numeric: tabular-nums; }
.td-line { width: 24px; height: 1px; background: var(--ref-line); }

/* ═══ Hero ═══ */
.td-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px 28px;
  padding: 40px 0 8px;
}
.td-head-copy { min-width: 0; }
.td-title {
  margin: 18px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(32px, 4.4vw, 46px);
  line-height: 1.08;
  letter-spacing: -0.01em;
  font-weight: 500;
  color: var(--ref-ink);
  text-wrap: balance;
}
.td-desc {
  margin: 14px 0 0;
  max-width: 620px;
  font-size: 14px;
  line-height: 1.75;
  color: color-mix(in srgb, var(--ref-ink-soft) 82%, transparent);
}
.td-facts {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px 20px;
  margin: 32px 0 0;
  padding: 26px 0 0;
  border-top: 1px solid var(--ref-line);
}
.td-facts dt {
  font-size: 10px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.td-facts dd {
  margin: 8px 0 0;
  font-size: 14px;
  line-height: 1;
  color: var(--ref-ink);
  font-variant-numeric: tabular-nums;
}
.td-head-actions { display: flex; flex-wrap: wrap; gap: 10px; padding-bottom: 4px; }

/* ═══ Result box ═══ */
.td-result {
  margin-top: 28px;
  padding: 18px 22px;
  border: 1px solid var(--ref-line);
  border-radius: 14px;
  background: color-mix(in srgb, var(--ref-cream) 55%, var(--ref-surface));
}
.td-result-label {
  margin: 0;
  font-size: 10px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.td-result-text {
  margin: 10px 0 0;
  font-size: 13.5px;
  line-height: 1.75;
  color: var(--ref-ink-soft);
}

/* ═══ Section ═══ */
.td-section { margin-top: 52px; }
.td-sec-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px 24px;
}
.td-sec-copy { min-width: 0; }
.td-sec-title {
  margin: 12px 0 0;
  font-family: var(--ref-font-display);
  font-size: 24px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: var(--ref-ink);
}
.td-sec-desc {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ref-ink-soft);
  opacity: 0.8;
}

/* ═══ Thread ═══ */
.td-thread {
  margin-top: 20px;
  padding: 24px;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-lg);
  background: var(--ref-surface);
  display: grid;
  gap: 10px;
}
.td-thread-empty {
  margin: 0;
  padding: 12px;
  font-size: 13px;
  color: var(--ref-muted);
  text-align: center;
}
.td-msg { display: flex; }
.td-msg.is-me { justify-content: flex-end; }
.td-msg-bubble {
  max-width: 78%;
  padding: 10px 14px;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--ref-sand) 55%, var(--ref-surface));
  color: var(--ref-ink);
  font-size: 13.5px;
  line-height: 1.7;
  overflow-wrap: anywhere;
}
.td-msg.is-me .td-msg-bubble {
  background: color-mix(in srgb, var(--ref-brand) 10%, var(--ref-surface));
  border-color: color-mix(in srgb, var(--ref-brand) 26%, var(--ref-line));
}
.td-msg-meta {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 4px;
  font-size: 11px;
  color: var(--ref-muted);
}
.td-msg-meta strong { color: var(--ref-ink-soft); }

/* ═══ Reply ═══ */
.td-reply {
  margin-top: 20px;
  padding: 20px 24px;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-lg);
  background: var(--ref-surface);
}
.td-reply-input {
  width: 100%;
  border: 1px solid var(--ref-line);
  border-radius: 10px;
  background: var(--ref-surface);
  color: var(--ref-ink);
  font-size: 14px;
  font-family: inherit;
  padding: 12px 14px;
  resize: vertical;
  line-height: 1.6;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.td-reply-input::placeholder { color: var(--ref-muted); }
.td-reply-input:focus {
  outline: none;
  border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 12%, transparent);
}
.td-reply-foot {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
}
.td-reply-note { margin: 0; font-size: 11.5px; line-height: 1.6; color: var(--ref-muted); }
.td-locked {
  margin: 20px 0 0;
  padding: 16px 22px;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--ref-sand) 50%, var(--ref-surface));
  font-size: 13px;
  color: var(--ref-ink-soft);
}

/* ═══ Badges ═══ */
.badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: var(--radius-inline);
  border: 1px solid transparent;
  padding: 4px 10px;
  font-size: 11px;
  font-weight: 500;
  line-height: 1;
  white-space: nowrap;
  margin-top: -4px;
}
.badge-warning { background: var(--ref-brand); color: #fff; }
.badge-info { background: var(--ref-surface); color: var(--ref-ink); border-color: color-mix(in srgb, var(--ref-ink) 20%, transparent); }
.badge-success { background: color-mix(in srgb, var(--color-success) 13%, var(--ref-surface)); color: color-mix(in srgb, var(--color-success) 72%, var(--ref-ink)); border-color: color-mix(in srgb, var(--color-success) 28%, transparent); }
.badge-secondary { background: transparent; color: var(--ref-muted); border-color: var(--ref-line); }
.badge-danger, .badge-error { background: color-mix(in srgb, var(--color-danger) 11%, var(--ref-surface)); color: color-mix(in srgb, var(--color-danger) 78%, var(--ref-ink)); border-color: color-mix(in srgb, var(--color-danger) 28%, transparent); }

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
.cta-danger { background: color-mix(in srgb, var(--color-danger) 9%, transparent); color: var(--color-danger); border-color: color-mix(in srgb, var(--color-danger) 28%, transparent); }
.cta-danger:hover:not(:disabled) { background: color-mix(in srgb, var(--color-danger) 15%, transparent); }

/* ═══ Skeleton ═══ */
.td-loading { padding: 40px 0 8px; }
.td-skel {
  height: 12px;
  border-radius: var(--radius-inline);
  background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%);
  background-size: 200% 100%;
  animation: td-shimmer 1.3s linear infinite;
  margin-bottom: 16px;
}
.td-skel-eyebrow { width: 112px; }
.td-skel-title { width: 46%; height: 40px; }
.td-skel-lede { width: 72%; }
.td-skel-facts { width: 100%; height: 96px; }

/* ═══ Not found ═══ */
.td-notfound {
  margin-top: 60px;
  padding: 72px 24px;
  border: 1px dashed var(--ref-line);
  border-radius: 20px;
  background: var(--ref-surface);
  text-align: center;
}
.td-notfound-title {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 22px;
  font-weight: 500;
  color: var(--ref-ink);
}
.td-notfound-desc { margin: 10px auto 0; max-width: 420px; font-size: 13.5px; line-height: 1.7; color: var(--ref-muted); }
.td-notfound .cta { margin-top: 22px; }

@keyframes td-shimmer { to { background-position: -200% 0; } }

/* ═══ Responsive ═══ */
@media (max-width: 900px) {
  .td-facts { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 560px) {
  .td-shell { padding: 0 16px; }
  .td-head { padding: 30px 0 4px; }
  .td-section { margin-top: 44px; }
  .td-thread { padding: 16px; }
  .td-reply { padding: 16px; }
  .td-facts { gap: 20px 16px; }
}
@media (prefers-reduced-motion: reduce) {
  .td-skel { animation: none; }
}
</style>