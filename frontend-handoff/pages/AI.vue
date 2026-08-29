<template>
  <div class="ai-page">
    <div class="ai-layout">
      <!-- 会话列表侧边栏 -->
      <aside class="chat-sidebar">
        <div class="sidebar-header">
          <h3>历史对话</h3>
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

      <!-- 主区域 -->
      <div class="ai-main">
        <!-- 顶部：宠物/订单选择 + 报告生成 -->
        <div class="report-toolbar">
          <div class="toolbar-fields">
            <div class="form-group">
              <label>选择宠物</label>
              <select v-model="selectedPetId" class="ai-select" @change="onPetChange">
                <option value="">-- 请选择 --</option>
                <option v-for="p in pets" :key="p.id_wsh" :value="p.id_wsh">{{ p.name_wsh || p.pet_name_wsh }}</option>
              </select>
            </div>
            <div class="form-group">
              <label>已完成订单</label>
              <select v-model="selectedOrderId" class="ai-select" :disabled="!selectedPetId">
                <option value="">-- 请选择 --</option>
                <option v-for="o in completedOrders" :key="o.id_wsh" :value="o.id_wsh">
                  #{{ o.id_wsh }} {{ formatDate(o.created_at_wsh) }}
                </option>
              </select>
            </div>
            <button class="btn btn-primary btn-sm" :disabled="!selectedPetId || !selectedOrderId || reportLoading" @click="generateReport('care_suggestion')">
              培养建议
            </button>
            <button class="btn btn-success btn-sm" :disabled="!selectedPetId || !selectedOrderId || reportLoading" @click="generateReport('boarding_report')">
              寄养报告
            </button>
          </div>
        </div>

        <!-- 聊天区域 -->
        <div class="chat-conversation" ref="messagesRef">
          <div v-for="msg in messages" :key="msg.id" :class="['message-row', `message-row--${msg.role}`]">
            <div class="message-bubble">
              <p>{{ msg.content }}</p>
              <div v-if="msg.reportType" class="message-report-tag">
                <span class="badge" :class="msg.reportType === 'care_suggestion' ? 'badge-info' : 'badge-success'">
                  {{ msg.reportType === 'care_suggestion' ? '培养建议' : '寄养报告' }}
                </span>
              </div>
              <div v-if="msg.sources && msg.sources.length" class="message-sources">
                参考文档：{{ msg.sources.join('、') }}
              </div>
            </div>
          </div>
          <div v-if="loading" class="loading">AI 思考中...</div>
          <div v-if="reportLoading" class="loading">生成报告中...</div>
        </div>

        <!-- 输入区域 -->
        <form class="chat-input-area" @submit.prevent="ask">
          <input
            v-model.trim="question"
            placeholder="输入您的问题，例如：宠物寄养注意事项？"
            maxlength="500"
            :disabled="loading"
          >
          <button class="btn btn-primary btn-sm" type="submit" :disabled="loading || !question">
            {{ loading ? '发送中...' : '发送' }}
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref, computed } from 'vue'
import { getPets } from '@/api/pet'
import { getOrders } from '@/api/order'
import { aiChat, getCareSuggestion, getBoardingReport, getAiChatSessions, getAiChatSessionMessages, clearAiChatSession } from '@/api/ai'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()

// --- 聊天状态 ---
const messages = ref([{ id: 0, role: 'ai', content: '您好！我是宠物护理 AI 助手，可以帮您解答寄养、护理、报告等问题。' }])
const question = ref('')
const loading = ref(false)
const reportLoading = ref(false)
const messagesRef = ref(null)

// --- 宠物/订单 ---
const pets = ref([])
const orders = ref([])
const selectedPetId = ref('')
const selectedOrderId = ref('')

const completedOrders = computed(() => {
  if (!selectedPetId.value) return []
  return orders.value.filter(o =>
    o.status_wsh === 'completed' && String(o.pet_id_wsh) === String(selectedPetId.value)
  )
})

// --- 会话管理 ---
const sessions = ref([])
const currentSessionId = ref('')

onMounted(async () => {
  await loadSessions()
  try {
    const r = await getPets()
    if (r.code === 200) pets.value = Array.isArray(r.data) ? r.data : []
  } catch (e) {}
  try {
    const r = await getOrders()
    if (r.code === 200) orders.value = Array.isArray(r.data) ? r.data : []
  } catch (e) {}
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
  messages.value = [{
    id: 0,
    role: 'ai',
    content: '您好！我是宠物护理 AI 助手，可以帮您解答寄养、护理、报告等问题。',
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
        reportType: null,
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

// --- 宠物/订单 ---
function formatDate(dt) {
  if (!dt) return ''
  return dt.slice(0, 10)
}

function onPetChange() {
  selectedOrderId.value = ''
}

// --- 报告生成 ---
async function generateReport(type) {
  if (!selectedPetId.value || !selectedOrderId.value) return
  reportLoading.value = true
  const label = type === 'care_suggestion' ? '培养建议' : '培养完成报告'
  messages.value.push({ id: Date.now(), role: 'user', content: `为宠物 #${selectedPetId.value} 订单 #${selectedOrderId.value} 生成${label}` })
  scrollToBottom()
  try {
    const fn = type === 'care_suggestion' ? getCareSuggestion : getBoardingReport
    const r = await fn({ pet_id_wsh: Number(selectedPetId.value), order_id_wsh: Number(selectedOrderId.value) })
    if (r.code === 200) {
      messages.value.push({ id: Date.now(), role: 'ai', content: r.data.content_wsh || '生成成功', reportType: type })
    } else {
      messages.value.push({ id: Date.now(), role: 'ai', content: '生成失败：' + (r.message || '未知错误') })
    }
  } catch (e) {
    messages.value.push({ id: Date.now(), role: 'ai', content: '生成报告失败，请稍后重试。' })
  }
  reportLoading.value = false
  scrollToBottom()
}

// --- 聊天提问 ---
async function ask() {
  if (!question.value.trim() || loading.value) return
  const text = question.value
  messages.value.push({ id: Date.now(), role: 'user', content: text })
  question.value = ''
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
      })
    } else {
      messages.value.push({ id: Date.now(), role: 'ai', content: r.message || '服务暂时不可用，请稍后再试。' })
    }
  } catch (e) {
    messages.value.push({ id: Date.now(), role: 'ai', content: '请求失败，请稍后重试。' })
  } finally {
    loading.value = false
    scrollToBottom()
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
.ai-page {
  display: grid;
  gap: 16px;
}

.ai-layout {
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

/* ===== 主区域 ===== */
.ai-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ===== 报告工具栏 ===== */
.report-toolbar {
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-card);
}

.toolbar-fields {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  flex-wrap: wrap;
}

.toolbar-fields .form-group {
  margin-bottom: 0;
  min-width: 160px;
}

.toolbar-fields label {
  display: block;
  font-size: 12px;
  margin-bottom: 3px;
  color: var(--color-muted-foreground);
  font-weight: 600;
}

.ai-select {
  height: 38px;
  padding: 0 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  color: var(--color-foreground);
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
}

.ai-select option {
  background: var(--color-card);
  color: var(--color-foreground);
  padding: 6px 8px;
}

.ai-select:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.toolbar-fields .btn {
  height: 34px;
  min-height: 0;
  padding: 0 14px;
  font-size: 13px;
}

/* ===== 聊天消息 ===== */
.chat-conversation {
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

.message-report-tag {
  margin-top: 8px;
}

.message-sources {
  margin-top: 8px;
  font-size: 12px;
  color: var(--color-muted-foreground);
}

/* ===== 输入区域 ===== */
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
