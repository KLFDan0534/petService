<template>
  <div class="ac-page">
    <div class="ac-shell">
      <!-- ═══ Breadcrumb ═══ -->
      <nav class="ac-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="ac-crumb-link">首页</router-link>
        <span class="ac-crumb-sep" aria-hidden="true">›</span>
        <router-link to="/ai" class="ac-crumb-link">AI 助手</router-link>
        <span class="ac-crumb-sep" aria-hidden="true">›</span>
        <span class="ac-crumb-here">照护助手</span>
      </nav>

      <!-- ═══ Hero ═══ -->
      <header class="ac-head">
        <div class="ac-head-copy">
          <div class="ac-eyebrow" aria-hidden="true">
            <span class="ac-eyebrow-line"></span>
            <span>提问</span>
          </div>
          <h1 class="ac-title">照护助手</h1>
          <p class="ac-sub">问寄养准备、饮食与应激处理。回答会附上依据来源；模型没把握时会建议转人工客服。</p>
        </div>
        <div class="ac-actions">
          <button type="button" class="cta cta-primary" @click="newSession">新对话</button>
          <router-link to="/chat" class="cta cta-outline">找人工客服</router-link>
        </div>
      </header>

      <!-- ═══ 01 · 对话 ═══ -->
      <section class="ac-section" aria-label="对话">
        <header class="ac-sec-head">
          <div class="ac-head-copy">
            <p class="ac-eyebrow ac-sec-eyebrow">
              <span class="ac-idx">01</span>
              <span class="ac-line" aria-hidden="true"></span>
              <span>对话</span>
            </p>
            <h2 class="ac-sec-title">对话</h2>
            <p class="ac-sec-desc">Enter 发送，Shift + Enter 换行。</p>
          </div>
        </header>

        <div class="ac-grid">
          <!-- 对话面板 -->
          <div class="ac-chat">
            <div ref="messagesRef" class="ac-messages">
              <div v-if="!messages.length" class="ac-welcome">
                <p class="ac-welcome-title">你好，我是照护助手</p>
                <p class="ac-welcome-desc">可以问我寄养准备、饮食、应激处理等照护问题。</p>
                <div class="ac-suggestions">
                  <button v-for="s in suggestions" :key="s" type="button" class="ac-chip" @click="useSuggestion(s)">
                    {{ s }}
                  </button>
                </div>
              </div>

              <div
                v-for="msg in messages"
                :key="msg.id"
                class="ac-row"
                :class="msg.role === 'user' ? 'is-user' : ''"
              >
                <div class="ac-bubble">
                  <p>{{ msg.content }}</p>
                  <div v-if="msg.sources && msg.sources.length" class="ac-sources">
                    参考文档：{{ msg.sources.join('、') }}
                  </div>
                  <div v-if="msg.needHuman" class="ac-handoff">
                    <p class="ac-handoff-text">您的问题需要人工客服协助，点击下方按钮转接。</p>
                    <button type="button" class="cta cta-primary cta-sm" :disabled="handoffLoading" @click="handoffToHuman">
                      {{ handoffLoading ? '正在接入客服...' : '转人工客服' }}
                    </button>
                  </div>
                </div>
              </div>

              <div v-if="loading" class="ac-typing">
                <span class="ac-typing-dot" /><span class="ac-typing-dot" /><span class="ac-typing-dot" />
                <span class="ac-typing-text">AI 思考中...</span>
              </div>
            </div>

            <form class="ac-input" @submit.prevent="send">
              <textarea
                v-model.trim="input"
                rows="2"
                maxlength="500"
                placeholder="输入您的问题，例如：退款规则是什么？"
                :disabled="loading"
                @keydown.enter.exact.prevent="send"
              />
              <button type="submit" class="cta cta-primary" :disabled="loading || !input">
                {{ loading ? '发送中...' : '发送' }}
              </button>
            </form>
          </div>

          <!-- 历史会话 -->
          <aside class="ac-side">
            <header class="ac-side-head">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M3 3v5h5" /><path d="M3.05 13A9 9 0 1 0 6 5.3L3 8" /><path d="M12 7v5l4 2" />
              </svg>
              <h3 class="ac-side-title">历史会话</h3>
              <span class="ac-side-count tabular">{{ sessions.length }}</span>
            </header>

            <div v-if="!sessions.length" class="ac-side-empty">
              <p class="ac-side-empty-text">还没有历史会话。这次的对话结束后会自动保存在这里。</p>
            </div>

            <ul v-else class="ac-session-list">
              <li
                v-for="s in sessions"
                :key="s.session_id_wsh"
                :class="['ac-session', { 'is-active': currentSessionId === s.session_id_wsh }]"
              >
                <button type="button" class="ac-session-main" @click="switchSession(s.session_id_wsh)">
                  <span class="ac-session-title">{{ s.first_message_wsh || '未命名会话' }}</span>
                  <span class="ac-session-meta tabular">
                    {{ dateTimeText(s.created_at_wsh) }}
                    <template v-if="s.message_count_wsh"> · {{ s.message_count_wsh }} 条</template>
                  </span>
                </button>
                <button
                  type="button"
                  class="ac-session-del"
                  :aria-label="`删除会话 ${s.first_message_wsh || ''}`"
                  title="删除该会话"
                  @click="deleteSession(s.session_id_wsh)"
                >
                  <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <path d="M3 6h18" /><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6" /><path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                  </svg>
                </button>
              </li>
            </ul>
          </aside>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { aiChat, getAiChatSessions, getAiChatSessionMessages, clearAiChatSession } from '@/api/ai'
import { assignCustomerServiceAgent } from '@/api/chat'
import { useAppStore } from '@/stores/app'

const suggestions = [
  '柯基第一次寄养要准备什么？',
  '猫到新环境不吃饭怎么办？',
  '老年犬寄养要盯哪些健康指标？',
  '寄养期间可以自己送粮吗？',
]

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const handoffLoading = ref(false)
const messages = ref([])
const input = ref('')
const loading = ref(false)
const messagesRef = ref(null)
const sessions = ref([])
const currentSessionId = ref('')

onMounted(async () => {
  const targetSession = route.query.session
  await loadSessions()
  if (targetSession && sessions.value.some(s => s.session_id_wsh === targetSession)) {
    await switchSession(targetSession)
  }
})

async function loadSessions() {
  try {
    const r = await getAiChatSessions()
    if (r.code === 200) {
      sessions.value = r.data || []
      if (sessions.value.length > 0 && !currentSessionId.value) {
        await switchSession(sessions.value[0].session_id_wsh)
      } else if (sessions.value.length === 0) {
        showWelcome()
      }
    }
  } catch (e) {
    showWelcome()
  }
}

function showWelcome() {
  messages.value = []
}

function newSession() {
  currentSessionId.value = ''
  showWelcome()
  scrollToBottom()
}

function useSuggestion(text) {
  input.value = text
  send()
}

async function switchSession(sessionId) {
  currentSessionId.value = sessionId
  try {
    const r = await getAiChatSessionMessages(sessionId)
    if (r.code === 200) {
      const records = r.data || []
      messages.value = records.map((msg, idx) => ({
        id: idx,
        role: msg.role_wsh === 'assistant' ? 'ai' : msg.role_wsh,
        content: msg.content_wsh,
        sources: msg.sources_wsh ? msg.sources_wsh.split(',').filter(Boolean) : [],
        needHuman: !!msg.need_human_wsh,
      }))
      scrollToBottom()
    }
  } catch (e) {
    showWelcome()
  }
}

async function deleteSession(sessionId) {
  if (!confirm('确认删除这条聊天记录？')) return
  try {
    await clearAiChatSession(sessionId)
    sessions.value = sessions.value.filter(s => s.session_id_wsh !== sessionId)
    if (currentSessionId.value === sessionId) {
      currentSessionId.value = ''
      if (sessions.value.length > 0) {
        await switchSession(sessions.value[0].session_id_wsh)
      } else {
        showWelcome()
      }
    }
  } catch (e) {
    appStore.addToast('删除失败', 'error')
  }
}

async function send() {
  if (!input.value || loading.value) return
  const text = input.value
  messages.value.push({ id: Date.now(), role: 'user', content: text })
  input.value = ''
  loading.value = true
  scrollToBottom()
  try {
    const history = messages.value
      .filter(m => m.role === 'user' || m.role === 'ai')
      .slice(-10)
      .map(m => ({ role: m.role === 'ai' ? 'assistant' : m.role, content: m.content }))
    const r = await aiChat({
      message_wsh: text,
      session_id_wsh: currentSessionId.value || undefined,
      history_wsh: history.slice(0, -1),
    })
    if (r.code === 200) {
      if (r.data.session_id_wsh && !currentSessionId.value) {
        currentSessionId.value = r.data.session_id_wsh
        loadSessions()
      }
      messages.value.push({
        id: Date.now(),
        role: 'ai',
        content: r.data.reply_wsh || '暂无回复',
        sources: r.data.sources_wsh || [],
        needHuman: !!r.data.need_human_wsh,
      })
    } else {
      messages.value.push({ id: Date.now(), role: 'ai', content: r.message || '服务暂时不可用，请稍后再试。' })
    }
  } catch (e) {
    messages.value.push({ id: Date.now(), role: 'ai', content: e?.response?.data?.message || '请求失败，请稍后重试。' })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

async function handoffToHuman() {
  if (handoffLoading.value) return
  handoffLoading.value = true
  try {
    const r = await assignCustomerServiceAgent()
    if (r.code === 200 && r.data?.user_id_wsh) {
      router.push({
        path: '/chat',
        query: { userId: r.data.user_id_wsh, name: r.data.name_wsh || '' },
      })
    } else {
      appStore.addToast(r.message || '暂无可用的在线客服，请稍后再试', 'error')
    }
  } catch (e) {
    appStore.addToast(e?.response?.data?.message || '转接失败，请稍后再试', 'error')
  } finally {
    handoffLoading.value = false
  }
}

function dateTimeText(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

function scrollToBottom() {
  nextTick(() => {
    const el = messagesRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}
</script>

<style scoped>
.ac-page {
  width: 100%;
  padding: 6px 0 72px;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}

.ac-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ═══ Breadcrumb ═══ */
.ac-crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 12.5px;
  color: var(--ref-muted);
}
.ac-crumb-link { color: var(--ref-muted); text-decoration: none; }
.ac-crumb-link:hover { color: var(--ref-ink); }
.ac-crumb-sep { color: var(--ref-line); }
.ac-crumb-here { color: var(--ref-ink-soft); }

/* ═══ Eyebrow ═══ */
.ac-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 9.5px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.ac-eyebrow-line { width: 32px; height: 1px; background: var(--ref-line); }
.ac-idx { font-variant-numeric: tabular-nums; }
.ac-line { width: 24px; height: 1px; background: var(--ref-line); }

/* ═══ Hero ═══ */
.ac-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px 24px;
  padding: 40px 0 28px;
}
.ac-head-copy { min-width: 0; }
.ac-title {
  margin: 18px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(34px, 4.4vw, 52px);
  line-height: 1.12;
  letter-spacing: -0.01em;
  font-weight: 500;
  color: var(--ref-ink);
  text-wrap: balance;
}
.ac-sub {
  margin: 14px 0 0;
  max-width: 620px;
  font-size: 14px;
  line-height: 1.75;
  color: color-mix(in srgb, var(--ref-ink-soft) 82%, transparent);
}
.ac-actions { display: flex; flex-wrap: wrap; gap: 10px; }

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
.cta-outline:hover { border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent); }
.cta-sm { height: var(--control-height-sm); padding: 0 14px; font-size: 12px; border-radius: 9px; }

/* ═══ Section ═══ */
.ac-section { margin-top: 44px; }
.ac-sec-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px 24px;
}
.ac-sec-eyebrow { font-size: 10px; letter-spacing: 0.22em; }
.ac-sec-title {
  margin: 10px 0 0;
  font-family: var(--ref-font-display);
  font-size: 24px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: var(--ref-ink);
}
.ac-sec-desc {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ref-ink-soft);
  opacity: 0.8;
}

/* ═══ 对话布局 ═══ */
.ac-grid {
  display: grid;
  grid-template-columns: 1fr 18rem;
  gap: 18px;
  margin-top: 20px;
  align-items: start;
}

/* ═══ 对话面板 ═══ */
.ac-chat {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-card);
  background: var(--ref-surface);
  overflow: hidden;
}
.ac-messages {
  flex: 1;
  min-height: 420px;
  max-height: 60vh;
  overflow-y: auto;
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.ac-welcome {
  margin: auto;
  max-width: 460px;
  text-align: center;
  padding: 30px 0;
}
.ac-welcome-title {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 22px;
  font-weight: 500;
  color: var(--ref-ink);
}
.ac-welcome-desc {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ref-muted);
}
.ac-suggestions {
  display: grid;
  gap: 8px;
  margin-top: 22px;
}
.ac-chip {
  padding: 11px 14px;
  border: 1px solid var(--ref-line);
  border-radius: 10px;
  background: var(--ref-surface);
  color: color-mix(in srgb, var(--ref-ink-soft) 85%, transparent);
  font-size: 12.5px;
  line-height: 1.6;
  cursor: pointer;
  text-align: left;
  transition: border-color 0.15s, background 0.15s;
}
.ac-chip:hover {
  border-color: color-mix(in srgb, var(--ref-ink) 25%, transparent);
  background: color-mix(in srgb, var(--ref-sand) 50%, transparent);
}

.ac-row { display: flex; }
.ac-row.is-user { justify-content: flex-end; }
.ac-bubble {
  max-width: min(620px, 85%);
  border: 1px solid var(--ref-line);
  border-radius: 14px;
  padding: 12px 16px;
  background: color-mix(in srgb, var(--ref-cream) 55%, transparent);
  color: var(--ref-ink-soft);
}
.ac-row.is-user .ac-bubble {
  background: var(--ref-ink);
  color: var(--ref-cream);
  border-color: transparent;
}
.ac-bubble p {
  margin: 0;
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13.5px;
}
.ac-sources {
  margin-top: 9px;
  padding-top: 9px;
  border-top: 1px solid color-mix(in srgb, var(--ref-line) 80%, transparent);
  font-size: 11.5px;
  color: var(--ref-muted);
  line-height: 1.6;
}
.ac-row.is-user .ac-sources {
  border-top-color: color-mix(in srgb, var(--ref-cream) 15%, transparent);
  color: color-mix(in srgb, var(--ref-cream) 55%, transparent);
}
.ac-handoff {
  margin-top: 10px;
  padding: 10px 12px;
  border: 1px solid color-mix(in srgb, var(--ref-brand) 40%, var(--ref-line));
  border-radius: 10px;
  background: color-mix(in srgb, var(--ref-brand) 8%, var(--ref-surface));
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.ac-handoff-text {
  margin: 0;
  font-size: 12.5px;
  color: var(--ref-ink-soft);
}
.ac-typing {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  align-self: flex-start;
  padding: 10px 14px;
  border: 1px solid var(--ref-line);
  border-radius: 14px;
  background: color-mix(in srgb, var(--ref-cream) 55%, transparent);
}
.ac-typing-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--ref-muted);
  animation: ac-blink 1.2s infinite both;
}
.ac-typing-dot:nth-child(2) { animation-delay: 0.15s; }
.ac-typing-dot:nth-child(3) { animation-delay: 0.3s; }
.ac-typing-text { font-size: 11.5px; color: var(--ref-muted); margin-left: 2px; }

/* ═══ 输入区 ═══ */
.ac-input {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 14px 16px;
  border-top: 1px solid var(--ref-line);
  background: color-mix(in srgb, var(--ref-cream) 35%, transparent);
}
.ac-input textarea {
  flex: 1;
  min-width: 0;
  resize: none;
  height: 46px;
  padding: 12px 14px;
  border: 1px solid var(--ref-line);
  border-radius: 10px;
  background: var(--ref-surface);
  color: var(--ref-ink);
  font-size: 13.5px;
  line-height: 1.5;
  font-family: inherit;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.ac-input textarea::placeholder { color: var(--ref-muted); }
.ac-input textarea:focus {
  outline: none;
  border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 12%, transparent);
}
.ac-input textarea:disabled { opacity: 0.6; }

/* ═══ 历史会话 ═══ */
.ac-side {
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-card);
  background: var(--ref-surface);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.ac-side-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 15px 18px;
  border-bottom: 1px solid var(--ref-line);
  color: var(--ref-muted);
}
.ac-side-title {
  margin: 0;
  font-size: 13px;
  font-weight: 500;
  color: var(--ref-ink);
}
.ac-side-count {
  margin-left: auto;
  font-size: 11px;
  color: var(--ref-muted);
}
.ac-side-empty {
  padding: 34px 20px;
  text-align: center;
}
.ac-side-empty-text {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--ref-muted);
}
.ac-session-list {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 26rem;
  overflow-y: auto;
}
.ac-session {
  display: flex;
  align-items: stretch;
  border-bottom: 1px solid var(--ref-line);
}
.ac-session:last-child { border-bottom: none; }
.ac-session.is-active { background: color-mix(in srgb, var(--ref-sand) 40%, transparent); }
.ac-session-main {
  flex: 1;
  min-width: 0;
  padding: 13px 16px;
  background: none;
  border: none;
  cursor: pointer;
  text-align: left;
}
.ac-session-title {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-size: 12.5px;
  line-height: 1.6;
  color: color-mix(in srgb, var(--ref-ink-soft) 85%, transparent);
}
.ac-session.is-active .ac-session-title { color: var(--ref-ink); font-weight: 500; }
.ac-session-meta {
  display: block;
  margin-top: 6px;
  font-size: 11px;
  color: var(--ref-muted);
}
.ac-session-del {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 8px;
  background: none;
  border: none;
  color: var(--ref-muted);
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.15s, color 0.15s;
}
.ac-session:hover .ac-session-del { opacity: 1; }
.ac-session-del:hover { color: var(--ref-brand-deep); }

/* ═══ Animations ═══ */
@keyframes ac-blink {
  0%, 80%, 100% { opacity: 0.25; }
  40% { opacity: 1; }
}

/* ═══ Responsive ═══ */
@media (max-width: 980px) {
  .ac-grid { grid-template-columns: 1fr; }
  .ac-session-list { max-height: 16rem; }
}
@media (max-width: 640px) {
  .ac-shell { padding: 0 16px; }
  .ac-head { padding: 30px 0 22px; }
  .ac-sub { font-size: 13.5px; }
  .ac-section { margin-top: 36px; }
  .ac-messages { min-height: 360px; padding: 16px; }
  .ac-input { padding: 12px; }
}
@media (prefers-reduced-motion: reduce) {
  .ac-typing-dot { animation: none; }
}
</style>
