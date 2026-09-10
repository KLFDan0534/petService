<template>
  <div class="ch-page">
    <div class="ch-shell">
      <!-- ═══ Breadcrumb ═══ -->
      <nav class="ch-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="ch-crumb-link">首页</router-link>
        <span class="ch-crumb-sep" aria-hidden="true">›</span>
        <span class="ch-crumb-here">消息</span>
      </nav>

      <!-- ═══ Hero ═══ -->
      <header class="ch-head">
        <div class="ch-head-copy">
          <div class="ch-eyebrow" aria-hidden="true">
            <span class="ch-eyebrow-line"></span>
            <span>消息</span>
          </div>
          <h1 class="ch-title">消息</h1>
          <p class="ch-sub">与照护师、门店和客服的沟通都在这里。照护期间的临时安排建议直接在会话里说明。</p>

          <p class="ch-facts">
            <span class="ch-fact"><span class="ch-fact-num tabular">{{ conversations.length }}</span><span class="ch-fact-label">会话</span></span>
            <span class="ch-fact"><span class="ch-fact-num tabular">{{ unreadTotal }}</span><span class="ch-fact-label">未读</span></span>
          </p>
        </div>
        <div class="ch-actions">
          <button type="button" class="cta cta-outline" :disabled="assigning" @click="requestAgent">
            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M3 11h18v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" /><path d="M7 11V7a5 5 0 0 1 10 0v4" />
            </svg>
            {{ assigning ? '接入中…' : '转人工客服' }}
          </button>
          <router-link to="/ai/chat" class="cta cta-primary">问 AI 助手</router-link>
        </div>
      </header>

      <!-- ═══ 01 · 会话 ═══ -->
      <section class="ch-section" aria-label="会话">
        <header class="ch-sec-head">
          <div class="ch-head-copy">
            <p class="ch-eyebrow ch-sec-eyebrow">
              <span class="ch-idx">01</span>
              <span class="ch-line" aria-hidden="true"></span>
              <span>收件箱</span>
            </p>
            <h2 class="ch-sec-title">会话</h2>
            <p class="ch-sec-desc">下单后照护师会主动联系你，也可以先向客服咨询。</p>
          </div>
        </header>

        <!-- 加载中 -->
        <div v-if="convLoading" class="ch-skeletons">
          <div v-for="i in 3" :key="i" class="ch-skeleton"></div>
        </div>

        <!-- 空状态 -->
        <div v-else-if="!conversations.length" class="ch-empty">
          <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          </svg>
          <p class="ch-empty-title">还没有会话</p>
          <p class="ch-empty-desc">下单后照护师会主动联系你，也可以先向客服咨询。</p>
          <div class="ch-empty-actions">
            <router-link to="/services" class="cta cta-outline">浏览照护服务</router-link>
            <router-link to="/ai/chat" class="cta cta-primary">问 AI 助手</router-link>
          </div>
        </div>

        <!-- 两栏布局 -->
        <div v-else class="ch-grid">
          <aside class="ch-side">
            <button
              v-for="c in conversations"
              :key="c.other_user_id_wsh"
              type="button"
              class="ch-conv"
              :class="{ 'is-active': activeUserId === c.other_user_id_wsh }"
              @click="openConversation(c.other_user_id_wsh)"
            >
              <span class="ch-avatar" aria-hidden="true">
                <img
                  v-if="c.other_user_avatar_wsh"
                  :src="c.other_user_avatar_wsh"
                  alt=""
                  loading="lazy"
                  @error="$event.target.style.display = 'none'"
                >
                <template v-else>{{ String(c.other_user_name_wsh || c.other_user_id_wsh).slice(0, 1) }}</template>
              </span>
              <span class="ch-conv-copy">
                <span class="ch-conv-head">
                  <span class="ch-conv-name">{{ c.other_user_name_wsh || `用户 ${c.other_user_id_wsh}` }}</span>
                  <span v-if="c.unread_count_wsh > 0" class="ch-badge tabular">{{ c.unread_count_wsh }}</span>
                </span>
                <span class="ch-conv-preview">{{ c.last_message_wsh || '暂无消息' }}</span>
              </span>
            </button>
          </aside>

          <section class="ch-pane">
            <template v-if="activeUserId">
              <header class="ch-pane-head">
                <div class="ch-pane-head-copy">
                  <h3 class="ch-pane-title">{{ activeUserName }}</h3>
                  <p v-if="activeLastTime" class="ch-pane-meta tabular">最近活动 {{ dateTimeText(activeLastTime) }}</p>
                </div>
                <button type="button" class="ch-refresh" title="刷新会话" @click="loadConversations">
                  <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <path d="M21 2v6h-6" /><path d="M3 12a9 9 0 0 1 15-6.7L21 8" /><path d="M3 22v-6h6" /><path d="M21 12a9 9 0 0 1-15 6.7L3 16" />
                  </svg>
                </button>
              </header>

              <div ref="messageBox" class="ch-msgs">
                <div
                  v-for="m in messages"
                  :key="m.id_wsh"
                  class="ch-row"
                  :class="m.from_user_id_wsh === currentUserId ? 'is-mine' : ''"
                >
                  <time class="ch-msg-time tabular">{{ formatTime(m.created_at_wsh) }}</time>
                  <div class="ch-bubble">
                    <a
                      v-if="m.file_url_wsh"
                      :href="m.file_url_wsh"
                      target="_blank"
                      rel="noreferrer"
                      class="ch-photo"
                    >
                      <img v-if="isImageUrl(m.file_url_wsh)" :src="m.file_url_wsh" alt="图片消息" loading="lazy" @error="$event.target.style.display = 'none'">
                      <span v-else class="ch-photo-file">查看附件</span>
                    </a>
                    <p v-if="m.content_wsh" class="ch-msg-text">{{ m.content_wsh }}</p>
                  </div>
                </div>
                <div v-if="!messages.length" class="ch-msgs-empty">还没有消息，发一条开始沟通。</div>
              </div>

              <div class="ch-composer">
                <div v-if="photoUrls.length" class="ch-photos">
                  <div v-for="(url, i) in photoUrls" :key="`${url}-${i}`" class="ch-photos-item">
                    <img :src="url" :alt="`待发送照片 ${i + 1}`">
                    <button class="ch-photos-remove" type="button" :disabled="sending" aria-label="移除照片" @click="removePhoto(i)">✕</button>
                  </div>
                </div>

                <div class="ch-input">
                  <input
                    ref="fileInput"
                    type="file"
                    accept="image/*"
                    multiple
                    hidden
                    @change="handlePhoto"
                  >
                  <button
                    type="button"
                    class="ch-photo-btn"
                    title="发送照片（可多选）"
                    :disabled="sending"
                    @click="fileInput.click()"
                  >
                    <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                      <rect x="3" y="3" width="18" height="18" rx="2" /><circle cx="8.5" cy="8.5" r="1.5" /><path d="m21 15-5-5L5 21" />
                    </svg>
                  </button>
                  <textarea
                    v-model.trim="draft"
                    rows="2"
                    maxlength="500"
                    class="ch-input-box"
                    placeholder="输入消息，Enter 发送"
                    @keydown.enter.exact.prevent="send"
                  />
                  <button type="button" class="cta cta-primary ch-send" :disabled="(!draft.trim() && !photoUrls.length) || sending" @click="send">
                    {{ sending ? '发送中…' : '发送' }}
                  </button>
                </div>
              </div>
            </template>

            <div v-else class="ch-pane-empty">
              <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
              </svg>
              <p class="ch-pane-empty-text">选择左侧会话查看聊天记录</p>
            </div>
          </section>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { getCsConversations } from '@/api/csWorkbench'
import { getConversations, sendChatMessage, markConversationRead, assignCustomerServiceAgent } from '@/api/chat'
import { uploadFileToDirectory } from '@/api/file'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()
const conversations = ref([])
const convLoading = ref(true)
const activeUserId = ref(null)
const activeUserName = ref('')
const messages = ref([])
const draft = ref('')
const sending = ref(false)
const assigning = ref(false)
const photoUrls = ref([])
const fileInput = ref(null)
const messageBox = ref(null)
let eventSource = null

const currentUserId = computed(() => authStore.user?.id_wsh)
const token = computed(() => authStore.token || localStorage.getItem('token'))
const unreadTotal = computed(() => conversations.value.reduce((sum, c) => sum + (c.unread_count_wsh || 0), 0))
const activeLastTime = computed(() => conversations.value.find(c => c.other_user_id_wsh === activeUserId.value)?.last_time_wsh || '')

async function loadConversations() {
  convLoading.value = true
  try {
    const r = await getCsConversations()
    if (r.code === 200) conversations.value = r.data || []
  } catch (e) {
    appStore.addToast('会话加载失败', 'error')
  } finally {
    convLoading.value = false
  }
}

async function openConversation(userId, fallbackName) {
  activeUserId.value = userId
  const conv = conversations.value.find(c => c.other_user_id_wsh === userId)
  activeUserName.value = conv?.other_user_name_wsh || fallbackName || `用户#${userId}`
  messages.value = []
  try {
    const r = await getConversations({ otherUserId: userId })
    if (r.code === 200) {
      messages.value = r.data || []
      const unread = messages.value.filter(m => m.to_user_id_wsh === currentUserId.value && !m.read_wsh)
      if (unread.length) {
        await markConversationRead({ other_user_id_wsh: userId })
        if (conv) conv.unread_count_wsh = 0
      }
    }
  } catch (e) { /* ignore */ }
  await nextTick()
  scrollToBottom()
}

async function requestAgent() {
  if (assigning.value) return
  assigning.value = true
  try {
    const r = await assignCustomerServiceAgent()
    if (r.code === 200 && r.data?.user_id_wsh) {
      await openConversation(Number(r.data.user_id_wsh), r.data.name_wsh || '')
    } else {
      appStore.addToast(r.message || '暂无可用的在线客服，请稍后再试', 'error')
    }
  } catch (e) {
    appStore.addToast(e?.response?.data?.message || '转接失败，请稍后再试', 'error')
  } finally {
    assigning.value = false
  }
}

function scrollToBottom() {
  if (messageBox.value) messageBox.value.scrollTop = messageBox.value.scrollHeight
}

async function handlePhoto(event) {
  const files = Array.from(event.target.files || [])
  event.target.value = ''
  if (!files.length) return
  for (const file of files) {
    const fd = new FormData()
    fd.append('file', file)
    try {
      const r = await uploadFileToDirectory('chat', fd)
      if (r.code === 200 && r.data?.url_wsh) {
        photoUrls.value.push(r.data.url_wsh)
      } else {
        appStore.addToast(r.message || `照片 ${file.name} 上传失败`, 'error')
      }
    } catch (e) {
      appStore.addToast(e?.response?.data?.message || `照片 ${file.name} 上传失败`, 'error')
    }
  }
  if (photoUrls.value.length) {
    appStore.addToast(`已选择 ${photoUrls.value.length} 张照片，点击发送`, 'success')
  }
}

function removePhoto(index) {
  photoUrls.value.splice(index, 1)
}

async function send() {
  if (!activeUserId.value || sending.value) return
  if (!draft.value.trim() && !photoUrls.value.length) return
  sending.value = true
  const content = draft.value.trim()
  const urls = [...photoUrls.value]
  try {
    for (const url of urls) {
      const r = await sendChatMessage({
        to_user_id_wsh: activeUserId.value,
        content_wsh: '',
        type_wsh: 'image',
        file_url_wsh: url,
      })
      if (r.code === 200) {
        messages.value.push(r.data)
        refreshConversationOrder(r.data)
      } else {
        appStore.addToast(r.message || '照片发送失败', 'error')
      }
    }
    if (content) {
      const r = await sendChatMessage({
        to_user_id_wsh: activeUserId.value,
        content_wsh: content,
        type_wsh: 'text',
        file_url_wsh: null,
      })
      if (r.code === 200) {
        messages.value.push(r.data)
        refreshConversationOrder(r.data)
      } else {
        appStore.addToast(r.message || '发送失败', 'error')
      }
    }
    draft.value = ''
    photoUrls.value = []
    await nextTick()
    scrollToBottom()
  } catch (e) {
    appStore.addToast(e?.response?.data?.message || '发送失败', 'error')
  } finally {
    sending.value = false
  }
}

function refreshConversationOrder(sent) {
  const idx = conversations.value.findIndex(c => c.other_user_id_wsh === activeUserId.value)
  let conv
  if (idx >= 0) {
    conv = conversations.value[idx]
    conversations.value.splice(idx, 1)
  } else {
    conv = { other_user_id_wsh: activeUserId.value, other_user_name_wsh: activeUserName.value }
  }
  conv.last_message_wsh = sent.content_wsh
  conv.last_time_wsh = sent.created_at_wsh
  conversations.value.unshift(conv)
}

function formatTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString('zh-CN', { hour12: false })
}

function dateTimeText(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

function isImageUrl(value) {
  if (!value) return false
  return /\.(jpe?g|png|gif|webp|bmp|svg|avif)(\?.*)?$/i.test(String(value))
}

function connectSse() {
  if (!token.value) return
  try {
    eventSource = new EventSource(`/api/chat-events/stream?token=${encodeURIComponent(token.value)}`)
    eventSource.addEventListener('chat-message', (event) => {
      try {
        const msg = JSON.parse(event.data)
        if (!msg || msg.from_user_id_wsh === currentUserId.value) return
        const conv = conversations.value.find(c => c.other_user_id_wsh === msg.from_user_id_wsh)
        if (conv) {
          if (activeUserId.value !== msg.from_user_id_wsh) {
            conv.unread_count_wsh = (conv.unread_count_wsh || 0) + 1
          }
          conv.last_message_wsh = msg.content_wsh
          conv.last_time_wsh = msg.created_at_wsh
          conversations.value = [conv, ...conversations.value.filter(c => c.other_user_id_wsh !== conv.other_user_id_wsh)]
        } else {
          conversations.value.unshift({
            other_user_id_wsh: msg.from_user_id_wsh,
            other_user_name_wsh: `用户#${msg.from_user_id_wsh}`,
            last_message_wsh: msg.content_wsh,
            last_time_wsh: msg.created_at_wsh,
            unread_count_wsh: msg.to_user_id_wsh === currentUserId.value ? 1 : 0,
          })
        }
        if (activeUserId.value === msg.from_user_id_wsh && msg.to_user_id_wsh === currentUserId.value) {
          messages.value.push(msg)
          markConversationRead({ other_user_id_wsh: msg.from_user_id_wsh }).catch(() => {})
          nextTick(scrollToBottom)
        }
      } catch (e) { /* ignore malformed event */ }
    })
  } catch (e) { /* SSE unavailable */ }
}

onMounted(async () => {
  await loadConversations()
  connectSse()
  if (route.query.userId) {
    await openConversation(Number(route.query.userId), String(route.query.name || ''))
  } else if (route.query.orderId) {
    router.replace({
      name: 'OrderDetail',
      params: { id: route.query.orderId },
      query: { tab: 'chat' },
    })
  }
})

onBeforeUnmount(() => {
  if (eventSource) eventSource.close()
})
</script>

<style scoped>
.ch-page {
  width: 100%;
  padding: 6px 0 72px;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}

.ch-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ═══ Breadcrumb ═══ */
.ch-crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 12.5px;
  color: var(--ref-muted);
}
.ch-crumb-link { color: var(--ref-muted); text-decoration: none; }
.ch-crumb-link:hover { color: var(--ref-ink); }
.ch-crumb-sep { color: var(--ref-line); }
.ch-crumb-here { color: var(--ref-ink-soft); }

/* ═══ Eyebrow ═══ */
.ch-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 9.5px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.ch-eyebrow-line { width: 32px; height: 1px; background: var(--ref-line); }
.ch-idx { font-variant-numeric: tabular-nums; }
.ch-line { width: 24px; height: 1px; background: var(--ref-line); }

/* ═══ Hero ═══ */
.ch-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px 24px;
  padding: 40px 0 28px;
}
.ch-head-copy { min-width: 0; }
.ch-title {
  margin: 18px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(34px, 4.4vw, 52px);
  line-height: 1.12;
  letter-spacing: -0.01em;
  font-weight: 500;
  color: var(--ref-ink);
  text-wrap: balance;
}
.ch-sub {
  margin: 14px 0 0;
  max-width: 660px;
  font-size: 14px;
  line-height: 1.75;
  color: color-mix(in srgb, var(--ref-ink-soft) 82%, transparent);
}
.ch-facts {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 28px;
  margin: 22px 0 0;
}
.ch-fact {
  display: inline-flex;
  align-items: baseline;
  gap: 8px;
}
.ch-fact-num {
  font-family: var(--ref-font-display);
  font-size: 26px;
  font-weight: 500;
  color: var(--ref-ink);
}
.ch-fact-label {
  font-size: 12px;
  color: var(--ref-muted);
}
.ch-actions { display: flex; flex-wrap: wrap; gap: 10px; }

/* ═══ CTA ═══ */
.cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 42px;
  padding: 0 18px;
  border-radius: var(--radius-control);
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
.cta-outline:hover:not(:disabled) { border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent); }
.cta.ch-send { height: 40px; padding: 0 16px; flex: 0 0 auto; }

/* ═══ Section ═══ */
.ch-section { margin-top: 40px; }
.ch-sec-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px 24px;
}
.ch-sec-eyebrow { font-size: 10px; letter-spacing: 0.22em; }
.ch-sec-title {
  margin: 10px 0 0;
  font-family: var(--ref-font-display);
  font-size: 24px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: var(--ref-ink);
}
.ch-sec-desc {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ref-ink-soft);
  opacity: 0.8;
}

/* ═══ 两栏布局 ═══ */
.ch-grid {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 18px;
  margin-top: 20px;
  align-items: start;
}

.ch-side {
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-lg);
  background: var(--ref-surface);
  overflow-y: auto;
  max-height: min(64vh, 540px);
  padding: 8px;
  display: grid;
  gap: 2px;
}
.ch-conv {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  width: 100%;
  padding: 12px;
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  background: transparent;
  cursor: pointer;
  text-align: left;
  transition: background 0.15s, border-color 0.15s;
}
.ch-conv:hover { background: color-mix(in srgb, var(--ref-cream) 50%, transparent); }
.ch-conv.is-active { background: color-mix(in srgb, var(--ref-sand) 55%, transparent); border-color: var(--ref-line); }
.ch-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: color-mix(in srgb, var(--ref-brand) 14%, transparent);
  color: var(--ref-brand);
  font-size: 14px;
  font-weight: 600;
  overflow: hidden;
}
.ch-avatar img { width: 100%; height: 100%; object-fit: cover; }
.ch-conv-copy { min-width: 0; flex: 1; }
.ch-conv-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.ch-conv-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13.5px;
  font-weight: 500;
  color: var(--ref-ink);
}
.ch-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 6px;
  border-radius: var(--radius-pill);
  background: var(--ref-brand);
  color: #fff;
  font-size: 10.5px;
  line-height: 1;
}
.ch-conv-preview {
  display: block;
  margin-top: 5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: var(--ref-muted);
}

/* ═══ 对话面板 ═══ */
.ch-pane {
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-lg);
  background: var(--ref-surface);
  overflow: hidden;
  height: min(64vh, 540px);
  display: flex;
  flex-direction: column;
}
.ch-pane-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--ref-line);
  flex: 0 0 auto;
}
.ch-pane-head-copy { min-width: 0; }
.ch-pane-title {
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 500;
  color: var(--ref-ink);
}
.ch-pane-meta {
  margin: 4px 0 0;
  font-size: 11.5px;
  color: var(--ref-muted);
}
.ch-refresh {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  flex: 0 0 auto;
  border: 1px solid var(--ref-line);
  border-radius: 9px;
  background: var(--ref-surface);
  color: var(--ref-muted);
  cursor: pointer;
  transition: color 0.15s, border-color 0.15s;
}
.ch-refresh:hover { color: var(--ref-ink); border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent); }

.ch-msgs {
  flex: 1 1 auto;
  min-height: 120px;
  overflow-y: auto;
  padding: 20px;
  display: grid;
  gap: 14px;
  align-content: start;
}
.ch-row {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 5px;
}
.ch-row.is-mine { align-items: flex-end; }
.ch-msg-time {
  font-size: 10.5px;
  color: var(--ref-muted);
}
.ch-bubble {
  max-width: 85%;
  border-radius: 14px;
  padding: 10px 14px;
  border: 1px solid var(--ref-line);
  background: color-mix(in srgb, var(--ref-cream) 50%, transparent);
  color: var(--ref-ink-soft);
}
.ch-row.is-mine .ch-bubble {
  background: var(--ref-ink);
  border-color: transparent;
  color: var(--ref-cream);
}
.ch-msg-text {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.7;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.ch-photo { display: block; }
.ch-photo img {
  display: block;
  max-width: 220px;
  max-height: 220px;
  border-radius: 10px;
  margin-bottom: 4px;
}
.ch-photo-file {
  display: inline-block;
  font-size: 12.5px;
  color: inherit;
  text-decoration: underline;
}
.ch-msgs-empty {
  padding: 28px 0;
  text-align: center;
  font-size: 13px;
  color: var(--ref-muted);
}

/* ═══ 输入区 ═══ */
.ch-composer {
  border-top: 1px solid var(--ref-line);
  background: color-mix(in srgb, var(--ref-cream) 40%, transparent);
  flex: 0 0 auto;
}
.ch-photos {
  display: flex;
  gap: 10px;
  padding: 10px 14px 2px;
  overflow-x: auto;
}
.ch-photos-item {
  position: relative;
  flex: 0 0 auto;
}
.ch-photos-item img {
  display: block;
  width: 64px;
  height: 64px;
  object-fit: cover;
  border-radius: 10px;
  border: 1px solid var(--ref-line);
}
.ch-photos-remove {
  position: absolute;
  top: -7px;
  right: -7px;
  display: grid;
  width: 20px;
  height: 20px;
  place-items: center;
  padding: 0;
  color: #fff;
  background: var(--color-danger, #ef4444);
  border: none;
  border-radius: 50%;
  font-size: 12px;
  line-height: 1;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}
.ch-photos-remove:disabled { opacity: 0.5; cursor: not-allowed; }
.ch-input {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 12px 14px;
}
.ch-photo-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  flex: 0 0 auto;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-control);
  background: var(--ref-surface);
  color: var(--ref-muted);
  cursor: pointer;
  transition: color 0.15s, border-color 0.15s;
}
.ch-photo-btn:hover:not(:disabled) { color: var(--ref-ink); border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent); }
.ch-photo-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.ch-input-box {
  flex: 1;
  min-width: 0;
  padding: 10px 13px;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-control);
  background: var(--ref-surface);
  color: var(--ref-ink);
  font-size: 13.5px;
  font-family: inherit;
  line-height: 1.5;
  resize: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.ch-input-box::placeholder { color: color-mix(in srgb, var(--ref-muted) 70%, transparent); }
.ch-input-box:focus {
  outline: none;
  border-color: color-mix(in srgb, var(--ref-brand) 60%, transparent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 14%, transparent);
}

/* ═══ 加载 / 空状态 ═══ */
.ch-skeletons {
  display: grid;
  gap: 12px;
  margin-top: 20px;
}
.ch-skeleton {
  height: 74px;
  border-radius: 14px;
  background: linear-gradient(90deg, color-mix(in srgb, var(--ref-line) 55%, transparent), color-mix(in srgb, var(--ref-line) 25%, transparent), color-mix(in srgb, var(--ref-line) 55%, transparent));
  background-size: 200% 100%;
  animation: ch-shimmer 1.4s infinite;
}
@keyframes ch-shimmer {
  to { background-position: -200% 0; }
}
.ch-empty {
  margin-top: 20px;
  padding: 48px 22px;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-lg);
  background: var(--ref-surface);
  text-align: center;
}
.ch-empty svg { color: var(--ref-muted); }
.ch-empty-title {
  margin: 14px 0 0;
  font-size: 15px;
  font-weight: 500;
  color: var(--ref-ink);
}
.ch-empty-desc {
  margin: 8px auto 0;
  max-width: 420px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ref-muted);
}
.ch-empty-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  margin-top: 20px;
}
.ch-pane-empty {
  flex: 1;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 12px;
  color: var(--ref-muted);
}
.ch-pane-empty svg { color: var(--ref-line); }
.ch-pane-empty-text {
  margin: 0;
  font-size: 13px;
  color: var(--ref-muted);
}

/* ═══ Responsive ═══ */
@media (max-width: 900px) {
  .ch-grid { grid-template-columns: 1fr; }
  .ch-side {
    max-height: 190px;
    grid-auto-flow: column;
    grid-auto-columns: 240px;
    overflow-x: auto;
    overflow-y: hidden;
  }
  .ch-pane { height: min(58vh, 480px); }
}
@media (max-width: 640px) {
  .ch-shell { padding: 0 16px; }
  .ch-head { padding: 30px 0 22px; }
  .ch-sub { font-size: 13.5px; }
  .ch-section { margin-top: 32px; }
  .ch-msgs { padding: 16px; }
  .ch-bubble { max-width: 88%; }
  .ch-input { padding: 10px; }
}
@media (prefers-reduced-motion: reduce) {
  .ch-skeleton { animation: none; }
}
</style>
