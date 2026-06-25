<template>
  <div>
    <PageHero title="AI 智能体" subtitle="智能任务执行代理" />
    <div class="chat-container" style="height:600px">
      <div class="chat-conversation">
        <div class="chat-messages">
          <div v-for="msg in messages" :key="msg.id" style="margin-bottom:16px">
            <div :style="{ textAlign: msg.role === 'user' ? 'right' : 'left' }">
              <span class="card" :style="{ display:'inline-block', padding:'8px 16px', borderRadius:'12px',
                background: msg.role === 'user' ? 'var(--color-primary)' : 'var(--color-muted)',
                color: msg.role === 'user' ? 'var(--color-on-primary)' : 'var(--color-foreground)' }">
                {{ msg.content }}
              </span>
            </div>
            <div v-if="msg.steps" style="margin-top:8px;margin-left:16px">
              <div v-for="(step, i) in msg.steps" :key="i" style="font-size:13px;color:var(--color-muted-foreground);margin-bottom:4px">
                {{ i + 1 }}. {{ step }}
              </div>
            </div>
          </div>
          <div v-if="loading" class="loading">智能体执行中...</div>
        </div>
        <div class="chat-input-area">
          <input v-model="input" @keyup.enter="executeTask" placeholder="描述您想让智能体执行的任务..." style="flex:1">
          <button class="btn btn-primary btn-sm" @click="executeTask">执行</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import PageHero from '@/components/common/PageHero.vue'

const authStore = useAuthStore()
const messages = ref([{ id: 0, role: 'ai', content: '您好！我是您的智能助理，可以帮您执行各种任务。', steps: [] }])
const input = ref('')
const loading = ref(false)

async function executeTask() {
  if (!input.value.trim()) return
  const task = input.value
  messages.value.push({ id: Date.now(), role: 'user', content: task, steps: [] })
  input.value = ''
  loading.value = true
  try {
    const r = await authStore.apiPost('/api/agent/execute', { input_wsh: task })
    if (r.code === 200) {
      messages.value.push({ id: Date.now(), role: 'ai', content: r.data.result_wsh, steps: r.data.steps || [] })
    }
  } catch (e) {
    messages.value.push({ id: Date.now(), role: 'ai', content: '任务执行失败，请稍后重试。', steps: [] })
  }
  loading.value = false
}
</script>
