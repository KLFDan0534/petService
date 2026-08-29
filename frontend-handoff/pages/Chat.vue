<template>
  <div class="user-chat-page">
    <div class="chat-shell">
      <aside class="conv-sidebar">
        <div class="conv-sidebar__head">
          <h3>会话</h3>
          <button class="btn btn-sm btn-outline" type="button" @click="loadConversations">刷新</button>
        </div>
        <div v-if="convLoading" class="loading small">加载中...</div>
        <div v-else-if="!conversations.length" class="empty-inline">
          暂无会话<br>联系商家或客服后，对话会显示在这里
        </div>
        <button
          v-for="c in conversations"
          :key="c.other_user_id_wsh"
          class="conv-item"
          :class="{ active: activeUserId === c.other_user_id_wsh }"
          type="button"
          @click="openConversation(c.other_user_id_wsh)"
        >
          <div class="conv-head">
            <strong>{{ c.other_user_name_wsh }}</strong>
            <span v-if="c.unread_count > 0" class="badge badge-danger">{{ c.unread_count }}</span>
          </div>
          <span class="conv-preview">{{ c.last_message_wsh || '(空)' }}</span>
        </button>
      </aside>

      <section class="chat-main">
        <template v-if="activeUserId">
          <header class="chat-header">
            <strong>{{ activeUserName }}</strong>
          </header>
          <div ref="messageBox" class="message-box">
            <div
              v-for="m in messages"
              :key="m.id_wsh"
              class="msg"
              :class="{ mine: m.from_user_id_wsh === currentUserId }"
            >
              <div class="msg-bubble">
                <span class="msg-meta">{{ m.from_user_id_wsh === currentUserId ? '我' : activeUserName }}</span>
                <a
                  v-if="m.file_url_wsh"
                  :href="m.file_url_wsh"
                  target="_blank"
                  rel="noreferrer"
                  class="chat-photo"
                >
                  <img v-if="isImageUrl(m.file_url_wsh)" :src="m.file_url_wsh" alt="图片消息" loading="lazy" @error="$event.target.style.display = 'none'">
                  <span v-else>查看附件</span>
                </a>
                <p v-if="m.content_wsh">{{ m.content_wsh }}</p>
                <time>{{ formatTime(m.created_at_wsh) }}</time>
              </div>
            </div>
            <div v-if="!messages.length" class="empty-inline">开始对话吧</div>
          </div>
          <div class="chat-composer">
            <div v-if="photoUrls.length" class="photo-preview">
              <div v-for="(url, i) in photoUrls" :key="`${url}-${i}`" class="photo-preview-item">
                <img :src="url" :alt="`待发送照片 ${i + 1}`">
                <button class="photo-preview-remove" type="button" :disabled="sending" aria-label="移除照片" @click="removePhoto(i)">✕</button>
              </div>
            </div>
            <footer class="chat-input">
              <input
                ref="fileInput"
                type="file"
                accept="image/*"
                multiple
                class="chat-file-input"
                hidden
                @change="handlePhoto"
              >
              <button class="btn btn-outline chat-photo-btn" type="button" title="发送照片（可多选）" :disabled="sending" @click="fileInput.click()">
                📷
              </button>
              <input
                v-model="draft"
                class="form-control"
                placeholder="输入消息，Enter 发送"
                @keyup.enter="send"
              >
              <button class="btn btn-primary" type="button" :disabled="(!draft.trim() && !photoUrls.length) || sending" @click="send">
                {{ sending ? '发送中...' : '发送' }}
              </button>
            </footer>
          </div>
        </template>
        <div v-else class="chat-empty">
          <div class="chat-empty-inner">
            <div class="chat-empty-icon">💬</div>
            <p>选择左侧会话查看聊天记录</p>
          </div>
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
import { getConversations, sendChatMessage, markConversationRead } from '@/api/chat'
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
const photoUrls = ref([])
const fileInput = ref(null)
const messageBox = ref(null)
let eventSource = null

const currentUserId = computed(() => authStore.user?.id_wsh)
const token = computed(() => authStore.token || localStorage.getItem('token'))

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
        if (conv) conv.unread_count = 0
      }
    }
  } catch (e) { /* ignore */ }
  await nextTick()
  scrollToBottom()
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
    // 每张照片发送一条图片消息
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
    // 文字作为最后一条消息发送
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
    // 首次发送（如转人工后的新会话）：在列表中创建会话条目
    conv = { other_user_id_wsh: activeUserId.value, other_user_name_wsh: activeUserName.value }
  }
  conv.last_message_wsh = sent.content_wsh
  conv.last_time_wsh = sent.created_at_wsh
  conversations.value.unshift(conv)
}

function formatTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString()
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
            conv.unread_count = (conv.unread_count || 0) + 1
          }
          conv.last_message_wsh = msg.content_wsh
          conv.last_time_wsh = msg.created_at_wsh
          conversations.value = [conv, ...conversations.value.filter(c => c.other_user_id_wsh !== conv.other_user_id_wsh)]
        } else {
          // 新会话：从对端发来的第一条消息
          conversations.value.unshift({
            other_user_id_wsh: msg.from_user_id_wsh,
            other_user_name_wsh: `用户#${msg.from_user_id_wsh}`,
            last_message_wsh: msg.content_wsh,
            last_time_wsh: msg.created_at_wsh,
            unread_count: msg.to_user_id_wsh === currentUserId.value ? 1 : 0,
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
    // 兼容旧的订单聊天入口：跳转订单详情聊天页
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
.user-chat-page {
  display: grid;
  gap: 16px;
}
.chat-shell {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 16px;
  height: calc(100vh - 260px);
  min-height: 420px;
}
@media (max-width: 800px) {
  .chat-shell {
    grid-template-columns: 1fr;
    grid-template-rows: minmax(0, 30%) minmax(0, 1fr);
    height: calc(100vh - 280px);
    min-height: 480px;
  }
  .conv-sidebar {
    overflow-y: auto;
  }
  .chat-main {
    min-height: 0;
  }
}
.conv-sidebar {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-card);
  overflow-y: auto;
  padding: 12px;
}
.conv-sidebar__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.conv-sidebar__head h3 {
  margin: 0;
}
.conv-item {
  display: grid;
  gap: 4px;
  width: 100%;
  text-align: left;
  padding: 10px;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  background: transparent;
  cursor: pointer;
  color: var(--color-foreground);
}
.conv-item:hover,
.conv-item.active {
  background: var(--color-muted);
  border-color: var(--color-border);
}
.conv-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 6px;
}
.conv-preview {
  font-size: 12px;
  color: var(--color-muted-foreground);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.chat-main {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-card);
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}
.chat-header {
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-border);
  flex: 0 0 auto;
}
.message-box {
  flex: 1 1 auto;
  min-height: 120px;
  overflow-y: auto;
  padding: 16px;
  display: grid;
  gap: 10px;
  align-content: start;
}
.msg {
  display: flex;
  justify-content: flex-start;
}
.msg.mine {
  justify-content: flex-end;
}
.msg-bubble {
  max-width: 70%;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 8px 12px;
  background: var(--color-muted);
  color: var(--color-foreground);
}
.msg.mine .msg-bubble {
  background: color-mix(in srgb, var(--color-primary) 12%, var(--color-card));
}
.msg-meta {
  font-size: 11px;
  font-weight: 600;
  color: var(--color-primary);
}
.msg-bubble p {
  margin: 4px 0;
}
.msg-bubble time {
  font-size: 11px;
  color: var(--color-muted-foreground);
}
.chat-composer {
  border-top: 1px solid var(--color-border);
  flex: 0 0 auto;
  margin-top: auto;
}
.photo-preview {
  display: flex;
  gap: 10px;
  padding: 10px 12px 2px;
  overflow-x: auto;
}
.photo-preview-item {
  position: relative;
  flex: 0 0 auto;
}
.photo-preview-item img {
  display: block;
  width: 64px;
  height: 64px;
  object-fit: cover;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
}
.photo-preview-remove {
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
.photo-preview-remove:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.chat-input {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  height: 56px;
  flex: 0 0 56px;
  box-sizing: border-box;
}
.chat-input input {
  flex: 1;
  min-width: 0;
  height: 36px;
  padding: 0 12px;
  font-size: 14px;
}
.chat-input .btn {
  height: 36px;
  min-height: 0;
  padding: 0 18px;
  flex: 0 0 auto;
}
.chat-photo-btn {
  width: 36px;
  height: 36px;
  min-height: 0;
  padding: 0;
  flex: 0 0 36px;
  font-size: 17px;
  line-height: 1;
}
.chat-photo {
  display: block;
  margin: 4px 0;
}
.chat-photo img {
  max-width: 220px;
  max-height: 220px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
  display: block;
}
.chat-empty {
  flex: 1 1 auto;
  min-height: 120px;
  display: grid;
  place-items: center;
  color: var(--color-muted-foreground);
}
.chat-empty-inner {
  text-align: center;
}
.chat-empty-icon {
  font-size: 36px;
  margin-bottom: 8px;
}
.loading.small {
  font-size: 13px;
  padding: 8px 0;
}
.empty-inline {
  color: var(--color-muted-foreground);
  font-size: 13px;
  padding: 12px 0;
  line-height: 1.6;
}
</style>
