<template>
  <div>
    <PageHero title="消息中心" subtitle="与商家和寄养员沟通" />
    <div class="chat-container">
      <div class="chat-sidebar">
        <div style="padding:16px;border-bottom:1px solid var(--color-border);font-weight:600">会话列表</div>
        <div v-for="conv in conversations" :key="conv.id_wsh" class="card" style="margin:8px;padding:12px;border-radius:8px;cursor:pointer"
          @click="activeConv = conv.id_wsh">
          <div style="font-weight:600;font-size:14px">{{ conv.name_wsh }}</div>
          <div style="font-size:12px;color:var(--color-muted-foreground);margin-top:4px">{{ conv.last_message_wsh || '暂无消息' }}</div>
        </div>
      </div>
      <div class="chat-conversation">
        <div v-if="!activeConv" style="display:flex;align-items:center;justify-content:center;height:100%;color:var(--color-muted-foreground)">
          选择一个会话开始聊天
        </div>
        <template v-else>
          <div style="display:flex;justify-content:space-between;align-items:center;padding:12px 16px;border-bottom:1px solid var(--color-border)">
            <span style="font-weight:600">消息</span>
            <button class="btn btn-sm btn-outline" @click="markAllRead">标记已读</button>
          </div>
          <div class="chat-messages" ref="msgContainer">
            <div v-for="msg in messages" :key="msg.id_wsh" style="margin-bottom:16px;cursor:pointer" @click="markMessageRead(msg)">
              <div :style="{ textAlign: msg.from_user_id_wsh === authStore.user?.id_wsh ? 'right' : 'left' }">
                <span class="card" :style="{ display:'inline-block', padding:'8px 16px', borderRadius:'12px',
                  background: !msg.is_read_wsh && msg.from_user_id_wsh !== authStore.user?.id_wsh ? '#d0e8ff' : (msg.from_user_id_wsh === authStore.user?.id_wsh ? 'var(--color-primary)' : 'var(--color-muted)'),
                  color: msg.from_user_id_wsh === authStore.user?.id_wsh ? 'var(--color-on-primary)' : 'var(--color-foreground)' }">
                  {{ msg.content_wsh }}
                </span>
                <div style="font-size:11px;color:var(--color-muted-foreground);margin-top:4px">{{ new Date(msg.created_at_wsh).toLocaleTimeString() }}</div>
              </div>
            </div>
          </div>
          <div class="chat-input-area">
            <input v-model="newMsg" @keyup.enter="sendMsg" placeholder="输入消息..." style="flex:1">
            <button class="btn btn-primary btn-sm" @click="sendMsg">发送</button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import PageHero from '@/components/common/PageHero.vue'

const authStore = useAuthStore()
const appStore = useAppStore()
const conversations = ref([])
const messages = ref([])
const activeConv = ref(null)
const newMsg = ref('')
const msgContainer = ref(null)
const otherUserId = ref(null)
const orderId = ref(null)

onMounted(async () => {
  const params = new URLSearchParams(window.location.search)
  otherUserId.value = params.get('userId') || null
  orderId.value = params.get('orderId') || null
  try { const r = await authStore.apiGet('/api/chat/unread'); if (r.code === 200) conversations.value = r.data || [] }
  catch (e) {}
})

watch(activeConv, async (convId) => {
  if (convId == null) return
  try {
    const params = { otherUserId: otherUserId.value || 1 }
    if (orderId.value) params.orderId = orderId.value
    const r = await authStore.apiGet('/api/chat/conversation', params)
    if (r.code === 200) messages.value = Array.isArray(r.data) ? r.data : []
  } catch (e) {
    appStore.addToast('加载消息失败', 'error')
  }
})

async function sendMsg() {
  if (!newMsg.value.trim()) return
  try {
    const r = await authStore.apiPost('/api/chat/send', { to_user_id_wsh: otherUserId.value, content_wsh: newMsg.value, order_id_wsh: orderId.value })
    if (r.code === 200) { messages.value.push(r.data); newMsg.value = '' }
  } catch (e) { appStore.addToast('发送失败', 'error') }
}

async function markAllRead() {
  if (!otherUserId.value) return
  try {
    const r = await authStore.apiPost('/api/chat/read-conversation', { other_user_id_wsh: otherUserId.value, order_id_wsh: orderId.value })
    if (r.code === 200) { appStore.addToast('已标记已读', 'success'); messages.value.forEach(m => { m.is_read_wsh = true }) }
  } catch (e) { appStore.addToast('操作失败', 'error') }
}

async function markMessageRead(msg) {
  if (msg.is_read_wsh && msg.from_user_id_wsh === authStore.user?.id_wsh) return
  try {
    const r = await authStore.apiPost(`/api/chat/read/${msg.id_wsh}`)
    if (r.code === 200) msg.is_read_wsh = true
  } catch (e) {}
}
</script>
