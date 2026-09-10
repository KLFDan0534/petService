<template>
  <div class="cs-chat">
    <aside class="conv-sidebar">
      <div class="conv-sidebar__head">
        <h3>会话</h3>
        <button class="btn btn-sm btn-outline" type="button" @click="reloadAll">刷新</button>
      </div>

      <template v-if="threads.length">
        <h4 class="conv-section-title">投诉 / 工单</h4>
        <button
          v-for="t in threads"
          :key="`${t.type_wsh}-${t.biz_id_wsh}`"
          class="conv-item"
          :class="{ active: activeThreadKey === `${t.type_wsh}-${t.biz_id_wsh}` }"
          type="button"
          @click="openThread(t)"
        >
          <div class="conv-head">
            <strong class="conv-title">{{ t.title_wsh || `#${t.biz_id_wsh}` }}</strong>
            <span v-if="t.unread_count_wsh > 0" class="badge badge-danger thread-unread">{{ t.unread_count_wsh }}</span>
            <span :class="['badge', t.type_wsh === 'complaint' ? 'badge-warning' : 'badge-info']">
              {{ t.type_wsh === 'complaint' ? '投诉' : '工单' }}
            </span>
          </div>
          <span class="conv-sub">{{ t.other_user_name_wsh }} · {{ statusLabel(t.status_wsh) }}<template v-if="t.order_no_wsh"> · {{ t.order_no_wsh }}</template></span>
          <span v-if="t.last_message_wsh" class="conv-preview">{{ t.last_message_wsh }}</span>
        </button>
      </template>

      <h4 v-if="conversations.length || threads.length" class="conv-section-title">普通会话</h4>
      <div v-if="convLoading" class="loading small">加载中...</div>
      <div v-else-if="!conversations.length && !threads.length" class="empty-inline">
        暂无会话，用户可从投诉/工单或联系入口发起沟通
      </div>
      <button
        v-for="c in conversations"
        :key="c.other_user_id_wsh"
        class="conv-item"
        :class="{ active: !activeThreadKey && activeUserId === c.other_user_id_wsh }"
        type="button"
        @click="openConversation(c.other_user_id_wsh)"
      >
        <div class="conv-head">
          <strong>{{ c.other_user_name_wsh }}</strong>
          <span v-if="c.unread_count_wsh > 0" class="badge badge-danger">{{ c.unread_count_wsh }}</span>
        </div>
        <span class="conv-preview">{{ c.last_message_wsh || '(空)' }}</span>
      </button>
    </aside>

    <section class="chat-main">
      <!-- 投诉/工单线程 -->
      <template v-if="activeThreadKey">
        <header class="chat-header thread-header">
          <div class="thread-header__info">
            <strong>{{ threadContext.title_wsh || `#${threadContext.biz_id_wsh}` }}</strong>
            <span class="thread-meta">
              <span :class="['badge', threadContext.type_wsh === 'complaint' ? 'badge-warning' : 'badge-info']">
                {{ threadContext.type_wsh === 'complaint' ? '投诉' : '工单' }}
              </span>
              <span>{{ threadContext.other_user_name_wsh || `用户#${threadContext.other_user_id_wsh}` }}</span>
              <span v-if="threadContext.status_wsh">状态：{{ statusLabel(threadContext.status_wsh) }}</span>
              <span v-if="threadContext.order_no_wsh">订单 {{ threadContext.order_no_wsh }}</span>
            </span>
          </div>
          <button v-if="threadContext.type_wsh === 'complaint'" class="btn btn-sm btn-outline" type="button" @click="threadShowEvidence = !threadShowEvidence">
            {{ threadShowEvidence ? '收起证据' : '查看证据' }}
          </button>
        </header>

        <div v-if="threadShowEvidence && threadEvidence" class="thread-evidence">
          <section v-if="threadEvidence.chat_messages_wsh?.length" class="evidence-section">
            <h4>订单聊天（{{ threadEvidence.chat_messages_wsh.length }} 条）</h4>
            <article v-for="m in threadEvidence.chat_messages_wsh" :key="m.id_wsh" class="evidence-item">
              <div class="item-meta">
                <span>{{ m.from_user_name_wsh || `用户#${m.from_user_id_wsh}` }}</span>
                <time>{{ formatTime(m.created_at_wsh) }}</time>
              </div>
              <p v-if="m.content_wsh">{{ m.content_wsh }}</p>
              <a
                v-if="m.file_url_wsh"
                :href="m.file_url_wsh"
                target="_blank"
                rel="noreferrer"
                class="evidence-photo"
              >
                <img
                  v-if="isImageUrl(m.file_url_wsh)"
                  :src="m.file_url_wsh"
                  alt="聊天附件照片"
                  loading="lazy"
                  @error="$event.target.style.display = 'none'"
                >
                <span v-else>查看附件</span>
              </a>
            </article>
          </section>
          <section v-if="threadEvidence.care_records_wsh?.length" class="evidence-section">
            <h4>照护记录（{{ threadEvidence.care_records_wsh.length }} 条）</h4>
            <article v-for="r in threadEvidence.care_records_wsh" :key="r.id_wsh" class="evidence-item">
              <div class="item-meta">
                <span>{{ r.type_wsh || '记录' }}</span>
                <time>{{ formatTime(r.record_time_wsh || r.created_at_wsh) }}</time>
              </div>
              <p v-if="r.content_wsh">{{ r.content_wsh }}</p>
              <div v-if="parseImages(r.images_wsh).length" class="evidence-photos">
                <a
                  v-for="(url, index) in parseImages(r.images_wsh)"
                  :key="`${url}-${index}`"
                  :href="url"
                  target="_blank"
                  rel="noreferrer"
                  class="evidence-photo"
                >
                  <img :src="url" :alt="`证据照片 ${index + 1}`" loading="lazy" @error="$event.target.style.display = 'none'">
                </a>
              </div>
            </article>
          </section>
          <div v-if="!threadEvidence.chat_messages_wsh?.length && !threadEvidence.care_records_wsh?.length" class="empty-inline">该投诉暂无关联证据</div>
        </div>

        <div ref="messageBox" class="message-box">
          <div v-for="m in threadMessages" :key="m.id_wsh" class="msg" :class="{ mine: m.fromUserId === currentUserId }">
            <div class="msg-bubble">
              <span class="msg-meta">{{ m.fromUserId === currentUserId ? '我' : (m.fromName || '用户') }}</span>
              <a
                v-if="m.fileUrl"
                :href="m.fileUrl"
                target="_blank"
                rel="noreferrer"
                class="chat-photo"
              >
                <img v-if="isImageUrl(m.fileUrl)" :src="m.fileUrl" alt="图片消息" loading="lazy" @error="$event.target.style.display = 'none'">
                <span v-else>查看附件</span>
              </a>
              <p v-if="m.content">{{ m.content }}</p>
              <time>{{ formatTime(m.time) }}</time>
            </div>
          </div>
          <div v-if="!threadMessages.length" class="empty-inline">
            {{ threadContext.type_wsh === 'complaint' ? '暂无投诉留言，发送第一条回复吧（用户会在投诉详情中看到）' : '暂无工单留言，发送第一条回复吧（用户会在工单详情中看到）' }}
          </div>
        </div>
        <div class="chat-composer">
          <div v-if="threadPhotoUrls.length" class="photo-preview">
            <div v-for="(url, i) in threadPhotoUrls" :key="`${url}-${i}`" class="photo-preview-item">
              <img :src="url" :alt="`待发送照片 ${i + 1}`">
              <button class="photo-preview-remove" type="button" :disabled="threadSending" aria-label="移除照片" @click="removeThreadPhoto(i)">✕</button>
            </div>
          </div>
          <footer class="chat-input">
            <input
              ref="threadFileInput"
              type="file"
              accept="image/*"
              multiple
              class="chat-file-input"
              hidden
              @change="handleThreadPhoto"
            >
            <button class="btn btn-outline chat-photo-btn" type="button" title="发送照片（可多选）" :disabled="threadSending" @click="threadFileInput.click()">
              📷
            </button>
            <input
              v-model="threadDraft"
              class="form-control"
              placeholder="输入消息，Enter 发送"
              @keyup.enter="sendThreadMessage"
            >
            <button class="btn btn-primary" type="button" :disabled="(!threadDraft.trim() && !threadPhotoUrls.length) || threadSending" @click="sendThreadMessage">
              {{ threadSending ? '发送中...' : '发送' }}
            </button>
          </footer>
        </div>
      </template>

      <!-- 普通会话 -->
      <template v-else-if="activeUserId">
        <header class="chat-header">
          <strong>{{ activeUserName }}</strong>
        </header>
        <div ref="messageBox" class="message-box">
          <div v-for="m in messages" :key="m.id_wsh" class="msg" :class="{ mine: m.from_user_id_wsh === currentUserId }">
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
          <p>选择左侧会话开始沟通</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { useCsChatStore } from '@/stores/csChat'
import { getCsConversations, getCsThreads, markCsThreadRead } from '@/api/csWorkbench'
import { getConversations, sendChatMessage, markConversationRead } from '@/api/chat'
import { getComplaintMessages, sendComplaintMessage, getComplaintEvidence } from '@/api/complaint'
import { getTicketMessages, sendTicketMessage } from '@/api/ticket'
import { uploadFileToDirectory } from '@/api/file'

const route = useRoute()
const authStore = useAuthStore()
const appStore = useAppStore()
const csChatStore = useCsChatStore()
const conversations = ref([])
const threads = ref([])
const convLoading = ref(true)
const activeUserId = ref(null)
const activeUserName = ref('')
const messages = ref([])
const draft = ref('')
const sending = ref(false)
const photoUrls = ref([])
const fileInput = ref(null)
const messageBox = ref(null)
// 线程状态
const activeThreadKey = ref(null)
const threadContext = ref({})
const threadMessages = ref([])
const threadEvidence = ref(null)
const threadShowEvidence = ref(false)
const threadDraft = ref('')
const threadSending = ref(false)
const threadPhotoUrls = ref([])
const threadFileInput = ref(null)
// 订阅全局客服消息中心（SSE 由 csChat store 统一维护）
let unsubscribeChat = null
let unsubscribeThread = null
let eventSource = null

const currentUserId = computed(() => authStore.user?.id_wsh)
const token = computed(() => authStore.token || localStorage.getItem('token'))

const THREAD_STATUS = {
  complaint: {
    pending: '待处理',
    processing: '受理中',
    resolved: '已处理',
    rejected: '已驳回',
  },
  ticket: {
    pending: '待处理',
    processing: '受理中',
    resolved: '已解决',
    closed: '已关闭',
  },
}

function statusLabel(status) {
  if (!status) return '-'
  for (const map of Object.values(THREAD_STATUS)) {
    if (map[status]) return map[status]
  }
  return status
}

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

async function loadThreads() {
  try {
    const r = await getCsThreads()
    if (r.code === 200) threads.value = r.data || []
  } catch (e) {
    appStore.addToast('投诉/工单会话加载失败', 'error')
  }
}

async function reloadAll() {
  await Promise.all([loadConversations(), loadThreads()])
}

async function openConversation(userId) {
  activeThreadKey.value = null
  threadContext.value = {}
  activeUserId.value = userId
  csChatStore.setActive('conv', userId)
  const conv = conversations.value.find(c => c.other_user_id_wsh === userId)
  activeUserName.value = conv?.other_user_name_wsh || `用户#${userId}`
  messages.value = []
  try {
    const r = await getConversations({ otherUserId: userId })
    if (r.code === 200) {
      messages.value = r.data || []
      const unread = messages.value.filter(m => m.to_user_id_wsh === currentUserId.value && !m.read_wsh)
      if (unread.length) {
        await markConversationRead({ other_user_id_wsh: userId })
      }
      const conv = conversations.value.find(c => c.other_user_id_wsh === userId)
      if (conv) conv.unread_count_wsh = 0
    }
  } catch (e) { /* ignore */ }
  await nextTick()
  scrollToBottom()
}

async function openThread(thread) {
  const key = `${thread.type_wsh}-${thread.biz_id_wsh}`
  activeThreadKey.value = key
  activeUserId.value = null
  csChatStore.setActive('thread', key)
  threadContext.value = { ...thread }
  threadMessages.value = []
  threadEvidence.value = null
  threadShowEvidence.value = false
  threadDraft.value = ''
  // 打开线程即标记已读（红点消除）
  const found = threads.value.find(t => t.type_wsh === thread.type_wsh && t.biz_id_wsh === thread.biz_id_wsh)
  if (found) found.unread_count_wsh = 0
  markCsThreadRead(thread.type_wsh, thread.biz_id_wsh).catch(() => {})
  try {
    if (thread.type_wsh === 'complaint') {
      const [mr, er] = await Promise.all([
        getComplaintMessages(thread.biz_id_wsh),
        getComplaintEvidence(thread.biz_id_wsh),
      ])
      threadMessages.value = (mr.data || []).map(m => ({
        id_wsh: m.id_wsh,
        fromUserId: m.from_user_id_wsh,
        fromName: m.from_user_name_wsh,
        content: m.content_wsh,
        fileUrl: m.file_url_wsh,
        time: m.created_at_wsh,
      }))
      threadEvidence.value = er.code === 200 ? er.data : null
    } else {
      const mr = await getTicketMessages(thread.biz_id_wsh)
      threadMessages.value = (mr.data || []).map(m => ({
        id_wsh: m.id_wsh,
        fromUserId: m.user_id_wsh,
        fromName: null,
        content: m.content_wsh,
        fileUrl: m.file_url_wsh,
        time: m.created_at_wsh,
      }))
    }
  } catch (e) { /* ignore */ }
  await nextTick()
  scrollToBottom()
}

async function handleThreadPhoto(event) {
  const files = Array.from(event.target.files || [])
  event.target.value = ''
  if (!files.length) return
  for (const file of files) {
    const fd = new FormData()
    fd.append('file', file)
    try {
      const r = await uploadFileToDirectory('chat', fd)
      if (r.code === 200 && r.data?.url_wsh) {
        threadPhotoUrls.value.push(r.data.url_wsh)
      } else {
        appStore.addToast(r.message || `照片 ${file.name} 上传失败`, 'error')
      }
    } catch (e) {
      appStore.addToast(e?.response?.data?.message || `照片 ${file.name} 上传失败`, 'error')
    }
  }
  if (threadPhotoUrls.value.length) {
    appStore.addToast(`已选择 ${threadPhotoUrls.value.length} 张照片，点击发送`, 'success')
  }
}

function removeThreadPhoto(index) {
  threadPhotoUrls.value.splice(index, 1)
}

async function sendThreadMessage() {
  if (!activeThreadKey.value || threadSending.value) return
  if (!threadDraft.value.trim() && !threadPhotoUrls.value.length) return
  const type = threadContext.value.type_wsh
  const bizId = threadContext.value.biz_id_wsh
  threadSending.value = true
  const content = threadDraft.value.trim()
  const urls = [...threadPhotoUrls.value]
  let lastOk = null
  try {
    // 每张照片发送一条图片留言
    for (const url of urls) {
      let r
      if (type === 'complaint') {
        r = await sendComplaintMessage(bizId, '', url)
      } else {
        r = await sendTicketMessage(bizId, { content_wsh: '', file_url_wsh: url })
      }
      if (r && r.code === 200) {
        threadMessages.value.push({
          id_wsh: r.data?.id_wsh || Date.now(),
          fromUserId: currentUserId.value,
          fromName: authStore.user?.nickname_wsh,
          content: r.data?.content_wsh || '',
          fileUrl: r.data?.file_url_wsh || url,
          time: r.data?.created_at_wsh,
        })
        lastOk = r
      } else {
        appStore.addToast(r?.message || '照片发送失败', 'error')
      }
    }
    // 文字作为最后一条留言发送
    if (content) {
      let r
      if (type === 'complaint') {
        r = await sendComplaintMessage(bizId, content, null)
      } else {
        r = await sendTicketMessage(bizId, { content_wsh: content, file_url_wsh: null })
      }
      if (r && r.code === 200) {
        threadMessages.value.push({
          id_wsh: r.data?.id_wsh || Date.now(),
          fromUserId: currentUserId.value,
          fromName: authStore.user?.nickname_wsh,
          content: r.data?.content_wsh || content,
          fileUrl: r.data?.file_url_wsh || null,
          time: r.data?.created_at_wsh,
        })
        lastOk = r
      } else {
        appStore.addToast(r?.message || '发送失败', 'error')
      }
    }
    if (lastOk) {
      threadDraft.value = ''
      threadPhotoUrls.value = []
      const idx = threads.value.findIndex(t => t.type_wsh === type && t.biz_id_wsh === bizId)
      if (idx >= 0) {
        threads.value[idx].last_message_wsh = threadMessages.value[threadMessages.value.length - 1].content || '[图片]'
        threads.value[idx].last_time_wsh = new Date().toISOString()
        const t = threads.value.splice(idx, 1)[0]
        threads.value.unshift(t)
      }
      await nextTick()
      scrollToBottom()
    }
  } catch (e) {
    appStore.addToast(e?.response?.data?.message || '发送失败', 'error')
  } finally {
    threadSending.value = false
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
  if (idx >= 0) {
    const conv = conversations.value[idx]
    conv.last_message_wsh = sent.content_wsh
    conv.last_time_wsh = sent.created_at_wsh
    conversations.value.splice(idx, 1)
    conversations.value.unshift(conv)
  }
}

function formatTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString()
}

function parseImages(value) {
  if (!value) return []
  return String(value).split(',').map(item => item.trim()).filter(Boolean)
}

function isImageUrl(value) {
  if (!value) return false
  return /\.(jpe?g|png|gif|webp|bmp|svg|avif)(\?.*)?$/i.test(String(value))
}

// 订阅全局客服消息中心（SSE 由 csChat store 统一维护，任意页面都能收到提醒）
function connectSse() {
  if (!token.value) return
  // 进入聊天页：重置全局未读角标，避免与页面内会话未读重复
  csChatStore.resetUnread()
  unsubscribeChat = csChatStore.on('chat-message', (msg) => {
    try {
      if (!msg || msg.from_user_id_wsh === currentUserId.value) return
      const conv = conversations.value.find(c => c.other_user_id_wsh === msg.from_user_id_wsh)
      if (conv && msg.to_user_id_wsh === currentUserId.value) {
        if (activeUserId.value !== msg.from_user_id_wsh) {
          conv.unread_count_wsh = (conv.unread_count_wsh || 0) + 1
        }
        conv.last_message_wsh = msg.content_wsh
        conv.last_time_wsh = msg.created_at_wsh
        conversations.value = [conv, ...conversations.value.filter(c => c.other_user_id_wsh !== conv.other_user_id_wsh)]
      } else if (conv) {
        conv.last_message_wsh = msg.content_wsh
        conv.last_time_wsh = msg.created_at_wsh
      }
      if (activeUserId.value === msg.from_user_id_wsh && msg.to_user_id_wsh === currentUserId.value) {
        messages.value.push(msg)
        markConversationRead({ other_user_id_wsh: msg.from_user_id_wsh }).catch(() => {})
        nextTick(scrollToBottom)
      }
    } catch (e) { /* ignore malformed event */ }
  })
  unsubscribeThread = csChatStore.on('thread-message', (data) => {
    try {
      const key = `${data.type}-${data.biz_id_wsh}`
      const activeKey = activeThreadKey.value
      if (key === activeKey) {
        // 当前打开的线程：直接追加消息并标记已读
        const fromUserId = data.from_user_id_wsh
        if (fromUserId !== currentUserId.value) {
          threadMessages.value.push({
            id_wsh: Date.now(),
            fromUserId,
            fromName: threadContext.value.other_user_name_wsh || `用户#${fromUserId}`,
            content: data.content_wsh,
            fileUrl: data.file_url_wsh,
            time: data.created_at_wsh,
          })
          markCsThreadRead(data.type, data.biz_id_wsh).catch(() => {})
          nextTick(scrollToBottom)
        }
      } else {
        const thread = threads.value.find(t => `${t.type_wsh}-${t.biz_id_wsh}` === key)
        if (thread) {
          if (data.from_user_id_wsh !== currentUserId.value) {
            thread.unread_count_wsh = (thread.unread_count_wsh || 0) + 1
          }
          thread.last_message_wsh = data.content_wsh
          thread.last_time_wsh = data.created_at_wsh
          const idx = threads.value.indexOf(thread)
          if (idx > 0) {
            threads.value.splice(idx, 1)
            threads.value.unshift(thread)
          }
        } else {
          // 新线程（如刚创建的投诉）出现在列表
          loadThreads()
        }
      }
    } catch (e) { /* ignore malformed event */ }
  })
  csChatStore.connect()
}

function openThreadFromRoute() {
  const type = route.query.bizType
  const bizId = route.query.bizId ? Number(route.query.bizId) : null
  if (type && bizId) {
    const found = threads.value.find(t => t.type_wsh === type && t.biz_id_wsh === bizId)
    if (found) {
      openThread(found)
    } else {
      // 线程列表没有（如刚创建的投诉），用路由上下文打开
      openThread({
        type_wsh: type,
        biz_id_wsh: bizId,
        title_wsh: route.query.title || '',
        other_user_id_wsh: route.query.userId ? Number(route.query.userId) : null,
        other_user_name_wsh: route.query.name || '用户',
        status_wsh: route.query.status || '',
        order_no_wsh: route.query.orderNo || '',
      })
    }
    return true
  }
  return false
}

onMounted(async () => {
  await Promise.all([loadConversations(), loadThreads()])
  connectSse()
  if (openThreadFromRoute()) return
  if (route.query.userId) {
    const userId = Number(route.query.userId)
    const conv = conversations.value.find(c => c.other_user_id_wsh === userId)
    if (conv) {
      await openConversation(userId)
    } else {
      activeUserId.value = userId
      activeUserName.value = route.query.name || `用户#${userId}`
      await openConversation(userId)
    }
  }
})

onBeforeUnmount(() => {
  if (unsubscribeChat) unsubscribeChat()
  if (unsubscribeThread) unsubscribeThread()
  csChatStore.clearActive()
  csChatStore.resetUnread()
  if (eventSource) eventSource.close()
  eventSource = null
})
</script>

<style scoped>
.cs-chat {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 16px;
  height: calc(100vh - 180px);
  min-height: 420px;
}
@media (max-width: 800px) {
  .cs-chat {
    grid-template-columns: 1fr;
    grid-template-rows: minmax(0, 30%) minmax(0, 1fr);
    height: calc(100vh - 200px);
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
.conv-section-title {
  margin: 14px 0 6px;
  font-size: 12px;
  text-transform: uppercase;
  color: var(--color-muted-foreground);
  letter-spacing: 0.04em;
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
.conv-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.conv-sub {
  font-size: 12px;
  color: var(--color-muted-foreground);
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
.chat-header,
.thread-header {
  flex: 0 0 auto;
}
.thread-evidence {
  flex: 0 0 auto;
}
.chat-header {
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-border);
}
.thread-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}
.thread-header__info {
  display: grid;
  gap: 6px;
  min-width: 0;
}
.thread-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--color-muted-foreground);
}
.thread-evidence {
  max-height: 260px;
  overflow-y: auto;
  border-bottom: 1px solid var(--color-border);
  padding: 12px 16px;
  background: var(--color-muted);
  display: grid;
  gap: 12px;
}
.evidence-section h4 {
  margin: 0 0 8px;
  font-size: 13px;
}
.evidence-item {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  background: var(--color-card);
  margin-bottom: 8px;
}
.evidence-item p {
  margin: 4px 0;
}
.item-meta {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 12px;
  color: var(--color-muted-foreground);
}
.evidence-photos {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.evidence-photo {
  display: inline-block;
  line-height: 0;
}
.evidence-photo img {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  cursor: zoom-in;
}
.evidence-photo span {
  line-height: normal;
  color: var(--color-primary);
  font-size: 13px;
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
