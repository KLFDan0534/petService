<template>
  <div :class="['chat-bubble-wrapper', isMe ? 'is-me' : '']">
    <div class="bubble-avatar" :style="avatarStyle">
      <span v-if="!avatar">{{ name?.charAt(0) || '?' }}</span>
    </div>
    <div class="bubble-body">
      <div class="bubble-name">{{ name || (isMe ? '我' : '对方') }}</div>
      <div class="bubble-content">
        <img v-if="fileUrl" :src="fileUrl" class="bubble-image" alt="聊天图片">
        <p v-if="content" class="bubble-text">{{ content }}</p>
      </div>
      <div class="bubble-time">{{ formatTime(createdAt) }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  isMe: { type: Boolean, default: false },
  name: { type: String, default: '' },
  avatar: { type: String, default: '' },
  content: { type: String, default: '' },
  fileUrl: { type: String, default: '' },
  createdAt: { type: [String, Number, Date], default: '' },
})

const avatarStyle = computed(() => props.avatar ? { backgroundImage: `url(${props.avatar})` } : {})

function formatTime(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  const pad = number => String(number).padStart(2, '0')
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}
</script>

<style scoped>
.chat-bubble-wrapper {
  display: flex;
  gap: 10px;
  max-width: 80%;
  margin-bottom: 16px;
}
.chat-bubble-wrapper.is-me {
  flex-direction: row-reverse;
  align-self: flex-end;
}
.bubble-avatar {
  width: 36px;
  height: 36px;
  border-radius: 999px;
  background: var(--color-muted);
  background-size: cover;
  background-position: center;
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  margin-top: 4px;
  font-weight: 800;
}
.bubble-body { min-width: 0; }
.is-me .bubble-body { text-align: right; }
.bubble-name {
  font-size: 11px;
  color: var(--color-muted-foreground);
  margin-bottom: 4px;
}
.bubble-content {
  display: inline-block;
  padding: 10px 14px;
  border-radius: var(--radius-md);
  background: var(--color-muted);
  text-align: left;
  max-width: 100%;
}
.is-me .bubble-content {
  background: var(--color-primary);
  color: var(--color-on-primary);
}
.bubble-text {
  margin: 0;
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}
.bubble-image {
  max-width: 220px;
  max-height: 220px;
  border-radius: var(--radius-md);
  display: block;
}
.bubble-time {
  font-size: 11px;
  color: var(--color-muted-foreground);
  margin-top: 4px;
}
</style>
