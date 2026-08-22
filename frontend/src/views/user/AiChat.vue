<template>
  <div class="ai-chat-page">
    <PageHero title="智能客服" subtitle="7×24 在线，常见问题即时解答；需要人工时一键转接" />

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
</template>

<script setup>
import { nextTick, ref } from 'vue'
import { useRouter } from 'vue-router'
import { aiChat } from '@/api/ai'
import { assignCustomerServiceAgent } from '@/api/chat'
import { useAppStore } from '@/stores/app'
import PageHero from '@/components/common/PageHero.vue'

const router = useRouter()
const appStore = useAppStore()
const handoffLoading = ref(false)
const messages = ref([
  {
    id: 0,
    role: 'ai',
    content: '您好，我是宠物寄养平台的智能客服。寄养规则、退款、投诉、护理等问题都可以问我。如果需要人工服务，也可以随时告诉我。',
  },
])
const input = ref('')
const loading = ref(false)
const messagesRef = ref(null)

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
      .map(m => ({ role: m.role, content: m.content }))
    const r = await aiChat({ message_wsh: text, history_wsh: history.slice(0, -1) })
    if (r.code === 200) {
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
      // 系统随机分配客服后直接进入消息界面，与该客服实时沟通
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
.ai-chat-shell {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 280px);
  min-height: 480px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-card);
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
  max-width: min(720px, 88%);
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
