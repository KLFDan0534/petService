<template>
  <div class="ai-chat-page">
    <div class="ai-chat-layout">
      <!-- 会话列表侧边栏 -->
      <aside class="chat-sidebar">
        <div class="sidebar-header">
          <h3>聊天记录</h3>
          <button class="btn btn-primary btn-sm" @click="newSession" title="新对话">+ 新对话</button>
        </div>
        <div class="session-list" v-if="sessions.length">
          <div
            v-for="s in sessions"
            :key="s.session_id_wsh"
            :class="['session-item', { active: currentSessionId === s.session_id_wsh }]"
            @click="switchSession(s.session_id_wsh)"
          >
            <div class="session-title">{{ s.first_message_wsh || '新对话' }}</div>
            <div class="session-meta">
              <span>{{ s.message_count_wsh }} 条消息</span>
              <button class="session-delete" @click.stop="deleteSession(s.session_id_wsh)" title="删除">✕</button>
            </div>
          </div>
        </div>
        <div v-else class="session-empty">暂无聊天记录</div>
      </aside>

      <!-- 聊天主区域 -->
      <div class="ai-chat-shell">
        <div class="chat-messages" ref="messagesRef">
          <article v-for="msg in messages" :key="msg.id" :class="['message-row', `message-row--${msg.role}`]">
            <div class="message-bubble">
              <p>{{ msg.content }}</p>
              <div v-if="msg.sources && msg.sources.length" class="message-sources">
                参考文档：{{ msg.sources.join('、') }}
              </div>
              <div v-if="msg.needHuman" class="handoff-panel">
                <p class="handoff-text">您的问题需要人工客服协助，点击下方按钮转接。</p>
                <button class="btn btn-primary btn-sm" type="button" :disabled="handoffLoading" @click="handoffToHuman">
                  {{ handoffLoading ? '正在接入客服...' : '转人工客服' }}
                </button>
              </div>
            </div>
          </article>

          <div v-if="loading" class="loading">AI 思考中...</div>
        </div>

        <form class="chat-input-area" @submit.prevent="send">
          <input
            v-model.trim="input"
            placeholder="请输入您的问题，例如：退款规则是什么？"
            maxlength="500"
            :disabled="loading"
          >
          <button class="btn btn-primary btn-sm" type="submit" :disabled="loading || !input">
            {{ loading ? '发送中...' : '发送' }}
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { aiChat, getAiChatSessions, getAiChatSessionMessages, clearAiChatSession } from '@/api/ai'
import { assignCustomerServiceAgent } from '@/api/chat'
import { useAppStore } from '@/stores/app'

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
  // 支持从导航栏下拉跳转时携带的 session query 参数
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
      // 如果有历史会话，加载最新一个
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
  messages.value = [{
    id: 0,
    role: 'ai',
    content: '您好，我是宠物寄养平台的智能客服。寄养规则、退款、投诉、护理等问题都可以问我。如果需要人工服务，也可以随时告诉我。',
  }]
}

function newSession() {
  currentSessionId.value = ''
  showWelcome()
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
      // 更新 session_id（新会话时后端返回）
      if (r.data.session_id_wsh && !currentSessionId.value) {
        currentSessionId.value = r.data.session_id_wsh
        // 刷新侧边栏
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

function scrollToBottom() {
  nextTick(() => {
    const el = messagesRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}
</script>

<style scoped>
.ai-chat-page {
  display: grid;
  gap: 16px;
}

.ai-chat-layout {
  display: flex;
  height: calc(100vh - 280px);
  min-height: 480px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-card);
  overflow: hidden;
}

/* ===== 侧边栏 ===== */
.chat-sidebar {
  width: 260px;
  min-width: 200px;
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  background: var(--color-muted);
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid var(--color-border);
}

.sidebar-header h3 {
  margin: 0;
  font-size: 15px;
  color: var(--color-foreground);
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.session-item {
  padding: 10px 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  margin-bottom: 4px;
  transition: background 0.15s;
}

.session-item:hover {
  background: var(--color-border);
}

.session-item.active {
  background: var(--color-primary);
  color: var(--color-on-primary);
}

.session-title {
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
}

.session-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11px;
  color: var(--color-muted-foreground);
  margin-top: 4px;
}

.session-item.active .session-meta {
  color: rgba(255, 255, 255, 0.7);
}

.session-delete {
  background: none;
  border: none;
  color: var(--color-muted-foreground);
  cursor: pointer;
  font-size: 12px;
  padding: 0 4px;
  line-height: 1;
  opacity: 0;
  transition: opacity 0.15s;
}

.session-item:hover .session-delete {
  opacity: 1;
}

.session-item.active .session-delete {
  color: rgba(255, 255, 255, 0.7);
}

.session-empty {
  padding: 24px 16px;
  text-align: center;
  color: var(--color-muted-foreground);
  font-size: 13px;
}

/* ===== 聊天区域 ===== */
.ai-chat-shell {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 18px;
}

.message-row {
  display: flex;
  margin-bottom: 14px;
}

.message-row--user {
  justify-content: flex-end;
}

.message-bubble {
  max-width: min(600px, 85%);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 12px 14px;
  background: var(--color-muted);
  color: var(--color-foreground);
}

.message-row--user .message-bubble {
  background: var(--color-primary);
  color: var(--color-on-primary);
  border-color: transparent;
}

.message-bubble p {
  margin: 0;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-sources {
  margin-top: 8px;
  font-size: 12px;
  color: var(--color-muted-foreground);
}

.handoff-panel {
  margin-top: 10px;
  padding: 10px 12px;
  border: 1px solid color-mix(in srgb, var(--color-primary) 40%, var(--color-border));
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--color-primary) 10%, var(--color-card));
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.handoff-text {
  color: var(--color-foreground) !important;
  font-size: 13px;
}

.chat-input-area {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  border-top: 1px solid var(--color-border);
  height: 60px;
  flex: 0 0 60px;
  box-sizing: border-box;
}

.chat-input-area input {
  flex: 1;
  min-width: 0;
  height: 38px;
  padding: 0 12px;
  font-size: 14px;
}

.chat-input-area .btn {
  height: 38px;
  min-height: 0;
  padding: 0 20px;
  flex: 0 0 auto;
}
</style>
