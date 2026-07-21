<template>
  <section class="chat-card card">
    <div ref="messageContainer" class="chat-messages">
      <div v-if="loading" class="loading compact-loading">加载中...</div>
      <div v-else-if="messages.length === 0" class="empty-state compact-empty">
        <h3>暂无聊天记录</h3>
        <p>可以在这里和商家或看护人沟通订单细节。</p>
      </div>
      <template v-else>
        <ChatBubble
          v-for="message in messages"
          :key="message.id_wsh"
          :content="message.content_wsh"
          :file-url="message.file_url_wsh"
          :created-at="message.created_at_wsh"
          :is-me="isCurrentUser(message.from_user_id_wsh)"
          :name="message.from_user_name_wsh"
          :avatar="message.from_user_avatar_wsh"
        />
      </template>
    </div>

    <div class="chat-input-area">
      <div v-if="visibleError" class="chat-error">{{ visibleError }}</div>
      <textarea
        v-model="inputText"
        rows="3"
        placeholder="输入消息..."
        class="form-control chat-input"
        :disabled="sending"
        @keydown.enter.exact.prevent="handleSend"
      ></textarea>
      <div class="chat-actions">
        <label class="btn btn-outline btn-sm upload-btn" :class="{ disabled: sending }">
          选择图片
          <input
            ref="fileInput"
            type="file"
            accept="image/*"
            hidden
            :disabled="sending"
            @change="handleFileChange"
          >
        </label>
        <span v-if="selectedFile" class="file-name">{{ selectedFile.name }}</span>
        <button
          v-if="selectedFile"
          class="btn btn-outline btn-sm remove-file"
          type="button"
          :disabled="sending"
          aria-label="移除已选图片"
          @click="clearSelectedFile"
        >
          ×
        </button>
        <button class="btn btn-primary btn-sm send-btn" type="button" :disabled="!canSend" @click="handleSend">
          {{ sending ? '发送中...' : '发送' }}
        </button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import ChatBubble from './ChatBubble.vue'

const props = defineProps({
  messages: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  sending: { type: Boolean, default: false },
  sendError: { type: String, default: '' },
  orderId: { type: Number, required: true },
})

const emit = defineEmits(['send', 'refresh'])
const authStore = useAuthStore()
const inputText = ref('')
const selectedFile = ref(null)
const fileInput = ref(null)
const localError = ref('')
const messageContainer = ref(null)

const visibleError = computed(() => props.sendError || localError.value)
const canSend = computed(() => {
  return !props.sending && (inputText.value.trim().length > 0 || selectedFile.value)
})

function isCurrentUser(fromUserId) {
  return Number(fromUserId) === Number(authStore.user?.id_wsh)
}

function handleFileChange(event) {
  localError.value = ''
  const file = event.target.files?.[0]
  if (!file) return
  if (!file.type || !file.type.toLowerCase().startsWith('image/')) {
    localError.value = '请选择图片文件'
    event.target.value = ''
    return
  }
  selectedFile.value = file
  event.target.value = ''
}

function clearSelectedFile() {
  selectedFile.value = null
  if (fileInput.value) fileInput.value.value = ''
}

function clearDraft() {
  inputText.value = ''
  clearSelectedFile()
  localError.value = ''
}

function handleSend() {
  if (!canSend.value) return
  localError.value = ''
  emit('send', {
    content: inputText.value,
    file: selectedFile.value,
    done(success) {
      if (success) clearDraft()
    },
  })
}

function scrollToBottom() {
  nextTick(() => {
    if (messageContainer.value) {
      messageContainer.value.scrollTop = messageContainer.value.scrollHeight
    }
  })
}

watch(() => props.messages.length, scrollToBottom, { flush: 'post' })
watch(() => props.orderId, clearDraft)
</script>

<style scoped>
.card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
}
.chat-card {
  overflow: hidden;
}
.chat-messages {
  padding: 18px;
  min-height: 380px;
  max-height: 520px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  background: var(--color-card);
}
.compact-loading,
.compact-empty {
  padding: 36px 20px;
}
.chat-input-area {
  border-top: 1px solid var(--color-border);
  padding: 14px;
  background: var(--color-muted);
}
.chat-error {
  margin-bottom: 10px;
  padding: 8px 10px;
  border-radius: var(--radius-sm);
  background: rgba(239, 68, 68, .1);
  color: var(--color-danger, #b91c1c);
  font-size: 13px;
}
.chat-input {
  resize: vertical;
  min-height: 86px;
  background: var(--color-card);
}
.chat-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  min-width: 0;
}
.upload-btn {
  cursor: pointer;
}
.upload-btn.disabled {
  pointer-events: none;
  opacity: .65;
}
.file-name {
  color: var(--color-muted-foreground);
  font-size: 12px;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.remove-file {
  width: 30px;
  padding-left: 0;
  padding-right: 0;
  flex: 0 0 auto;
}
.send-btn {
  margin-left: auto;
}
</style>
